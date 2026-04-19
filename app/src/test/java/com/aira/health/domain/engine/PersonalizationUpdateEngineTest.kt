package com.aira.health.domain.engine

import com.aira.health.domain.model.PersonalizationParameters
import com.aira.health.domain.model.PersonalizationSkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizationUpdateEngineTest {

    private val engine = PersonalizationUpdateEngine()

    @Test
    fun `engine skips update when usable history is less than seven days`() {
        val previous = PersonalizationParameters(420f, 1.0f, 1.0f)
        val observed = PersonalizationParameters(450f, 1.2f, 0.8f)

        val result = engine.update(previous, observed, usableDays = 6)

        assertFalse(result.applied)
        assertEquals(PersonalizationSkipReason.INSUFFICIENT_HISTORY, result.skipReason)
        assertEquals(previous, result.parameters)
    }

    @Test
    fun `engine applies ema update for all personalization parameters once daily`() {
        val previous = PersonalizationParameters(420f, 1.0f, 1.0f)
        val observed = PersonalizationParameters(430f, 1.1f, 0.9f)

        val result = engine.update(previous, observed, usableDays = 8, alpha = 0.5f)

        assertTrue(result.applied)
        assertEquals(425f, result.parameters.sleepNeedMinutes, 0.0001f)
        assertEquals(1.03f, result.parameters.recoverySpeed, 0.0001f)
        assertEquals(0.97f, result.parameters.stressSensitivity, 0.0001f)
    }

    @Test
    fun `daily parameter movement is clamped to plus or minus three percent`() {
        val previous = PersonalizationParameters(420f, 1.0f, 1.0f)
        val observed = PersonalizationParameters(700f, 2.0f, 0.1f)

        val result = engine.update(previous, observed, usableDays = 10, alpha = 1.0f)

        assertTrue(result.applied)
        assertEquals(432.6f, result.parameters.sleepNeedMinutes, 0.0001f)
        assertEquals(1.03f, result.parameters.recoverySpeed, 0.0001f)
        assertEquals(0.97f, result.parameters.stressSensitivity, 0.0001f)
        assertTrue(result.capped)
    }
}
