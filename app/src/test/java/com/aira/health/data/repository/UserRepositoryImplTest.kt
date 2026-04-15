package com.aira.health.data.repository

import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserRepositoryImplTest {

    @Test
    fun `signInAsGuest returns expected local guest session`() = runTest {
        val repository = UserRepositoryImpl(
            supabase = mockk<SupabaseClient>(relaxed = true),
            isGuestMode = true
        )

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
}
