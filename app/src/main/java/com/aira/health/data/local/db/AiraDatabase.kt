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
        CausalInsight::class,
        PersonalizationState::class,
        CorrectionInfluenceState::class,
        AiConversationMessage::class,
        StravaActivityRaw::class,
    ],
    version = 6,
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
    abstract fun causalInsightDao(): CausalInsightDao
    abstract fun personalizationStateDao(): PersonalizationStateDao
    abstract fun correctionInfluenceDao(): CorrectionInfluenceDao
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS causal_insights (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "date TEXT NOT NULL, " +
                        "metricKey TEXT NOT NULL, " +
                        "confidence REAL NOT NULL, " +
                        "factor1Key TEXT, " +
                        "factor1Direction TEXT, " +
                        "factor1Weight REAL, " +
                        "factor1WindowLabel TEXT, " +
                        "factor1WindowTimestamp INTEGER, " +
                        "factor2Key TEXT, " +
                        "factor2Direction TEXT, " +
                        "factor2Weight REAL, " +
                        "factor2WindowLabel TEXT, " +
                        "factor2WindowTimestamp INTEGER, " +
                        "factor3Key TEXT, " +
                        "factor3Direction TEXT, " +
                        "factor3Weight REAL, " +
                        "factor3WindowLabel TEXT, " +
                        "factor3WindowTimestamp INTEGER, " +
                        "calculatedAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_causal_insights_metricKey_date " +
                        "ON causal_insights(metricKey, date)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS personalization_state (" +
                        "date TEXT NOT NULL, " +
                        "sleepNeedMinutes REAL NOT NULL, " +
                        "recoverySpeed REAL NOT NULL, " +
                        "stressSensitivity REAL NOT NULL, " +
                        "usableDays INTEGER NOT NULL, " +
                        "applied INTEGER NOT NULL, " +
                        "skipReason TEXT, " +
                        "correctionInfluenceApplied REAL NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(date)" +
                        ")"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS correction_influence_state (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "date TEXT NOT NULL, " +
                        "parameterKey TEXT NOT NULL, " +
                        "sourceCorrectionId INTEGER NOT NULL, " +
                        "sourceFieldName TEXT NOT NULL, " +
                        "ageDays INTEGER NOT NULL, " +
                        "decayWeight REAL NOT NULL, " +
                        "rawInfluence REAL NOT NULL, " +
                        "decayedInfluence REAL NOT NULL, " +
                        "cappedInfluence REAL NOT NULL, " +
                        "createdAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_correction_influence_state_date " +
                        "ON correction_influence_state(date)"
                )
            }
        }

        fun create(context: Context, passphrase: ByteArray): AiraDatabase {
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
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration() // Replace with explicit migrations before v1 release
                .build()
        }
    }
}
