package com.aira.health

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiraApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Disable crash reporting in builds where ENABLE_CRASH_REPORTING = false (debug flavor)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(BuildConfig.ENABLE_CRASH_REPORTING)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
