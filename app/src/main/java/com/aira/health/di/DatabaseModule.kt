package com.aira.health.di

import android.content.Context
import com.aira.health.data.local.dao.*
import com.aira.health.data.local.db.AiraDatabase
import com.aira.health.util.security.KeystoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAiraDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): AiraDatabase {
        // Key generated at first cold start — Keystore key is independent of biometrics
        val passphrase = keystoreManager.getDatabasePassphrase()
        return AiraDatabase.create(context, passphrase)
    }

    @Provides
    fun provideDailyMetricsDao(db: AiraDatabase): DailyMetricsDao = db.dailyMetricsDao()

    @Provides
    fun provideSleepSessionDao(db: AiraDatabase): SleepSessionDao = db.sleepSessionDao()

    @Provides
    fun provideHrSampleDao(db: AiraDatabase): HrSampleDao = db.hrSampleDao()

    @Provides
    fun provideHrvSampleDao(db: AiraDatabase): HrvSampleDao = db.hrvSampleDao()

    @Provides
    fun provideBaselineDao(db: AiraDatabase): BaselineDao = db.baselineDao()

    @Provides
    fun provideDataSourceDao(db: AiraDatabase): DataSourceDao = db.dataSourceDao()

    @Provides
    fun provideUserCorrectionDao(db: AiraDatabase): UserCorrectionDao = db.userCorrectionDao()

    @Provides
    fun provideAiConversationDao(db: AiraDatabase): AiConversationDao = db.aiConversationDao()

    @Provides
    fun provideNutritionLogDao(db: AiraDatabase): NutritionLogDao = db.nutritionLogDao()
}
