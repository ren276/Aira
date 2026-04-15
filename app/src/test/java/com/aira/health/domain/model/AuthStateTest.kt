package com.aira.health.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AuthStateTest {

    @Test
    fun `authenticated state wraps the provided session`() {
        val session = UserSession(
            userId = "u1",
            email = "test@example.com",
            displayName = "Tester",
            avatarUrl = null,
            isGuest = false,
            isAuthenticated = true
        )

        val state = AuthState.Authenticated(session)

        assertEquals(session, state.session)
    }

    @Test
    fun `error state preserves message`() {
        val state = AuthState.Error("Network error")

        assertEquals("Network error", state.message)
    }

    @Test
    fun `loading and guest states are representable`() {
        assertNotNull(AuthState.Loading)
        assertNotNull(AuthState.Guest)
    }
}
