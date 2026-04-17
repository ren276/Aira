package com.aira.health.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aira.health.data.local.dao.*
import com.aira.health.data.local.model.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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
        StravaActivityRaw::class,
    ],
    version = 4,
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
    abstract fun stravaActivityRawDao(): StravaActivityRawDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN distanceMeters REAL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN steps INTEGER")
                db.execSQL("ALTER TABLE daily_metrics ADD COLUMN totalDistanceMeters REAL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS strava_activities_raw (" +
                        "activityId INTEGER NOT NULL, " +
                        "startTime INTEGER NOT NULL, " +
                        "endTime INTEGER NOT NULL, " +
                        "sportType TEXT, " +
                        "distanceMeters REAL, " +
                        "movingTimeSec INTEGER, " +
                        "elapsedTimeSec INTEGER, " +
                        "steps INTEGER, " +
                        "averageHeartRate REAL, " +
                        "maxHeartRate REAL, " +
                        "calories REAL, " +
                        "kiloJoules REAL, " +
                        "sourcePackage TEXT NOT NULL, " +
                        "rawJson TEXT NOT NULL, " +
                        "syncedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(activityId)" +
                        ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_strava_activities_raw_startTime ON strava_activities_raw(startTime)")
            }
        }

        fun create(context: Context, passphrase: ByteArray): AiraDatabase {
            // sqlcipher-android requires explicitly loading the native library once.
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase.copyOf())

            return Room.databaseBuilder(
                context.applicationContext,
                AiraDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration() // Replace with explicit migrations before v1 release
                .build()
        }
    }
}
