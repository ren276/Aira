package com.aira.health.presentation.supplementary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.UserCorrectionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataCorrectionsUiState(
    val sleepCorrections: Int = 0,
    val hrvCorrections: Int = 0,
    val sleepBaselineLabel: String = "unknown",
    val hrvBaselineLabel: String = "unknown",
    val timelineMessage: String = "No correction timeline available yet."
)

@HiltViewModel
class DataCorrectionsViewModel @Inject constructor(
    private val userCorrectionDao: UserCorrectionDao,
    private val baselineDao: BaselineDao
) : ViewModel() {

    private val mutableState = MutableStateFlow(DataCorrectionsUiState())
    val uiState: StateFlow<DataCorrectionsUiState> = mutableState.asStateFlow()

    init {
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

            mutableState.update {
                it.copy(
                    sleepCorrections = sleepCorrections,
                    hrvCorrections = hrvCorrections,
                    sleepBaselineLabel = sleepBaseline?.let { value -> "${value.toInt()} score" } ?: "not set",
                    hrvBaselineLabel = hrvBaseline?.let { value -> "${value.toInt()} ms" } ?: "not set",
                    timelineMessage = timelineMessage
                )
            }
        }
    }
}
