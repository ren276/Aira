package com.aira.health.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.util.permission.HealthConnectStatus
import com.aira.health.util.permission.HealthPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionUiState(
    val currentBatch: HealthPermissionManager.PermissionBatch = HealthPermissionManager.PermissionBatch.CORE,
    val showRationaleScreen: Boolean = true,
    val isLimitedModeSelected: Boolean = false,
    val healthConnectStatus: HealthConnectStatus = HealthConnectStatus.Available,
    val isCoreGranted: Boolean = false,
    val isBodyGranted: Boolean = false,
    val isAdvancedGranted: Boolean = false,
    val onboardingComplete: Boolean = false
)

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionManager: HealthPermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    init {
        checkHealthConnectAvailability()
    }

    private fun checkHealthConnectAvailability() {
        viewModelScope.launch {
            val status = permissionManager.getHealthConnectStatus()
            _uiState.value = _uiState.value.copy(healthConnectStatus = status)
        }
    }

    fun onBatchPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            val coreGranted = permissionManager.isCoreGranted()
            val bodyGranted = permissionManager.isBodyGranted()
            _uiState.value = _uiState.value.copy(
                isCoreGranted = coreGranted,
                isBodyGranted = bodyGranted,
                showRationaleScreen = false
            )
        }
    }

    /** User taps "Grant access" on the rationale screen — dismiss rationale, launch system dialog */
    fun onGrantAccessTapped() {
        _uiState.value = _uiState.value.copy(showRationaleScreen = false)
    }

    /** Core was denied — hard block. User can tap "Use limited mode" to proceed */
    fun onUseLimitedModeTapped() {
        _uiState.value = _uiState.value.copy(
            isLimitedModeSelected = true,
            onboardingComplete = true
        )
        // NOTE: NOT persisted. On next cold launch, permission flow shows again.
    }

    /** User granted Core and is ready to proceed to Body batch */
    fun advanceToBodyBatch() {
        _uiState.value = _uiState.value.copy(
            currentBatch = HealthPermissionManager.PermissionBatch.BODY,
            showRationaleScreen = true
        )
    }

    /** User granted/dismissed Body and is ready to proceed to Advanced batch */
    fun advanceToAdvancedBatch() {
        _uiState.value = _uiState.value.copy(
            currentBatch = HealthPermissionManager.PermissionBatch.ADVANCED,
            showRationaleScreen = true
        )
    }

    /** Skip Advanced batch — go to main app */
    fun skipAdvancedBatch() {
        _uiState.value = _uiState.value.copy(onboardingComplete = true)
    }

    fun getPermissionsForCurrentBatch(): Set<String> =
        permissionManager.getPermissionsForBatch(_uiState.value.currentBatch)
}
