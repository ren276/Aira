package com.aira.health.di

import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.BackendGeminiAuthTokenProvider
import com.aira.health.ai.runtime.GeminiAuthTokenProvider
import com.aira.health.ai.runtime.GeminiCloudRuntimeGateway
import com.aira.health.ai.runtime.RuntimeConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing AI runtime dependencies.
 *
 * All bindings are singleton-scoped — one gateway, one config for the app lifetime.
 * Consumers inject [AiRuntimeGateway]; they never depend on the concrete
 * [GeminiCloudRuntimeGateway] type directly (AIM-01 adapter isolation).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiRuntimeModule {

    /**
     * Binds the concrete [GeminiCloudRuntimeGateway] to the [AiRuntimeGateway] interface.
     */
    @Binds
    @Singleton
    abstract fun bindAiRuntimeGateway(impl: GeminiCloudRuntimeGateway): AiRuntimeGateway

    @Binds
    @Singleton
    abstract fun bindGeminiAuthTokenProvider(impl: BackendGeminiAuthTokenProvider): GeminiAuthTokenProvider

    companion object {

        /**
         * Provides immutable [RuntimeConfig] defaults for the active runtime backend.
         * Override in tests via [dagger.hilt.android.testing.BindValue] or a test module.
         */
        @Provides
        @Singleton
        fun provideRuntimeConfig(): RuntimeConfig = RuntimeConfig()
    }
}
