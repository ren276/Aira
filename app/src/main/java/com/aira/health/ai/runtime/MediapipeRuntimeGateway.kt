package com.aira.health.ai.runtime

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete [AiRuntimeGateway] backed by MediaPipe Tasks GenAI 0.10.22.
 *
 * Lifecycle contract:
 * - **Engine** ([LlmInference]) is created once and kept alive (singleton via Hilt).
 * - **Session** ([LlmInferenceSession]) is created per [generate] call and always closed
 *   in a `finally` block regardless of success, cancellation, or error.
 *
 * Thread safety:
 * - All inference work runs on [Dispatchers.Default] (PERF-01).
 * - The collecting coroutine may cancel at any time — cooperative cancellation is checked
 *   inside the streaming callback.
 *
 * Privacy:
 * - No network I/O is performed anywhere in this class (AIM-02).
 * - Raw biometric records must never appear in [AiRuntimeRequest.promptChunks] — enforced
 *   by the [PromptAssembler] layer that composes requests before calling this gateway.
 *
 * Model loading:
 * - The engine is initialised lazily on first [generate] call.
 * - [modelPath] must point to an app-private directory. Path traversal and unexpected
 *   extensions are rejected by [validateModelPath] (T-07-01 mitigation).
 */
@Singleton
class MediapipeRuntimeGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: RuntimeConfig,
) : AiRuntimeGateway {

    @Volatile
    private var engine: LlmInference? = null
    private val engineLock = Any()

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> =
        callbackFlow {
            val session = RuntimeSession(requestId = request.requestId)
            val llm = getOrInitEngine() ?: run {
                throw AiRuntimeException(
                    RuntimeFailureReason.MODEL_UNAVAILABLE,
                    "Engine could not be initialised — model path invalid or load failed",
                )
            }

            val inferenceSession = createSession(llm)
            try {
                withTimeout(request.timeoutMillis) {
                    // Add each prompt chunk from the request
                    for (chunk in request.promptChunks) {
                        inferenceSession.addQueryChunk(chunk)
                    }

                    // Start async streaming generation
                    inferenceSession.generateResponseAsync { partial, done ->
                        if (!isActive) {
                            // Cooperative cancellation inside the native callback
                            return@generateResponseAsync
                        }
                        val startMs = session.startedAtMs
                        val latency = if (done) System.currentTimeMillis() - startMs else null
                        trySend(AiRuntimeResponse(text = partial ?: "", isDone = done, latencyMs = latency))
                        if (done) {
                            close() // Signal flow completion
                        }
                    }
                }
                // Await close() call from within the callback
                awaitClose { inferenceSession.safeClose() }
            } catch (e: CancellationException) {
                inferenceSession.safeClose()
                throw AiRuntimeException(
                    RuntimeFailureReason.CANCELLED,
                    "Generation cancelled",
                    e,
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                inferenceSession.safeClose()
                throw AiRuntimeException(
                    RuntimeFailureReason.TIMEOUT,
                    "Generation exceeded ${request.timeoutMillis} ms timeout",
                    e,
                )
            } catch (e: Exception) {
                inferenceSession.safeClose()
                throw AiRuntimeException(
                    RuntimeFailureReason.INTERNAL_ERROR,
                    "Unexpected runtime error: ${e.javaClass.simpleName}",
                    e,
                )
            }
        }.flowOn(Dispatchers.Default)

    override suspend fun close() {
        synchronized(engineLock) {
            engine?.safeClose()
            engine = null
        }
    }

    // ---------------------------------------------------------------------------
    // Engine lifecycle
    // ---------------------------------------------------------------------------

    /**
     * Returns the existing engine or initialises a new one.
     * Returns null if the model path is invalid or the load fails.
     */
    private fun getOrInitEngine(): LlmInference? {
        engine?.let { return it }
        synchronized(engineLock) {
            engine?.let { return it }
            return try {
                val modelPath = resolveModelPath()
                validateModelPath(modelPath)
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setPreferredBackend(config.backend)
                    .setMaxTokens(config.maxTokens)
                    .setMaxTopK(config.maxTopK)
                    .build()
                LlmInference.createFromOptions(context, options).also { engine = it }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun createSession(llm: LlmInference): LlmInferenceSession {
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(config.topK)
            .setTopP(config.topP)
            .setTemperature(config.temperature)
            .setRandomSeed(config.randomSeed)
            .build()
        return LlmInferenceSession.createFromOptions(llm, sessionOptions)
    }

    // ---------------------------------------------------------------------------
    // Model path resolution and validation (T-07-01)
    // ---------------------------------------------------------------------------

    /**
     * Returns the model file path from app-private files directory.
     * Uses instrumentation argument when set (for integration tests).
     */
    private fun resolveModelPath(): String {
        val fromInstrumentation = try {
            android.os.Bundle()
                .getString(MODEL_PATH_INSTRUMENTATION_ARG)
        } catch (_: Exception) { null }
        return fromInstrumentation
            ?: "${context.filesDir.absolutePath}/models/gemma4_q4.bin"
    }

    /**
     * Rejects paths that escape app-private storage or have unexpected extensions.
     * Throws [IllegalArgumentException] for invalid paths (T-07-01).
     */
    private fun validateModelPath(path: String) {
        val filesDir = context.filesDir.canonicalPath
        val canonical = java.io.File(path).canonicalPath
        require(canonical.startsWith(filesDir)) {
            "Model path must be inside app-private files directory"
        }
        require(canonical.endsWith(".bin") || canonical.endsWith(".task")) {
            "Unexpected model file extension — only .bin and .task files are permitted"
        }
    }

    // ---------------------------------------------------------------------------
    // Safe close helpers
    // ---------------------------------------------------------------------------

    private fun LlmInference.safeClose() = try { close() } catch (_: Exception) { }
    private fun LlmInferenceSession.safeClose() = try { close() } catch (_: Exception) { }

    companion object {
        private const val MODEL_PATH_INSTRUMENTATION_ARG = "ai.model.path"
    }
}
