package com.aira.health.presentation.supplementary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.UserCorrectionDao
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackRequest
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackUseCase
import com.aira.health.domain.usecase.CorrectionInfluencePreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CorrectionTarget(
    val recordType: String,
    val fieldName: String,
    val displayLabel: String
) {
    SLEEP(recordType = "sleep", fieldName = "sleep_score", displayLabel = "Sleep Score"),
    HRV(recordType = "hrv", fieldName = "hrv_rmssd", displayLabel = "HRV Baseline")
}

data class DataCorrectionsUiState(
    val sleepCorrections: Int = 0,
    val hrvCorrections: Int = 0,
    val sleepBaselineLabel: String = "unknown",
    val hrvBaselineLabel: String = "unknown",
    val timelineMessage: String = "No correction timeline available yet.",
    val selectedTarget: CorrectionTarget = CorrectionTarget.SLEEP,
    val preview: CorrectionInfluencePreview? = null,
    val originalValueInput: String = "",
    val correctedValueInput: String = "",
    val confirmationChecked: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccessMessage: String? = null,
    val submitErrorMessage: String? = null,
    val insufficientData: Boolean = false,
    val insufficientDataMessage: String = "We need at least 7 days of recent local data before ranking drivers. Keep syncing wearables and check back tomorrow."
)

@HiltViewModel
class DataCorrectionsViewModel @Inject constructor(
    private val userCorrectionDao: UserCorrectionDao,
    private val baselineDao: BaselineDao,
    private val applyUserCorrectionFeedbackUseCase: ApplyUserCorrectionFeedbackUseCase
) : ViewModel() {

    private val mutableState = MutableStateFlow(DataCorrectionsUiState())
    val uiState: StateFlow<DataCorrectionsUiState> = mutableState.asStateFlow()

    init {
        previewInfluence()
        refreshSummaryState()
    }

    fun selectTarget(target: CorrectionTarget) {
        mutableState.update {
            it.copy(
                selectedTarget = target,
                confirmationChecked = false,
                submitSuccessMessage = null,
                submitErrorMessage = null
            )
        }
        previewInfluence()
    }

    fun updateOriginalValue(value: String) {
        mutableState.update {
            it.copy(
                originalValueInput = value,
                submitSuccessMessage = null,
                submitErrorMessage = null
            )
        }
    }

    fun updateCorrectedValue(value: String) {
        mutableState.update {
            it.copy(
                correctedValueInput = value,
                submitSuccessMessage = null,
                submitErrorMessage = null
            )
        }
    }

    fun updateConfirmation(checked: Boolean) {
        mutableState.update {
            it.copy(
                confirmationChecked = checked,
                submitSuccessMessage = null,
                submitErrorMessage = null
            )
        }
    }

    fun previewInfluence() {
        val current = mutableState.value
        val preview = applyUserCorrectionFeedbackUseCase.previewInfluence(
            recordType = current.selectedTarget.recordType,
            fieldName = current.selectedTarget.fieldName
        )

        mutableState.update { it.copy(preview = preview) }
    }

    fun applyCorrection() {
        val snapshot = mutableState.value

        if (snapshot.insufficientData) {
            mutableState.update {
                it.copy(submitErrorMessage = "Not Enough Recent Data")
            }
            return
        }

        if (!snapshot.confirmationChecked) {
            mutableState.update {
                it.copy(submitErrorMessage = "Please confirm before applying this correction.")
            }
            return
        }

        val original = snapshot.originalValueInput.toFloatOrNull()
        val corrected = snapshot.correctedValueInput.toFloatOrNull()

        if (original == null || corrected == null) {
            mutableState.update {
                it.copy(submitErrorMessage = "Enter valid numeric values for both original and corrected data.")
            }
            return
        }

        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isSubmitting = true,
                    submitSuccessMessage = null,
                    submitErrorMessage = null
                )
            }

            val target = mutableState.value.selectedTarget
            val result = applyUserCorrectionFeedbackUseCase.applyCorrection(
                ApplyUserCorrectionFeedbackRequest(
                    recordType = target.recordType,
                    recordDate = LocalDate.now().toString(),
                    fieldName = target.fieldName,
                    originalValue = original,
                    correctedValue = corrected,
                    confirmed = true
                )
            )

            if (result.success) {
                refreshSummaryState(
                    successMessage = result.message,
                    resetForm = true
                )
            } else {
                mutableState.update {
                    it.copy(
                        isSubmitting = false,
                        submitErrorMessage = result.message
                    )
                }
            }
        }
    }

    private fun refreshSummaryState(successMessage: String? = null, resetForm: Boolean = false) {
        viewModelScope.launch {
            val sleepCorrections = runCatching { userCorrectionDao.getCountByType("sleep") }.getOrDefault(0)
            val hrvCorrections = runCatching { userCorrectionDao.getCountByType("hrv") }.getOrDefault(0)

            val sleepBaseline = runCatching { baselineDao.get("sleep_score")?.value }.getOrNull()
            val hrvBaseline = runCatching { baselineDao.get("hrv_rmssd")?.value }.getOrNull()

            val total = sleepCorrections + hrvCorrections
            val timelineMessage = if (total == 0) {
                "No user corrections recorded yet. Corrections will appear here after validated edits."
            } else {
                "Correction timeline contains $total validated correction(s)."
            }
            val insufficientData = sleepBaseline == null && hrvBaseline == null
            val preview = applyUserCorrectionFeedbackUseCase.previewInfluence(
                recordType = mutableState.value.selectedTarget.recordType,
                fieldName = mutableState.value.selectedTarget.fieldName
            )

            mutableState.update {
                it.copy(
                    sleepCorrections = sleepCorrections,
                    hrvCorrections = hrvCorrections,
                    sleepBaselineLabel = sleepBaseline?.let { value -> "${value.toInt()} score" } ?: "not set",
                    hrvBaselineLabel = hrvBaseline?.let { value -> "${value.toInt()} ms" } ?: "not set",
                    timelineMessage = timelineMessage,
                    preview = preview,
                    insufficientData = insufficientData,
                    isSubmitting = false,
                    submitSuccessMessage = successMessage,
                    submitErrorMessage = null,
                    confirmationChecked = if (resetForm) false else it.confirmationChecked,
                    originalValueInput = if (resetForm) "" else it.originalValueInput,
                    correctedValueInput = if (resetForm) "" else it.correctedValueInput
                )
            }
        }
    }
}
