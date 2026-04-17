package com.aira.health.ai.runtime

import android.util.Log
import com.aira.health.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.CancellationException
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            level = LogLevel.INFO
        }
        install(SSE)
    }

    override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> =
        callbackFlow {
            val startMs = System.currentTimeMillis()
            val fullPrompt = request.promptChunks.joinToString("\n")

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
                Log.i("GeminiGateway", "Starting SSE request to: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent")
                
                // Correct Ktor 3 SSE signature
                client.sse(
                    request = {
                        method = HttpMethod.Post
                        url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent")
                        parameter("alt", "sse")
                        parameter("key", BuildConfig.GEMINI_API_KEY)
                        contentType(ContentType.Application.Json)
                        setBody(geminiRequest)
                    }
                ) {
                    this.incoming.collect { event ->
                        val data = event.data ?: return@collect
                        Log.v("GeminiGateway", "Received chunk: $data")
                        
                        try {
                            // Gemini SSE sends JSON chunks in the 'data' field
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

                // Signal completion
                val latency = System.currentTimeMillis() - startMs
                Log.i("GeminiGateway", "SSE stream completed in ${latency}ms")
                trySend(AiRuntimeResponse(text = "", isDone = true, latencyMs = latency))
                close()

            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i("GeminiGateway", "SSE stream cancelled by caller")
                    throw AiRuntimeException(RuntimeFailureReason.CANCELLED, "Generation cancelled", e)
                }
                
                Log.e("GeminiGateway", "SSE stream error: ${e.message}", e)
                val reason = when (e) {
                    is HttpRequestTimeoutException -> RuntimeFailureReason.TIMEOUT
                    else -> RuntimeFailureReason.INTERNAL_ERROR
                }
                throw AiRuntimeException(reason, "Gemini REST error: ${e.message}", e)
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun close() {
        client.close()
    }
}
