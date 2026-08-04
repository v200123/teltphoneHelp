package com.example.appupdater

data class UpdateCheckResult(
    val needUpdate: Boolean,
    val forceUpdate: Boolean,
    val latestVersion: String?,
    val latestVersionCode: Int?,
    val updateLog: String?,
    val downloadUrl: String?,
    val fileSize: Long?,
    val md5: String?
)
