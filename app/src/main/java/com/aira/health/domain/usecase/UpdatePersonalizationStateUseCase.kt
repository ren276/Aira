package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.CorrectionInfluenceDao
import com.aira.health.data.local.dao.PersonalizationStateDao
import com.aira.health.data.local.dao.UserCorrectionDao
import com.aira.health.data.local.model.CorrectionInfluenceState
import com.aira.health.data.local.model.PersonalizationState
import com.aira.health.domain.engine.CorrectionInfluenceEngine
import com.aira.health.domain.engine.PersonalizationUpdateEngine
import com.aira.health.domain.model.PersonalizationParameters
import javax.inject.Inject
import kotlin.math.max

class UpdatePersonalizationStateUseCase @Inject constructor(
    private val personalizationStateDao: PersonalizationStateDao,
    private val correctionInfluenceDao: CorrectionInfluenceDao,
    private val userCorrectionDao: UserCorrectionDao,
    private val personalizationUpdateEngine: PersonalizationUpdateEngine,
    private val correctionInfluenceEngine: CorrectionInfluenceEngine
) {

    suspend fun updateForDate(
        date: String,
        observed: PersonalizationParameters,
        usableDays: Int
    ): PersonalizationState {
        val previous = personalizationStateDao.getLatest()
        val previousParameters = if (previous == null) {
            observed
        } else {
            PersonalizationParameters(
                sleepNeedMinutes = previous.sleepNeedMinutes,
                recoverySpeed = previous.recoverySpeed,
                stressSensitivity = previous.stressSensitivity
            )
        }

        val decision = personalizationUpdateEngine.update(
            previous = previousParameters,
            observed = observed,
            usableDays = usableDays
        )

        if (!decision.applied) {
            val skipped = PersonalizationState(
                date = date,
                sleepNeedMinutes = previousParameters.sleepNeedMinutes,
                recoverySpeed = previousParameters.recoverySpeed,
                stressSensitivity = previousParameters.stressSensitivity,
                usableDays = usableDays,
                applied = false,
                skipReason = decision.skipReason?.name,
                correctionInfluenceApplied = 0f,
                updatedAt = System.currentTimeMillis()
            )
            personalizationStateDao.upsert(skipped)
            return skipped
        }

        val startDate = java.time.LocalDate.parse(date).minusDays(14).toString()
        val corrections = userCorrectionDao.getByDateRange(startDate = startDate, endDate = date)
        val influence = correctionInfluenceEngine.applyDecay(corrections = corrections, targetDate = date)

        val merged = decision.parameters.let { params ->
            PersonalizationParameters(
                sleepNeedMinutes = (params.sleepNeedMinutes * (1f + (influence.combinedByParameter["sleepNeed"] ?: 0f))).coerceAtLeast(1f),
                recoverySpeed = (params.recoverySpeed * (1f + (influence.combinedByParameter["recoverySpeed"] ?: 0f))).coerceAtLeast(0.1f),
                stressSensitivity = (params.stressSensitivity * (1f + (influence.combinedByParameter["stressSensitivity"] ?: 0f))).coerceAtLeast(0.1f)
            )
        }

        if (influence.details.isNotEmpty()) {
            correctionInfluenceDao.insertAll(
                influence.details.map { detail ->
                    CorrectionInfluenceState(
                        date = date,
                        parameterKey = detail.parameterKey,
                        sourceCorrectionId = detail.correctionId,
                        sourceFieldName = detail.sourceFieldName,
                        ageDays = detail.ageDays,
                        decayWeight = detail.decayWeight,
                        rawInfluence = detail.rawInfluence,
                        decayedInfluence = detail.decayedInfluence,
                        cappedInfluence = detail.cappedInfluence,
                        createdAt = detail.createdAt
                    )
                }
            )
        }

        val maxInfluence = influence.combinedByParameter.values.fold(0f) { acc, value ->
            max(acc, kotlin.math.abs(value))
        }

        val state = PersonalizationState(
            date = date,
            sleepNeedMinutes = merged.sleepNeedMinutes,
            recoverySpeed = merged.recoverySpeed,
            stressSensitivity = merged.stressSensitivity,
            usableDays = usableDays,
            applied = true,
            skipReason = null,
            correctionInfluenceApplied = maxInfluence,
            updatedAt = System.currentTimeMillis()
        )
        personalizationStateDao.upsert(state)
        return state
    }
}
