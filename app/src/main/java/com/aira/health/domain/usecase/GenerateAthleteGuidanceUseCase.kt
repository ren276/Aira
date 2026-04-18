package com.aira.health.domain.usecase

import com.aira.health.ai.fallback.DeterministicGuidanceService
import com.aira.health.ai.fallback.GuidanceFallbackReason
import com.aira.health.ai.prompt.AthleteGuidancePromptAssembler
import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.AiRuntimeRequest
import com.aira.health.ai.runtime.AiRuntimePolicyGuard
import com.aira.health.domain.model.AthleteGuidanceOutput
import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.model.PredictionConfidenceTier
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

/**
 * Main orchestration for daily athlete guidance generation.
 *
 * Runtime generation is only used when local-only policy compliance is satisfied.
 * Otherwise this use case routes to deterministic local fallback output.
 */
class GenerateAthleteGuidanceUseCase @Inject constructor(
    private val aiRuntimeGateway: AiRuntimeGateway,
    private val runtimePolicyGuard: AiRuntimePolicyGuard,
    private val promptAssembler: AthleteGuidancePromptAssembler,
    private val deterministicGuidanceService: DeterministicGuidanceService,
) {

    suspend fun generate(request: AthleteGuidanceRequest): AthleteGuidanceOutput {
        request.validate()

        val policyDecision = runtimePolicyGuard.requireLocalOnly(aiRuntimeGateway)
        if (!policyDecision.allowed) {
            return deterministicGuidanceService.build(
                request = request,
                reason = GuidanceFallbackReason.POLICY_BLOCKED,
                citations = request.rationaleSignalKeys,
            )
        }

        if (request.confidenceTier() == PredictionConfidenceTier.LOW) {
            return deterministicGuidanceService.build(
                request = request,
                reason = GuidanceFallbackReason.LOW_CONFIDENCE,
                citations = request.rationaleSignalKeys,
            )
        }

        val contract = runCatching { promptAssembler.assemble(request) }
            .getOrElse {
                return deterministicGuidanceService.build(
                    request = request,
                    reason = GuidanceFallbackReason.RUNTIME_FAILURE,
                    citations = request.rationaleSignalKeys,
                )
            }

        val runtimeText = runCatching {
            collectRuntimeText(contract.toChunks())
        }.getOrElse {
            return deterministicGuidanceService.build(
                request = request,
                reason = GuidanceFallbackReason.RUNTIME_FAILURE,
                citations = contract.citationKeys,
            )
        }

        if (runtimeText.isBlank() || containsBlockedContent(runtimeText)) {
            return deterministicGuidanceService.build(
                request = request,
                reason = GuidanceFallbackReason.RUNTIME_FAILURE,
                citations = contract.citationKeys,
            )
        }

        val sections = parseSections(runtimeText)
        if (sections == null) {
            return deterministicGuidanceService.build(
                request = request,
                reason = GuidanceFallbackReason.RUNTIME_FAILURE,
                citations = contract.citationKeys,
            )
        }

        return AthleteGuidanceOutput(
            summary = sections.summary,
            actions = AthleteGuidanceOutput.ActionGuidance(
                training = sections.training,
                recovery = sections.recovery,
                nutrition = sections.nutrition,
            ),
            confidenceTier = request.confidenceTier(),
            confidenceScore = request.dataConfidence,
            citations = contract.citationKeys,
            uncertaintyNote = if (contract.lowConfidence) {
                "Confidence is low, so recommendations should be treated as directional."
            } else {
                null
            },
            usedDeterministicFallback = false,
        )
    }

    private suspend fun collectRuntimeText(promptChunks: List<String>): String {
        val buffer = StringBuilder()
        aiRuntimeGateway.generate(AiRuntimeRequest(promptChunks = promptChunks)).collect { response ->
            if (response.text.isNotBlank()) {
                if (buffer.isNotEmpty()) {
                    buffer.append('\n')
                }
                buffer.append(response.text.trim())
            }
        }
        return buffer.toString().trim()
    }

    private fun parseSections(rawText: String): GuidanceSections? {
        val fields = mutableMapOf<String, String>()

        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val colonIndex = line.indexOf(':')
                if (colonIndex <= 0) return@forEach
                val key = line.substring(0, colonIndex).trim().uppercase()
                val value = line.substring(colonIndex + 1).trim()
                if (value.isNotBlank()) {
                    fields[key] = value
                }
            }

        val summary = fields["SUMMARY"]
        val training = fields["TRAINING"]
        val recovery = fields["RECOVERY"]
        val nutrition = fields["NUTRITION"]

        if (summary.isNullOrBlank() || training.isNullOrBlank() || recovery.isNullOrBlank() || nutrition.isNullOrBlank()) {
            return null
        }

        val allParts = listOf(summary, training, recovery, nutrition)
        if (allParts.any { containsBlockedContent(it) }) {
            return null
        }

        return GuidanceSections(
            summary = summary,
            training = training,
            recovery = recovery,
            nutrition = nutrition,
        )
    }

    private fun containsBlockedContent(text: String): Boolean {
        val normalized = text.lowercase()
        return BLOCKED_TERMS.any { normalized.contains(it) } ||
            RAW_PAYLOAD_MARKERS.any { marker -> normalized.contains(marker.lowercase()) }
    }

    private data class GuidanceSections(
        val summary: String,
        val training: String,
        val recovery: String,
        val nutrition: String,
    )

    companion object {
        private val BLOCKED_TERMS: List<String> = listOf(
            "diagnos",
            "prescri",
            "medical treatment",
            "disease",
            "symptom",
        )

        private val RAW_PAYLOAD_MARKERS: List<String> = listOf(
            "healthrecordraw",
            "hrsample",
            "hrvsample",
            "sleepsession",
            "room row",
        )
    }
}
