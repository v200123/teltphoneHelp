package com.example.myservicecenter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

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

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val LOG_BODY_MAX_LENGTH = 2000
    // TODO: 替换成你的升级服务真实地址，例如 http://192.168.1.10:8080
    private const val UPDATE_SERVICE_BASE_URL = "http://www.lastcoffee.top:8082"
    private const val CONNECT_TIMEOUT_MILLIS = 15000
    private const val READ_TIMEOUT_MILLIS = 15000

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    fun checkForUpdate(context: Context): UpdateCheckResult? {
        // 启动时把当前包名和 versionCode 传给服务端，由服务端决定是否需要更新。
        val versionCode = getCurrentVersionCode(context)
        val requestUrl = Uri.parse("${UPDATE_SERVICE_BASE_URL.trimEnd('/')}/api/check")
            .buildUpon()
            .appendQueryParameter("packageName", context.packageName)
            .appendQueryParameter("versionCode", versionCode.toString())
            .build()
            .toString()

        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()

        logRequestStart("检查更新", requestUrl)
        return try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                logResponse("检查更新", response, body)
                if (!response.isSuccessful) {
                    null
                } else {
                    parseCheckResponse(body)
                }
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "检查更新请求失败: url=$requestUrl", throwable)
            null
        }
    }

    fun downloadApk(
        context: Context,
        result: UpdateCheckResult,
        onProgressChanged: ((Int) -> Unit)? = null
    ): File {
        val targetVersionCode = result.latestVersionCode
            ?: throw IllegalStateException("缺少最新版本号，无法下载升级包")
        val resolvedUrl = resolveDownloadUrl(result.downloadUrl)
            ?: throw IllegalStateException("缺少下载地址，无法下载升级包")
        val targetDirectory =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val targetFile = File(targetDirectory, "update_${context.packageName}_$targetVersionCode.apk")

        // 如果本地已经有同版本 APK，并且大小/MD5 校验通过，就直接复用，避免重复下载。
        if (targetFile.exists() && verifyDownloadedFile(targetFile, result)) {
            Log.d(TAG, "升级包复用本地缓存: path=${targetFile.absolutePath}, size=${targetFile.length()}")
            onProgressChanged?.invoke(100)
            return targetFile
        }

        val tempFile = File(targetDirectory, "${targetFile.name}.download")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val request = Request.Builder()
            .url(resolvedUrl)
            .get()
            .build()

        logRequestStart("下载升级包", resolvedUrl)
        try {
            httpClient.newCall(request).execute().use { response ->
                logDownloadResponse(response)
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载升级包失败，HTTP状态码: ${response.code}")
                }

                val body = response.body
                    ?: throw IllegalStateException("下载升级包失败，响应体为空")
                val expectedSize = result.fileSize ?: body.contentLength().takeIf { it > 0L }
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloadedBytes = 0L
                var lastProgress = -1
                var lastLoggedProgressBucket = -1

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        while (true) {
                            val readSize = input.read(buffer)
                            if (readSize <= 0) break
                            output.write(buffer, 0, readSize)
                            downloadedBytes += readSize
                            if (expectedSize != null && expectedSize > 0L) {
                                val progress = ((downloadedBytes * 100) / expectedSize).toInt().coerceIn(0, 100)
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    onProgressChanged?.invoke(progress)
                                }
                                val progressBucket = progress / 10
                                if (progressBucket != lastLoggedProgressBucket) {
                                    lastLoggedProgressBucket = progressBucket
                                    Log.d(
                                        TAG,
                                        "下载升级包进度: $progress%, downloaded=$downloadedBytes, total=$expectedSize"
                                    )
                                }
                            }
                        }
                        output.flush()
                    }
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    throw IllegalStateException("升级包写入本地失败")
                }

                if (!verifyDownloadedFile(targetFile, result)) {
                    targetFile.delete()
                    throw IllegalStateException("下载完成，但升级包校验失败")
                }

                Log.d(
                    TAG,
                    "下载升级包完成: path=${targetFile.absolutePath}, size=${targetFile.length()}, md5=${result.md5.orEmpty()}"
                )
                onProgressChanged?.invoke(100)
                return targetFile
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "下载升级包请求失败: url=$resolvedUrl", throwable)
            throw throwable
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    fun createInstallPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun installApk(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }

    private fun logRequestStart(scene: String, url: String) {
        Log.d(TAG, "$scene 请求开始: method=GET, url=$url")
    }

    private fun logResponse(scene: String, response: Response, body: String) {
        Log.d(
            TAG,
            "$scene 响应: code=${response.code}, successful=${response.isSuccessful}, message=${response.message}, body=${body.take(LOG_BODY_MAX_LENGTH)}"
        )
    }

    private fun logDownloadResponse(response: Response) {
        Log.d(
            TAG,
            "下载升级包响应: code=${response.code}, successful=${response.isSuccessful}, message=${response.message}, contentLength=${response.body?.contentLength() ?: -1L}"
        )
    }

    private fun parseCheckResponse(body: String): UpdateCheckResult {
        val jsonObject = JSONObject(body)
        return UpdateCheckResult(
            needUpdate = jsonObject.optBoolean("needUpdate", false),
            forceUpdate = jsonObject.optBoolean("forceUpdate", false),
            latestVersion = jsonObject.optNullableString("latestVersion"),
            latestVersionCode = jsonObject.optNullableInt("latestVersionCode"),
            updateLog = jsonObject.optNullableString("updateLog"),
            downloadUrl = jsonObject.optNullableString("downloadUrl"),
            fileSize = jsonObject.optNullableLong("fileSize"),
            md5 = jsonObject.optNullableString("md5")
        )
    }

    private fun resolveDownloadUrl(downloadUrl: String?): String? {
        if (downloadUrl.isNullOrBlank()) return null
        if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
            return downloadUrl
        }
        // 服务端返回相对路径时，自动拼到统一的升级服务根地址后面。
        val baseUri = Uri.parse(UPDATE_SERVICE_BASE_URL.trimEnd('/'))
        return baseUri.buildUpon()
            .encodedPath(downloadUrl.ensureStartsWithSlash())
            .build()
            .toString()
    }

    private fun verifyDownloadedFile(file: File, result: UpdateCheckResult): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val expectedSize = result.fileSize
        if (expectedSize != null && expectedSize > 0L && file.length() != expectedSize) {
            Log.w(TAG, "升级包大小校验失败: expected=$expectedSize, actual=${file.length()}")
            return false
        }
        val expectedMd5 = result.md5
        if (!expectedMd5.isNullOrBlank()) {
            val actualMd5 = calculateMd5(file)
            if (!actualMd5.equals(expectedMd5, ignoreCase = true)) {
                Log.w(TAG, "升级包MD5校验失败: expected=$expectedMd5, actual=$actualMd5")
                return false
            }
        }
        return true
    }

    private fun calculateMd5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val readSize = input.read(buffer)
                if (readSize <= 0) break
                digest.update(buffer, 0, readSize)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getCurrentVersionCode(context: Context): Long {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (isNull(key)) null else optString(key, null)
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        return if (isNull(key)) null else optInt(key)
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (isNull(key)) null else optLong(key)
    }

    private fun String.ensureStartsWithSlash(): String {
        return if (startsWith("/")) this else "/$this"
    }
}
