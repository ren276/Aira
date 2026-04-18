package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.PersonalizationStateDao
import com.aira.health.data.local.dao.UserCorrectionDao
import com.aira.health.data.local.model.UserCorrection
import com.aira.health.domain.model.PersonalizationParameters
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

data class CorrectionInfluencePreview(
    val affectedParameter: String,
    val influenceWindowDays: Int,
    val influenceWindowLabel: String,
    val maxInfluenceCap: Float
)

data class ApplyUserCorrectionFeedbackRequest(
    val recordType: String,
    val recordDate: String,
    val fieldName: String,
    val originalValue: Float,
    val correctedValue: Float,
    val confirmed: Boolean
)

data class ApplyUserCorrectionFeedbackResult(
    val success: Boolean,
    val message: String,
    val correctionId: Long? = null,
    val affectedParameter: String? = null,
    val influenceWindowLabel: String? = null,
    val maxInfluenceCap: Float? = null
)

class ApplyUserCorrectionFeedbackUseCase @Inject constructor(
    private val userCorrectionDao: UserCorrectionDao,
    private val personalizationStateDao: PersonalizationStateDao,
    private val updatePersonalizationStateUseCase: UpdatePersonalizationStateUseCase
) {

    fun previewInfluence(recordType: String, fieldName: String): CorrectionInfluencePreview {
        val affectedParameter = when {
            fieldName.equals("sleep_score", ignoreCase = true) || recordType.equals("sleep", ignoreCase = true) -> "Sleep Need Baseline"
            fieldName.equals("hrv_rmssd", ignoreCase = true) || recordType.equals("hrv", ignoreCase = true) -> "Recovery Speed"
            else -> "Stress Sensitivity"
        }

        return CorrectionInfluencePreview(
            affectedParameter = affectedParameter,
            influenceWindowDays = 14,
            influenceWindowLabel = "next 14 days",
            maxInfluenceCap = 0.20f
        )
    }

    suspend fun applyCorrection(request: ApplyUserCorrectionFeedbackRequest): ApplyUserCorrectionFeedbackResult {
        if (!request.confirmed) {
            return ApplyUserCorrectionFeedbackResult(
                success = false,
                message = "Confirmation is required before applying this correction."
            )
        }

        if (!isWithinAcceptedRange(request.fieldName, request.correctedValue)) {
            return ApplyUserCorrectionFeedbackResult(
                success = false,
                message = "Correction value is outside accepted local safety bounds."
            )
        }

        val preview = previewInfluence(request.recordType, request.fieldName)
        val denominator = max(abs(request.originalValue), 1f)
        val confidenceDelta = ((request.correctedValue - request.originalValue) / denominator)
            .coerceIn(-preview.maxInfluenceCap, preview.maxInfluenceCap)

        val correctionId = userCorrectionDao.insert(
            UserCorrection(
                recordType = request.recordType,
                recordDate = request.recordDate,
                fieldName = request.fieldName,
                originalValue = request.originalValue,
                correctedValue = request.correctedValue,
                confidenceDelta = confidenceDelta,
                createdAt = System.currentTimeMillis(),
                synced = false
            )
        )

        runCatching {
            refreshPersonalizationState(request.recordDate)
        }

        return ApplyUserCorrectionFeedbackResult(
            success = true,
            message = "Correction saved. Future explanations will adapt over the next 14 days.",
            correctionId = correctionId,
            affectedParameter = preview.affectedParameter,
            influenceWindowLabel = preview.influenceWindowLabel,
            maxInfluenceCap = preview.maxInfluenceCap
        )
    }

    private suspend fun refreshPersonalizationState(date: String) {
        val latest = personalizationStateDao.getLatest()
        val observed = if (latest != null) {
            PersonalizationParameters(
                sleepNeedMinutes = latest.sleepNeedMinutes,
                recoverySpeed = latest.recoverySpeed,
                stressSensitivity = latest.stressSensitivity
            )
        } else {
            PersonalizationParameters(
                sleepNeedMinutes = 480f,
                recoverySpeed = 1f,
                stressSensitivity = 1f
            )
        }

        val usableDays = latest?.usableDays?.coerceAtLeast(7) ?: 7
        val targetDate = date.ifBlank { LocalDate.now().toString() }

        updatePersonalizationStateUseCase.updateForDate(
            date = targetDate,
            observed = observed,
            usableDays = usableDays
        )
    }

    private fun isWithinAcceptedRange(fieldName: String, value: Float): Boolean {
        if (!value.isFinite()) return false

        return when (fieldName.lowercase()) {
            "sleep_score" -> value in 0f..100f
            "hrv_rmssd" -> value in 0f..250f
            "stress_score" -> value in 0f..100f
            else -> value in 0f..300f
        }
    }
}