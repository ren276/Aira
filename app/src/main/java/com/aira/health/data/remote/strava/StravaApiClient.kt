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

class StravaApiException(
    val statusCode: Int,
    message: String,
    val retryAfterSeconds: Long? = null
) : RuntimeException(message) {
    val isAuthFailure: Boolean
        get() = statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || statusCode == HttpURLConnection.HTTP_FORBIDDEN

    val isRateLimited: Boolean
        get() = statusCode == 429
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

    suspend fun getActivities(
        accessToken: String,
        page: Int,
        perPage: Int,
        afterEpochSeconds: Long?,
        beforeEpochSeconds: Long?
    ): List<StravaActivityDto> = withContext(Dispatchers.IO) {
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
        if (status in 200..299) {
            return@withContext json.decodeFromString<List<StravaActivityDto>>(payload)
        }

        throw StravaApiException(
            statusCode = status,
            message = payload.ifBlank { "Strava activities request failed ($status)" },
            retryAfterSeconds = connection.getHeaderField("Retry-After")?.toLongOrNull()
        )
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
            retryAfterSeconds = connection.getHeaderField("Retry-After")?.toLongOrNull()
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
