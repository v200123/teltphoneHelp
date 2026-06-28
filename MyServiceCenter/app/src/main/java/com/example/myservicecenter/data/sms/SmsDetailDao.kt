package com.example.myservicecenter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SmsDetailDao {

    @Query("SELECT * FROM sms_detail_records ORDER BY timestampMillis ASC, id ASC")
    suspend fun getAll(): List<SmsDetailRecordEntity>

    @Query("SELECT * FROM sms_detail_records WHERE timestampMillis >= :startMillis AND timestampMillis < :endMillis ORDER BY timestampMillis ASC, id ASC")
    suspend fun getByMonth(startMillis: Long, endMillis: Long): List<SmsDetailRecordEntity>

    @Query("SELECT * FROM sms_detail_records WHERE phoneNumber LIKE '%' || :query || '%' ORDER BY timestampMillis ASC, id ASC")
    suspend fun searchByPhone(query: String): List<SmsDetailRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SmsDetailRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<SmsDetailRecordEntity>)

    @Update
    suspend fun update(record: SmsDetailRecordEntity)

    @Delete
    suspend fun delete(record: SmsDetailRecordEntity)

    @Query("DELETE FROM sms_detail_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sms_detail_records")
    suspend fun clearAll()
}
