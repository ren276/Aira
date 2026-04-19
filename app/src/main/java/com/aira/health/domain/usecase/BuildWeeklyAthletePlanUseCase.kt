package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.model.BurnoutRiskProjection
import com.aira.health.domain.model.BurnoutRiskTier
import com.aira.health.domain.model.BurnoutTrajectory
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionProjection
import com.aira.health.domain.model.PredictionScenario
import com.aira.health.domain.model.WeeklyAthletePlanDraft
import java.util.Locale
import javax.inject.Inject

class BuildWeeklyAthletePlanUseCase @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    private val runWhatIfSimulationUseCase: RunWhatIfSimulationUseCase,
    private val generateAthleteGuidanceUseCase: GenerateAthleteGuidanceUseCase,
) {

    suspend fun build(scenario: PredictionScenario): WeeklyAthletePlanDraft {
        scenario.validate()

        val baseline = dailyMetricsDao.getLast14Days().firstOrNull()
            ?: error("Cannot build weekly plan without baseline daily metrics")

        val simulation = runWhatIfSimulationUseCase.runScenario(scenario)
        val confidenceTier = simulation.confidenceTier.toPredictionTier()

        val predictionProjection = PredictionProjection(
            projectedRecoveryDelta = simulation.projectedRecoveryDelta,
            projectedEnergyDelta = simulation.projectedEnergyDelta,
            confidenceTier = confidenceTier,
            confidenceScore = simulation.confidenceScore,
            rationaleSignalKeys = simulation.rationaleSignalKeys.toSignalKeys(),
        )

        val burnoutProjection = BurnoutRiskProjection(
            tier = simulation.projectedBurnoutTier.toBurnoutTier(),
            trajectory = simulation.projectedBurnoutTrajectory.toBurnoutTrajectory(),
            confidenceTier = confidenceTier,
            confidenceScore = simulation.confidenceScore,
            rationaleSignalKeys = simulation.rationaleSignalKeys.toSignalKeys(),
        )

        val guidanceRequest = AthleteGuidanceRequest(
            date = simulation.targetDate,
            recoveryScore = baseline.recoveryScore,
            sleepScore = baseline.sleepScore,
            strainScore = baseline.strainScore,
            stressScore = baseline.stressScore,
            energyBankScore = baseline.energyBankScore,
            dataConfidence = simulation.confidenceScore,
            predictionProjection = predictionProjection,
            burnoutProjection = burnoutProjection,
            rationaleSignalKeys = simulation.rationaleSignalKeys.toSignalKeys(),
        )

        val guidance = generateAthleteGuidanceUseCase.generate(guidanceRequest)

        val projectedLoadText = scenario.trainingLoadDeltaPercent.toSignedPercent()
        val recoveryDeltaText = simulation.projectedRecoveryDelta.toSignedScoreDelta()
        val energyDeltaText = simulation.projectedEnergyDelta.toSignedScoreDelta()

        val balanceSummary = buildString {
            append("Load shift ")
            append(projectedLoadText)
            append(" with projected recovery ")
            append(recoveryDeltaText)
            append(" and energy ")
            append(energyDeltaText)
            append(".")
            if (confidenceTier == PredictionConfidenceTier.LOW) {
                append(" Confidence is limited, so treat this as directional guidance.")
            }
        }

        val weeklyFocus = when {
            simulation.projectedRecoveryDelta >= 3 && scenario.trainingLoadDeltaPercent <= 10f -> {
                "Progress steadily with controlled load and keep recovery routines consistent."
            }
            simulation.projectedBurnoutTier.toBurnoutTier() == BurnoutRiskTier.HIGH ||
                simulation.projectedRecoveryDelta <= -3 -> {
                "Protect recovery first this week by reducing spikes and spacing hard sessions."
            }
            else -> {
                "Balance moderate load with proactive recovery blocks to stabilize readiness."
            }
        }

        val cautionNotes = buildList {
            add(
                "Burnout outlook: ${simulation.projectedBurnoutTier.lowercase(Locale.US)} risk with " +
                    "${simulation.projectedBurnoutTrajectory.lowercase(Locale.US)} trajectory."
            )
            guidance.uncertaintyNote?.let(::add)
            if (confidenceTier == PredictionConfidenceTier.LOW &&
                guidance.uncertaintyNote.isNullOrBlank()
            ) {
                add("Confidence is limited; keep recommendations conservative and non-diagnostic.")
            }
        }

        val uncertaintyLabel = guidance.uncertaintyNote ?: if (confidenceTier == PredictionConfidenceTier.LOW) {
            "Confidence is limited; guidance is directional and non-diagnostic."
        } else {
            null
        }

        return WeeklyAthletePlanDraft(
            targetDate = simulation.targetDate,
            scenario = scenario,
            projectedRecoveryDelta = simulation.projectedRecoveryDelta,
            projectedEnergyDelta = simulation.projectedEnergyDelta,
            projectedBurnoutTier = simulation.projectedBurnoutTier.toBurnoutTier(),
            projectedBurnoutTrajectory = simulation.projectedBurnoutTrajectory.toBurnoutTrajectory(),
            confidenceTier = confidenceTier,
            confidenceScore = simulation.confidenceScore,
            loadRecoveryBalanceSummary = balanceSummary,
            weeklyFocus = weeklyFocus,
            guidanceSummary = guidance.summary,
            priorityActions = listOf(
                guidance.actions.training,
                guidance.actions.recovery,
                guidance.actions.nutrition,
            ),
            cautionNotes = cautionNotes,
            citations = guidance.citations,
            uncertaintyLabel = uncertaintyLabel,
        )
    }

    private fun String.toSignalKeys(): List<String> {
        return split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun String.toPredictionTier(): PredictionConfidenceTier {
        return runCatching { PredictionConfidenceTier.valueOf(uppercase(Locale.US)) }
            .getOrDefault(PredictionConfidenceTier.LOW)
    }

    private fun String.toBurnoutTier(): BurnoutRiskTier {
        return runCatching { BurnoutRiskTier.valueOf(uppercase(Locale.US)) }
            .getOrDefault(BurnoutRiskTier.MODERATE)
    }

    private fun String.toBurnoutTrajectory(): BurnoutTrajectory {
        return runCatching { BurnoutTrajectory.valueOf(uppercase(Locale.US)) }
            .getOrDefault(BurnoutTrajectory.STABLE)
    }

    private fun Int.toSignedScoreDelta(): String {
        return if (this > 0) "+$this" else toString()
    }

    private fun Float.toSignedPercent(): String {
        val rounded = toInt()
        return if (rounded > 0) "+$rounded%" else "$rounded%"
    }
}
