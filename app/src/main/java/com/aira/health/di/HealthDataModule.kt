package com.aira.health.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.aira.health.data.repository.HealthConnectRepositoryImpl
import com.aira.health.data.repository.GoogleFitRepositoryImpl
import com.aira.health.domain.repository.HealthDataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthDataModule {

    /**
     * Provides a [HealthConnectClient] if Health Connect is available on this device.
     * Returns null for Android 10-12 devices where the provider APK is not installed.
     * Downstream consumers must handle the nullable — the GoogleFit fallback is injected
     * separately.
     */
    @Provides
    @Singleton
    fun provideHealthConnectClient(
        @ApplicationContext context: Context
    ): HealthConnectClient? {
        return if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    /**
     * Provides the correct [HealthDataRepository] implementation based on device capability:
     * - Health Connect available → [HealthConnectRepositoryImpl] (primary)
     * - Health Connect unavailable → [GoogleFitRepositoryImpl] (fallback for Android 10-12)
     */
    @Provides
    @Singleton
    fun provideHealthDataRepository(
        @ApplicationContext context: Context,
        healthConnectClient: HealthConnectClient?
    ): HealthDataRepository {
        return if (healthConnectClient != null) {
            HealthConnectRepositoryImpl(healthConnectClient)
        } else {
            GoogleFitRepositoryImpl(context)
        }
    }
}
