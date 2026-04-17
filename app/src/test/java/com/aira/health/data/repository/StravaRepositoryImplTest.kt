package com.aira.health.data.repository

import com.aira.health.data.local.dao.StravaActivityRawDao
import com.aira.health.data.local.dao.WorkoutSessionDao
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.data.remote.strava.StravaActivitiesPage
import com.aira.health.data.remote.strava.StravaActivityDto
import com.aira.health.data.remote.strava.StravaApiClient
import com.aira.health.data.remote.strava.StravaApiException
import com.aira.health.data.remote.strava.StravaAthleteDto
import com.aira.health.data.remote.strava.StravaTokenResponse
import com.aira.health.data.strava.StravaConnectionStore
import com.aira.health.data.strava.StravaSyncCursor
import com.aira.health.data.strava.StravaTokenSession
import com.aira.health.data.strava.StravaTokenStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StravaRepositoryImplTest {

    private lateinit var apiClient: StravaApiClient
    private lateinit var tokenStore: StravaTokenStore
    private lateinit var connectionStore: StravaConnectionStore
    private lateinit var workoutSessionDao: WorkoutSessionDao
    private lateinit var stravaActivityRawDao: StravaActivityRawDao
    private lateinit var repository: StravaRepositoryImpl

    @Before
    fun setUp() {
        apiClient = mockk(relaxed = true)
        tokenStore = mockk(relaxed = true)
        connectionStore = mockk(relaxed = true)
        workoutSessionDao = mockk(relaxed = true)
        stravaActivityRawDao = mockk(relaxed = true)

        repository = StravaRepositoryImpl(
            stravaApiClient = apiClient,
            tokenStore = tokenStore,
            connectionStore = connectionStore,
            workoutSessionDao = workoutSessionDao,
            stravaActivityRawDao = stravaActivityRawDao
        )
    }

    @Test
    fun `syncActivities returns throttled summary when deferred window is active`() = runTest {
        val deferredUntil = System.currentTimeMillis() + 60_000L
        coEvery { connectionStore.getSyncDeferredUntilEpochMs() } returns deferredUntil
        coEvery { connectionStore.getSyncCursor() } returns StravaSyncCursor(
            backfillComplete = false,
            backfillBeforeEpochMs = null,
            incrementalAfterEpochMs = null
        )

        val summary = repository.syncActivities(maxPagesPerRun = 1).getOrThrow()

        assertTrue(summary.throttled)
        assertEquals(deferredUntil, summary.deferredUntilEpochMs)
        assertEquals(0, summary.pagesFetched)
        coVerify(exactly = 0) { apiClient.getActivitiesPage(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `syncActivities defers on Strava rate limit errors`() = runTest {
        coEvery { connectionStore.getSyncDeferredUntilEpochMs() } returns null
        coEvery { connectionStore.getSyncCursor() } returns StravaSyncCursor(
            backfillComplete = true,
            backfillBeforeEpochMs = null,
            incrementalAfterEpochMs = 0L
        )
        coEvery { tokenStore.read() } returns validToken(expiresAtEpochSec = Long.MAX_VALUE)
        coEvery {
            apiClient.getActivitiesPage(
                accessToken = any(),
                page = any(),
                perPage = any(),
                afterEpochSeconds = any(),
                beforeEpochSeconds = any()
            )
        } throws StravaApiException(
            statusCode = 429,
            message = "rate limit",
            retryAfterSeconds = 120L
        )

        val result = repository.syncActivities(maxPagesPerRun = 1)

        assertTrue(result.isFailure)
        coVerify {
            connectionStore.deferSyncUntil(
                deferredUntilEpochMs = match { it > System.currentTimeMillis() },
                errorCode = 429,
                errorMessage = any()
            )
        }
    }

    @Test
    fun `syncActivities keeps strava activity even when another source overlaps`() = runTest {
        coEvery { connectionStore.getSyncDeferredUntilEpochMs() } returns null
        coEvery { connectionStore.getSyncCursor() } returns StravaSyncCursor(
            backfillComplete = true,
            backfillBeforeEpochMs = null,
            incrementalAfterEpochMs = 0L
        )
        coEvery { tokenStore.read() } returns validToken(expiresAtEpochSec = Long.MAX_VALUE)
        coEvery {
            apiClient.getActivitiesPage(
                accessToken = any(),
                page = any(),
                perPage = any(),
                afterEpochSeconds = any(),
                beforeEpochSeconds = any()
            )
        } returns StravaActivitiesPage(
            activities = listOf(
                StravaActivityDto(
                    id = 12345L,
                    sportType = "Ride",
                    movingTimeSec = 3600,
                    elapsedTimeSec = 3600,
                    averageHeartRate = 132f,
                    maxHeartRate = 168f,
                    calories = 420f,
                    startDate = "2026-04-16T05:45:00Z"
                )
            )
        )
        coEvery { workoutSessionDao.insertOrIgnore(any()) } returns 11L

        val summary = repository.syncActivities(maxPagesPerRun = 1).getOrThrow()

        assertEquals(1, summary.insertedCount)
        assertEquals(0, summary.skippedCount)
        coVerify(exactly = 1) { workoutSessionDao.insertOrIgnore(any()) }
        coVerify(exactly = 1) { stravaActivityRawDao.upsert(any()) }
    }

    @Test
    fun `syncActivities refreshes expired token before fetching activities`() = runTest {
        coEvery { connectionStore.getSyncDeferredUntilEpochMs() } returns null
        coEvery { connectionStore.getSyncCursor() } returns StravaSyncCursor(
            backfillComplete = true,
            backfillBeforeEpochMs = null,
            incrementalAfterEpochMs = 0L
        )
        coEvery { tokenStore.read() } returns validToken(expiresAtEpochSec = 1L)
        coEvery {
            apiClient.refreshToken(
                clientId = any(),
                clientSecret = any(),
                refreshToken = "refresh-old"
            )
        } returns StravaTokenResponse(
            tokenType = "Bearer",
            accessToken = "fresh-access",
            refreshToken = "refresh-new",
            expiresAt = Long.MAX_VALUE,
            expiresIn = 3600,
            scope = "activity:read,activity:read_all",
            athlete = StravaAthleteDto(id = 1001L)
        )
        coEvery {
            apiClient.getActivitiesPage(
                accessToken = "fresh-access",
                page = any(),
                perPage = any(),
                afterEpochSeconds = any(),
                beforeEpochSeconds = any()
            )
        } returns StravaActivitiesPage(emptyList())

        val summary = repository.syncActivities(maxPagesPerRun = 1).getOrThrow()

        assertEquals(0, summary.insertedCount)
        coVerify { tokenStore.save(match { it.accessToken == "fresh-access" && it.refreshToken == "refresh-new" }) }
        coVerify { apiClient.getActivitiesPage(accessToken = "fresh-access", page = any(), perPage = any(), afterEpochSeconds = any(), beforeEpochSeconds = any()) }
    }

    private fun validToken(expiresAtEpochSec: Long): StravaTokenSession {
        return StravaTokenSession(
            accessToken = "access",
            refreshToken = "refresh-old",
            expiresAtEpochSec = expiresAtEpochSec,
            scope = "activity:read,activity:read_all",
            athleteId = 1001L
        )
    }
}
