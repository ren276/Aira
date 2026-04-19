package com.aira.health.domain.engine

import com.aira.health.data.local.model.UserCorrection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionInfluenceEngineTest {

    private val engine = CorrectionInfluenceEngine()

    @Test
    fun `correction weight decays over fourteen day horizon and trends to zero`() {
        val corrections = listOf(
            UserCorrection(id = 1, recordType = "daily", recordDate = "2026-04-17", fieldName = "sleep_duration", originalValue = 0f, correctedValue = 0f, confidenceDelta = 0.2f),
            UserCorrection(id = 2, recordType = "daily", recordDate = "2026-04-04", fieldName = "sleep_duration", originalValue = 0f, correctedValue = 0f, confidenceDelta = 0.2f)
        )

        val result = engine.applyDecay(corrections, targetDate = "2026-04-18")

        val details = result.details.sortedBy { it.correctionId }
        assertTrue(details[0].decayedInfluence > details[1].decayedInfluence)
        assertEquals(0f, details[1].decayWeight, 0.0001f)
    }

    @Test
    fun `combined correction influence never exceeds twenty percent cap`() {
        val corrections = listOf(
            UserCorrection(id = 1, recordType = "daily", recordDate = "2026-04-17", fieldName = "stress_level", originalValue = 0f, correctedValue = 0f, confidenceDelta = 0.5f),
            UserCorrection(id = 2, recordType = "daily", recordDate = "2026-04-16", fieldName = "stress_level", originalValue = 0f, correctedValue = 0f, confidenceDelta = 0.5f)
        )

        val result = engine.applyDecay(corrections, targetDate = "2026-04-18")

        assertTrue((result.combinedByParameter["stressSensitivity"] ?: 0f) <= 0.2f)
    }

    @Test
    fun `influence details retain timestamp and target parameter provenance`() {
        val corrections = listOf(
            UserCorrection(id = 9, recordType = "daily", recordDate = "2026-04-17", fieldName = "hrv_morning", originalValue = 0f, correctedValue = 0f, confidenceDelta = -0.1f, createdAt = 12345L)
        )

        val result = engine.applyDecay(corrections, targetDate = "2026-04-18")

        assertEquals(1, result.details.size)
        assertEquals("recoverySpeed", result.details.first().parameterKey)
        assertEquals(12345L, result.details.first().createdAt)
    }
}
