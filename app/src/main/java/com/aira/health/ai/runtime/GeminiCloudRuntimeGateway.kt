package com.aira.health.ai.runtime

import android.os.SystemClock
import android.util.Log
import com.aira.health.BuildConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete [AiRuntimeGateway] backed by direct Gemini REST API calls using Ktor 3.
 *
 * This implementation resolves the Ktor 2 vs Ktor 3 dependency conflict by bypassing
 * the official Google SDK and using the project's native networking stack.
 */
@Singleton
class GeminiCloudRuntimeGateway @Inject constructor(
    private val config: RuntimeConfig,
) : AiRuntimeGateway {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMillis
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = config.timeoutMillis
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("GeminiGatewayHttp", message.redactSensitiveTokens())
                }
            }
        }
        install(SSE)
    }

    override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> =
        channelFlow {
            val startMs = SystemClock.elapsedRealtime()
            val fullPrompt = request.promptChunks.joinToString("\n")
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (apiKey.isBlank()) {
                throw AiRuntimeException(
                    RuntimeFailureReason.MODEL_UNAVAILABLE,
                    "Gemini API key is unavailable for the active build variant"
                )
            }

            val geminiRequest = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = fullPrompt)))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = config.temperature,
                    topK = config.topK,
                    topP = config.topP,
                    maxOutputTokens = config.maxTokens
                )
            )

            try {
                Log.i("GeminiGateway", "Starting SSE request to Gemini stream endpoint")

                client.sse(
                    request = {
                        method = HttpMethod.Post
                        url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent")
                        parameter("alt", "sse")
                        header("x-goog-api-key", apiKey)
                        contentType(ContentType.Application.Json)
                        setBody(geminiRequest)
                    }
                ) {
                    this.incoming.collect { event ->
                        val data = event.data ?: return@collect
                        Log.v("GeminiGateway", "Received SSE chunk (${data.length} chars)")
                        
                        try {
                            val response = json.decodeFromString<GeminiResponse>(data)
                            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

                            if (text.isNotEmpty()) {
                                trySend(AiRuntimeResponse(text = text, isDone = false))
                            }
                        } catch (e: Exception) {
                            Log.w("GeminiGateway", "Failed to parse chunk: ${e.message}")
                        }
                    }
                }

                val latency = SystemClock.elapsedRealtime() - startMs
                Log.i("GeminiGateway", "SSE stream completed in ${latency}ms")
                trySend(AiRuntimeResponse(text = "", isDone = true, latencyMs = latency))

            } catch (e: Exception) {
                val reason = when (e) {
                    is TimeoutCancellationException,
                    is HttpRequestTimeoutException -> RuntimeFailureReason.TIMEOUT
                    is CancellationException       -> RuntimeFailureReason.CANCELLED
                    else                           -> RuntimeFailureReason.INTERNAL_ERROR
                }

                when (reason) {
                    RuntimeFailureReason.CANCELLED -> Log.i("GeminiGateway", "SSE stream cancelled by caller")
                    RuntimeFailureReason.TIMEOUT   -> Log.w("GeminiGateway", "SSE stream timed out")
                    else                           -> Log.e("GeminiGateway", "SSE stream error: ${e.message}", e)
                }

                val message = when (reason) {
                    RuntimeFailureReason.CANCELLED -> "Generation cancelled"
                    RuntimeFailureReason.TIMEOUT   -> "Generation timed out"
                    RuntimeFailureReason.MODEL_UNAVAILABLE -> "Model unavailable"
                    RuntimeFailureReason.INTERNAL_ERROR -> "Gemini REST error: ${e.message}"
                }
                throw AiRuntimeException(reason, message, e)
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun close() {
        client.close()
    }
}

private fun String.redactSensitiveTokens(): String =
    this
        .replace(Regex("([?&]key=)[^&\\s]+", RegexOption.IGNORE_CASE), "$1REDACTED")
        .replace(Regex("(x-goog-api-key:)\\s*[^\\s]+", RegexOption.IGNORE_CASE), "$1 REDACTED")
