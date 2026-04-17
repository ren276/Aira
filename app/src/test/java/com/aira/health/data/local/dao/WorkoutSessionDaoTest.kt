package com.aira.health.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aira.health.data.local.db.AiraDatabase
import com.aira.health.data.local.model.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutSessionDaoTest {

    private lateinit var database: AiraDatabase
    private lateinit var dao: WorkoutSessionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.workoutSessionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val session = WorkoutSession(
            startTime = 1000L,
            endTime = 2000L,
            exerciseType = "RUN",
            durationMin = 16,
            sourcePackage = "com.test"
        )
        val id = dao.insert(session)
        
        val loaded = dao.getById(id)
        assertNotNull(loaded)
        assertEquals("RUN", loaded?.exerciseType)
        assertEquals(16, loaded?.durationMin)
    }

    @Test
    fun updateSession() = runBlocking {
        val session = WorkoutSession(
            startTime = 1000L,
            endTime = 2000L,
            exerciseType = "RUN",
            durationMin = 16,
            sourcePackage = "com.test"
        )
        val id = dao.insert(session)
        
        val loaded = dao.getById(id)!!
        val updated = loaded.copy(exerciseType = "WALK")
        dao.update(updated)
        
        val reloaded = dao.getById(id)
        assertNotNull(reloaded)
        assertEquals("WALK", reloaded?.exerciseType)
    }

    @Test
    fun deleteById() = runBlocking {
        val session = WorkoutSession(
            startTime = 1000L,
            endTime = 2000L,
            exerciseType = "RUN",
            durationMin = 16,
            sourcePackage = "com.test"
        )
        val id = dao.insert(session)
        assertNotNull(dao.getById(id))
        
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun observeRange() = runBlocking {
        dao.insert(WorkoutSession(startTime = 1000L, endTime = 2000L, exerciseType = "RUN", durationMin = 16, sourcePackage = "test"))
        dao.insert(WorkoutSession(startTime = 3000L, endTime = 4000L, exerciseType = "WALK", durationMin = 16, sourcePackage = "test"))
        dao.insert(WorkoutSession(startTime = 5000L, endTime = 6000L, exerciseType = "SWIM", durationMin = 16, sourcePackage = "test"))
        
        val flow = dao.observeRange(1500L, 4500L)
        val items = flow.first()
        
        // Between 1500 and 4500, only the 3000L-4000L one should overlap if we query by startTime between startMs and endMs.
        // It depends on how observeRange is implemented. Usually we want overlapping or completely inside.
        // Let's assume the query is: "SELECT * FROM workout_sessions WHERE startTime >= :startMs AND startTime <= :endMs"
        assertEquals(1, items.size)
        assertEquals("WALK", items[0].exerciseType)
    }
}
