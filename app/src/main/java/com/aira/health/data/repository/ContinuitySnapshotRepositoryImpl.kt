package com.aira.health.data.repository

import com.aira.health.domain.model.ContinuitySnapshot
import com.aira.health.domain.repository.ContinuitySnapshotRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContinuitySnapshotRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : ContinuitySnapshotRepository {

    private val snapshotsRef get() = firebaseDatabase.getReference("continuity_snapshots")

    override suspend fun uploadSnapshot(userId: String, snapshot: ContinuitySnapshot): Result<Unit> = runCatching {
        val key = "$userId/${snapshot.snapshotId}"
        snapshotsRef.child(key).setValue(snapshot).await()
    }

    override suspend fun getLatestSnapshot(userId: String): Result<ContinuitySnapshot?> = runCatching {
        val userRef = snapshotsRef.child(userId)
        val dataSnapshot = userRef.orderByChild("capturedAtEpochMs").limitToLast(1).get().await()
        dataSnapshot.children.firstOrNull()?.getValue(ContinuitySnapshot::class.java)
    }
}