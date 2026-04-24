package com.example.myservicecenter

import android.net.Uri

/**
 * 通话记录数据契约类
 * 与 TelephoneHelper 项目的 CallRecordContract 保持一致
 */
object CallRecordContract {

    const val CONTENT_AUTHORITY = "com.u2tzjtne.telephonehelper.provider"
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY")

    object CallRecord {
        const val PATH_CALL_RECORDS = "callrecords"
        val CONTENT_URI: Uri =  BASE_CONTENT_URI.buildUpon().appendPath(PATH_CALL_RECORDS).build()

        const val COLUMN_ID = "id"
        const val COLUMN_PHONE_NUMBER = "phoneNumber"
        const val COLUMN_ATTRIBUTION = "attribution"
        const val COLUMN_OPERATOR = "operator"
        const val COLUMN_START_TIME = "startTime"
        const val COLUMN_CONNECTED_TIME = "connectedTime"
        const val COLUMN_END_TIME = "endTime"
        const val COLUMN_IS_CONNECTED = "isConnected"
        const val COLUMN_CALL_NUMBER = "callNumber"
        const val COLUMN_CALL_TYPE = "callType"
        const val COLUMN_RECORDING_PATH = "recordingPath"
        const val COLUMN_RECORDING_START_TIME = "recordingStartTime"
        const val COLUMN_RECORDING_END_TIME = "recordingEndTime"
        const val DEFAULT_SORT_ORDER = "$COLUMN_START_TIME DESC"
    }
}
