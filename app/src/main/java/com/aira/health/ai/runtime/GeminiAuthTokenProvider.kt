package com.aira.health.ai.runtime

import com.aira.health.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides short-lived Gemini bearer tokens from a trusted backend endpoint.
 *
 * Quota and abuse controls must be enforced server-side by the token endpoint.
 */
interface GeminiAuthTokenProvider {
    suspend fun getToken(): String
}

@Singleton
class BackendGeminiAuthTokenProvider @Inject constructor() : GeminiAuthTokenProvider {

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = TOKEN_REQUEST_TIMEOUT_MS
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = TOKEN_REQUEST_TIMEOUT_MS
        }
    }

    override suspend fun getToken(): String {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            throw IllegalStateException("Backend token endpoint base URL is unavailable")
        }

        val response = client.post("$baseUrl/functions/v1/gemini-ephemeral-token") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            }
            setBody("{}")
        }

        if (response.status.value !in 200..299) {
            throw IllegalStateException("Token endpoint failed with HTTP ${response.status.value}")
        }

        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val token = payload["token"]?.jsonPrimitive?.content
            ?: payload["access_token"]?.jsonPrimitive?.content
            ?: payload["accessToken"]?.jsonPrimitive?.content
            ?: ""

        if (token.isBlank()) {
            throw IllegalStateException("Token endpoint returned an empty token")
        }

        return token
    }

    private companion object {
        private const val TOKEN_REQUEST_TIMEOUT_MS = 10_000L
    }
}
