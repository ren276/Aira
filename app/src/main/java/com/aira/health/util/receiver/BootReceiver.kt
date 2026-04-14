package com.aira.health.util.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aira.health.data.worker.HealthSyncWorker

/**
 * Reschedules the [HealthSyncWorker] periodic sync after device reboot.
 *
 * WorkManager's periodic work survives reboots automatically IF the app is installed
 * on internal storage. This receiver provides an extra safety net — the call is
 * idempotent due to [ExistingPeriodicWorkPolicy.KEEP].
 *
 * Registered in AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 *   <receiver android:name=".util.receiver.BootReceiver" android:exported="true">
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            HealthSyncWorker.schedule(context)
        }
    }
}
