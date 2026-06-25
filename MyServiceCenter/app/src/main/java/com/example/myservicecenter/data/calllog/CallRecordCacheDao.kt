package com.example.myservicecenter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CallRecordCacheDao {

    @Query("SELECT * FROM cached_call_records ORDER BY CASE WHEN startTime > 0 THEN startTime WHEN connectedTime > 0 THEN connectedTime ELSE endTime END ASC")
    suspend fun getAll(): List<CachedCallRecordEntity>

    @Query("DELETE FROM cached_call_records")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CachedCallRecordEntity>)

    @Transaction
    suspend fun replaceAll(records: List<CachedCallRecordEntity>) {
        deleteAll()
        if (records.isNotEmpty()) {
            insertAll(records)
        }
    }
}
