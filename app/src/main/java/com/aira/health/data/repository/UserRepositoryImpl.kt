package com.aira.health.data.repository

import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val isGuestMode: Boolean = false
) : UserRepository {

    override fun observeAuthState(): Flow<AuthState> {
        if (isGuestMode) {
            return kotlinx.coroutines.flow.flowOf(AuthState.Guest)
        }
        return supabase.auth.sessionStatus.map(::mapSessionStatus)
    }

    internal fun mapSessionStatus(status: SessionStatus): AuthState {
        return when (status) {
            is SessionStatus.Authenticated -> {
                status.session.user?.let { user ->
                    AuthState.Authenticated(
                        session = UserSession(
                            userId = user.id,
                            email = user.email,
                            displayName = user.userMetadata?.get("full_name")?.toString(),
                            avatarUrl = user.userMetadata?.get("avatar_url")?.toString(),
                            isGuest = false,
                            isAuthenticated = true
                        )
                    )
                } ?: AuthState.Guest
            }
            is SessionStatus.NotAuthenticated -> AuthState.Guest
            is SessionStatus.LoadingFromStorage -> AuthState.Loading
            is SessionStatus.NetworkError -> AuthState.Error("Network error")
            is SessionStatus.Initializing -> AuthState.Loading
            is SessionStatus.RefreshFailure -> AuthState.Error("Refresh failed")
        }
    }

    override suspend fun signInWithGoogle(): Result<UserSession> = runCatching {
        supabase.auth.signInWith(Google)
        getCurrentSession() ?: throw IllegalStateException("No session after Google sign-in")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        getCurrentSession() ?: throw IllegalStateException("No session after email sign-in")
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserSession> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        getCurrentSession() ?: throw IllegalStateException("No session after email sign-up")
    }

    override suspend fun signInAsGuest(): Result<UserSession> = runCatching {
        // Guest mode: Supabase is never used. Return a local pseudo-session.
        UserSession(
            userId = "guest",
            email = null,
            displayName = "Guest",
            avatarUrl = null,
            isGuest = true,
            isAuthenticated = false
        )
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }

    override suspend fun upgradeGuestAccount(email: String, password: String): Result<UserSession> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // TODO Phase 4: trigger Room → Supabase data migration job
        getCurrentSession() ?: throw IllegalStateException("No session after account upgrade")
    }

    override suspend fun getCurrentSession(): UserSession? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return UserSession(
            userId = user.id,
            email = user.email,
            displayName = user.userMetadata?.get("full_name")?.toString(),
            avatarUrl = user.userMetadata?.get("avatar_url")?.toString(),
            isGuest = false,
            isAuthenticated = true
        )
    }
}
