package com.aira.health.ai.fallback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AIM-04 — Verifies that [DeterministicSummaryService] produces stable, reason-coded,
 * wellness-safe output for all failure paths.
 */
class DeterministicSummaryServiceTest {

    private val service = DeterministicSummaryService()

    // ---------------------------------------------------------------------------
    // Determinism — same input always yields same output
    // ---------------------------------------------------------------------------

    @Test
    fun `same snapshot and reason always produce identical text`() {
        val snapshot = sampleSnapshot(recoveryScore = 70)
        val first = service.buildSummary(snapshot, FallbackReason.TIMEOUT)
        val second = service.buildSummary(snapshot, FallbackReason.TIMEOUT)
        assertEquals(first.text, second.text, "Same input must yield identical fallback text")
    }

    @Test
    fun `different reasons produce different qualifier sentences`() {
        val snapshot = sampleSnapshot(recoveryScore = 65)
        val timeout = service.buildSummary(snapshot, FallbackReason.TIMEOUT)
        val unavailable = service.buildSummary(snapshot, FallbackReason.MODEL_UNAVAILABLE)
        assertFalse(
            timeout.text == unavailable.text,
            "Different reasons should produce different qualifier text",
        )
    }

    // ---------------------------------------------------------------------------
    // Reason code mapping (AIM-04)
    // ---------------------------------------------------------------------------

    @Test
    fun `buildSummary returns correct reason in result for MODEL_UNAVAILABLE`() {
        val summary = service.buildSummary(sampleSnapshot(), FallbackReason.MODEL_UNAVAILABLE)
        assertEquals(FallbackReason.MODEL_UNAVAILABLE, summary.reason)
    }

    @Test
    fun `buildSummary returns correct reason for TIMEOUT`() {
        val summary = service.buildTimeoutSummary(sampleSnapshot())
        assertEquals(FallbackReason.TIMEOUT, summary.reason)
    }

    @Test
    fun `buildSummary returns correct reason for LOW_CONFIDENCE`() {
        val summary = service.buildLowConfidenceSummary(sampleSnapshot(confidence = 0.2f))
        assertEquals(FallbackReason.LOW_CONFIDENCE, summary.reason)
    }

    @Test
    fun `buildSummary returns correct reason for RUNTIME_ERROR`() {
        val summary = service.buildSummary(sampleSnapshot(), FallbackReason.RUNTIME_ERROR)
        assertEquals(FallbackReason.RUNTIME_ERROR, summary.reason)
    }

    @Test
    fun `buildSummary returns correct reason for CANCELLED`() {
        val summary = service.buildSummary(sampleSnapshot(), FallbackReason.CANCELLED)
        assertEquals(FallbackReason.CANCELLED, summary.reason)
    }

    @Test
    fun `buildSummary returns correct reason for STALE_DATA`() {
        val summary = service.buildSummary(sampleSnapshot(), FallbackReason.STALE_DATA)
        assertEquals(FallbackReason.STALE_DATA, summary.reason)
    }

    // ---------------------------------------------------------------------------
    // Confidence language thresholds
    // ---------------------------------------------------------------------------

    @Test
    fun `low confidence snapshot includes data quality qualifier in text`() {
        val summary = service.buildSummary(
            sampleSnapshot(confidence = 0.2f),
            FallbackReason.LOW_CONFIDENCE,
        )
        assertTrue(
            summary.text.contains("quality", ignoreCase = true) ||
                summary.text.contains("limited", ignoreCase = true) ||
                summary.text.contains("rough guide", ignoreCase = true),
            "Low confidence should add data quality qualifier: ${summary.text}",
        )
    }

    @Test
    fun `high confidence snapshot does not add unnecessary qualification`() {
        val summary = service.buildSummary(
            sampleSnapshot(confidence = 0.90f),
            FallbackReason.RUNTIME_ERROR,
        )
        assertNotNull(summary.text)
        assertTrue(summary.text.isNotBlank())
        // Should not catastrophise high-confidence data
        assertFalse(
            summary.text.contains("very low", ignoreCase = true),
            "HIGH confidence should not show 'very low' warning: ${summary.text}",
        )
    }

    // ---------------------------------------------------------------------------
    // Score-based label thresholds
    // ---------------------------------------------------------------------------

    @Test
    fun `recovery score 80+ yields strong label`() {
        assertEquals("strong", service.scoreLabel(80))
        assertEquals("strong", service.scoreLabel(100))
    }

    @Test
    fun `recovery score 60-79 yields moderate label`() {
        assertEquals("moderate", service.scoreLabel(60))
        assertEquals("moderate", service.scoreLabel(79))
    }

    @Test
    fun `recovery score 40-59 yields low label`() {
        assertEquals("low", service.scoreLabel(40))
        assertEquals("low", service.scoreLabel(59))
    }

    @Test
    fun `recovery score below 40 yields poor label`() {
        assertEquals("poor", service.scoreLabel(0))
        assertEquals("poor", service.scoreLabel(39))
    }

    @Test
    fun `high strain snapshot surfaces recovery activity suggestion`() {
        val summary = service.buildSummary(
            sampleSnapshot(strain = 80, recoveryScore = 60),
            FallbackReason.TIMEOUT,
        )
        assertTrue(
            summary.text.contains("recovery", ignoreCase = true),
            "High strain should surface recovery suggestion: ${summary.text}",
        )
    }

    @Test
    fun `low recovery snapshot surfaces lighter activity suggestion`() {
        val summary = service.buildSummary(
            sampleSnapshot(recoveryScore = 30, strain = 40),
            FallbackReason.TIMEOUT,
        )
        assertTrue(
            summary.text.contains("lighter", ignoreCase = true) ||
                summary.text.contains("lower", ignoreCase = true) ||
                summary.text.contains("on the lower side", ignoreCase = true),
            "Low recovery should suggest lighter activity: ${summary.text}",
        )
    }

    // ---------------------------------------------------------------------------
    // Wellness boundary — no medical/diagnostic language
    // ---------------------------------------------------------------------------

    @Test
    fun `fallback text never contains diagnosis or treatment language`() {
        val blockedTerms = listOf("diagnos", "prescri", "medica", "treat", "symptom")
        FallbackReason.entries.forEach { reason ->
            val summary = service.buildSummary(sampleSnapshot(), reason)
            blockedTerms.forEach { term ->
                assertFalse(
                    summary.text.contains(term, ignoreCase = true),
                    "Fallback text must not contain '$term' for reason $reason: ${summary.text}",
                )
            }
        }
    }

    @Test
    fun `all fallback reasons produce non-blank text`() {
        FallbackReason.entries.forEach { reason ->
            val summary = service.buildSummary(sampleSnapshot(), reason)
            assertTrue(summary.text.isNotBlank(), "Fallback text must not be blank for reason $reason")
        }
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun sampleSnapshot(
        recoveryScore: Int = 68,
        strain: Int = 50,
        confidence: Float = 0.75f,
    ) = com.aira.health.ai.prompt.MetricSnapshot(
        date = "2026-04-17",
        recoveryScore = recoveryScore,
        sleepScore = 65,
        strainScore = strain,
        stressScore = 40,
        dataConfidence = confidence,
        compositeReadiness = recoveryScore,
    )
}
