package com.aira.health.data.remote.supabase

import com.aira.health.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Provides the Supabase client singleton.
 * In guest mode, this is never initialised — no network calls are made.
 * URL and key come from BuildConfig flavor fields set in build.gradle.kts.
 */
object SupabaseClientProvider {

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                // Token stored in EncryptedSharedPreferences via Supabase SDK default
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
            install(Realtime) {
                // Real-time channels disabled in Phase 1 — activated in Phase 4
            }
            install(Storage)
        }
    }
}
