package com.aira.health.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_09_X: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS what_if_simulation_results (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "targetDate TEXT NOT NULL, " +
                "baselineDate TEXT NOT NULL, " +
                "baselineRecoveryScore INTEGER NOT NULL, " +
                "baselineEnergyScore INTEGER NOT NULL, " +
                "sleepDeltaHours REAL NOT NULL, " +
                "trainingLoadDeltaPercent REAL NOT NULL, " +
                "projectedRecoveryDelta INTEGER NOT NULL, " +
                "projectedEnergyDelta INTEGER NOT NULL, " +
                "projectedBurnoutTier TEXT NOT NULL, " +
                "projectedBurnoutTrajectory TEXT NOT NULL, " +
                "confidenceTier TEXT NOT NULL, " +
                "confidenceScore REAL NOT NULL, " +
                "rationaleSignalKeys TEXT NOT NULL, " +
                "simulatedAt INTEGER NOT NULL" +
                ")"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_what_if_simulation_results_targetDate_simulatedAt " +
                "ON what_if_simulation_results(targetDate, simulatedAt)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS prediction_calibration (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "targetDate TEXT NOT NULL, " +
                "predictedRecoveryDelta INTEGER NOT NULL, " +
                "observedRecoveryDelta INTEGER NOT NULL, " +
                "recoveryAbsoluteError INTEGER NOT NULL, " +
                "predictedEnergyDelta INTEGER NOT NULL, " +
                "observedEnergyDelta INTEGER NOT NULL, " +
                "energyAbsoluteError INTEGER NOT NULL, " +
                "rollingMeanAbsoluteError REAL NOT NULL, " +
                "recordedAt INTEGER NOT NULL" +
                ")"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_prediction_calibration_targetDate " +
                "ON prediction_calibration(targetDate)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_prediction_calibration_recordedAt " +
                "ON prediction_calibration(recordedAt)"
        )
    }
}
