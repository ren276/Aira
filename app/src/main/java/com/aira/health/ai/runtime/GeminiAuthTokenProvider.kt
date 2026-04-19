package com.aira.health.ai.runtime

import com.aira.health.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the Gemini API key directly from local properties.
 *
 * In production, this should ideally be moved back to a trusted backend endpoint
 * once a replacement for the Supabase Edge Function is established.
 */
interface GeminiAuthTokenProvider {
    suspend fun getToken(): String
}

@Singleton
class LocalGeminiAuthTokenProvider @Inject constructor() : GeminiAuthTokenProvider {
    override suspend fun getToken(): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException("GEMINI_API_KEY is not configured in local.properties")
        }
        return apiKey
    }
}
