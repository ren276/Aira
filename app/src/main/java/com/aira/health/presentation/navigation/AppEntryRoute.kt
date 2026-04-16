package com.aira.health.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.R
import com.aira.health.presentation.onboarding.AuthOnboardingScreen
import com.aira.health.presentation.onboarding.PermissionBatchScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

internal enum class AppEntryDestination {
    LOADING,
    MAIN_NAV,
    AUTH_ONBOARDING,
    PERMISSION_ONBOARDING
}

internal fun resolveAppEntryDestination(uiState: AppEntryUiState): AppEntryDestination {
    return when {
        uiState.loading -> AppEntryDestination.LOADING
        uiState.onboardingCompleted -> AppEntryDestination.MAIN_NAV
        !uiState.authStepCompleted -> AppEntryDestination.AUTH_ONBOARDING
        else -> AppEntryDestination.PERMISSION_ONBOARDING
    }
}

@Composable
fun AppEntryRoute(
    modifier: Modifier = Modifier,
    viewModel: AppEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val resolvedDarkTheme = uiState.forceOledDarkTheme ?: isSystemInDarkTheme()
    val webClientId = context.getString(R.string.default_web_client_id)

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                viewModel.setAuthErrorMessage("Google sign-in did not return a token")
            } else {
                viewModel.signInWithGoogleIdToken(idToken)
            }
        } catch (ex: ApiException) {
            viewModel.setAuthErrorMessage("Google sign-in failed (${ex.statusCode})")
        }
    }

    com.aira.health.presentation.theme.AiraTheme(oledDark = resolvedDarkTheme) {
        when (resolveAppEntryDestination(uiState)) {
            AppEntryDestination.LOADING -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            AppEntryDestination.MAIN_NAV -> {
                AiraNavHost(modifier = modifier)
            }

            AppEntryDestination.AUTH_ONBOARDING -> {
                AuthOnboardingScreen(
                    isLoading = uiState.authInProgress,
                    errorMessage = uiState.authErrorMessage,
                    onGoogleSignIn = {
                        if (webClientId.isBlank()) {
                            viewModel.setAuthErrorMessage("Google auth is not configured for this build")
                        } else {
                            val googleSignInClient = GoogleSignIn.getClient(
                                appContext,
                                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestIdToken(webClientId)
                                    .build()
                            )
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    onSignInWithEmail = viewModel::signInWithEmail,
                    onSignUpWithEmail = viewModel::signUpWithEmail,
                    onContinueAsGuest = viewModel::continueAsGuest
                )
            }

            AppEntryDestination.PERMISSION_ONBOARDING -> {
                PermissionBatchScreen(
                    onOnboardingComplete = {
                        viewModel.completeOnboarding()
                    }
                )
            }
        }
    }
}
