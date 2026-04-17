package com.aira.health.data.remote.strava

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONArray

class StravaApiException(
    val statusCode: Int,
    message: String,
    val retryAfterSeconds: Long? = null,
    val endpointPath: String? = null,
    val rateLimits: StravaRateLimitInfo? = null
) : RuntimeException(message) {
    val isAuthFailure: Boolean
        get() = statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || statusCode == HttpURLConnection.HTTP_FORBIDDEN

    val isRateLimited: Boolean
        get() = statusCode == 429

    val isTransientServerFailure: Boolean
        get() = statusCode in 500..599
}

class StravaApiClient @Inject constructor() {

    private companion object {
        const val BASE_URL = "https://www.strava.com/api/v3"
        const val OAUTH_URL = "https://www.strava.com/oauth"
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun exchangeToken(
        clientId: String,
        clientSecret: String,
        code: String
    ): StravaTokenResponse {
        val body = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "code" to code,
            "grant_type" to "authorization_code"
        )
        return postToken(body)
    }

    suspend fun refreshToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String
    ): StravaTokenResponse {
        val body = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "refresh_token" to refreshToken,
            "grant_type" to "refresh_token"
        )
        return postToken(body)
    }

    suspend fun deauthorize(accessToken: String) {
        val body = mapOf(
            "access_token" to accessToken
        )
        postOAuthForm(body = body, endpointPath = "deauthorize")
    }

    suspend fun getActivitiesPage(
        accessToken: String,
        page: Int,
        perPage: Int,
        afterEpochSeconds: Long?,
        beforeEpochSeconds: Long?
    ): StravaActivitiesPage = withContext(Dispatchers.IO) {
        val query = buildString {
            append("per_page=").append(perPage)
            append("&page=").append(page)
            if (afterEpochSeconds != null) {
                append("&after=").append(afterEpochSeconds)
            }
            if (beforeEpochSeconds != null) {
                append("&before=").append(beforeEpochSeconds)
            }
        }
        val url = URL("$BASE_URL/athlete/activities?$query")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }

        val status = connection.responseCode
        val payload = readResponsePayload(connection)
        val rateLimits = parseRateLimitInfo(connection)
        val retryAfter = connection.getHeaderField("Retry-After")?.toLongOrNull()
        if (status in 200..299) {
            val activitiesJson = JSONArray(payload)
            val activities = buildList {
                for (index in 0 until activitiesJson.length()) {
                    val raw = activitiesJson.get(index).toString()
                    add(
                        json.decodeFromString<StravaActivityDto>(raw)
                            .copy(rawPayload = raw)
                    )
                }
            }
            return@withContext StravaActivitiesPage(
                activities = activities,
                rateLimits = rateLimits,
                retryAfterSeconds = retryAfter
            )
        }

        throw StravaApiException(
            statusCode = status,
            message = payload.ifBlank { "Strava activities request failed ($status)" },
            retryAfterSeconds = retryAfter,
            endpointPath = "/athlete/activities",
            rateLimits = rateLimits
        )
    }

    suspend fun getActivities(
        accessToken: String,
        page: Int,
        perPage: Int,
        afterEpochSeconds: Long?,
        beforeEpochSeconds: Long?
    ): List<StravaActivityDto> {
        return getActivitiesPage(
            accessToken = accessToken,
            page = page,
            perPage = perPage,
            afterEpochSeconds = afterEpochSeconds,
            beforeEpochSeconds = beforeEpochSeconds
        ).activities
    }

    private suspend fun postToken(body: Map<String, String>): StravaTokenResponse = withContext(Dispatchers.IO) {
        val payload = postOAuthForm(body = body, endpointPath = "token")
        return@withContext json.decodeFromString<StravaTokenResponse>(payload)
    }

    private suspend fun postOAuthForm(
        body: Map<String, String>,
        endpointPath: String
    ): String = withContext(Dispatchers.IO) {
        val url = URL("$OAUTH_URL/$endpointPath")
        val encodedBody = body.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }

        connection.outputStream.use { stream ->
            stream.write(encodedBody.toByteArray(Charsets.UTF_8))
        }

        val status = connection.responseCode
        val payload = readResponsePayload(connection)
        if (status in 200..299) {
            return@withContext payload
        }

        throw StravaApiException(
            statusCode = status,
            message = payload.ifBlank { "Strava oauth $endpointPath request failed ($status)" },
            retryAfterSeconds = connection.getHeaderField("Retry-After")?.toLongOrNull(),
            endpointPath = "/oauth/$endpointPath",
            rateLimits = parseRateLimitInfo(connection)
        )
    }

    private fun parseRateLimitInfo(connection: HttpURLConnection): StravaRateLimitInfo? {
        val overall = parseRateLimitWindow(
            limitHeader = connection.getHeaderField("X-RateLimit-Limit"),
            usageHeader = connection.getHeaderField("X-RateLimit-Usage")
        )
        val read = parseRateLimitWindow(
            limitHeader = connection.getHeaderField("X-ReadRateLimit-Limit"),
            usageHeader = connection.getHeaderField("X-ReadRateLimit-Usage")
        )
        if (overall == null && read == null) {
            return null
        }
        return StravaRateLimitInfo(overall = overall, read = read)
    }

    private fun parseRateLimitWindow(
        limitHeader: String?,
        usageHeader: String?
    ): StravaRateLimitWindow? {
        val limitParts = limitHeader
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: return null
        val usageParts = usageHeader
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: return null
        if (limitParts.size < 2 || usageParts.size < 2) {
            return null
        }
        return StravaRateLimitWindow(
            shortWindowLimit = limitParts[0],
            dailyLimit = limitParts[1],
            shortWindowUsage = usageParts[0],
            dailyUsage = usageParts[1]
        )
    }

    private fun readResponsePayload(connection: HttpURLConnection): String {
        val stream = connection.errorStream ?: connection.inputStream
        if (stream == null) {
            return ""
        }
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            buildString {
                var line = reader.readLine()
                while (line != null) {
                    append(line)
                    line = reader.readLine()
                }
            }
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
