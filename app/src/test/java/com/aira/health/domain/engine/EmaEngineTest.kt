package com.aira.health.domain.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [EmaEngine] — SCORE-05, D-07, D-08.
 *
 * Covers:
 *  - First 7 samples use flat-average cold-start behavior
 *  - Subsequent samples switch to EMA smoothing
 *  - coldStartComplete flag transitions correctly at sample 7
 *  - Deterministic output for same inputs
 */
class EmaEngineTest {

    private val engine = EmaEngine()

    companion object {
        private const val ALPHA = 0.2f
        private const val COLD_START_SAMPLES = 7
    }

    // ── Cold-start tests (D-08) ───────────────────────────────────────────────

    @Test
    fun `first sample initialises baseline to that value`() {
        val result = engine.update(
            previousValue = 0f,
            previousSampleCount = 0,
            coldStartComplete = false,
            newMeasurement = 60f,
            alpha = ALPHA
        )
        assertEquals(60f, result.newValue, 0.01f)
        assertEquals(1, result.newSampleCount)
        assertFalse("Cold start incomplete with 1 sample", result.coldStartComplete)
    }

    @Test
    fun `cold start baseline is running average for first 7 samples`() {
        var value = 0f
        var count = 0
        var coldComplete = false

        val measurements = listOf(60f, 62f, 58f, 61f, 59f, 63f, 61f)

        for (m in measurements) {
            val r = engine.update(value, count, coldComplete, m, ALPHA)
            value = r.newValue
            count = r.newSampleCount
            coldComplete = r.coldStartComplete
        }

        val expectedAvg = measurements.average().toFloat()
        // At sample 7 (index 6), should have transitioned
        assertTrue("Cold start should be complete after 7 samples", coldComplete)
        assertEquals(7, count)
        assertEquals(expectedAvg, value, 2.0f) // within 2 ms tolerance
    }

    @Test
    fun `cold start transitions at exactly 7th sample`() {
        var value = 0f; var count = 0; var coldComplete = false
        repeat(6) {
            val r = engine.update(value, count, coldComplete, 60f, ALPHA)
            value = r.newValue; count = r.newSampleCount; coldComplete = r.coldStartComplete
        }
        assertFalse("Should not be complete with 6 samples", coldComplete)

        val r7 = engine.update(value, count, coldComplete, 60f, ALPHA)
        assertTrue("Should be complete after 7th sample", r7.coldStartComplete)
    }

    // ── EMA tests (post cold-start) ───────────────────────────────────────────

    @Test
    fun `post cold-start uses EMA formula`() {
        val baseline = 60f
        val newMeasurement = 70f
        // EMA: new = α × measurement + (1−α) × previous
        val expected = ALPHA * newMeasurement + (1 - ALPHA) * baseline

        val result = engine.update(
            previousValue = baseline,
            previousSampleCount = 10,
            coldStartComplete = true,
            newMeasurement = newMeasurement,
            alpha = ALPHA
        )
        assertEquals(expected, result.newValue, 0.01f)
        assertEquals(11, result.newSampleCount)
        assertTrue(result.coldStartComplete)
    }

    @Test
    fun `EMA converges toward new steady state over time`() {
        var value = 50f
        var count = 10
        repeat(50) {
            val r = engine.update(value, count, true, 80f, ALPHA)
            value = r.newValue
            count = r.newSampleCount
        }
        // After enough samples at 80, baseline should be close to 80
        assertTrue("EMA should converge to ~80 after 50 samples: $value", value > 75f)
        assertTrue(value <= 80f)
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same inputs always produce same output`() {
        val a = engine.update(55f, 8, true, 62f, 0.2f)
        val b = engine.update(55f, 8, true, 62f, 0.2f)
        assertEquals(a.newValue, b.newValue, 0.0001f)
        assertEquals(a.newSampleCount, b.newSampleCount)
        assertEquals(a.coldStartComplete, b.coldStartComplete)
    }
}
