package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.VideoView
import com.u2tzjtne.telephonehelper.db.RingVideo
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import java.io.File
import kotlin.concurrent.thread

/**
 * 视频播放器工具类 - 用于播放彩铃视频
 *
 * 功能说明：
 * - 在接通电话前播放彩铃视频
 * - 支持从 Room 数据库读取用户上传的彩铃视频
 * - 支持彩铃与手机号码绑定，一个彩铃可绑定多个号码
 * - 拨打电话时根据号码查找绑定的彩铃
 * - 没有绑定则不播放彩铃
 * - 支持循环播放
 * - 接通后自动停止并隐藏
 */
class VideoPlayerHelper private constructor() {

    private var videoView: VideoView? = null
    private var isPrepared = false
    private var currentContext: Context? = null

    companion object {
        @Volatile
        private var instance: VideoPlayerHelper? = null

        @JvmStatic
        fun getInstance(): VideoPlayerHelper {
            return instance ?: synchronized(this) {
                instance ?: VideoPlayerHelper().also { instance = it }
            }
        }

        // 默认视频文件名配置
        const val RINGTONE_VIDEO_FILENAME = "ringtone_video.mp4"
        const val RINGTONE_VIDEO_ASSETS_PATH = "video/ringtone_video.mp4"
    }

    /**
     * 初始化 VideoView
     * @param videoView 视频播放视图
     */
    fun init(videoView: VideoView) {
        this.videoView = videoView
        this.currentContext = videoView.context
        setupVideoView()
    }

    /**
     * 设置视频播放视图
     */
    private fun setupVideoView() {
        videoView?.setOnPreparedListener { mediaPlayer ->
            isPrepared = true
            // 设置循环播放
            mediaPlayer.isLooping = true
            // 开始播放
            mediaPlayer.start()
        }

        videoView?.setOnErrorListener { _, what, extra ->
            // 播放错误时隐藏视频视图
            videoView?.visibility = View.GONE
            false
        }

        videoView?.setOnCompletionListener {
            // 播放完成（非循环模式下）
            videoView?.visibility = View.GONE
        }
    }

    /**
     * 开始播放视频
     * @param uri 视频URI
     */
    fun startPlaying(uri: Uri) {
        videoView?.let { view ->
            try {
                view.setVideoURI(uri)
                view.visibility = View.VISIBLE
                // 准备完成后自动播放
            } catch (e: Exception) {
                e.printStackTrace()
                view.visibility = View.GONE
            }
        }
    }

    /**
     * 开始播放本地资源视频
     * @param packageName 应用包名
     * @param resId 视频资源ID
     */
    fun startPlaying(packageName: String, resId: Int) {
        val uri = Uri.parse("android.resource://$packageName/$resId")
        startPlaying(uri)
    }

    /**
     * 开始播放指定路径的视频文件
     * @param videoPath 视频文件路径
     */
    fun startPlaying(videoPath: String) {
        videoView?.let { view ->
            try {
                val file = File(videoPath)
                if (file.exists()) {
                    view.setVideoPath(videoPath)
                    view.visibility = View.VISIBLE
                } else {
                    // 文件不存在，尝试作为 URI 解析
                    val uri = Uri.parse(videoPath)
                    view.setVideoURI(uri)
                    view.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                view.visibility = View.GONE
            }
        }
    }

    /**
     * 格式化电话号码，去除空格和其他非数字字符
     * @param phoneNumber 原始电话号码
     * @return 纯数字格式的电话号码
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9]"), "")
    }

    /**
     * 从 Room 数据库获取指定电话号码绑定的彩铃
     * @param context 上下文
     * @param phoneNumber 电话号码（支持带空格或其他格式的号码）
     * @param onResult 查询结果回调（主线程），返回彩铃对象，没有则返回 null
     */
    fun getRingtoneByPhoneNumber(context: Context, phoneNumber: String, onResult: (RingVideo?) -> Unit) {
        thread {
            try {
                val db = RingVideoDatabase.getInstance()
                
                // 1. 格式化电话号码，去除空格等非数字字符
                val normalizedNumber = normalizePhoneNumber(phoneNumber)
                
                // 2. 查询号码绑定的彩铃ID
                val ringtoneId = db.ringtonePhoneBindingDao().getRingtoneIdByPhone(normalizedNumber)
                
                if (ringtoneId != null) {
                    // 3. 根据ID获取彩铃信息
                    val ringVideo = db.ringVideoDao().getByIdSync(ringtoneId)
                    videoView?.post {
                        onResult(ringVideo)
                    }
                } else {
                    // 号码未绑定彩铃
                    videoView?.post {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                videoView?.post {
                    onResult(null)
                }
            }
        }
    }

    /**
     * 从 Room 数据库获取彩铃视频并随机播放
     * @param context 上下文
     * @param onComplete 播放完成后的回调（主线程）
     */
    fun playRingtoneFromDatabase(context: Context, onComplete: (() -> Unit)? = null) {
        thread {
            try {
                val ringVideos = RingVideoDatabase.getInstance().ringVideoDao().getAllSync()

                if (ringVideos.isNotEmpty()) {
                    // 有彩铃视频，随机选择一个
                    val randomVideo = ringVideos.random()
                    val videoUri = randomVideo.videoUri

                    // 在主线程播放视频
                    videoView?.post {
                        if (!videoUri.isNullOrEmpty()) {
                            startPlaying(videoUri)
                        }
                        onComplete?.invoke()
                    }
                } else {
                    // 没有彩铃视频，在主线程回调
                    videoView?.post {
                        onComplete?.invoke()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                videoView?.post {
                    onComplete?.invoke()
                }
            }
        }
    }

    /**
     * 根据电话号码播放绑定的彩铃视频
     * - 如果号码绑定了彩铃，播放绑定的彩铃
     * - 如果没有绑定，不播放视频
     * 
     * @param context 上下文
     * @param phoneNumber 电话号码
     * @param onVideoPlaying 视频是否正在播放的回调（主线程）
     */
    fun playRingtoneByPhoneNumber(
        context: Context, 
        phoneNumber: String,
        onVideoPlaying: ((Boolean) -> Unit)? = null
    ) {
        getRingtoneByPhoneNumber(context, phoneNumber) { ringVideo ->
            if (ringVideo != null && !ringVideo.videoUri.isNullOrEmpty()) {
                // 找到绑定的彩铃，播放
                startPlaying(ringVideo.videoUri)
                onVideoPlaying?.invoke(true)
            } else {
                // 号码未绑定彩铃，不播放
                videoView?.visibility = View.GONE
                onVideoPlaying?.invoke(false)
            }
        }
    }

    /**
     * 播放彩铃视频 - 仅播放绑定的彩铃
     * 如果号码没有绑定彩铃，则不播放任何视频
     *
     * @param context 上下文
     * @param packageName 应用包名（保留参数用于兼容，实际不再使用）
     * @param phoneNumber 电话号码（用于查询绑定彩铃）
     * @param onVideoPlaying 视频是否正在播放的回调（主线程）
     *                       参数为 true 表示正在播放视频，false 表示号码未绑定彩铃
     */
    fun playRingtoneVideo(
        context: Context, 
        packageName: String,
        phoneNumber: String? = null,
        onVideoPlaying: ((Boolean) -> Unit)? = null
    ) {
        // 如果提供了号码，按号码查找绑定的彩铃
        if (!phoneNumber.isNullOrEmpty()) {
            playRingtoneByPhoneNumber(context, phoneNumber) { isPlaying ->
                // 只播放绑定的彩铃，没有绑定则不播放
                onVideoPlaying?.invoke(isPlaying)
            }
        } else {
            // 没有提供号码，不播放视频
            videoView?.visibility = View.GONE
            onVideoPlaying?.invoke(false)
        }
    }



    /**
     * 检查是否存在彩铃视频（包括数据库和默认位置）
     */
    fun hasRingtoneVideo(context: Context, packageName: String, callback: (Boolean) -> Unit) {
        thread {
            try {
                // 先检查数据库
                val count = RingVideoDatabase.getInstance().ringVideoDao().getCount()
                if (count > 0) {
                    context.mainExecutor.execute {
                        callback(true)
                    }
                    return@thread
                }

                // 检查 res/raw
                val resId = try {
                    context.resources.getIdentifier("ringtone_video", "raw", packageName)
                } catch (e: Exception) {
                    0
                }
                if (resId != 0) {
                    context.mainExecutor.execute {
                        callback(true)
                    }
                    return@thread
                }

                // 检查 assets
                try {
                    context.assets.openFd(RINGTONE_VIDEO_ASSETS_PATH).use {
                        context.mainExecutor.execute {
                            callback(true)
                        }
                        return@thread
                    }
                } catch (e: Exception) {
                    // ignore
                }

                // 检查外部存储
                val externalVideoFile = File(context.getExternalFilesDir(null), RINGTONE_VIDEO_FILENAME)
                val exists = externalVideoFile.exists()

                context.mainExecutor.execute {
                    callback(exists)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                context.mainExecutor.execute {
                    callback(false)
                }
            }
        }
    }

    /**
     * 停止播放并隐藏视频
     */
    fun stopPlaying() {
        videoView?.let { view ->
            if (view.isPlaying) {
                view.stopPlayback()
            }
            view.visibility = View.GONE
            view.suspend()
        }
        isPrepared = false
    }

    /**
     * 暂停播放
     */
    fun pausePlaying() {
        videoView?.let { view ->
            if (view.canPause()) {
                view.pause()
            }
        }
    }

    /**
     * 恢复播放
     */
    fun resumePlaying() {
        videoView?.let { view ->
            if (isPrepared && !view.isPlaying) {
                view.start()
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        videoView?.let { view ->
            if (view.isPlaying) {
                view.stopPlayback()
            }
            view.visibility = View.GONE
            view.suspend()
        }
        videoView = null
        currentContext = null
        isPrepared = false
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean {
        return videoView?.isPlaying ?: false
    }

    /**
     * 设置视频可见性
     */
    fun setVisibility(visibility: Int) {
        videoView?.visibility = visibility
    }
}