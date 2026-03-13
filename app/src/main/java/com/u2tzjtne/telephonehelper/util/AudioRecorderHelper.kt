package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import com.u2tzjtne.telephonehelper.base.App
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 录音工具类
 * 使用 MediaRecorder 实现通话录音功能
 * 
 * @author u2tzjtne@gmail.com
 */
class AudioRecorderHelper private constructor() {

    companion object {
        private const val TAG = "AudioRecorderHelper"
        
        @Volatile
        private var instance: AudioRecorderHelper? = null
        
        fun getInstance(): AudioRecorderHelper {
            return instance ?: synchronized(this) {
                instance ?: AudioRecorderHelper().also { instance = it }
            }
        }
    }

    // MediaRecorder 实例
    private var mediaRecorder: MediaRecorder? = null
    
    // 是否正在录音
    private var isRecording = false
    
    // 当前录音文件路径
    private var currentRecordingPath: String? = null
    
    // 录音开始时间
    private var recordingStartTime: Long = 0
    
    // 录音结束时间
    private var recordingEndTime: Long = 0
    
    // 录音文件保存目录
    private val recordingsDir: File by lazy {
        val dir = File(App.getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * 开始录音
     * @param phoneNumber 电话号码，用于生成文件名
     * @return 录音文件路径，如果失败返回 null
     */
    fun startRecording(phoneNumber: String): String? {
        if (isRecording) {
            Log.w(TAG, "已经正在录音中，请先停止当前录音")
            return currentRecordingPath
        }
        
        try {
            // 记录开始时间
            recordingStartTime = System.currentTimeMillis()
            
            // 生成文件名：号码_开始时间
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val startTimeStr = dateFormat.format(Date(recordingStartTime))
            val fileName = "${formatPhoneNumber(phoneNumber)}_${startTimeStr}.m4a"
            
            // 创建录音文件
            val recordingFile = File(recordingsDir, fileName)
            currentRecordingPath = recordingFile.absolutePath
            
            Log.d(TAG, "开始录音，文件路径: $currentRecordingPath")
            
            // 初始化 MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(App.getContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                // 设置音频源：麦克风
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // 设置输出格式：MPEG_4（支持更好的压缩）
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                // 设置音频编码器：AAC
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // 设置音频采样率
                setAudioSamplingRate(44100)
                // 设置音频比特率
                setAudioEncodingBitRate(128000)
                // 设置输出文件
                setOutputFile(currentRecordingPath)
                
                // 准备并开始录音
                prepare()
                start()
            }
            
            isRecording = true
            Log.d(TAG, "录音已开始")
            
            return currentRecordingPath
            
        } catch (e: IOException) {
            Log.e(TAG, "录音启动失败: ${e.message}")
            releaseMediaRecorder()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "录音启动异常: ${e.message}")
            releaseMediaRecorder()
            return null
        }
    }

    /**
     * 停止录音
     * @return 录音信息（文件路径、开始时间、结束时间），如果未在录音返回 null
     */
    fun stopRecording(): RecordingInfo? {
        if (!isRecording || mediaRecorder == null) {
            Log.w(TAG, "当前没有进行中的录音")
            return null
        }
        
        try {
            // 记录结束时间
            recordingEndTime = System.currentTimeMillis()
            
            // 停止并释放 MediaRecorder
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            Log.d(TAG, "录音已停止，文件: $currentRecordingPath")
            
            // 重命名文件（添加结束时间）
            val newPath = renameFileWithEndTime()
            
            return RecordingInfo(
                filePath = newPath ?: currentRecordingPath ?: "",
                startTime = recordingStartTime,
                endTime = recordingEndTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败: ${e.message}")
            releaseMediaRecorder()
            return null
        }
    }

    /**
     * 重命名文件，添加结束时间
     */
    private fun renameFileWithEndTime(): String? {
        currentRecordingPath?.let { path ->
            val oldFile = File(path)
            if (!oldFile.exists()) return null
            
            // 生成新的文件名：号码_开始时间_结束时间
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val endTimeStr = dateFormat.format(Date(recordingEndTime))
            
            val oldName = oldFile.name
            // 去掉原来的 .m4a 后缀
            val baseName = oldName.substringBeforeLast(".")
            val newFileName = "${baseName}_${endTimeStr}.m4a"
            
            val newFile = File(oldFile.parent, newFileName)
            if (oldFile.renameTo(newFile)) {
                currentRecordingPath = newFile.absolutePath
                Log.d(TAG, "文件重命名成功: ${newFile.absolutePath}")
                return newFile.absolutePath
            } else {
                Log.w(TAG, "文件重命名失败，使用原文件名")
            }
        }
        return null
    }

    /**
     * 释放 MediaRecorder 资源
     */
    private fun releaseMediaRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "释放 MediaRecorder 失败: ${e.message}")
        }
        mediaRecorder = null
        isRecording = false
    }

    /**
     * 取消录音（删除录音文件）
     */
    fun cancelRecording() {
        if (!isRecording) {
            return
        }
        
        // 先停止录音
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "取消录音时停止失败: ${e.message}")
        }
        
        mediaRecorder = null
        isRecording = false
        
        // 删除录音文件
        currentRecordingPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                if (file.delete()) {
                    Log.d(TAG, "录音文件已删除: $path")
                } else {
                    Log.w(TAG, "录音文件删除失败: $path")
                }
            }
        }
        currentRecordingPath = null
    }

    /**
     * 是否正在录音
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * 获取当前录音文件路径
     */
    fun getCurrentRecordingPath(): String? = currentRecordingPath

    /**
     * 格式化电话号码（去除空格和特殊字符）
     */
    private fun formatPhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[\\s\\-\\(\\)]"), "")
    }

    /**
     * 获取录音文件目录
     */
    fun getRecordingsDirectory(): File = recordingsDir

    /**
     * 获取所有录音文件列表
     */
    fun getAllRecordings(): List<File> {
        return recordingsDir.listFiles()?.filter { 
            it.extension == "m4a" || it.extension == "mp3" || it.extension == "amr"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 删除指定的录音文件
     */
    fun deleteRecording(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * 录音信息数据类
     */
    data class RecordingInfo(
        val filePath: String,      // 录音文件路径
        val startTime: Long,       // 开始时间（毫秒）
        val endTime: Long          // 结束时间（毫秒）
    ) {
        /**
         * 获取录音时长（毫秒）
         */
        fun getDuration(): Long = endTime - startTime
        
        /**
         * 获取格式化的时长字符串
         */
        fun getFormattedDuration(): String {
            val duration = getDuration() / 1000
            val minutes = duration / 60
            val seconds = duration % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
