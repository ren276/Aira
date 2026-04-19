package com.aira.health.presentation.supplementary

import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.StravaConnectionState
import com.aira.health.domain.model.StravaSyncSummary
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.StravaRepository
import com.aira.health.domain.repository.UserRepository
import com.aira.health.domain.usecase.ExecuteLocalResetUseCase
import com.aira.health.domain.usecase.LocalResetResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountResetFlowViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resetLocalData blocks on failed final upload and requires explicit override confirmation`() = runTest {
        val executeLocalResetUseCase = mockk<ExecuteLocalResetUseCase>()
        coEvery { executeLocalResetUseCase.invoke(allowIrreversibleOverride = false) } returns LocalResetResult.Blocked(
            "upload failed"
        )
        coEvery { executeLocalResetUseCase.invoke(allowIrreversibleOverride = true) } returns LocalResetResult.Completed

        val viewModel = AccountViewModel(
            userRepository = FakeResetUserRepository(),
            stravaRepository = FakeResetStravaRepository(),
            executeLocalResetUseCase = executeLocalResetUseCase
        )

        val collector = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.resetLocalData()
        advanceUntilIdle()

        val blockedState = viewModel.uiState.value
        assertTrue(blockedState.resetBlocked)
        assertFalse(blockedState.overrideConfirmationRequired)

        viewModel.requestIrreversibleOverrideConfirmation()
        advanceUntilIdle()

        val armedState = viewModel.uiState.value
        assertTrue(armedState.overrideConfirmationRequired)

        collector.cancel()
    }
}

private class FakeResetUserRepository : UserRepository {

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

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<UserSession> = Result.failure(UnsupportedOperationException())

    override suspend fun signInWithGoogle(): Result<UserSession> = Result.failure(UnsupportedOperationException())

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> = Result.failure(UnsupportedOperationException())

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserSession> = Result.failure(UnsupportedOperationException())

    override suspend fun signInAsGuest(): Result<UserSession> = Result.failure(UnsupportedOperationException())

    override suspend fun signOut(): Result<Unit> = Result.success(Unit)

    override suspend fun upgradeGuestAccount(email: String, password: String): Result<UserSession> = Result.failure(UnsupportedOperationException())

    override suspend fun getCurrentSession(): UserSession? = (authState.value as? AuthState.Authenticated)?.session
}

private class FakeResetStravaRepository : StravaRepository {
    override fun observeConnectionState(): Flow<StravaConnectionState> = MutableStateFlow(StravaConnectionState())

    override suspend fun createAuthorizationUrl(): Result<String> = Result.failure(UnsupportedOperationException())

    override suspend fun handleAuthorizationCallback(callbackUri: android.net.Uri): Result<Unit> = Result.failure(UnsupportedOperationException())

    override suspend fun disconnect(): Result<Unit> = Result.success(Unit)

    override suspend fun syncActivities(maxPagesPerRun: Int): Result<StravaSyncSummary> = Result.failure(UnsupportedOperationException())
}
