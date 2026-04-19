package com.aira.health.di

import com.aira.health.data.repository.ContinuitySnapshotRepositoryImpl
import com.aira.health.data.repository.NutritionRepositoryImpl
import com.aira.health.data.repository.StravaRepositoryImpl
import com.aira.health.data.repository.UserRepositoryImpl
import com.aira.health.data.repository.WorkoutRepositoryImpl
import com.aira.health.domain.repository.ContinuitySnapshotRepository
import com.aira.health.domain.repository.NutritionRepository
import com.aira.health.domain.repository.StravaRepository
import com.aira.health.domain.repository.UserRepository
import com.aira.health.domain.repository.WorkoutRepository
import com.aira.health.presentation.nutrition.scanner.BarcodeScannerGateway
import com.aira.health.presentation.nutrition.scanner.MlKitBarcodeScannerGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContinuitySnapshotRepository(
        impl: ContinuitySnapshotRepositoryImpl
    ): ContinuitySnapshotRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindNutritionRepository(impl: NutritionRepositoryImpl): NutritionRepository

    @Binds
    @Singleton
    abstract fun bindStravaRepository(impl: StravaRepositoryImpl): StravaRepository

    @Binds
    @Singleton
    abstract fun bindBarcodeScannerGateway(impl: MlKitBarcodeScannerGateway): BarcodeScannerGateway
}
