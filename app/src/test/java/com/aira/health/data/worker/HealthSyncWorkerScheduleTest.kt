package com.aira.health.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Operation
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        val requestSlot = slot<androidx.work.PeriodicWorkRequest>()
        every {
            workManager.enqueueUniquePeriodicWork(
                HealthSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                capture(requestSlot)
            )
        } returns mockk<Operation>(relaxed = true)

        HealthSyncWorker.schedule(context)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                HealthSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                any()
            )
        }

        assertEquals(HealthSyncWorker::class.java.name, requestSlot.captured.workSpec.workerClassName)
    }
}
