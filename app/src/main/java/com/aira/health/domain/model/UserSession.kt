package com.aira.health.domain.model

/**
 * Pure Kotlin domain model — NO Android framework imports allowed in this layer.
 */
data class UserSession(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val isGuest: Boolean,
    val isAuthenticated: Boolean
)

sealed class AuthState {
    object Loading : AuthState()
    object Guest : AuthState()
    data class Authenticated(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
}
