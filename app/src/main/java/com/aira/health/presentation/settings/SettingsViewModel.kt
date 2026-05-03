package com.aira.health.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.repository.UserRepository
import com.aira.health.util.permission.HealthConnectStatus
import com.aira.health.util.permission.HealthPermissionManager
import com.aira.health.ai.runtime.AiRuntimeGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val forceOledDarkTheme: Boolean? = null,
    val loading: Boolean = true,
    val profileName: String = "Profile unavailable",
    val planStatus: String = "Plan status unavailable",
    val healthConnectSyncEnabled: Boolean = false,
    val cloudBackupEnabled: Boolean = false,
    val continuityResetPolicyLabel: String = "Local reset requires final continuity upload before wipe",
    val localModelStatus: String = "Local model status unavailable",
    val confidencePercent: Int? = null,
    val isSigningOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val dailyMetricsDao: DailyMetricsDao,
    private val userRepository: UserRepository,
    private val healthPermissionManager: HealthPermissionManager,
    private val aiRuntimeGateway: AiRuntimeGateway
) : ViewModel() {

    private companion object {
        val FORCE_OLED_DARK_THEME = booleanPreferencesKey("force_oled_dark_theme")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
    }

    private data class PermissionState(
        val coreGranted: Boolean = false,
        val status: HealthConnectStatus = HealthConnectStatus.NotInstalled
    )

    private val permissionState = MutableStateFlow(PermissionState())
    private val signActionState = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val coreGranted = runCatching { healthPermissionManager.isCoreGranted() }.getOrDefault(false)
            val status = healthPermissionManager.getHealthConnectStatus()
            permissionState.value = PermissionState(coreGranted = coreGranted, status = status)
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        dataStore.data
            .catch { emit(emptyPreferences()) },
        dailyMetricsDao.observeRecent(7),
        permissionState,
        userRepository.observeAuthState(),
        signActionState
    ) { prefs, recentMetrics, permissions, authState, isSigningOut ->
        val latest = recentMetrics.firstOrNull()
        val confidence = latest?.dataConfidence?.times(100f)?.toInt()?.coerceIn(0, 100)
        val profileName = when (authState) {
            is AuthState.Authenticated -> {
                authState.session.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: authState.session.email
                        ?.substringBefore('@')
                        ?.takeIf { it.isNotBlank() }
                    ?: "Signed-in user"
            }
            AuthState.Guest -> "Guest user"
            AuthState.Loading -> "Loading profile"
            is AuthState.Error -> "Profile unavailable"
        }
        val planStatus = when {
            authState is AuthState.Authenticated -> "Firebase account connected"
            authState is AuthState.Guest -> "Guest mode"
            authState is AuthState.Error -> "Auth issue detected"
            recentMetrics.size >= 7 -> "Baseline calibrated"
            recentMetrics.isEmpty() -> "Awaiting first sync"
            else -> "Calibrating (${recentMetrics.size}/7 days)"
        }
        val localModelStatus = when {
            confidence == null -> "Local model warming up"
            confidence >= 75 -> "Local model ready ($confidence% confidence)"
            confidence >= 40 -> "Local model learning ($confidence% confidence)"
            else -> "Local model low confidence ($confidence%)"
        }

        val syncEnabled = permissions.coreGranted && permissions.status == HealthConnectStatus.Available

        SettingsUiState(
            forceOledDarkTheme = prefs[FORCE_OLED_DARK_THEME],
            loading = false,
            profileName = profileName,
            planStatus = planStatus,
            healthConnectSyncEnabled = syncEnabled,
            cloudBackupEnabled = prefs[CLOUD_BACKUP_ENABLED] ?: false,
            continuityResetPolicyLabel = if ((prefs[CLOUD_BACKUP_ENABLED] ?: false)) {
                "Reset is blocked if final upload fails unless you confirm irreversible override"
            } else {
                "Enable cloud backup to preserve continuity snapshots before local reset"
            },
            localModelStatus = localModelStatus,
            confidencePercent = confidence,
            isSigningOut = isSigningOut
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun signOutWithContinuity() {
        if (signActionState.value) return
        signActionState.value = true

        viewModelScope.launch {
            try {
                // 1. Generate continuity summary using Gemini
                val recent = dailyMetricsDao.getLast14Days()
                if (recent.isNotEmpty()) {
                    val contextText = recent.joinToString("\n") { 
                         "Date: ${it.date}, HRV: ${it.hrvMorning}, Sleep: ${it.sleepScore}, Stress: ${it.stressScore}"
                    }
                    
                    val request = com.aira.health.ai.runtime.AiRuntimeRequest(
                        promptChunks = listOf(
                            "You are Aira Health Continuity Engine. Summarize the user's current physiological state for a session transition.",
                            "Focus on recent scores and any notable trends or goals. Limit to 3 concise sentences. No PII.",
                            "Context data:\n$contextText"
                        )
                    )

                    var summary = ""
                    aiRuntimeGateway.generate(request).collect { response ->
                        summary += response.text
                    }

                    if (summary.isNotBlank()) {
                        userRepository.saveLogoutSummary(summary)
                    }
                }
            } catch (e: Exception) {
                // Log and continue sign out anyway — don't block auth flow for summary failures
            } finally {
                userRepository.signOut()
                signActionState.value = false
            }
        }
    }

    fun setForceOledDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[FORCE_OLED_DARK_THEME] = enabled
            }
        }
    }

    fun setCloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[CLOUD_BACKUP_ENABLED] = enabled
            }
        }
    }
}
