package com.aira.health.util.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives BOOT_COMPLETED broadcast to re-schedule WorkManager sync after device reboot.
 * Implementation added in Phase 2 (HealthSyncWorker).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO Phase 2: Re-schedule WorkManager health sync after boot
        }
    }
}
