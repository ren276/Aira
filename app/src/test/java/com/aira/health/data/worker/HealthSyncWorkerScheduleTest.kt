package com.aira.health.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class HealthSyncWorkerScheduleTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `schedule enqueues unique periodic work with KEEP policy`() {
        val context = mockk<Context>(relaxed = true)
        val workManager = mockk<WorkManager>(relaxed = true)

        HealthSyncWorker.schedule(context, workManager)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                HealthSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                any()
            )
        }
    }
}
