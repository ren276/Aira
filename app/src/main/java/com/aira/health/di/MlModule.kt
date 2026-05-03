package com.aira.health.di

import android.content.Context
import com.aira.health.data.ml.PersonalizedWeightsStore
import com.aira.health.data.ml.RecoveryModelInference
import com.aira.health.data.ml.SleepModelInference
import com.aira.health.data.ml.StrainModelInference
import com.aira.health.data.ml.StressModelInference
import com.aira.health.data.ml.TFLiteModelLoader
import com.aira.health.domain.engine.HybridRecoveryEngine
import com.aira.health.domain.engine.HybridSleepEngine
import com.aira.health.domain.engine.HybridStrainEngine
import com.aira.health.domain.engine.HybridStressEngine
import com.aira.health.domain.engine.RecoveryEngine
import com.aira.health.domain.engine.SleepEngine
import com.aira.health.domain.engine.StrainEngine
import com.aira.health.domain.engine.StressEngine
import com.aira.health.domain.ml.PhysiologicalModelProxy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt DI bindings for Phase 12: Applied Physiological ML.
 *
 * Provides:
 *  - [TFLiteModelLoader]        — shared loader for all metric model files
 *  - [PersonalizedWeightsStore] — singleton on-device bias store
 *  - Named [PhysiologicalModelProxy] — one per metric (recovery, strain, stress, sleep)
 *  - [HybridRecoveryEngine], [HybridStrainEngine], [HybridStressEngine], [HybridSleepEngine]
 */
@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun provideTFLiteModelLoader(
        @ApplicationContext context: Context
    ): TFLiteModelLoader = TFLiteModelLoader(context)

    @Provides
    @Singleton
    fun providePersonalizedWeightsStore(
        @ApplicationContext context: Context
    ): PersonalizedWeightsStore = PersonalizedWeightsStore(context)

    // ── Per-metric model proxies ───────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("recovery")
    fun provideRecoveryProxy(
        @ApplicationContext context: Context,
        loader: TFLiteModelLoader,
        weightsStore: PersonalizedWeightsStore
    ): PhysiologicalModelProxy = RecoveryModelInference(context, loader, weightsStore)

    @Provides
    @Singleton
    @Named("strain")
    fun provideStrainProxy(
        @ApplicationContext context: Context,
        loader: TFLiteModelLoader,
        weightsStore: PersonalizedWeightsStore
    ): PhysiologicalModelProxy = StrainModelInference(context, loader, weightsStore)

    @Provides
    @Singleton
    @Named("stress")
    fun provideStressProxy(
        @ApplicationContext context: Context,
        loader: TFLiteModelLoader,
        weightsStore: PersonalizedWeightsStore
    ): PhysiologicalModelProxy = StressModelInference(context, loader, weightsStore)

    @Provides
    @Singleton
    @Named("sleep")
    fun provideSleepProxy(
        @ApplicationContext context: Context,
        loader: TFLiteModelLoader,
        weightsStore: PersonalizedWeightsStore
    ): PhysiologicalModelProxy = SleepModelInference(context, loader, weightsStore)

    // ── Hybrid engines ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideHybridRecoveryEngine(
        @Named("recovery") model: PhysiologicalModelProxy,
        heuristic: RecoveryEngine
    ): HybridRecoveryEngine = HybridRecoveryEngine(model, heuristic)

    @Provides
    @Singleton
    fun provideHybridStrainEngine(
        @Named("strain") model: PhysiologicalModelProxy,
        heuristic: StrainEngine
    ): HybridStrainEngine = HybridStrainEngine(model, heuristic)

    @Provides
    @Singleton
    fun provideHybridStressEngine(
        @Named("stress") model: PhysiologicalModelProxy,
        heuristic: StressEngine
    ): HybridStressEngine = HybridStressEngine(model, heuristic)

    @Provides
    @Singleton
    fun provideHybridSleepEngine(
        @Named("sleep") model: PhysiologicalModelProxy,
        heuristic: SleepEngine
    ): HybridSleepEngine = HybridSleepEngine(model, heuristic)
}
