package com.aira.health.presentation.navigation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppEntryUiState(
    val onboardingCompleted: Boolean = false,
    val forceOledDarkTheme: Boolean? = null,
    val authState: AuthState = AuthState.Loading,
    val authStepCompleted: Boolean = false,
    val authInProgress: Boolean = false,
    val authErrorMessage: String? = null,
    val loading: Boolean = true
)

private data class AuthActionState(
    val inProgress: Boolean = false,
    val guestAccessGranted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AppEntryViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val userRepository: UserRepository
) : ViewModel() {

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FORCE_OLED_DARK_THEME = booleanPreferencesKey("force_oled_dark_theme")
    }

    private val authActionState = MutableStateFlow(AuthActionState())

    private val appPrefs = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            Pair(
                prefs[ONBOARDING_COMPLETED] ?: false,
                prefs[FORCE_OLED_DARK_THEME]
            )
        }

    private val authState = userRepository.observeAuthState()
        .catch { emit(AuthState.Error("Unable to read auth state")) }

    val uiState: StateFlow<AppEntryUiState> = combine(
        appPrefs,
        authState,
        authActionState
    ) { prefs, currentAuthState, actionState ->
        val onboardingCompleted = prefs.first
        val forceOledDarkTheme = prefs.second
        val authStepCompleted =
            currentAuthState is AuthState.Authenticated || actionState.guestAccessGranted

        AppEntryUiState(
            onboardingCompleted = onboardingCompleted,
            forceOledDarkTheme = forceOledDarkTheme,
            authState = currentAuthState,
            authStepCompleted = authStepCompleted,
            authInProgress = actionState.inProgress,
            authErrorMessage = actionState.errorMessage,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppEntryUiState()
    )

    fun signInWithGoogleIdToken(idToken: String) {
        authActionState.update { it.copy(inProgress = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.signInWithGoogleIdToken(idToken)
            authActionState.update {
                if (result.isSuccess) {
                    it.copy(inProgress = false, errorMessage = null)
                } else {
                    it.copy(
                        inProgress = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Google sign-in failed"
                    )
                }
            }
        }
    }

    fun setAuthErrorMessage(message: String) {
        authActionState.update { it.copy(inProgress = false, errorMessage = message) }
    }

    fun signInWithEmail(email: String, password: String) {
        authActionState.update { it.copy(inProgress = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.signInWithEmail(email, password)
            authActionState.update {
                if (result.isSuccess) {
                    it.copy(inProgress = false, errorMessage = null)
                } else {
                    it.copy(
                        inProgress = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Email sign-in failed"
                    )
                }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        authActionState.update { it.copy(inProgress = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.signUpWithEmail(email, password)
            authActionState.update {
                if (result.isSuccess) {
                    it.copy(inProgress = false, errorMessage = null)
                } else {
                    it.copy(
                        inProgress = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Email sign-up failed"
                    )
                }
            }
        }
    }

    fun continueAsGuest() {
        authActionState.update { it.copy(inProgress = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.signInAsGuest()
            authActionState.update {
                if (result.isSuccess) {
                    it.copy(
                        inProgress = false,
                        guestAccessGranted = true,
                        errorMessage = null
                    )
                } else {
                    it.copy(
                        inProgress = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Guest sign-in failed"
                    )
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[ONBOARDING_COMPLETED] = true
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
}
