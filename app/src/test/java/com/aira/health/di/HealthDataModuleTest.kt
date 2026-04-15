package com.aira.health.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.aira.health.data.repository.HealthConnectRepositoryImpl
import com.aira.health.data.repository.GoogleFitRepositoryImpl
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HealthDataModuleTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `provideHealthDataRepository returns health connect repo when client exists`() {
        val context = mockk<Context>(relaxed = true)
        val client = mockk<HealthConnectClient>(relaxed = true)

        val repository = HealthDataModule.provideHealthDataRepository(context, client)

        assertTrue(repository is HealthConnectRepositoryImpl)
    }

    @Test
    fun `provideHealthDataRepository returns google fit repo when health connect unavailable`() {
        val context = mockk<Context>(relaxed = true)

        val repository = HealthDataModule.provideHealthDataRepository(context, null)

        assertTrue(repository is GoogleFitRepositoryImpl)
    }
}
