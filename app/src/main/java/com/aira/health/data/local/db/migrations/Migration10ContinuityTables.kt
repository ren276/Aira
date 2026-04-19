package com.aira.health.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_CONTINUITY: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS continuity_sync_state (" +
                "`key` TEXT NOT NULL, " +
                "userId TEXT NOT NULL, " +
                "lastSnapshotId TEXT, " +
                "lastSuccessEpochMs INTEGER, " +
                "lastAttemptEpochMs INTEGER NOT NULL, " +
                "retryCount INTEGER NOT NULL, " +
                "lastErrorCode TEXT, " +
                "PRIMARY KEY(`key`)" +
                ")"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_continuity_sync_state_userId " +
                "ON continuity_sync_state(userId)"
        )
    }
}
