package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.DataSource
import kotlinx.coroutines.flow.Flow

@Dao
interface DataSourceDao {
    @Query("SELECT * FROM data_sources")
    fun observeAll(): Flow<List<DataSource>>

    @Query("SELECT * FROM data_sources WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): DataSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dataSource: DataSource)

    @Query("UPDATE data_sources SET lastSeen = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastSeen(packageName: String, timestamp: Long)
}
