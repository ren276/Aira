package com.aira.health.domain.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [RecoveryEngine] — SCORE-01.
 *
 * Covers:
 *  - Full-input weighted formula (HRV 40%, RHR 25%, Sleep 25%, prior Strain 10%)
 *  - Missing-input partial scoring with confidence downgrade (D-03, D-04, D-11, D-12)
 *  - Boundary: output is always clamped to 0..100
 */
class RecoveryEngineTest {

    private val engine = RecoveryEngine()

    // ── Full-input tests ──────────────────────────────────────────────────────

    @Test
    fun `full input applies SCORE-01 weighting and returns bounded score`() {
        // All inputs at their "perfect" normalised values → expect score near 100
        val result = engine.compute(
            hrvNormalized = 1.0f,   // HRV at/above baseline → best
            rhrNormalized = 1.0f,   // RHR at/below baseline → best
            sleepScore = 100f,
            priorStrainScore = 0f   // no prior strain → best recovery
        )
        assertEquals(1.0f, result.confidence, 0.01f)
        assertTrue("Score should be near 100 with all optimal inputs", result.score >= 95)
        assertTrue("Score must be ≤ 100", result.score <= 100)
    }

    @Test
    fun `full input with mid-range values produces expected weighted score`() {
        // Each component contributes 50% of its weight
        // Expected: 0.40*50 + 0.25*50 + 0.25*50 + 0.10*50 = 50
        val result = engine.compute(
            hrvNormalized = 0.5f,
            rhrNormalized = 0.5f,
            sleepScore = 50f,
            priorStrainScore = 50f
        )
        assertEquals(1.0f, result.confidence, 0.01f)
        assertTrue("Score should be near 50 with mid-range inputs: ${result.score}", result.score in 45..55)
    }

    @Test
    fun `extreme low physiology values still return bounded score`() {
        // Very poor day: HRV way below baseline, RHR very elevated, no sleep, max strain yesterday
        val result = engine.compute(
            hrvNormalized = 0.0f,
            rhrNormalized = 0.0f,
            sleepScore = 0f,
            priorStrainScore = 100f
        )
        assertEquals(1.0f, result.confidence, 0.01f)
        assertTrue("Score must be ≥ 0", result.score >= 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
        assertTrue("Worst-day score should be near 0: ${result.score}", result.score <= 10)
    }

    // ── Missing-input tests (D-03, D-04, D-11, D-12) ─────────────────────────

    @Test
    fun `missing HRV still returns visible score with reduced confidence`() {
        val result = engine.compute(
            hrvNormalized = null,
            rhrNormalized = 0.7f,
            sleepScore = 70f,
            priorStrainScore = 30f
        )
        // Score should still be present (not null, not 0 due to suppression)
        assertNotNull("Score must not be null", result.score)
        assertTrue("Score must be > 0 when other inputs are good", result.score > 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
        // Confidence must be < 1.0 since HRV (40% weight) is missing
        assertTrue("Confidence should be < 1.0 when HRV is missing: ${result.confidence}", result.confidence < 1.0f)
        assertTrue("Confidence must be > 0", result.confidence > 0f)
    }

    @Test
    fun `missing multiple inputs still returns visible score and further reduced confidence`() {
        val result = engine.compute(
            hrvNormalized = null,
            rhrNormalized = null,
            sleepScore = 80f,
            priorStrainScore = null
        )
        assertNotNull(result.score)
        assertTrue("Score must be > 0 when sleep input is good", result.score > 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
        // Only sleep present — confidence should be very low (only 30% of weight available)
        assertTrue("Confidence should be ≤ 0.35 with only sleep present", result.confidence <= 0.35f)
    }

    @Test
    fun `all inputs missing returns zero score with zero confidence`() {
        val result = engine.compute(
            hrvNormalized = null,
            rhrNormalized = null,
            sleepScore = null,
            priorStrainScore = null
        )
        assertEquals(0, result.score)
        assertEquals(0f, result.confidence, 0.001f)
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same inputs always produce same outputs (deterministic)`() {
        val a = engine.compute(0.6f, 0.7f, 65f, 40f)
        val b = engine.compute(0.6f, 0.7f, 65f, 40f)
        assertEquals(a.score, b.score)
        assertEquals(a.confidence, b.confidence, 0.0001f)
    }
}
