package com.aira.health.data.local.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aira.health.data.local.db.migrations.MIGRATION_09_X
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiraDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "aira-migration-09-test.db"
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS daily_metrics (" +
                            "date TEXT NOT NULL, " +
                            "recoveryScore INTEGER NOT NULL, " +
                            "sleepScore INTEGER NOT NULL, " +
                            "strainScore INTEGER NOT NULL, " +
                            "stressScore INTEGER NOT NULL, " +
                            "energyBankScore INTEGER NOT NULL, " +
                            "readinessToLearnScore INTEGER NOT NULL, " +
                            "nutritionScore INTEGER NOT NULL, " +
                            "burnoutRiskIndex REAL NOT NULL, " +
                            "compositeReadiness INTEGER NOT NULL, " +
                            "dataConfidence REAL NOT NULL, " +
                            "hrvMorning REAL, " +
                            "rhrMorning REAL, " +
                            "sleepDurationMin INTEGER, " +
                            "sleepEfficiency REAL, " +
                            "totalSteps INTEGER, " +
                            "totalDistanceMeters REAL, " +
                            "activeCalories INTEGER, " +
                            "spo2 REAL, " +
                            "skinTemperature REAL, " +
                            "calculatedAt INTEGER NOT NULL, " +
                            "PRIMARY KEY(date)" +
                            ")"
                    )
                    db.execSQL(
                        "INSERT INTO daily_metrics(" +
                            "date, recoveryScore, sleepScore, strainScore, stressScore, " +
                            "energyBankScore, readinessToLearnScore, nutritionScore, burnoutRiskIndex, " +
                            "compositeReadiness, dataConfidence, calculatedAt" +
                            ") VALUES(" +
                            "'2026-04-18', 72, 75, 48, 41, 63, 70, 0, 0.35, 71, 0.82, 1713398400000" +
                            ")"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `migration 09 creates prediction tables and preserves existing rows`() {
        val db = helper.writableDatabase
        MIGRATION_09_X.migrate(db)

        assertTableExists(db, "what_if_simulation_results")
        assertTableExists(db, "prediction_calibration")

        db.query("SELECT recoveryScore, energyBankScore FROM daily_metrics WHERE date = '2026-04-18'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(72, cursor.getInt(0))
            assertEquals(63, cursor.getInt(1))
        }
    }

    private fun assertTableExists(db: SupportSQLiteDatabase, tableName: String) {
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$tableName'").use { cursor ->
            assertTrue("Expected table $tableName to exist", cursor.moveToFirst())
        }
    }
}
