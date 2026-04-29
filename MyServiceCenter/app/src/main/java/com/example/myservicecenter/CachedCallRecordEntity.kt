package com.example.myservicecenter

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_call_records")
data class CachedCallRecordEntity(
    @PrimaryKey
    val id: Long,
    val phoneNumber: String?,
    val attribution: String?,
    val operator: String?,
    val startTime: Long,
    val connectedTime: Long,
    val endTime: Long,
    val isConnected: Boolean,
    val callNumber: Int,
    val callType: Int,
    val recordingPath: String?,
    val recordingStartTime: Long,
    val recordingEndTime: Long
)

fun CachedCallRecordEntity.toCallRecord(): CallRecord {
    return CallRecord(
        id = id,
        phoneNumber = phoneNumber,
        attribution = attribution,
        operator = operator,
        startTime = startTime,
        connectedTime = connectedTime,
        endTime = endTime,
        isConnected = isConnected,
        callNumber = callNumber,
        callType = callType,
        recordingPath = recordingPath,
        recordingStartTime = recordingStartTime,
        recordingEndTime = recordingEndTime
    )
}

fun CallRecord.toCachedEntity(): CachedCallRecordEntity {
    return CachedCallRecordEntity(
        id = id,
        phoneNumber = phoneNumber,
        attribution = attribution,
        operator = operator,
        startTime = startTime,
        connectedTime = connectedTime,
        endTime = endTime,
        isConnected = isConnected,
        callNumber = callNumber,
        callType = callType,
        recordingPath = recordingPath,
        recordingStartTime = recordingStartTime,
        recordingEndTime = recordingEndTime
    )
}
