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
 * - 多个彩铃时随机选择一个播放
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
     * 播放彩铃视频 - 优先从数据库读取，如果没有则尝试默认视频
     * 优先级：Room数据库 > res/raw > assets > 外部存储
     *
     * @param context 上下文
     * @param packageName 应用包名
     * @param onVideoPlaying 视频是否正在播放的回调（主线程）
     *                       参数为 true 表示正在播放视频，false 表示没有找到视频
     */
    fun playRingtoneVideo(context: Context, packageName: String, onVideoPlaying: ((Boolean) -> Unit)? = null) {
        // 先尝试从数据库读取
        playRingtoneFromDatabase(context) {
            // 如果数据库没有彩铃（视图仍隐藏），则尝试默认视频
            if (videoView?.visibility != View.VISIBLE) {
                playDefaultRingtoneVideo(context, packageName)
            }
            // 回调通知视频播放状态
            onVideoPlaying?.invoke(videoView?.visibility == View.VISIBLE)
        }
    }

    /**
     * 播放默认彩铃视频（res/raw、assets、外部存储）
     */
    private fun playDefaultRingtoneVideo(context: Context, packageName: String) {
        // 尝试从 res/raw 加载
        val resId = try {
            context.resources.getIdentifier("ringtone_video", "raw", packageName)
        } catch (e: Exception) {
            0
        }

        if (resId != 0) {
            startPlaying(packageName, resId)
            return
        }

        // 尝试从 assets 加载
        try {
            context.assets.openFd(RINGTONE_VIDEO_ASSETS_PATH).use {
                videoView?.let { view ->
                    try {
                        view.setVideoPath("asset:///" + RINGTONE_VIDEO_ASSETS_PATH)
                        view.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                        view.visibility = View.GONE
                    }
                }
                return
            }
        } catch (e: Exception) {
            // assets 中不存在，继续尝试外部存储
        }

        // 尝试从外部存储加载
        val externalVideoFile = File(context.getExternalFilesDir(null), RINGTONE_VIDEO_FILENAME)
        if (externalVideoFile.exists()) {
            videoView?.let { view ->
                try {
                    view.setVideoPath(externalVideoFile.absolutePath)
                    view.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                    view.visibility = View.GONE
                }
            }
            return
        }

        // 没有找到视频文件，隐藏视频视图
        videoView?.visibility = View.GONE
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