package com.aira.health.data.strava

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StravaTokenStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private companion object {
        const val PREFS_NAME = "strava_tokens"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_SCOPE = "scope"
        const val KEY_ATHLETE_ID = "athlete_id"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun read(): StravaTokenSession? {
        val accessToken = securePrefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = securePrefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val expiresAt = securePrefs.getLong(KEY_EXPIRES_AT, 0L)
        val athleteId = securePrefs.getLong(KEY_ATHLETE_ID, -1L)

        if (expiresAt <= 0L || athleteId <= 0L) {
            return null
        }

        return StravaTokenSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSec = expiresAt,
            scope = securePrefs.getString(KEY_SCOPE, "") ?: "",
            athleteId = athleteId
        )
    }

    fun save(session: StravaTokenSession) {
        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSec)
            .putString(KEY_SCOPE, session.scope)
            .putLong(KEY_ATHLETE_ID, session.athleteId)
            .apply()
    }

    fun clear() {
        securePrefs.edit().clear().apply()
    }
}
