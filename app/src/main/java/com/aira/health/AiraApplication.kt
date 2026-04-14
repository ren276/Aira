package com.aira.health

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AiraApplication : Application(), Configuration.Provider {

    /**
     * Injected by Hilt at runtime. Provides [HiltWorker]-annotated workers (e.g. [HealthSyncWorker])
     * with their constructor dependencies via @AssistedInject.
     */
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Disable crash reporting in builds where ENABLE_CRASH_REPORTING = false (debug flavor)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(BuildConfig.ENABLE_CRASH_REPORTING)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
