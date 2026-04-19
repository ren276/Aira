package com.aira.health.data.repository

import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : UserRepository {

    override fun observeAuthState(): Flow<AuthState> {
        return callbackFlow {
            trySend(mapFirebaseAuthState(firebaseAuth.currentUser))

            val listener = FirebaseAuth.AuthStateListener { auth ->
                trySend(mapFirebaseAuthState(auth.currentUser))
            }

            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    }

    private fun mapFirebaseAuthState(user: FirebaseUser?): AuthState {
        return user?.let {
            AuthState.Authenticated(session = mapFirebaseUser(it))
        } ?: AuthState.Guest
    }

    internal fun mapFirebaseUser(user: FirebaseUser): UserSession {
        return UserSession(
            userId = user.uid,
            email = user.email,
            displayName = user.displayName,
            avatarUrl = user.photoUrl?.toString(),
            isGuest = user.isAnonymous,
            isAuthenticated = !user.isAnonymous
        )
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<UserSession> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
        getCurrentSession() ?: throw IllegalStateException("No session after Google sign-in")
    }

    override suspend fun signInWithGoogle(): Result<UserSession> = runCatching {
        throw UnsupportedOperationException("Google sign-in requires ID token flow")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserSession> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        getCurrentSession() ?: throw IllegalStateException("No session after email sign-in")
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserSession> = runCatching {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        getCurrentSession() ?: throw IllegalStateException("No session after email sign-up")
    }

    override suspend fun signInAsGuest(): Result<UserSession> = runCatching {
        firebaseAuth.signInAnonymously().await()
        getCurrentSession() ?: throw IllegalStateException("No session after guest sign-in")
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
    }

    override suspend fun upgradeGuestAccount(email: String, password: String): Result<UserSession> = runCatching {
        val currentUser = firebaseAuth.currentUser
        val credential = EmailAuthProvider.getCredential(email, password)

        if (currentUser?.isAnonymous == true) {
            currentUser.linkWithCredential(credential).await()
        } else if (currentUser == null) {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        } else {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
        }

        // TODO Phase 4: trigger Room → Firebase Realtime Database data sync job
        getCurrentSession() ?: throw IllegalStateException("No session after account upgrade")
    }

    override suspend fun getCurrentSession(): UserSession? {
        val user = firebaseAuth.currentUser ?: return null
        return mapFirebaseUser(user)
    }
}
