package com.aira.health.presentation.supplementary

import android.net.Uri
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.StravaConnectionState
import com.aira.health.domain.model.StravaSyncSummary
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.StravaRepository
import com.aira.health.domain.repository.UserRepository
import com.aira.health.domain.usecase.ExecuteLocalResetUseCase
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

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
    fun disconnectStrava_success_callsRepositoryAndClearsError() = runTest {
        val userRepository = FakeUserRepository()
        val stravaRepository = FakeStravaRepository(
            initialConnectionState = StravaConnectionState(isConnected = true)
        )
        stravaRepository.disconnectResult = Result.success(Unit)
        val executeLocalResetUseCase = mockk<ExecuteLocalResetUseCase>(relaxed = true)

        val viewModel = AccountViewModel(userRepository, stravaRepository, executeLocalResetUseCase)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.disconnectStrava()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(1, stravaRepository.disconnectCalls)
        assertFalse(state.disconnectInProgress)
        assertNull(state.disconnectErrorMessage)

        collector.cancel()
    }

    @Test
    fun disconnectStrava_failure_surfacesErrorMessage() = runTest {
        val userRepository = FakeUserRepository()
        val stravaRepository = FakeStravaRepository(
            initialConnectionState = StravaConnectionState(isConnected = true)
        )
        stravaRepository.disconnectResult = Result.failure(IllegalStateException("disconnect failed"))
        val executeLocalResetUseCase = mockk<ExecuteLocalResetUseCase>(relaxed = true)

        val viewModel = AccountViewModel(userRepository, stravaRepository, executeLocalResetUseCase)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.disconnectStrava()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(1, stravaRepository.disconnectCalls)
        assertEquals("disconnect failed", state.disconnectErrorMessage)
        assertTrue(state.stravaConnected)

        collector.cancel()
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
    var disconnectResult: Result<Unit> = Result.success(Unit)

    override fun observeConnectionState(): Flow<StravaConnectionState> = state

    override suspend fun createAuthorizationUrl(): Result<String> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun handleAuthorizationCallback(callbackUri: Uri): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }

    override suspend fun disconnect(): Result<Unit> {
        disconnectCalls += 1
        if (disconnectResult.isSuccess) {
            state.value = StravaConnectionState(isConnected = false, reconnectRequired = true)
        }
        return disconnectResult
    }

    override suspend fun syncActivities(maxPagesPerRun: Int): Result<StravaSyncSummary> {
        return Result.failure(UnsupportedOperationException("Not needed for this test"))
    }
}
