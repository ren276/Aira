package com.aira.health.ai.prompt

import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.model.BurnoutRiskProjection
import com.aira.health.domain.model.BurnoutRiskTier
import com.aira.health.domain.model.BurnoutTrajectory
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AthleteGuidancePromptAssemblerTest {

    private val assembler = AthleteGuidancePromptAssembler()

    @Test
    fun `guidance contract includes local daily context and practical output structure`() {
        val contract = assembler.assemble(sampleRequest())
        val chunks = contract.toChunks().joinToString("\n")

        assertTrue(chunks.contains("Recovery score"))
        assertTrue(chunks.contains("Strain score"))
        assertTrue(chunks.contains("Projected recovery delta"))
        assertTrue(contract.outputPolicy.contains("SUMMARY:"))
        assertTrue(contract.outputPolicy.contains("TRAINING:"))
        assertTrue(contract.outputPolicy.contains("NUTRITION:"))
    }

    @Test
    fun `low confidence prompt includes uncertainty wording and bounded recommendation policy`() {
        val lowConfidenceRequest = sampleRequest().copy(dataConfidence = 0.2f)

        val contract = assembler.assemble(lowConfidenceRequest)

        assertTrue(contract.lowConfidence)
        assertTrue(contract.localStateContext.contains("uncertainty", ignoreCase = true))
        assertTrue(contract.outputPolicy.contains("uncertainty", ignoreCase = true))
        assertTrue(contract.outputPolicy.contains("avoid definitive", ignoreCase = true))
    }

    @Test
    fun `assembler accepts only known local citation keys and rejects unsupported claims`() {
        val valid = assembler.assemble(sampleRequest().copy(rationaleSignalKeys = listOf("recovery_score", "stress_score")))
        assertEquals(listOf("recovery_score", "stress_score"), valid.citationKeys)

        val exception = runCatching {
            assembler.assemble(sampleRequest().copy(rationaleSignalKeys = listOf("recovery_score", "magic_signal")))
        }.exceptionOrNull()

        requireNotNull(exception)
        assertTrue(exception.message.orEmpty().contains("Unsupported citation keys"))
    }

    private fun sampleRequest(): AthleteGuidanceRequest = AthleteGuidanceRequest(
        date = "2026-04-18",
        recoveryScore = 74,
        sleepScore = 69,
        strainScore = 58,
        stressScore = 40,
        energyBankScore = 64,
        dataConfidence = 0.72f,
        predictionProjection = PredictionProjection(
            projectedRecoveryDelta = 3,
            projectedEnergyDelta = 2,
            confidenceTier = PredictionConfidenceTier.MEDIUM,
            confidenceScore = 0.63f,
            rationaleSignalKeys = listOf("recovery_score", "strain_score")
        ),
        burnoutProjection = BurnoutRiskProjection(
            tier = BurnoutRiskTier.MODERATE,
            trajectory = BurnoutTrajectory.STABLE,
            confidenceTier = PredictionConfidenceTier.MEDIUM,
            confidenceScore = 0.58f,
            rationaleSignalKeys = listOf("strain_score", "stress_score")
        ),
        rationaleSignalKeys = listOf("recovery_score", "strain_score", "data_confidence"),
        athleteNotes = "Felt heavy legs after intervals",
    )
}
