package com.aira.health.data.repository

import android.net.Uri
import com.aira.health.BuildConfig
import com.aira.health.data.local.dao.WorkoutSessionDao
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.data.model.ConfidenceRouter
import com.aira.health.data.remote.strava.StravaActivityDto
import com.aira.health.data.remote.strava.StravaApiClient
import com.aira.health.data.remote.strava.StravaApiException
import com.aira.health.data.strava.StravaConnectionStore
import com.aira.health.data.strava.StravaSyncCursor
import com.aira.health.data.strava.StravaTokenSession
import com.aira.health.data.strava.StravaTokenStore
import com.aira.health.domain.model.StravaConnectionState
import com.aira.health.domain.model.StravaSyncSummary
import com.aira.health.domain.repository.StravaRepository
import java.time.Instant
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.flow.Flow

@Singleton
class StravaRepositoryImpl @Inject constructor(
    private val stravaApiClient: StravaApiClient,
    private val tokenStore: StravaTokenStore,
    private val connectionStore: StravaConnectionStore,
    private val workoutSessionDao: WorkoutSessionDao
) : StravaRepository {

    private companion object {
        const val AUTH_BASE_URL = "https://www.strava.com/oauth/mobile/authorize"
        const val STRAVA_SOURCE_PACKAGE = "com.strava"
        const val STRAVA_SCOPE = "activity:read,activity:read_all"
        const val PER_PAGE = 50
        const val TOKEN_EXPIRY_SKEW_SECONDS = 90L
        const val INCREMENTAL_OVERLAP_MS = 60L * 60L * 1000L
        const val DEFAULT_INCREMENTAL_LOOKBACK_MS = 14L * 24L * 60L * 60L * 1000L
    }

    override fun observeConnectionState(): Flow<StravaConnectionState> {
        return connectionStore.observeConnectionState()
    }

    override suspend fun createAuthorizationUrl(): Result<String> = runCatching {
        ensureClientConfigured(requireSecret = false)

        val state = UUID.randomUUID().toString()
        connectionStore.setPendingOAuthState(state)

        Uri.parse(AUTH_BASE_URL).buildUpon()
            .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.STRAVA_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", STRAVA_SCOPE)
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    override suspend fun handleAuthorizationCallback(callbackUri: Uri): Result<Unit> = runCatching {
        ensureClientConfigured(requireSecret = true)

        val error = callbackUri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            throw IllegalStateException("Strava authorization failed: $error")
        }

        val pendingState = connectionStore.consumePendingOAuthState()
        val returnedState = callbackUri.getQueryParameter("state")
        if (pendingState.isNullOrBlank() || pendingState != returnedState) {
            throw IllegalStateException("Invalid Strava authorization state")
        }

        val code = callbackUri.getQueryParameter("code")
            ?: throw IllegalStateException("Strava did not return an authorization code")

        val tokenResponse = stravaApiClient.exchangeToken(
            clientId = BuildConfig.STRAVA_CLIENT_ID,
            clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
            code = code
        )

        if (!hasRequiredScopes(tokenResponse.scope)) {
            throw IllegalStateException(
                "Strava authorization missing required scope. Please allow activity permissions and retry."
            )
        }

        tokenStore.save(
            StravaTokenSession(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresAtEpochSec = tokenResponse.expiresAt,
                scope = tokenResponse.scope,
                athleteId = tokenResponse.athlete.id
            )
        )
        connectionStore.markConnected(tokenResponse.athlete.id)
    }

    override suspend fun disconnect(): Result<Unit> = runCatching {
        tokenStore.read()?.let { session ->
            // Best-effort revoke. Local disconnect should still succeed if network revoke fails.
            runCatching {
                stravaApiClient.deauthorize(session.accessToken)
            }
        }
        tokenStore.clear()
        connectionStore.markDisconnected()
    }

    override suspend fun syncActivities(maxPagesPerRun: Int): Result<StravaSyncSummary> {
        return runCatching {
            ensureClientConfigured(requireSecret = true)
            val accessToken = getValidAccessToken()
            val cursor = connectionStore.getSyncCursor()

            val summary = if (cursor.backfillComplete) {
                syncIncremental(
                    accessToken = accessToken,
                    maxPagesPerRun = maxPagesPerRun,
                    cursor = cursor
                )
            } else {
                syncBackfill(
                    accessToken = accessToken,
                    maxPagesPerRun = maxPagesPerRun,
                    cursor = cursor
                )
            }

            connectionStore.updateLastSync(System.currentTimeMillis())
            summary
        }.onFailure { throwable ->
            if (throwable is StravaApiException && throwable.isAuthFailure) {
                tokenStore.clear()
                connectionStore.markReconnectRequired()
            }
        }
    }

    private suspend fun syncBackfill(
        accessToken: String,
        maxPagesPerRun: Int,
        cursor: StravaSyncCursor
    ): StravaSyncSummary {
        var pagesFetched = 0
        var insertedCount = 0
        var skippedCount = 0
        var nextBeforeEpochMs = cursor.backfillBeforeEpochMs
        var newestEpochMs = cursor.incrementalAfterEpochMs ?: 0L

        repeat(maxPagesPerRun) {
            val activities = stravaApiClient.getActivities(
                accessToken = accessToken,
                page = 1,
                perPage = PER_PAGE,
                afterEpochSeconds = null,
                beforeEpochSeconds = nextBeforeEpochMs?.div(1000)
            )

            if (activities.isEmpty()) {
                val incrementalStart = if (newestEpochMs > 0L) newestEpochMs else System.currentTimeMillis()
                connectionStore.markBackfillComplete(incrementalStart)
                return StravaSyncSummary(
                    insertedCount = insertedCount,
                    skippedCount = skippedCount,
                    pagesFetched = pagesFetched,
                    backfillComplete = true
                )
            }

            val outcome = ingestActivities(activities)
            pagesFetched += 1
            insertedCount += outcome.insertedCount
            skippedCount += outcome.skippedCount
            newestEpochMs = max(newestEpochMs, outcome.newestStartEpochMs ?: 0L)

            nextBeforeEpochMs = (outcome.oldestStartEpochMs ?: nextBeforeEpochMs ?: 0L)
                .takeIf { it > 0L }
                ?.minus(1000L)
            connectionStore.setBackfillProgress(nextBeforeEpochMs)

            if (activities.size < PER_PAGE) {
                val incrementalStart = if (newestEpochMs > 0L) newestEpochMs else System.currentTimeMillis()
                connectionStore.markBackfillComplete(incrementalStart)
                return StravaSyncSummary(
                    insertedCount = insertedCount,
                    skippedCount = skippedCount,
                    pagesFetched = pagesFetched,
                    backfillComplete = true
                )
            }
        }

        return StravaSyncSummary(
            insertedCount = insertedCount,
            skippedCount = skippedCount,
            pagesFetched = pagesFetched,
            backfillComplete = false
        )
    }

    private suspend fun syncIncremental(
        accessToken: String,
        maxPagesPerRun: Int,
        cursor: StravaSyncCursor
    ): StravaSyncSummary {
        val baselineAfterEpochMs = cursor.incrementalAfterEpochMs
            ?: (System.currentTimeMillis() - DEFAULT_INCREMENTAL_LOOKBACK_MS)
        val incrementalAfterEpochSec =
            max(0L, (baselineAfterEpochMs - INCREMENTAL_OVERLAP_MS) / 1000L)

        var pagesFetched = 0
        var insertedCount = 0
        var skippedCount = 0
        var newestEpochMs = baselineAfterEpochMs

        for (page in 1..maxPagesPerRun) {
            val activities = stravaApiClient.getActivities(
                accessToken = accessToken,
                page = page,
                perPage = PER_PAGE,
                afterEpochSeconds = incrementalAfterEpochSec,
                beforeEpochSeconds = null
            )
            if (activities.isEmpty()) {
                break
            }

            val outcome = ingestActivities(activities)
            pagesFetched += 1
            insertedCount += outcome.insertedCount
            skippedCount += outcome.skippedCount
            newestEpochMs = max(newestEpochMs, outcome.newestStartEpochMs ?: newestEpochMs)

            if (activities.size < PER_PAGE) {
                break
            }
        }

        if (newestEpochMs > 0L) {
            connectionStore.updateIncrementalCursor(newestEpochMs)
        }

        return StravaSyncSummary(
            insertedCount = insertedCount,
            skippedCount = skippedCount,
            pagesFetched = pagesFetched,
            backfillComplete = true
        )
    }

    private suspend fun ingestActivities(activities: List<StravaActivityDto>): IngestOutcome {
        var insertedCount = 0
        var skippedCount = 0
        var newestStartEpochMs: Long? = null
        var oldestStartEpochMs: Long? = null

        activities.forEach { activity ->
            val startEpochMs = runCatching { Instant.parse(activity.startDate).toEpochMilli() }
                .getOrNull()
            if (startEpochMs == null) {
                skippedCount += 1
                return@forEach
            }

            newestStartEpochMs = max(newestStartEpochMs ?: 0L, startEpochMs)
            oldestStartEpochMs = minOf(oldestStartEpochMs ?: startEpochMs, startEpochMs)

            val durationSec = max(activity.movingTimeSec ?: 0, activity.elapsedTimeSec ?: 0)
            val safeDurationSec = max(durationSec, 60)
            val durationMin = max(1, safeDurationSec / 60)

            val calculatedCalories = activity.calories?.toInt()
                ?: activity.kiloJoules?.times(0.239_005_74f)?.toInt()
                ?: 0

            val rowId = workoutSessionDao.insertOrIgnore(
                WorkoutSession(
                    startTime = startEpochMs,
                    endTime = startEpochMs + safeDurationSec * 1000L,
                    exerciseType = (activity.sportType ?: activity.type ?: "WORKOUT")
                        .uppercase(Locale.US),
                    durationMin = durationMin,
                    activeCalories = max(0, calculatedCalories),
                    avgHr = max(0, (activity.averageHeartRate ?: 0f).toInt()),
                    maxHr = max(0, (activity.maxHeartRate ?: 0f).toInt()),
                    sourcePackage = STRAVA_SOURCE_PACKAGE,
                    externalId = activity.id.toString(),
                    confidence = ConfidenceRouter.getConfidenceFloat(STRAVA_SOURCE_PACKAGE)
                )
            )

            if (rowId == -1L) {
                skippedCount += 1
            } else {
                insertedCount += 1
            }
        }

        return IngestOutcome(
            insertedCount = insertedCount,
            skippedCount = skippedCount,
            newestStartEpochMs = newestStartEpochMs,
            oldestStartEpochMs = oldestStartEpochMs
        )
    }

    private suspend fun getValidAccessToken(): String {
        val current = tokenStore.read() ?: throw IllegalStateException("Strava is not connected")
        val nowEpochSec = Instant.now().epochSecond

        if (current.expiresAtEpochSec - nowEpochSec > TOKEN_EXPIRY_SKEW_SECONDS) {
            return current.accessToken
        }

        return try {
            val refreshed = stravaApiClient.refreshToken(
                clientId = BuildConfig.STRAVA_CLIENT_ID,
                clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
                refreshToken = current.refreshToken
            )
            tokenStore.save(
                StravaTokenSession(
                    accessToken = refreshed.accessToken,
                    refreshToken = refreshed.refreshToken,
                    expiresAtEpochSec = refreshed.expiresAt,
                    scope = refreshed.scope,
                    athleteId = refreshed.athlete.id
                )
            )
            refreshed.accessToken
        } catch (exception: StravaApiException) {
            if (exception.isAuthFailure) {
                tokenStore.clear()
                connectionStore.markReconnectRequired()
            }
            throw exception
        }
    }

    private fun ensureClientConfigured(requireSecret: Boolean) {
        if (BuildConfig.STRAVA_CLIENT_ID.isBlank()) {
            throw IllegalStateException("Strava client id is not configured")
        }
        if (BuildConfig.STRAVA_REDIRECT_URI.isBlank()) {
            throw IllegalStateException("Strava redirect uri is not configured")
        }
        if (requireSecret && BuildConfig.STRAVA_CLIENT_SECRET.isBlank()) {
            throw IllegalStateException("Strava client secret is not configured")
        }
    }

    private fun hasRequiredScopes(scope: String): Boolean {
        val granted = scope
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        return granted.contains("activity:read") || granted.contains("activity:read_all")
    }

    private data class IngestOutcome(
        val insertedCount: Int,
        val skippedCount: Int,
        val newestStartEpochMs: Long?,
        val oldestStartEpochMs: Long?
    )
}
