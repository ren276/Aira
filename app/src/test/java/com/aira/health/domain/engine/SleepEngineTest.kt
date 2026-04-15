package com.aira.health.domain.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [SleepEngine] — SCORE-02.
 *
 * Covers:
 *  - Full-input weighted formula (Duration 30%, Stage/Deep 30%, Continuity 20%, Consistency 20%)
 *  - Missing-input partial scoring with confidence downgrade (D-03, D-04, D-11, D-12)
 *  - Boundary: output is always clamped to 0..100; confidence reflects data availability
 */
class SleepEngineTest {

    private val engine = SleepEngine()

    // ── Full-input tests ──────────────────────────────────────────────────────

    @Test
    fun `full input applies SCORE-02 weighting and returns bounded score`() {
        val result = engine.compute(
            durationNormalized = 1.0f,   // Full target sleep achieved
            deepSleepNormalized = 1.0f,  // Ideal deep/REM ratio
            continuityNormalized = 1.0f, // No interruptions
            consistencyNormalized = 1.0f // Consistent sleep timing
        )
        assertEquals(1.0f, result.confidence, 0.01f)
        assertTrue("Score should be near 100 with optimal inputs: ${result.score}", result.score >= 95)
        assertTrue("Score must be ≤ 100", result.score <= 100)
    }

    @Test
    fun `mid-range inputs produce expected weighted sleep score`() {
        // Each normalised component = 0.5
        // Expected: 0.30*50 + 0.30*50 + 0.20*50 + 0.20*50 = 50
        val result = engine.compute(
            durationNormalized = 0.5f,
            deepSleepNormalized = 0.5f,
            continuityNormalized = 0.5f,
            consistencyNormalized = 0.5f
        )
        assertEquals(1.0f, result.confidence, 0.01f)
        assertTrue("Score near 50 with mid-range inputs: ${result.score}", result.score in 45..55)
    }

    @Test
    fun `worst-case inputs still return bounded score ≥ 0`() {
        val result = engine.compute(
            durationNormalized = 0.0f,
            deepSleepNormalized = 0.0f,
            continuityNormalized = 0.0f,
            consistencyNormalized = 0.0f
        )
        assertEquals(1.0f, result.confidence, 0.01f)
        assertEquals(0, result.score)
    }

    @Test
    fun `inputs outside nominal range are clamped to 0 to 100`() {
        val result = engine.compute(
            durationNormalized = 2.0f,   // over-normalised
            deepSleepNormalized = -0.5f, // negative
            continuityNormalized = 1.5f,
            consistencyNormalized = 0.8f
        )
        assertTrue("Score must be ≤ 100: ${result.score}", result.score <= 100)
        assertTrue("Score must be ≥ 0: ${result.score}", result.score >= 0)
    }

    // ── Missing-input tests (D-03, D-04, D-11, D-12) ─────────────────────────

    @Test
    fun `missing deep sleep component still returns visible score with reduced confidence`() {
        val result = engine.compute(
            durationNormalized = 0.8f,
            deepSleepNormalized = null,  // Missing
            continuityNormalized = 0.7f,
            consistencyNormalized = 0.6f
        )
        assertTrue("Score must be > 0 when available inputs are good: ${result.score}", result.score > 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
        // Deep sleep weight = 30%; confidence ≤ 0.70
        assertTrue("Confidence should be < 1.0 when deep sleep is missing: ${result.confidence}", result.confidence < 1.0f)
        assertTrue("Confidence must be > 0", result.confidence > 0f)
    }

    @Test
    fun `missing multiple components still produces visible score`() {
        val result = engine.compute(
            durationNormalized = 0.9f,
            deepSleepNormalized = null,
            continuityNormalized = null,
            consistencyNormalized = null
        )
        assertTrue("Score must be > 0: ${result.score}", result.score > 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
        // Only duration (30%) available → confidence ≤ 0.30
        assertTrue("Confidence ≤ 0.35 with only duration: ${result.confidence}", result.confidence <= 0.35f)
    }

    @Test
    fun `all inputs missing returns zero score with zero confidence`() {
        val result = engine.compute(
            durationNormalized = null,
            deepSleepNormalized = null,
            continuityNormalized = null,
            consistencyNormalized = null
        )
        assertEquals(0, result.score)
        assertEquals(0f, result.confidence, 0.001f)
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same inputs always produce same outputs`() {
        val a = engine.compute(0.7f, 0.6f, 0.8f, 0.5f)
        val b = engine.compute(0.7f, 0.6f, 0.8f, 0.5f)
        assertEquals(a.score, b.score)
        assertEquals(a.confidence, b.confidence, 0.0001f)
    }
}
