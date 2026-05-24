package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.coder.ffmpeg.call.IFFmpegCallBack
import com.coder.ffmpeg.jni.FFmpegCommand
import com.coder.ffmpeg.utils.CommandParams
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.concurrent.thread

class RingtoneVideoComposer(private val context: Context) {
    companion object {
        private const val MIN_BADGE_SCALE = 0.2f
        private const val MAX_BADGE_SCALE = 2.0f
    }

    data class ComposeResult(
        val outputFile: File,
        val durationMs: Long,
        val outputSizeBytes: Long,
        val mimeType: String = "video/mp4"
    )

    interface Callback {
        fun onStart()
        fun onProgress(progress: Int)
        fun onSuccess(result: ComposeResult)
        fun onError(message: String)
        fun onCancel()
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    fun compose(
        sourcePath: String,
        rule: BadgeRule,
        previewCanvasWidth: Int,
        previewCanvasHeight: Int,
        callback: Callback
    ) {
        thread(name = "ringtone-video-compose") {
            val workspace = mutableListOf<File>()
            try {
                ensureSupportedAbi()
                val addedItems = rule.items
                    .mapNotNull { (key, state) ->
                        val badgeId = BadgeId.fromKey(key) ?: return@mapNotNull null
                        if (!state.added) return@mapNotNull null
                        badgeId to state
                    }
                    .sortedBy { it.second.zIndex }
                if (addedItems.isEmpty()) {
                    postToMain { callback.onError("当前规则里没有可烧录的图标") }
                    return@thread
                }

                val inputFile = copySourceToWorkspace(sourcePath)
                workspace += inputFile
                val metadata = readVideoMetadata(inputFile.absolutePath)
                val outputFile = createOutputFile()
                val badgeFiles = addedItems.mapIndexed { index, (badgeId, state) ->
                    val badgeFile = exportBadgeBitmap(
                        index = index,
                        badgeId = badgeId,
                        scale = state.scale,
                        previewCanvasWidth = previewCanvasWidth,
                        previewCanvasHeight = previewCanvasHeight,
                        videoWidth = metadata.displayWidth,
                        videoHeight = metadata.displayHeight
                    )
                    workspace += badgeFile.file
                    badgeFile
                }

                val command = buildCommand(
                    inputFile = inputFile,
                    outputFile = outputFile,
                    overlays = addedItems.mapIndexed { index, (_, state) ->
                        OverlaySpec(
                            inputIndex = index + 1,
                            state = state,
                            videoWidth = metadata.displayWidth,
                            videoHeight = metadata.displayHeight,
                            badgeWidth = badgeFiles[index].width,
                            badgeHeight = badgeFiles[index].height
                        )
                    },
                    badgeFiles = badgeFiles
                )
                android.util.Log.d("RingtoneVideoComposer", "run ffmpeg: ${command.joinToString(" ")}")

                FFmpegCommand.setDebug(false)
                val taskId = FFmpegCommand.runCmd(command, object : IFFmpegCallBack {
                    override fun onStart() {
                        postToMain { callback.onStart() }
                    }

                    override fun onProgress(progress: Int, pts: Long) {
                        postToMain { callback.onProgress(progress) }
                    }

                    override fun onComplete() {
                        workspace.forEach { file ->
                            if (file != outputFile) {
                                file.delete()
                            }
                        }
                        postToMain {
                            callback.onSuccess(
                                ComposeResult(
                                    outputFile = outputFile,
                                    durationMs = metadata.durationMs,
                                    outputSizeBytes = outputFile.length()
                                )
                            )
                        }
                    }

                    override fun onCancel() {
                        outputFile.delete()
                        workspace.forEach { it.delete() }
                        postToMain { callback.onCancel() }
                    }

                    override fun onError(errorCode: Int, msg: String?) {
                        outputFile.delete()
                        workspace.forEach { it.delete() }
                        android.util.Log.e(
                            "RingtoneVideoComposer",
                            "ffmpeg failed, errorCode=$errorCode, message=$msg"
                        )
                        postToMain {
                            callback.onError(msg?.ifBlank { "视频烧录失败，错误码：$errorCode" } ?: "视频烧录失败，错误码：$errorCode")
                        }
                    }
                })

                if (taskId == null || taskId < 0) {
                    outputFile.delete()
                    workspace.forEach { it.delete() }
                    postToMain { callback.onError("FFmpeg 任务创建失败") }
                }
            } catch (e: Exception) {
                workspace.forEach { it.delete() }
                postToMain { callback.onError(e.message ?: "视频烧录失败") }
            }
        }
    }

    private fun ensureSupportedAbi() {
        val supported = Build.SUPPORTED_ABIS.any { abi ->
            abi == "armeabi-v7a" || abi == "arm64-v8a"
        }
        if (!supported) {
            throw IllegalStateException("当前设备暂不支持图标烧录，仅支持 armeabi-v7a / arm64-v8a 真机")
        }
    }

    private fun copySourceToWorkspace(sourcePath: String): File {
        val workspaceDir = getWorkspaceDir()
        val inputFile = File(workspaceDir, "input_${System.currentTimeMillis()}.mp4")
        val sourceFile = File(sourcePath)
        if (sourceFile.exists()) {
            sourceFile.inputStream().use { input ->
                input.copyToFile(inputFile)
            }
        } else {
            appContext.contentResolver.openInputStream(Uri.parse(sourcePath)).use { input ->
                if (input == null) {
                    throw IllegalStateException("无法读取原始视频")
                }
                input.copyToFile(inputFile)
            }
        }
        return inputFile
    }

    private fun readVideoMetadata(videoPath: String): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val displayWidth = if (rotation == 90 || rotation == 270) height else width
            val displayHeight = if (rotation == 90 || rotation == 270) width else height
            if (displayWidth <= 0 || displayHeight <= 0) {
                throw IllegalStateException("无法读取视频尺寸")
            }
            return VideoMetadata(displayWidth, displayHeight, duration)
        } finally {
            retriever.release()
        }
    }

    private fun exportBadgeBitmap(
        index: Int,
        badgeId: BadgeId,
        scale: Float,
        previewCanvasWidth: Int,
        previewCanvasHeight: Int,
        videoWidth: Int,
        videoHeight: Int
    ): ExportedBadgeFile {
        val bitmap = BitmapFactory.decodeResource(appContext.resources, badgeId.drawableRes)
            ?: throw IllegalStateException("读取图标资源失败")
        val safeScale = scale.coerceIn(MIN_BADGE_SCALE, MAX_BADGE_SCALE)
        val previewWidth = (bitmap.width * safeScale).coerceAtLeast(1f)
        val previewHeight = (bitmap.height * safeScale).coerceAtLeast(1f)
        val widthRatio = if (previewCanvasWidth > 0) {
            videoWidth.toFloat() / previewCanvasWidth.toFloat()
        } else {
            1f
        }
        val heightRatio = if (previewCanvasHeight > 0) {
            videoHeight.toFloat() / previewCanvasHeight.toFloat()
        } else {
            1f
        }
        val outputWidth = (previewWidth * widthRatio).toInt().coerceAtLeast(1)
        val outputHeight = (previewHeight * heightRatio).toInt().coerceAtLeast(1)
        val scaledBitmap = if (bitmap.width == outputWidth && bitmap.height == outputHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, outputWidth, outputHeight, true)
        }
        val outputFile = File(getWorkspaceDir(), "badge_${index}_${System.currentTimeMillis()}.png")
        FileOutputStream(outputFile).use { stream ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        return ExportedBadgeFile(outputFile, outputWidth, outputHeight)
    }

    private fun buildCommand(
        inputFile: File,
        outputFile: File,
        overlays: List<OverlaySpec>,
        badgeFiles: List<ExportedBadgeFile>
    ): Array<String?> {
        val params = CommandParams()
            .append("-i")
            .append(inputFile.absolutePath)

        badgeFiles.forEach { badgeFile ->
            params.append("-i").append(badgeFile.file.absolutePath)
        }

        params
            .append("-filter_complex")
            .append(buildFilterComplex(overlays))
            .append("-map")
            .append("[vout]")
            .append("-map")
            .append("0:a?")
            .append("-c:v")
            .append("libx264")
            .append("-preset")
            .append("veryfast")
            .append("-pix_fmt")
            .append("yuv420p")
            .append("-c:a")
            .append("copy")
            .append("-movflags")
            .append("+faststart")
            .append(outputFile.absolutePath)
        return params.get()
    }

    private fun buildFilterComplex(overlays: List<OverlaySpec>): String {
        var previousStream = "[0:v]"
        val segments = mutableListOf<String>()
        overlays.forEachIndexed { index, overlay ->
            val outputStream = if (index == overlays.lastIndex) "[vout]" else "[v${index + 1}]"
            segments += buildString {
                append(previousStream)
                append("[")
                append(overlay.inputIndex)
                append(":v]")
                append("overlay=")
                append(overlay.left)
                append(":")
                append(overlay.top)
                append(outputStream)
            }
            previousStream = outputStream
        }
        return segments.joinToString(";")
    }

    private fun createOutputFile(): File {
        return File(getOutputDir(), "ringtone_baked_${System.currentTimeMillis()}.mp4")
    }

    private fun getWorkspaceDir(): File {
        return File(appContext.cacheDir, "ringtone_ffmpeg").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun getOutputDir(): File {
        return File(appContext.cacheDir, "ringtone_video_baked").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun postToMain(block: () -> Unit) {
        mainHandler.post(block)
    }

    private fun InputStream.copyToFile(target: File) {
        target.outputStream().use { output ->
            copyTo(output)
        }
    }

    private data class VideoMetadata(
        val displayWidth: Int,
        val displayHeight: Int,
        val durationMs: Long
    )

    private data class OverlaySpec(
        val inputIndex: Int,
        val state: BadgeItemState,
        val videoWidth: Int,
        val videoHeight: Int,
        val badgeWidth: Int,
        val badgeHeight: Int
    ) {
        val left: Int
            get() = ((state.xPercent.coerceIn(0f, 1f) * videoWidth) - badgeWidth / 2f).toInt().coerceIn(0, maxLeft)

        val top: Int
            get() = ((state.yPercent.coerceIn(0f, 1f) * videoHeight) - badgeHeight / 2f).toInt().coerceIn(0, maxTop)

        private val maxLeft: Int
            get() = (videoWidth - badgeWidth).coerceAtLeast(0)

        private val maxTop: Int
            get() = (videoHeight - badgeHeight).coerceAtLeast(0)
    }

    private data class ExportedBadgeFile(
        val file: File,
        val width: Int,
        val height: Int
    )
}
