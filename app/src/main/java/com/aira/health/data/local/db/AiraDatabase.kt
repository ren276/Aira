package com.aira.health.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aira.health.data.local.dao.*
import com.aira.health.data.local.model.*
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        HealthRecordRaw::class,
        DailyMetrics::class,
        SleepSession::class,
        HrSample::class,
        HrvSample::class,
        WorkoutSession::class,
        NutritionLog::class,
        JournalEntry::class,
        Baseline::class,
        DataSource::class,
        UserCorrection::class,
        CardioLoadHistory::class,
        AiConversationMessage::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class AiraDatabase : RoomDatabase() {

    abstract fun dailyMetricsDao(): DailyMetricsDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun hrSampleDao(): HrSampleDao
    abstract fun hrvSampleDao(): HrvSampleDao
    abstract fun baselineDao(): BaselineDao
    abstract fun dataSourceDao(): DataSourceDao
    abstract fun userCorrectionDao(): UserCorrectionDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun nutritionLogDao(): NutritionLogDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        const val DATABASE_NAME = "aira_db"

        fun create(context: Context, passphrase: ByteArray): AiraDatabase {
            val factory = SupportFactory(passphrase.copyOf())

            return Room.databaseBuilder(
                context.applicationContext,
                AiraDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // Replace with explicit migrations before v1 release
                .build()
        }
    }
}
