package com.example.myservicecenter

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_detail_records")
data class SmsDetailRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val direction: String,
    val phoneNumber: String,
    val messageType: String,
    val location: String,
    val timestampMillis: Long,
    val packageName: String,
    val fee: String,
    val createdAtMillis: Long
)
