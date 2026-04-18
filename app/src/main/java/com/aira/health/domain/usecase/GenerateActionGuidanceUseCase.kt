package com.aira.health.domain.usecase

import com.aira.health.domain.model.AthleteGuidanceOutput
import com.aira.health.domain.model.AthleteGuidanceRequest
import javax.inject.Inject

/**
 * Produces practical training, recovery, and nutrition actions from local scores.
 */
class GenerateActionGuidanceUseCase @Inject constructor() {

    fun generate(request: AthleteGuidanceRequest, lowConfidence: Boolean): AthleteGuidanceOutput.ActionGuidance {
        val training = when {
            request.recoveryScore < 45 || request.strainScore > 80 ->
                "Keep training easy today: favor zone-1 or mobility over high intensity."
            request.recoveryScore >= 70 && request.strainScore <= 65 ->
                "Proceed with the planned session, but cap load increases to a small step."
            else ->
                "Use a moderate session and stop early if effort drifts above target."
        }

        val recovery = when {
            request.sleepScore < 50 || request.stressScore > 70 ->
                "Prioritize recovery blocks: extra sleep opportunity, light walk, and breathing reset."
            else ->
                "Maintain regular recovery habits with hydration, wind-down routine, and mobility work."
        }

        val nutritionBase = when {
            request.energyBankScore < 45 ->
                "Increase fueling reliability: add a balanced carb-protein meal and fluids after activity."
            else ->
                "Keep nutrition steady with protein across meals and carbs matched to training demand."
        }

        val nutrition = if (lowConfidence) {
            "$nutritionBase Keep adjustments conservative until confidence improves."
        } else {
            nutritionBase
        }

        return AthleteGuidanceOutput.ActionGuidance(
            training = training,
            recovery = recovery,
            nutrition = nutrition,
        )
    }
}
