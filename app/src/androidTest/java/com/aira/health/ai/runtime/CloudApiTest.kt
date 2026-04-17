package com.aira.health.ai.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudApiTest {

    @Test
    fun testGeminiCloudGeneration() = runBlocking {
        val config = RuntimeConfig() // Uses 30s timeout now
        val gateway = GeminiCloudRuntimeGateway(config)

        val request = AiRuntimeRequest(
            promptChunks = listOf("Say exactly: 'Aira Cloud API is working!'")
        )

        println("Sending request to Gemini via Cloud API...")
        val responses = gateway.generate(request).toList()

        val fullResponse = responses.joinToString("") { it.text }
        println("Gemini Response: $fullResponse")

        assertTrue("Response should not be empty", fullResponse.isNotEmpty())
        assertTrue("Response should contain 'Aira'", fullResponse.contains("Aira"))
    }

    @Test
    fun testGeminiCloudTimeout() = runBlocking {
        // 1. Force a 1ms timeout
        val config = RuntimeConfig(timeoutMillis = 1L)
        val gateway = GeminiCloudRuntimeGateway(config)

        val request = AiRuntimeRequest(
            promptChunks = listOf("Write a 500 word essay about health.")
        )

        try {
            gateway.generate(request).toList()
            assertTrue("Should have timed out", false)
        } catch (e: AiRuntimeException) {
            println("Caught expected timeout: ${e.message}")
            assertEquals(RuntimeFailureReason.TIMEOUT, e.reason)
        }
    }

    @Test
    fun testGeminiCloudCancellation() = runBlocking {
        val gateway = GeminiCloudRuntimeGateway(RuntimeConfig())
        val request = AiRuntimeRequest(promptChunks = listOf("Write a long story."))

        val job = launch {
            try {
                // Collect only the first item and then the flow will be cancelled when the scope is cancelled
                gateway.generate(request).collect {
                    println("Received token, now cancelling...")
                    throw CancellationException("Manual cancellation")
                }
            } catch (e: AiRuntimeException) {
                println("Caught expected cancellation: ${e.message}")
                assertEquals(RuntimeFailureReason.CANCELLED, e.reason)
            }
        }
        job.join()
    }
}
