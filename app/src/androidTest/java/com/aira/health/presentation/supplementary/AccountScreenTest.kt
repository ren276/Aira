package com.aira.health.presentation.supplementary

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.StravaConnectionState
import com.aira.health.domain.model.StravaSyncSummary
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.StravaRepository
import com.aira.health.domain.repository.UserRepository
import com.aira.health.presentation.theme.AiraTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun accountScreen_showsDisconnectControl_andInvokesDisconnect() {
        val userRepository = FakeUserRepository()
        val stravaRepository = FakeStravaRepository(
            initialConnectionState = StravaConnectionState(isConnected = true)
        )
        val viewModel = AccountViewModel(userRepository, stravaRepository)

        composeTestRule.setContent {
            AiraTheme {
                AccountScreen(
                    onNavigateBack = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Disconnect Strava")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        assertEquals(1, stravaRepository.disconnectCalls)
    }
}

private class FakeUserRepository : UserRepository {

    private val authState = MutableStateFlow<AuthState>(
        AuthState.Authenticated(
            UserSession(
                userId = "user-1",
                email = "user@aira.app",
                displayName = "User",
                avatarUrl = null,
                isGuest = false,
                isAuthenticated = true
            )
        )
    )

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<UserSession> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun signInWithGoogle(): Result<UserSession> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserSession> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun signInAsGuest(): Result<UserSession> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun signOut(): Result<Unit> = Result.success(Unit)

    override suspend fun upgradeGuestAccount(email: String, password: String): Result<UserSession> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun getCurrentSession(): UserSession? {
        return (authState.value as? AuthState.Authenticated)?.session
    }
}

private class FakeStravaRepository(
    initialConnectionState: StravaConnectionState
) : StravaRepository {

    private val state = MutableStateFlow(initialConnectionState)

    var disconnectCalls: Int = 0

    override fun observeConnectionState(): Flow<StravaConnectionState> = state

    override suspend fun createAuthorizationUrl(): Result<String> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun handleAuthorizationCallback(callbackUri: Uri): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun disconnect(): Result<Unit> {
        disconnectCalls += 1
        state.value = StravaConnectionState(isConnected = false, reconnectRequired = true)
        return Result.success(Unit)
    }

    override suspend fun syncActivities(maxPagesPerRun: Int): Result<StravaSyncSummary> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }
}
