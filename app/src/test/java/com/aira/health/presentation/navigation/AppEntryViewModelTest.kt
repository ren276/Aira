package com.aira.health.presentation.navigation

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.StravaConnectionState
import com.aira.health.domain.model.StravaSyncSummary
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.StravaRepository
import com.aira.health.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class AppEntryViewModelTest {

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
    fun unauthenticatedUserKeepsAuthStepIncomplete() = runTest {
        val repository = FakeUserRepository(initialAuthState = AuthState.Guest)
        val stravaRepository = FakeStravaRepository()
        val viewModel = AppEntryViewModel(createDataStore(), repository, stravaRepository)

        val state = viewModel.awaitReadyState()

        assertFalse(state.onboardingCompleted)
        assertFalse(state.authStepCompleted)
        assertNull(state.authErrorMessage)
    }

    @Test
    fun googleIdTokenSuccessClearsErrorAndCompletesAuthStep() = runTest {
        val repository = FakeUserRepository(initialAuthState = AuthState.Guest)
        repository.googleIdTokenResult = Result.success(authenticatedSession())
        val stravaRepository = FakeStravaRepository()
        val viewModel = AppEntryViewModel(createDataStore(), repository, stravaRepository)

        viewModel.signInWithGoogleIdToken("token")
        advanceUntilIdle()

        val state = viewModel.awaitReadyState()

        assertFalse(state.authInProgress)
        assertNull(state.authErrorMessage)
        assertTrue(state.authStepCompleted)
        assertTrue(state.authState is AuthState.Authenticated)
    }

    @Test
    fun googleIdTokenFailureExposesErrorAndKeepsAuthStepIncomplete() = runTest {
        val repository = FakeUserRepository(initialAuthState = AuthState.Guest)
        repository.googleIdTokenResult = Result.failure(IllegalStateException("Bad token"))
        val stravaRepository = FakeStravaRepository()
        val viewModel = AppEntryViewModel(createDataStore(), repository, stravaRepository)

        viewModel.signInWithGoogleIdToken("bad")
        advanceUntilIdle()

        val state = viewModel.awaitReadyState()

        assertFalse(state.authInProgress)
        assertEquals("Bad token", state.authErrorMessage)
        assertFalse(state.authStepCompleted)
        assertTrue(state.authState is AuthState.Guest)
    }

    @Test
    fun continueAsGuestMarksAuthStepCompleted() = runTest {
        val repository = FakeUserRepository(initialAuthState = AuthState.Guest)
        repository.guestResult = Result.success(guestSession())
        val stravaRepository = FakeStravaRepository()
        val viewModel = AppEntryViewModel(createDataStore(), repository, stravaRepository)

        viewModel.continueAsGuest()
        advanceUntilIdle()

        val state = viewModel.awaitReadyState()

        assertFalse(state.authInProgress)
        assertNull(state.authErrorMessage)
        assertTrue(state.authStepCompleted)
    }

    @Test
    fun completeOnboardingPersistsCompletionFlag() = runTest {
        val repository = FakeUserRepository(initialAuthState = AuthState.Guest)
        val stravaRepository = FakeStravaRepository(
            initialConnectionState = StravaConnectionState(isConnected = true)
        )
        val viewModel = AppEntryViewModel(createDataStore(), repository, stravaRepository)

        viewModel.completeOnboarding()
        advanceUntilIdle()

        val state = viewModel.uiState
            .filter { !it.loading && it.onboardingCompleted }
            .first()

        assertTrue(state.onboardingCompleted)
    }

    @Test
    fun startStravaConnectionPublishesPendingAuthUrl() = runTest {
        val repository = FakeUserRepository(initialAuthState = AuthState.Guest)
        val stravaRepository = FakeStravaRepository().apply {
            createAuthorizationUrlResult = Result.success("aira://example")
        }
        val viewModel = AppEntryViewModel(createDataStore(), repository, stravaRepository)

        viewModel.startStravaConnection()
        advanceUntilIdle()

        val state = viewModel.awaitReadyState()
        assertEquals("aira://example", state.pendingStravaAuthUrl)
        assertEquals(null, state.stravaErrorMessage)
        assertFalse(state.stravaAuthInProgress)
    }

    private suspend fun AppEntryViewModel.awaitReadyState(): AppEntryUiState {
        return uiState
            .filter { !it.loading }
            .first()
    }

    private fun createDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { Files.createTempFile("app-entry", ".preferences_pb").toFile() }
        )
    }

    private fun authenticatedSession(): UserSession {
        return UserSession(
            userId = "user-1",
            email = "user@aira.app",
            displayName = "User",
            avatarUrl = null,
            isGuest = false,
            isAuthenticated = true
        )
    }

    private fun guestSession(): UserSession {
        return UserSession(
            userId = "guest-1",
            email = null,
            displayName = "Guest",
            avatarUrl = null,
            isGuest = true,
            isAuthenticated = false
        )
    }
}

private class FakeUserRepository(
    initialAuthState: AuthState
) : UserRepository {

    private val authState = MutableStateFlow(initialAuthState)

    var googleIdTokenResult: Result<UserSession> = Result.success(
        UserSession(
            userId = "user-1",
            email = "user@aira.app",
            displayName = "User",
            avatarUrl = null,
            isGuest = false,
            isAuthenticated = true
        )
    )

    var guestResult: Result<UserSession> = Result.success(
        UserSession(
            userId = "guest-1",
            email = null,
            displayName = "Guest",
            avatarUrl = null,
            isGuest = true,
            isAuthenticated = false
        )
    )

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<UserSession> {
        googleIdTokenResult.getOrNull()?.let { session ->
            authState.value = AuthState.Authenticated(session)
        }
        return googleIdTokenResult
    }

    override suspend fun signInWithGoogle(): Result<UserSession> {
        return signInWithGoogleIdToken("legacy")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> {
        return googleIdTokenResult
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserSession> {
        return googleIdTokenResult
    }

    override suspend fun signInAsGuest(): Result<UserSession> {
        guestResult.getOrNull()?.let {
            authState.value = AuthState.Guest
        }
        return guestResult
    }

    override suspend fun signOut(): Result<Unit> {
        authState.value = AuthState.Guest
        return Result.success(Unit)
    }

    override suspend fun upgradeGuestAccount(email: String, password: String): Result<UserSession> {
        val upgraded = UserSession(
            userId = "user-2",
            email = email,
            displayName = "Upgraded",
            avatarUrl = null,
            isGuest = false,
            isAuthenticated = true
        )
        authState.value = AuthState.Authenticated(upgraded)
        return Result.success(upgraded)
    }

    override suspend fun getCurrentSession(): UserSession? {
        return (authState.value as? AuthState.Authenticated)?.session
    }
}

private class FakeStravaRepository(
    initialConnectionState: StravaConnectionState = StravaConnectionState()
) : StravaRepository {

    private val state = MutableStateFlow(initialConnectionState)

    var createAuthorizationUrlResult: Result<String> = Result.failure(
        IllegalStateException("not configured")
    )
    var handleCallbackResult: Result<Unit> = Result.success(Unit)
    var disconnectResult: Result<Unit> = Result.success(Unit)
    var syncResult: Result<StravaSyncSummary> = Result.success(
        StravaSyncSummary(
            insertedCount = 0,
            skippedCount = 0,
            pagesFetched = 0,
            backfillComplete = false
        )
    )

    override fun observeConnectionState(): Flow<StravaConnectionState> = state

    override suspend fun createAuthorizationUrl(): Result<String> = createAuthorizationUrlResult

    override suspend fun handleAuthorizationCallback(callbackUri: Uri): Result<Unit> {
        return handleCallbackResult
    }

    override suspend fun disconnect(): Result<Unit> = disconnectResult

    override suspend fun syncActivities(maxPagesPerRun: Int): Result<StravaSyncSummary> {
        return syncResult
    }
}
