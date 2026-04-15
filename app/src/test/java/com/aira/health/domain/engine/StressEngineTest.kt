package com.aira.health.domain.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [StressEngine] — SCORE-03.
 *
 * Covers:
 *  - Hourly stress snapshots are valid 0..100 values
 *  - Daily aggregate reflects non-linear amplification of high-stress hours (D-01)
 *  - Missing hourly windows degrade confidence but still return a score (D-03, D-04, D-11, D-12)
 */
class StressEngineTest {

    private val engine = StressEngine()

    // ── Hourly snapshot tests ─────────────────────────────────────────────────

    @Test
    fun `single hour at baseline HR and HRV produces low stress`() {
        val result = engine.computeHourlyStress(
            hrBpm = 60f,
            hrBaselineBpm = 60f,
            hrvRmssd = 50f,
            hrvBaselineRmssd = 50f
        )
        assertTrue("Baseline conditions → low stress: ${result.stressScore}", result.stressScore <= 20)
        assertTrue("Hourly score must be ≥ 0", result.stressScore >= 0)
        assertTrue("Hourly score must be ≤ 100", result.stressScore <= 100)
    }

    @Test
    fun `highly elevated HR with suppressed HRV produces high hourly stress`() {
        val result = engine.computeHourlyStress(
            hrBpm = 100f,
            hrBaselineBpm = 60f,      // HR 67% above baseline
            hrvRmssd = 15f,
            hrvBaselineRmssd = 60f    // HRV 75% below baseline
        )
        assertTrue("Elevated HR + depressed HRV → high stress: ${result.stressScore}", result.stressScore >= 60)
        assertTrue("Hourly stress score ≤ 100", result.stressScore <= 100)
    }

    @Test
    fun `hourly stress score is always bounded 0 to 100`() {
        // Extreme inputs
        val extreme = engine.computeHourlyStress(
            hrBpm = 200f,
            hrBaselineBpm = 50f,
            hrvRmssd = 1f,
            hrvBaselineRmssd = 100f
        )
        assertTrue(extreme.stressScore >= 0)
        assertTrue(extreme.stressScore <= 100)

        val calm = engine.computeHourlyStress(
            hrBpm = 40f,
            hrBaselineBpm = 65f,   // Well below baseline
            hrvRmssd = 120f,
            hrvBaselineRmssd = 50f // HRV well above baseline
        )
        assertTrue(calm.stressScore >= 0)
        assertTrue(calm.stressScore <= 100)
    }

    // ── Daily aggregate tests ─────────────────────────────────────────────────

    @Test
    fun `daily aggregate of all-baseline hours produces low daily stress`() {
        val hourlyScores = List(24) { 10f }   // All low-stress hours
        val result = engine.computeDailyStress(hourlyScores)
        assertTrue("All-calm hours → low daily stress: ${result.score}", result.score <= 25)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `daily aggregate with extreme-stress hours amplifies beyond linear mean`() {
        // 4 very high-stress hours + 20 calm hours
        val hourlyScores = List(20) { 10f } + List(4) { 90f }
        val result = engine.computeDailyStress(hourlyScores)
        val linearMean = hourlyScores.average().toFloat()
        assertTrue(
            "Non-linear daily score ${result.score} should exceed linear mean ${linearMean.toInt()}",
            result.score > linearMean.toInt()
        )
        assertTrue("Score must be ≤ 100", result.score <= 100)
    }

    @Test
    fun `daily aggregate output is always bounded 0 to 100`() {
        val allMaxStress = List(24) { 100f }
        val result = engine.computeDailyStress(allMaxStress)
        assertTrue("Score must be ≥ 0", result.score >= 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
    }

    // ── Missing-input tests (D-03, D-04, D-11, D-12) ─────────────────────────

    @Test
    fun `missing hourly windows reduce confidence but still return daily stress score`() {
        // Only 12 of 24 hours have data
        val partialHourlyScores = List(12) { 40f }
        val result = engine.computeDailyStress(partialHourlyScores, totalExpectedHours = 24)
        assertTrue("Score must be > 0: ${result.score}", result.score > 0)
        assertTrue("Score must be ≤ 100", result.score <= 100)
        // 12/24 hours = 0.5 confidence
        assertTrue("Confidence should be ~0.5 with half coverage: ${result.confidence}", result.confidence <= 0.55f)
        assertTrue("Confidence must be > 0", result.confidence > 0f)
    }

    @Test
    fun `empty hourly list returns zero score with zero confidence`() {
        val result = engine.computeDailyStress(emptyList())
        assertEquals(0, result.score)
        assertEquals(0f, result.confidence, 0.001f)
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same hourly inputs produce same daily output`() {
        val hours = listOf(20f, 35f, 80f, 15f, 10f)
        val a = engine.computeDailyStress(hours)
        val b = engine.computeDailyStress(hours)
        assertEquals(a.score, b.score)
        assertEquals(a.confidence, b.confidence, 0.0001f)
    }
}
