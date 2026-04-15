package com.aira.health.domain.usecase

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.longPreferencesKey
import com.aira.health.data.local.dao.HrSampleDao
import com.aira.health.data.local.dao.HrvSampleDao
import com.aira.health.data.local.dao.SleepSessionDao
import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.HrvSample
import com.aira.health.data.local.model.SleepSession
import com.aira.health.domain.repository.HealthDataRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant

class IngestHealthDataUseCaseTest {

    @Test
    fun `invoke resolves overlaps and persists sync timestamp`() = runTest {
        val repository = mockk<HealthDataRepository>()
        val hrDao = mockk<HrSampleDao>(relaxed = true)
        val hrvDao = mockk<HrvSampleDao>(relaxed = true)
        val sleepDao = mockk<SleepSessionDao>(relaxed = true)

        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { Files.createTempFile("aira-test", ".preferences_pb").toFile() }
        )

        val hr = listOf(
            HrSample(timestamp = 1_000L, bpm = 60, sourcePackage = "a", confidence = 0.5f),
            HrSample(timestamp = 1_000L, bpm = 61, sourcePackage = "b", confidence = 0.9f),
            HrSample(timestamp = 2_000L, bpm = 62, sourcePackage = "c", confidence = 0.8f)
        )
        val hrv = listOf(
            HrvSample(timestamp = 3_000L, rmssd = 20f, sourcePackage = "a", confidence = 0.4f),
            HrvSample(timestamp = 3_000L, rmssd = 25f, sourcePackage = "b", confidence = 0.7f)
        )
        val sleep = listOf(
            SleepSession(
                date = "2026-04-14",
                startTime = 10_000L,
                endTime = 20_000L,
                durationMin = 480,
                sourcePackage = "a",
                confidence = 0.4f
            ),
            SleepSession(
                date = "2026-04-14",
                startTime = 10_100L,
                endTime = 20_100L,
                durationMin = 481,
                sourcePackage = "b",
                confidence = 0.9f
            )
        )

        coEvery { repository.readHeartRate(any(), any()) } returns hr
        coEvery { repository.readHeartRateVariability(any(), any()) } returns hrv
        coEvery { repository.readSleepSessions(any(), any()) } returns sleep
        coEvery { sleepDao.getRange(any(), any()) } returns emptyList()

        val useCase = IngestHealthDataUseCase(repository, hrDao, hrvDao, sleepDao, dataStore)

        val ingestedCount = useCase.invoke()

        assertEquals(4, ingestedCount)

        coVerify(exactly = 1) {
            hrDao.insertAll(match { it.size == 2 && it.any { s -> s.confidence == 0.9f } })
        }
        coVerify(exactly = 1) {
            hrvDao.insertAll(match { it.size == 1 && it.first().confidence == 0.7f })
        }
        coVerify(exactly = 1) {
            sleepDao.insert(match { it.date == "2026-04-14" && it.confidence == 0.9f })
        }
        coVerify(exactly = 1) { hrDao.purgeOlderThan(any()) }
        coVerify(exactly = 1) { hrvDao.purgeOlderThan(any()) }

        val key = longPreferencesKey("health_last_sync_epoch_ms")
        val updated = dataStore.data.first()[key]
        requireNotNull(updated)
        assertEquals(true, updated <= Instant.now().toEpochMilli())
    }
}
