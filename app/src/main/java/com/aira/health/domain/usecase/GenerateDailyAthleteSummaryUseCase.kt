package com.aira.health.domain.usecase

import com.aira.health.domain.model.AthleteGuidanceRequest
import javax.inject.Inject

/**
 * Generates concise daily summary text from local aggregate state.
 */
class GenerateDailyAthleteSummaryUseCase @Inject constructor() {

    fun generate(request: AthleteGuidanceRequest, lowConfidence: Boolean): String {
        val readiness = readinessLabel(request.recoveryScore, request.energyBankScore)
        val strain = strainLabel(request.strainScore)

        val baseSummary = "Today looks $readiness with $strain training strain and sleep at ${request.sleepScore}/100."
        return if (lowConfidence) {
            "$baseSummary Signal confidence is limited, so treat this as a conservative guide."
        } else {
            baseSummary
        }
    }

    private fun readinessLabel(recovery: Int, energy: Int): String = when {
        recovery >= 75 && energy >= 70 -> "strong"
        recovery >= 55 && energy >= 50 -> "balanced"
        recovery >= 40 -> "constrained"
        else -> "fragile"
    }

    private fun strainLabel(strain: Int): String = when {
        strain >= 75 -> "high"
        strain >= 50 -> "moderate"
        else -> "light"
    }
}
