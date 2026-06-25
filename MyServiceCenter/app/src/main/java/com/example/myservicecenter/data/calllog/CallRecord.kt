package com.example.myservicecenter

/**
 * 通话记录数据类
 */
data class CallRecord(
    val id: Long = 0,
    val phoneNumber: String? = null,
    val attribution: String? = null,
    val operator: String? = null,
    val startTime: Long = 0,
    val connectedTime: Long = 0,
    val endTime: Long = 0,
    val isConnected: Boolean = false,
    val callNumber: Int = 0,
    val callType: Int = 0,
    val recordingPath: String? = null,
    val recordingStartTime: Long = 0,
    val recordingEndTime: Long = 0
)
