package com.aira.health.domain.repository

import android.net.Uri
import com.aira.health.domain.model.StravaConnectionState
import com.aira.health.domain.model.StravaSyncSummary
import kotlinx.coroutines.flow.Flow

interface StravaRepository {
    fun observeConnectionState(): Flow<StravaConnectionState>

    suspend fun createAuthorizationUrl(): Result<String>

    suspend fun handleAuthorizationCallback(callbackUri: Uri): Result<Unit>

    suspend fun disconnect(): Result<Unit>

    suspend fun syncActivities(maxPagesPerRun: Int = 6): Result<StravaSyncSummary>
}
