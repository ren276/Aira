package com.aira.health.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.aira.health.data.repository.HealthConnectRepositoryImpl
import com.aira.health.data.repository.GoogleFitRepositoryImpl
import com.aira.health.data.repository.SourceMergingHealthDataRepository
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
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        val client = if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
        return client
    }

    /**
     * Provides the correct [HealthDataRepository] implementation based on device capability:
     * - Health Connect available → merge Health Connect with Google Fit fallback/bridge
     * - Health Connect unavailable → Google Fit only (legacy fallback)
     */
    @Provides
    @Singleton
    fun provideHealthDataRepository(
        @ApplicationContext context: Context,
        healthConnectClient: HealthConnectClient?
    ): HealthDataRepository {
        val googleFitRepository = GoogleFitRepositoryImpl(context)
        val repository = if (healthConnectClient != null) {
            SourceMergingHealthDataRepository(
                healthConnectRepository = HealthConnectRepositoryImpl(healthConnectClient),
                googleFitRepository = googleFitRepository
            )
        } else {
            googleFitRepository
        }
        return repository
    }
}
