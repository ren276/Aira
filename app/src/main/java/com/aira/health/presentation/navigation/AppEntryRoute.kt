package com.aira.health.presentation.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.BuildConfig
import com.aira.health.R
import com.aira.health.domain.model.AuthState
import com.aira.health.presentation.onboarding.AuthOnboardingScreen
import com.aira.health.presentation.onboarding.PermissionBatchScreen
import com.aira.health.presentation.onboarding.StravaConnectScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

internal enum class AppEntryDestination {
    LOADING,
    MAIN_NAV,
    AUTH_ONBOARDING,
    STRAVA_ONBOARDING,
    PERMISSION_ONBOARDING
}

internal fun resolveAppEntryDestination(uiState: AppEntryUiState): AppEntryDestination {
    // 1. Wait for auth state to be resolved
    if (uiState.loading || uiState.authState == AuthState.Loading) {
        return AppEntryDestination.LOADING
    }
    // 2. Guard: no valid auth session → always go to onboarding (handles logout)
    if (!uiState.authStepCompleted) {
        return AppEntryDestination.AUTH_ONBOARDING
    }
    // 3. Strava reconnect required → show reconnect screen even for returning users
    if (uiState.stravaReconnectRequired) {
        return AppEntryDestination.STRAVA_ONBOARDING
    }
    // 4. Onboarding fully completed → go to main app
    if (uiState.onboardingCompleted) {
        return AppEntryDestination.MAIN_NAV
    }
    // 5. Still in onboarding flow
    if (!uiState.stravaConnected) {
        return AppEntryDestination.STRAVA_ONBOARDING
    }
    return AppEntryDestination.PERMISSION_ONBOARDING
}

@Composable
fun AppEntryRoute(
    modifier: Modifier = Modifier,
    viewModel: AppEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val activity = context as? Activity
    val resolvedDarkTheme = uiState.forceOledDarkTheme ?: isSystemInDarkTheme()
    val webClientId = context.getString(R.string.default_web_client_id)

    LaunchedEffect(activity?.intent?.dataString) {
        val callback = activity?.intent?.data ?: return@LaunchedEffect
        if (isStravaCallback(callback)) {
            viewModel.handleStravaAuthCallback(callback.toString())
            activity.intent = activity.intent?.apply { data = null }
        }
    }

    LaunchedEffect(uiState.pendingStravaAuthUrl) {
        val url = uiState.pendingStravaAuthUrl ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            appContext.startActivity(intent)
        }.onFailure {
            viewModel.setStravaErrorMessage("Unable to open Strava authorization")
        }
        viewModel.consumePendingStravaAuthUrl()
    }

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

            AppEntryDestination.STRAVA_ONBOARDING -> {
                StravaConnectScreen(
                    isLoading = uiState.stravaAuthInProgress,
                    reconnectRequired = uiState.stravaReconnectRequired,
                    errorMessage = uiState.stravaErrorMessage,
                    onConnectStrava = viewModel::startStravaConnection
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

private fun isStravaCallback(uri: Uri): Boolean {
    val configuredRedirect = BuildConfig.STRAVA_REDIRECT_URI
    if (configuredRedirect.isNotBlank() && uri.toString().startsWith(configuredRedirect)) {
        return true
    }
    return uri.getQueryParameter("code") != null || uri.getQueryParameter("error") != null
}
