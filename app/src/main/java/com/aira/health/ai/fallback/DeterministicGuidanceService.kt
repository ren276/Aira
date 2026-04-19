package com.aira.health.ai.fallback

import com.aira.health.ai.prompt.AthleteGuidancePromptContract
import com.aira.health.domain.model.AthleteGuidanceOutput
import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.usecase.GenerateActionGuidanceUseCase
import com.aira.health.domain.usecase.GenerateDailyAthleteSummaryUseCase
import javax.inject.Inject

/**
 * Local deterministic fallback for athlete guidance generation.
 */
class DeterministicGuidanceService @Inject constructor(
    private val generateDailyAthleteSummaryUseCase: GenerateDailyAthleteSummaryUseCase,
    private val generateActionGuidanceUseCase: GenerateActionGuidanceUseCase,
) {

    fun build(
        request: AthleteGuidanceRequest,
        reason: GuidanceFallbackReason,
        citations: List<String>,
    ): AthleteGuidanceOutput {
        val lowConfidence = reason == GuidanceFallbackReason.LOW_CONFIDENCE ||
            request.confidenceTier() == com.aira.health.domain.model.PredictionConfidenceTier.LOW

        val summary = generateDailyAthleteSummaryUseCase.generate(request, lowConfidence)
        val actions = generateActionGuidanceUseCase.generate(request, lowConfidence)

        return AthleteGuidanceOutput(
            summary = summary,
            actions = actions,
            confidenceTier = request.confidenceTier(),
            confidenceScore = request.dataConfidence,
            citations = sanitizeCitations(citations),
            uncertaintyNote = uncertaintyNote(reason, lowConfidence),
            usedDeterministicFallback = true,
        )
    }

    private fun uncertaintyNote(reason: GuidanceFallbackReason, lowConfidence: Boolean): String? {
        if (!lowConfidence && reason == GuidanceFallbackReason.RUNTIME_FAILURE) {
            return "Generated locally because runtime output was unavailable."
        }
        return when (reason) {
            GuidanceFallbackReason.POLICY_BLOCKED ->
                "Generated locally to preserve privacy: network-backed runtime is blocked for coaching."
            GuidanceFallbackReason.LOW_CONFIDENCE ->
                "Confidence is low, so recommendations are intentionally conservative."
            GuidanceFallbackReason.RUNTIME_FAILURE ->
                if (lowConfidence) {
                    "Runtime unavailable and confidence is limited; use these actions as a cautious baseline."
                } else {
                    null
                }
        }
    }

    private fun sanitizeCitations(citations: List<String>): List<String> {
        val normalized = citations
            .map { it.trim().lowercase() }
            .filter { it in AthleteGuidancePromptContract.ALLOWED_CITATION_KEYS }
            .distinct()
        return if (normalized.isEmpty()) {
            listOf("recovery_score", "strain_score", "sleep_score")
        } else {
            normalized
        }
    }
}

enum class GuidanceFallbackReason {
    POLICY_BLOCKED,
    LOW_CONFIDENCE,
    RUNTIME_FAILURE,
}
