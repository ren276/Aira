package com.aira.health.domain.usecase

import com.aira.health.data.local.db.AiraDatabase
import com.aira.health.data.local.model.PredictionCalibrationRecord
import javax.inject.Inject
import kotlin.math.abs

class RecordPredictionCalibrationUseCase @Inject constructor(
    private val airaDatabase: AiraDatabase
) {

    suspend fun recordCalibration(
        targetDate: String,
        observedRecoveryScore: Int,
        observedEnergyScore: Int,
        rollingWindowSize: Int = 7
    ): PredictionCalibrationRecord? {
        val simulation = airaDatabase.whatIfSimulationDao().getLatestForTargetDate(targetDate) ?: return null

        val observedRecoveryDelta = observedRecoveryScore - simulation.baselineRecoveryScore
        val observedEnergyDelta = observedEnergyScore - simulation.baselineEnergyScore

        val recoveryAbsoluteError = abs(simulation.projectedRecoveryDelta - observedRecoveryDelta)
        val energyAbsoluteError = abs(simulation.projectedEnergyDelta - observedEnergyDelta)

        val currentMae = (recoveryAbsoluteError + energyAbsoluteError) / 2f
        val boundedWindowSize = rollingWindowSize.coerceAtLeast(1)
        val previous = airaDatabase.predictionCalibrationDao().getRecent((boundedWindowSize - 1).coerceAtLeast(0))

        val rollingMae = (previous.map { (it.recoveryAbsoluteError + it.energyAbsoluteError) / 2f } + currentMae)
            .takeLast(boundedWindowSize)
            .average()
            .toFloat()

        val record = PredictionCalibrationRecord(
            targetDate = targetDate,
            predictedRecoveryDelta = simulation.projectedRecoveryDelta,
            observedRecoveryDelta = observedRecoveryDelta,
            recoveryAbsoluteError = recoveryAbsoluteError,
            predictedEnergyDelta = simulation.projectedEnergyDelta,
            observedEnergyDelta = observedEnergyDelta,
            energyAbsoluteError = energyAbsoluteError,
            rollingMeanAbsoluteError = rollingMae,
            recordedAt = System.currentTimeMillis()
        )

        airaDatabase.predictionCalibrationDao().upsert(record)
        return record
    }
}
