package com.aira.health.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 8 to 9:
 * Renaming syncedToSupabase to syncedToRemote in health_records_raw table.
 */
val MIGRATION_08_09_REMOTE_SYNC: Migration = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite 3.25.0+ supports RENAME COLUMN. Android 10 (API 29) uses 3.22.0, 
        // but SQLCipher typically bundles a newer version.
        // We use the safe multi-step approach since we don't want to risk failure on API 29.
        
        // 1. Create temporary table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS health_records_raw_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordType TEXT NOT NULL,
                sourcePackage TEXT NOT NULL,
                deviceDisplayName TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                valueJson TEXT NOT NULL,
                confidence REAL NOT NULL,
                syncedToRemote INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 2. Copy data
        db.execSQL("""
            INSERT INTO health_records_raw_new (
                id, recordType, sourcePackage, deviceDisplayName, startTime, endTime, valueJson, confidence, syncedToRemote
            )
            SELECT id, recordType, sourcePackage, deviceDisplayName, startTime, endTime, valueJson, confidence, syncedToSupabase
            FROM health_records_raw
        """.trimIndent())

        // 3. Drop old table
        db.execSQL("DROP TABLE health_records_raw")

        // 4. Rename new table
        db.execSQL("ALTER TABLE health_records_raw_new RENAME TO health_records_raw")
    }
}
