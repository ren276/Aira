package com.aira.health.domain.engine

import com.aira.health.domain.model.PersonalizationParameters
import com.aira.health.domain.model.PersonalizationSkipReason
import com.aira.health.domain.model.PersonalizationUpdateDecision
import javax.inject.Inject
import kotlin.math.abs

class PersonalizationUpdateEngine @Inject constructor() {

    companion object {
        const val MIN_USABLE_DAYS = 7
        private const val MAX_DAILY_DELTA_RATIO = 0.03f
    }

    fun update(
        previous: PersonalizationParameters,
        observed: PersonalizationParameters,
        usableDays: Int,
        alpha: Float = 0.2f
    ): PersonalizationUpdateDecision {
        if (usableDays < MIN_USABLE_DAYS) {
            return PersonalizationUpdateDecision(
                applied = false,
                parameters = previous,
                usableDays = usableDays,
                skipReason = PersonalizationSkipReason.INSUFFICIENT_HISTORY
            )
        }

        val sleepNeed = boundedEma(previous.sleepNeedMinutes, observed.sleepNeedMinutes, alpha)
        val recoverySpeed = boundedEma(previous.recoverySpeed, observed.recoverySpeed, alpha)
        val stressSensitivity = boundedEma(previous.stressSensitivity, observed.stressSensitivity, alpha)

        val capped = isCapped(previous.sleepNeedMinutes, sleepNeed) ||
            isCapped(previous.recoverySpeed, recoverySpeed) ||
            isCapped(previous.stressSensitivity, stressSensitivity)

        return PersonalizationUpdateDecision(
            applied = true,
            parameters = PersonalizationParameters(
                sleepNeedMinutes = sleepNeed,
                recoverySpeed = recoverySpeed,
                stressSensitivity = stressSensitivity
            ),
            usableDays = usableDays,
            capped = capped
        )
    }

    private fun boundedEma(previous: Float, observed: Float, alpha: Float): Float {
        val ema = previous + (alpha.coerceIn(0f, 1f) * (observed - previous))
        val maxDelta = (abs(previous) * MAX_DAILY_DELTA_RATIO).takeIf { it > 0f } ?: MAX_DAILY_DELTA_RATIO
        val boundedDelta = (ema - previous).coerceIn(-maxDelta, maxDelta)
        return previous + boundedDelta
    }

    private fun isCapped(previous: Float, updated: Float): Boolean {
        val delta = updated - previous
        val maxDelta = (abs(previous) * MAX_DAILY_DELTA_RATIO).takeIf { it > 0f } ?: MAX_DAILY_DELTA_RATIO
        return abs(delta) >= maxDelta - 1e-6f
    }
}
