package com.aira.health.ai.prompt

import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.model.PredictionConfidenceTier
import javax.inject.Inject

/**
 * Builds privacy-safe guidance prompt payloads from aggregate-only local state.
 */
class AthleteGuidancePromptAssembler @Inject constructor() {

    fun assemble(request: AthleteGuidanceRequest): AthleteGuidancePromptContract {
        request.validate()

        val citationKeys = normalizeCitationKeys(request.rationaleSignalKeys)
        val lowConfidence = request.confidenceTier() == PredictionConfidenceTier.LOW

        val context = buildString {
            appendLine("Date: ${request.date}")
            appendLine("Recovery score: ${request.recoveryScore}/100")
            appendLine("Sleep score: ${request.sleepScore}/100")
            appendLine("Strain score: ${request.strainScore}/100")
            appendLine("Stress score: ${request.stressScore}/100")
            appendLine("Energy bank score: ${request.energyBankScore}/100")
            appendLine("Data confidence: ${(request.dataConfidence * 100).toInt()}%")

            request.predictionProjection?.let { projection ->
                appendLine("Projected recovery delta: ${projection.projectedRecoveryDelta}")
                appendLine("Projected energy delta: ${projection.projectedEnergyDelta}")
                appendLine("Projection confidence tier: ${projection.confidenceTier}")
            }

            request.burnoutProjection?.let { burnout ->
                appendLine("Burnout tier: ${burnout.tier}")
                appendLine("Burnout trajectory: ${burnout.trajectory}")
            }

            if (citationKeys.isNotEmpty()) {
                appendLine("Citations: ${citationKeys.joinToString(",")}")
            }

            val safeNotes = request.athleteNotes?.let(::sanitizeNotes)
            if (!safeNotes.isNullOrBlank()) {
                appendLine("Athlete note: $safeNotes")
            }

            if (lowConfidence) {
                appendLine("Confidence policy: include uncertainty language and bounded recommendations.")
            }
        }.trim()

        val outputPolicy = if (lowConfidence) {
            AthleteGuidancePromptContract.OUTPUT_POLICY +
                " Include uncertainty wording and avoid definitive guarantees."
        } else {
            AthleteGuidancePromptContract.OUTPUT_POLICY
        }

        return AthleteGuidancePromptContract(
            systemGuidance = AthleteGuidancePromptContract.SYSTEM_GUIDANCE,
            localStateContext = context,
            outputPolicy = outputPolicy,
            citationKeys = citationKeys,
            lowConfidence = lowConfidence,
        )
    }

    private fun normalizeCitationKeys(rawKeys: List<String>): List<String> {
        if (rawKeys.isEmpty()) return DEFAULT_CITATION_KEYS

        val normalized = rawKeys
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

        val unsupported = normalized - AthleteGuidancePromptContract.ALLOWED_CITATION_KEYS
        require(unsupported.isEmpty()) {
            "Unsupported citation keys: ${unsupported.joinToString(",")}" +
                ". Allowed keys: ${AthleteGuidancePromptContract.ALLOWED_CITATION_KEYS.sorted().joinToString(",")}"
        }

        return normalized
    }

    private fun sanitizeNotes(notes: String): String = notes
        .take(MAX_NOTE_CHARS)
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

    companion object {
        private const val MAX_NOTE_CHARS = 240
        private val DEFAULT_CITATION_KEYS: List<String> = listOf(
            "recovery_score",
            "strain_score",
            "sleep_score",
        )
    }
}
