package com.aira.health.util.receiver

import android.content.Context
import android.content.Intent
import com.aira.health.data.worker.HealthSyncWorker
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BootReceiverTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onReceive schedules sync on boot completed`() {
        val context = mockk<Context>(relaxed = true)
        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        mockkObject(HealthSyncWorker.Companion)
        every { HealthSyncWorker.schedule(context) } just runs

        BootReceiver().onReceive(context, intent)

        verify(exactly = 1) { HealthSyncWorker.schedule(context) }
    }

    @Test
    fun `onReceive ignores non boot intents`() {
        val context = mockk<Context>(relaxed = true)
        val intent = mockk<Intent>()
        every { intent.action } returns "com.aira.health.NOT_BOOT"

        mockkObject(HealthSyncWorker.Companion)
        every { HealthSyncWorker.schedule(context) } just runs

        BootReceiver().onReceive(context, intent)

        verify(exactly = 0) { HealthSyncWorker.schedule(context) }
    }
}
