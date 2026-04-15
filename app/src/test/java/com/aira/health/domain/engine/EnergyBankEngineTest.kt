package com.aira.health.domain.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [EnergyBankEngine] — D-05, D-06.
 *
 * Covers:
 *  - Recovery increases internal energy state while Strain/Stress decrease it
 *  - Visible energyBankScore reflects internal state and stays in 0..100
 *  - Engine is distinct from Recovery/Strain/Stress outputs — derived from interaction
 *  - Confidence propagation when inputs are partial (D-03, D-04, D-11, D-12)
 */
class EnergyBankEngineTest {

    private val engine = EnergyBankEngine()

    // ── Recharge / depletion dynamics (D-05) ─────────────────────────────────

    @Test
    fun `high recovery with low strain and stress produces high energy bank score`() {
        val result = engine.compute(
            recoveryScore = 85f,
            strainScore = 20f,
            stressScore = 15f,
            previousInternalBalance = 50f
        )
        assertTrue("High recovery with low load → high energy bank: ${result.energyBankScore}", result.energyBankScore >= 70)
        assertTrue("Energy bank score must be ≤ 100", result.energyBankScore <= 100)
    }

    @Test
    fun `low recovery with high strain and stress depletes energy bank`() {
        val result = engine.compute(
            recoveryScore = 20f,
            strainScore = 90f,
            stressScore = 85f,
            previousInternalBalance = 50f
        )
        assertTrue("Low recovery + high load → depleted energy bank: ${result.energyBankScore}", result.energyBankScore <= 35)
        assertTrue("Energy bank score must be ≥ 0", result.energyBankScore >= 0)
    }

    @Test
    fun `recovery increases internal balance compared to high-load day`() {
        val restDay = engine.compute(
            recoveryScore = 80f,
            strainScore = 5f,
            stressScore = 10f,
            previousInternalBalance = 40f
        )
        val hardDay = engine.compute(
            recoveryScore = 40f,
            strainScore = 90f,
            stressScore = 80f,
            previousInternalBalance = 40f
        )
        assertTrue(
            "Rest day internal balance ${restDay.internalBalance} should exceed hard day ${hardDay.internalBalance}",
            restDay.internalBalance > hardDay.internalBalance
        )
    }

    // ── Distinct from source engines (D-06) ──────────────────────────────────

    @Test
    fun `energy bank score is derived from interaction not directly equal to recovery score`() {
        val result = engine.compute(
            recoveryScore = 70f,
            strainScore = 40f,
            stressScore = 30f,
            previousInternalBalance = 50f
        )
        assertNotEquals(
            "Energy bank should not equal raw recovery score",
            70,
            result.energyBankScore
        )
    }

    // ── Visibility bounds ─────────────────────────────────────────────────────

    @Test
    fun `energy bank score is always bounded 0 to 100`() {
        // Extreme positive scenario
        val abundant = engine.compute(100f, 0f, 0f, 100f)
        assertTrue(abundant.energyBankScore <= 100)
        assertTrue(abundant.energyBankScore >= 0)

        // Extreme depletion scenario
        val depleted = engine.compute(0f, 100f, 100f, 0f)
        assertTrue(depleted.energyBankScore <= 100)
        assertTrue(depleted.energyBankScore >= 0)
    }

    @Test
    fun `internal balance is always bounded 0 to 100`() {
        val result = engine.compute(50f, 50f, 50f, 50f)
        assertTrue(result.internalBalance >= 0f)
        assertTrue(result.internalBalance <= 100f)
    }

    // ── Confidence (D-03, D-04, D-11, D-12) ─────────────────────────────────

    @Test
    fun `missing strain input reduces confidence but still returns visible score`() {
        val result = engine.compute(
            recoveryScore = 65f,
            strainScore = null,
            stressScore = 35f,
            previousInternalBalance = 50f
        )
        assertTrue("Score must be > 0: ${result.energyBankScore}", result.energyBankScore > 0)
        assertTrue("Score must be ≤ 100", result.energyBankScore <= 100)
        assertTrue("Confidence < 1.0 when strain is missing: ${result.confidence}", result.confidence < 1.0f)
        assertTrue("Confidence > 0", result.confidence > 0f)
    }

    @Test
    fun `all inputs null returns zero score with zero confidence`() {
        val result = engine.compute(
            recoveryScore = null,
            strainScore = null,
            stressScore = null,
            previousInternalBalance = 50f
        )
        assertEquals(0, result.energyBankScore)
        assertEquals(0f, result.confidence, 0.001f)
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same inputs always produce same outputs`() {
        val a = engine.compute(60f, 40f, 35f, 55f)
        val b = engine.compute(60f, 40f, 35f, 55f)
        assertEquals(a.energyBankScore, b.energyBankScore)
        assertEquals(a.internalBalance, b.internalBalance, 0.001f)
        assertEquals(a.confidence, b.confidence, 0.0001f)
    }
}
