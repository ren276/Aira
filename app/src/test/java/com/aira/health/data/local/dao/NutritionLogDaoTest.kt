package com.aira.health.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aira.health.data.local.db.AiraDatabase
import com.aira.health.data.local.model.NutritionLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NutritionLogDaoTest {

    private lateinit var database: AiraDatabase
    private lateinit var dao: NutritionLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.nutritionLogDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val log = NutritionLog(
            timestamp = 1000L,
            foodName = "Apple",
            calories = 95f
        )
        val id = dao.insert(log)
        
        val loaded = dao.getById(id)
        assertNotNull(loaded)
        assertEquals("Apple", loaded?.foodName)
        assertEquals(95f, loaded?.calories)
    }

    @Test
    fun updateLog() = runBlocking {
        val log = NutritionLog(
            timestamp = 1000L,
            foodName = "Apple",
            calories = 95f
        )
        val id = dao.insert(log)
        
        val loaded = dao.getById(id)!!
        val updated = loaded.copy(foodName = "Banana", calories = 105f)
        dao.update(updated)
        
        val reloaded = dao.getById(id)
        assertNotNull(reloaded)
        assertEquals("Banana", reloaded?.foodName)
        assertEquals(105f, reloaded?.calories)
    }

    @Test
    fun deleteById() = runBlocking {
        val log = NutritionLog(
            timestamp = 1000L,
            foodName = "Apple",
            calories = 95f
        )
        val id = dao.insert(log)
        assertNotNull(dao.getById(id))
        
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }
}
