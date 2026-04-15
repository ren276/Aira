package com.aira.health.domain.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [StrainEngine] — SCORE-04.
 *
 * Covers:
 *  - Zone-based input produces bounded 0..100 strain values
 *  - Zone-5-heavy workloads produce disproportionately higher scores (D-01 non-linear)
 *  - Monotonicity: more high-zone time → higher score
 *  - Sparse zone data returns score + reduced confidence rather than suppression (D-03, D-04, D-11, D-12)
 */
class StrainEngineTest {

    private val engine = StrainEngine()

    // ── Boundary tests ────────────────────────────────────────────────────────

    @Test
    fun `zero minutes in all zones produces score of 0`() {
        val result = engine.compute(
            zone1Minutes = 0f,
            zone2Minutes = 0f,
            zone3Minutes = 0f,
            zone4Minutes = 0f,
            zone5Minutes = 0f,
            totalActiveMinutes = 0f
        )
        assertEquals(0, result.score)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `extreme zone-5 heavy session produces score near 100`() {
        // 60 minutes all in Zone 5 — maximum physiological stress
        val result = engine.compute(
            zone1Minutes = 0f,
            zone2Minutes = 0f,
            zone3Minutes = 0f,
            zone4Minutes = 0f,
            zone5Minutes = 60f,
            totalActiveMinutes = 60f
        )
        assertEquals(1.0f, result.confidence, 0.01f)
        assertTrue("Zone-5 only session should score very high: ${result.score}", result.score >= 85)
        assertTrue("Score must be ≤ 100", result.score <= 100)
    }

    @Test
    fun `zone-5-heavy session scores higher than equivalent time in zone-3`() {
        val highIntensity = engine.compute(
            zone1Minutes = 0f,
            zone2Minutes = 0f,
            zone3Minutes = 0f,
            zone4Minutes = 5f,
            zone5Minutes = 55f,
            totalActiveMinutes = 60f
        )
        val lowIntensity = engine.compute(
            zone1Minutes = 0f,
            zone2Minutes = 0f,
            zone3Minutes = 55f,
            zone4Minutes = 5f,
            zone5Minutes = 0f,
            totalActiveMinutes = 60f
        )
        assertTrue(
            "High-intensity (zone 4/5) score ${highIntensity.score} should exceed low-intensity ${lowIntensity.score}",
            highIntensity.score > lowIntensity.score
        )
    }

    @Test
    fun `output is always bounded 0 to 100 regardless of input magnitude`() {
        val result = engine.compute(
            zone1Minutes = 1000f,
            zone2Minutes = 1000f,
            zone3Minutes = 1000f,
            zone4Minutes = 1000f,
            zone5Minutes = 1000f,
            totalActiveMinutes = 5000f
        )
        assertTrue("Score must be ≥ 0", result.score >= 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
    }

    @Test
    fun `monotonic adding zone-4 time raises score`() {
        val baseline = engine.compute(0f, 30f, 20f, 0f, 0f, 50f)
        val withZ4   = engine.compute(0f, 30f, 20f, 20f, 0f, 70f)
        assertTrue("Adding zone-4 time should increase score", withZ4.score >= baseline.score)
    }

    // ── Missing-input tests (D-03, D-04, D-11, D-12) ─────────────────────────

    @Test
    fun `null zone data reduces confidence but still returns visible score`() {
        val result = engine.compute(
            zone1Minutes = null,
            zone2Minutes = null,
            zone3Minutes = 20f,
            zone4Minutes = 10f,
            zone5Minutes = null,
            totalActiveMinutes = 30f
        )
        assertTrue("Score must be > 0 when zone 3/4 data exists: ${result.score}", result.score > 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
        assertTrue("Confidence should be < 1.0 when zones are missing: ${result.confidence}", result.confidence < 1.0f)
        assertTrue("Confidence must be > 0", result.confidence > 0f)
    }

    @Test
    fun `all zone inputs null returns zero score with zero confidence`() {
        val result = engine.compute(
            zone1Minutes = null,
            zone2Minutes = null,
            zone3Minutes = null,
            zone4Minutes = null,
            zone5Minutes = null,
            totalActiveMinutes = null
        )
        assertEquals(0, result.score)
        assertEquals(0f, result.confidence, 0.001f)
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same inputs always produce same outputs`() {
        val a = engine.compute(10f, 20f, 15f, 10f, 5f, 60f)
        val b = engine.compute(10f, 20f, 15f, 10f, 5f, 60f)
        assertEquals(a.score, b.score)
        assertEquals(a.confidence, b.confidence, 0.0001f)
    }
}
