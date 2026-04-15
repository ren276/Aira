package com.aira.health.data.repository

import com.aira.health.domain.model.AuthState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.status.SessionStatus
import io.mockk.mockk
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserRepositoryImplTest {

    private fun createRepository(isGuestMode: Boolean = false): UserRepositoryImpl {
        return UserRepositoryImpl(
            supabase = mockk<SupabaseClient>(relaxed = true),
            isGuestMode = isGuestMode
        )
    }

    @Test
    fun `signInAsGuest returns expected local guest session`() = runTest {
        val repository = createRepository(isGuestMode = true)

        val result = repository.signInAsGuest()

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        requireNotNull(session)

        assertEquals("guest", session.userId)
        assertEquals("Guest", session.displayName)
        assertNull(session.email)
        assertNull(session.avatarUrl)
        assertTrue(session.isGuest)
        assertFalse(session.isAuthenticated)
    }

    @Test
    fun `mapSessionStatus maps Initializing to Loading`() {
        val repository = createRepository()

        val result = repository.mapSessionStatus(SessionStatus.Initializing)

        assertInstanceOf(AuthState.Loading::class.java, result)
    }

    @Test
    fun `mapSessionStatus maps RefreshFailure to Error`() {
        val repository = createRepository()

        val status = mockk<SessionStatus.RefreshFailure>()
        val result = repository.mapSessionStatus(status)

        val error = assertInstanceOf(AuthState.Error::class.java, result)
        assertEquals("Refresh failed", error.message)
    }

    @Test
    fun `mapSessionStatus maps NotAuthenticated to Guest`() {
        val repository = createRepository()

        val result = repository.mapSessionStatus(SessionStatus.NotAuthenticated(isSignOut = false))

        assertInstanceOf(AuthState.Guest::class.java, result)
    }

    @Test
    fun `mapSessionStatus maps Authenticated with null user to Guest`() {
        val repository = createRepository()
        val authenticated = mockk<SessionStatus.Authenticated>()
        every { authenticated.session.user } returns null

        val result = repository.mapSessionStatus(authenticated)

        assertInstanceOf(AuthState.Guest::class.java, result)
    }
}
