package com.aira.health.di

import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.MediapipeRuntimeGateway
import com.aira.health.ai.runtime.RuntimeConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the on-device AI runtime dependencies.
 *
 * All bindings are singleton-scoped — one engine, one config for the app lifetime.
 * Consumers inject [AiRuntimeGateway]; they never depend on the concrete
 * [MediapipeRuntimeGateway] type directly (AIM-01 adapter isolation).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiRuntimeModule {

    /**
     * Binds the concrete [MediapipeRuntimeGateway] to the [AiRuntimeGateway] interface.
     * A future phase can swap the binding to a LiteRT-LM implementation here without
     * touching consumers.
     */
    @Binds
    @Singleton
    abstract fun bindAiRuntimeGateway(impl: MediapipeRuntimeGateway): AiRuntimeGateway

    companion object {

        /**
         * Provides an immutable [RuntimeConfig] with phase-locked defaults from AI-SPEC.
         * Override in tests via [dagger.hilt.android.testing.BindValue] or a test module.
         */
        @Provides
        @Singleton
        fun provideRuntimeConfig(): RuntimeConfig = RuntimeConfig()
    }
}
