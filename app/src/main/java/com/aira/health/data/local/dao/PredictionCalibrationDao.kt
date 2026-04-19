package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aira.health.data.local.model.PredictionCalibrationRecord

@Dao
interface PredictionCalibrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PredictionCalibrationRecord)

    @Query("SELECT * FROM prediction_calibration WHERE targetDate = :targetDate LIMIT 1")
    suspend fun getForTargetDate(targetDate: String): PredictionCalibrationRecord?

    @Query("SELECT * FROM prediction_calibration ORDER BY recordedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<PredictionCalibrationRecord>
}
