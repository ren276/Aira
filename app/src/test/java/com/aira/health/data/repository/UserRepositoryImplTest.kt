package com.aira.health.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserRepositoryImplTest {

    private fun createRepository(): UserRepositoryImpl {
        return UserRepositoryImpl(
            firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
        )
    }

    @Test
    fun `mapFirebaseUser maps anonymous firebase user to guest session`() {
        val repository = createRepository()
        val user = mockk<FirebaseUser>(relaxed = true)

        io.mockk.every { user.uid } returns "anon-123"
        io.mockk.every { user.email } returns null
        io.mockk.every { user.displayName } returns "Guest"
        io.mockk.every { user.photoUrl } returns null
        io.mockk.every { user.isAnonymous } returns true

        val session = repository.mapFirebaseUser(user)

        assertEquals("anon-123", session.userId)
        assertNull(session.email)
        assertEquals("Guest", session.displayName)
        assertNull(session.avatarUrl)
        assertTrue(session.isGuest)
        assertFalse(session.isAuthenticated)
    }

    @Test
    fun `mapFirebaseUser maps non-anonymous firebase user to authenticated session`() {
        val repository = createRepository()
        val user = mockk<FirebaseUser>(relaxed = true)

        io.mockk.every { user.uid } returns "user-42"
        io.mockk.every { user.email } returns "user@aira.app"
        io.mockk.every { user.displayName } returns "Aira User"
        io.mockk.every { user.photoUrl } returns null
        io.mockk.every { user.isAnonymous } returns false

        val session = repository.mapFirebaseUser(user)

        assertEquals("user-42", session.userId)
        assertEquals("user@aira.app", session.email)
        assertEquals("Aira User", session.displayName)
        assertFalse(session.isGuest)
        assertTrue(session.isAuthenticated)
    }
}
