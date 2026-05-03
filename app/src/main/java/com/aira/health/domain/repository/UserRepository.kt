package com.aira.health.domain.repository

import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin interface — implementations live in the data layer.
 * No Android, Room, or Supabase imports here.
 */
interface UserRepository {
    /** Observe the current auth state reactively */
    fun observeAuthState(): Flow<AuthState>

    /** Sign in with Google using an ID token from Google Sign-In */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserSession>

    /** Legacy Google sign-in entry point kept for compatibility */
    suspend fun signInWithGoogle(): Result<UserSession>

    /** Sign in with email + password */
    suspend fun signInWithEmail(email: String, password: String): Result<UserSession>

    /** Create a new account with email + password */
    suspend fun signUpWithEmail(email: String, password: String): Result<UserSession>

    /** Start an anonymous guest session */
    suspend fun signInAsGuest(): Result<UserSession>

    /** Sign out the current user */
    suspend fun signOut(): Result<Unit>

    /** Upgrade guest to real account — migrate local data */
    suspend fun upgradeGuestAccount(email: String, password: String): Result<UserSession>

    /** Get the current session without observing */
    suspend fun getCurrentSession(): UserSession?

    /** Save a continuity summary before logout */
    suspend fun saveLogoutSummary(summary: String): Result<Unit>

    /** Get the latest continuity summary */
    suspend fun getLatestLogoutSummary(): String?
}
