package com.aira.health.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_metrics ADD COLUMN spo2 REAL")
                db.execSQL("ALTER TABLE daily_metrics ADD COLUMN skinTemperature REAL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN externalId TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_sessions_sourcePackage_externalId ON workout_sessions(sourcePackage, externalId)")
            }
        }

        fun create(context: Context, passphrase: ByteArray): AiraDatabase {
            val factory = SupportFactory(passphrase.copyOf())

            return Room.databaseBuilder(
                context.applicationContext,
                AiraDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration() // Replace with explicit migrations before v1 release
                .build()
        }
    }
}
