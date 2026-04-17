package com.aira.health.data.strava

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.aira.health.domain.model.StravaConnectionState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class StravaConnectionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private companion object {
        val STRAVA_CONNECTED = booleanPreferencesKey("strava_connected")
        val STRAVA_RECONNECT_REQUIRED = booleanPreferencesKey("strava_reconnect_required")
        val STRAVA_ATHLETE_ID = longPreferencesKey("strava_athlete_id")
        val STRAVA_LAST_SYNC_EPOCH_MS = longPreferencesKey("strava_last_sync_epoch_ms")
        val STRAVA_PENDING_STATE = stringPreferencesKey("strava_oauth_pending_state")
        val STRAVA_BACKFILL_COMPLETE = booleanPreferencesKey("strava_backfill_complete")
        val STRAVA_BACKFILL_BEFORE_EPOCH_MS = longPreferencesKey("strava_backfill_before_epoch_ms")
        val STRAVA_INCREMENTAL_AFTER_EPOCH_MS = longPreferencesKey("strava_incremental_after_epoch_ms")
        val STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS = longPreferencesKey("strava_sync_defer_until_epoch_ms")
        val STRAVA_LAST_SYNC_ERROR_CODE = longPreferencesKey("strava_last_sync_error_code")
        val STRAVA_LAST_SYNC_ERROR_MESSAGE = stringPreferencesKey("strava_last_sync_error_message")
        val STRAVA_HEALTH_CONNECT_MIRROR_ENABLED =
            booleanPreferencesKey("strava_health_connect_mirror_enabled")
    }

    fun observeConnectionState(): Flow<StravaConnectionState> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                StravaConnectionState(
                    isConnected = prefs[STRAVA_CONNECTED] ?: false,
                    reconnectRequired = prefs[STRAVA_RECONNECT_REQUIRED] ?: false,
                    athleteId = prefs[STRAVA_ATHLETE_ID],
                    lastSyncEpochMs = prefs[STRAVA_LAST_SYNC_EPOCH_MS],
                    deferredSyncUntilEpochMs = prefs[STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS],
                    lastSyncErrorCode = prefs[STRAVA_LAST_SYNC_ERROR_CODE]?.toInt(),
                    lastSyncErrorMessage = prefs[STRAVA_LAST_SYNC_ERROR_MESSAGE]
                )
            }
    }

    suspend fun setPendingOAuthState(state: String) {
        dataStore.edit { prefs ->
            prefs[STRAVA_PENDING_STATE] = state
        }
    }

    suspend fun consumePendingOAuthState(): String? {
        var pending: String? = null
        dataStore.edit { prefs ->
            pending = prefs[STRAVA_PENDING_STATE]
            prefs.remove(STRAVA_PENDING_STATE)
        }
        return pending
    }

    suspend fun markConnected(athleteId: Long) {
        dataStore.edit { prefs ->
            prefs[STRAVA_CONNECTED] = true
            prefs[STRAVA_RECONNECT_REQUIRED] = false
            prefs[STRAVA_ATHLETE_ID] = athleteId
            prefs[STRAVA_BACKFILL_COMPLETE] = false
            prefs.remove(STRAVA_BACKFILL_BEFORE_EPOCH_MS)
            prefs.remove(STRAVA_INCREMENTAL_AFTER_EPOCH_MS)
            prefs.remove(STRAVA_LAST_SYNC_EPOCH_MS)
            prefs.remove(STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_CODE)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_MESSAGE)
        }
    }

    suspend fun markReconnectRequired() {
        dataStore.edit { prefs ->
            prefs[STRAVA_CONNECTED] = false
            prefs[STRAVA_RECONNECT_REQUIRED] = true
        }
    }

    suspend fun markDisconnected() {
        dataStore.edit { prefs ->
            prefs[STRAVA_CONNECTED] = false
            prefs[STRAVA_RECONNECT_REQUIRED] = true
            prefs.remove(STRAVA_ATHLETE_ID)
            prefs.remove(STRAVA_PENDING_STATE)
            prefs.remove(STRAVA_LAST_SYNC_EPOCH_MS)
            prefs.remove(STRAVA_BACKFILL_COMPLETE)
            prefs.remove(STRAVA_BACKFILL_BEFORE_EPOCH_MS)
            prefs.remove(STRAVA_INCREMENTAL_AFTER_EPOCH_MS)
            prefs.remove(STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_CODE)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_MESSAGE)
        }
    }

    suspend fun getSyncCursor(): StravaSyncCursor {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map {
                StravaSyncCursor(
                    backfillComplete = it[STRAVA_BACKFILL_COMPLETE] ?: false,
                    backfillBeforeEpochMs = it[STRAVA_BACKFILL_BEFORE_EPOCH_MS],
                    incrementalAfterEpochMs = it[STRAVA_INCREMENTAL_AFTER_EPOCH_MS]
                )
            }
            .first()
    }

    suspend fun setBackfillProgress(nextBeforeEpochMs: Long?) {
        dataStore.edit { prefs ->
            if (nextBeforeEpochMs != null) {
                prefs[STRAVA_BACKFILL_BEFORE_EPOCH_MS] = nextBeforeEpochMs
            } else {
                prefs.remove(STRAVA_BACKFILL_BEFORE_EPOCH_MS)
            }
        }
    }

    suspend fun markBackfillComplete(incrementalAfterEpochMs: Long) {
        dataStore.edit { prefs ->
            prefs[STRAVA_BACKFILL_COMPLETE] = true
            prefs[STRAVA_INCREMENTAL_AFTER_EPOCH_MS] = incrementalAfterEpochMs
            prefs.remove(STRAVA_BACKFILL_BEFORE_EPOCH_MS)
        }
    }

    suspend fun updateIncrementalCursor(afterEpochMs: Long) {
        dataStore.edit { prefs ->
            prefs[STRAVA_INCREMENTAL_AFTER_EPOCH_MS] = afterEpochMs
        }
    }

    suspend fun updateLastSync(syncEpochMs: Long) {
        dataStore.edit { prefs ->
            prefs[STRAVA_LAST_SYNC_EPOCH_MS] = syncEpochMs
            prefs.remove(STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_CODE)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_MESSAGE)
        }
    }

    suspend fun getSyncDeferredUntilEpochMs(): Long? {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS] }
            .first()
    }

    suspend fun deferSyncUntil(
        deferredUntilEpochMs: Long,
        errorCode: Int? = null,
        errorMessage: String? = null
    ) {
        dataStore.edit { prefs ->
            prefs[STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS] = deferredUntilEpochMs
            if (errorCode != null) {
                prefs[STRAVA_LAST_SYNC_ERROR_CODE] = errorCode.toLong()
            } else {
                prefs.remove(STRAVA_LAST_SYNC_ERROR_CODE)
            }
            if (!errorMessage.isNullOrBlank()) {
                prefs[STRAVA_LAST_SYNC_ERROR_MESSAGE] = errorMessage
            } else {
                prefs.remove(STRAVA_LAST_SYNC_ERROR_MESSAGE)
            }
        }
    }

    suspend fun clearSyncDeferral() {
        dataStore.edit { prefs ->
            prefs.remove(STRAVA_SYNC_DEFER_UNTIL_EPOCH_MS)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_CODE)
            prefs.remove(STRAVA_LAST_SYNC_ERROR_MESSAGE)
        }
    }

    suspend fun isHealthConnectMirrorEnabled(): Boolean {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[STRAVA_HEALTH_CONNECT_MIRROR_ENABLED] ?: false }
            .first()
    }

    suspend fun setHealthConnectMirrorEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[STRAVA_HEALTH_CONNECT_MIRROR_ENABLED] = enabled
        }
    }
}
