package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.u2tzjtne.telephonehelper.db.RingVideo
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.widget.EmptyControlVideo
import java.io.File
import kotlin.concurrent.thread

/**
 * GSY视频播放器工具类 - 用于播放彩铃视频
 *
 * 功能说明:
 * - 在接通电话前播放彩铃视频
 * - 支持从 Room 数据库读取用户上传的彩铃视频
 * - 支持彩铃与手机号码绑定,一个彩铃可绑定多个号码
 * - 拨打电话时根据号码查找绑定的彩铃
 * - 没有绑定则不播放彩铃
 * - 支持循环播放
 * - 接通后自动停止并隐藏
 * - 使用 GSYVideoPlayer 避免加载黑屏问题
 * - 使用 EmptyControlVideo 移除所有控制UI
 */
class GSYVideoPlayerHelper private constructor() {

    private var videoPlayer: EmptyControlVideo? = null
    private var currentContext: Context? = null
    private var isPlaying = false

    companion object {
        @Volatile
        private var instance: GSYVideoPlayerHelper? = null

        @JvmStatic
        fun getInstance(): GSYVideoPlayerHelper {
            return instance ?: synchronized(this) {
                instance ?: GSYVideoPlayerHelper().also { instance = it }
            }
        }
    }

    /**
     * 初始化 GSYVideoPlayer
     * @param player 视频播放视图
     */
    fun init(player: EmptyControlVideo) {
        android.util.Log.d("GSYVideoPlayerHelper", "初始化播放器")
        this.videoPlayer = player
        this.currentContext = player.context
        // EmptyControlVideo 已经在布局和构造函数中移除了所有控制UI,无需额外设置
    }

    /**
     * 设置视频背景图
     * @param bitmap 背景图片
     */
    fun setBackground(bitmap: Bitmap) {
        videoPlayer?.background = BitmapDrawable(currentContext?.resources, bitmap)
    }

    /**
     * 开始播放视频
     * @param uri 视频URI
     */
    fun startPlaying(uri: Uri) {
        videoPlayer?.let { player ->
            try {
                GSYVideoOptionBuilder()
                    .setUrl(uri.toString())
                    .setCacheWithPlay(true)
                    .setLooping(true)
                    .build(player)
                player.visibility = View.VISIBLE
                player.startPlayLogic()
                isPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
                player.visibility = View.GONE
                isPlaying = false
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
        android.util.Log.d("GSYVideoPlayerHelper", "startPlaying 被调用，路径: $videoPath")
        videoPlayer?.let { player ->
            try {
                val file = File(videoPath)
                val uri = if (file.exists()) {
                    android.util.Log.d("GSYVideoPlayerHelper", "文件存在，使用文件URI")
                    Uri.fromFile(file)
                } else {
                    android.util.Log.d("GSYVideoPlayerHelper", "文件不存在，尝试作为URI解析")
                    Uri.parse(videoPath)
                }

                android.util.Log.d("GSYVideoPlayerHelper", "最终URI: $uri")
                
                GSYVideoOptionBuilder()
                    .setUrl(uri.toString())
                    .setCacheWithPlay(true)
                    .setLooping(true)
                    .build(player)
                
                player.visibility = View.VISIBLE
                player.startPlayLogic()
                isPlaying = true
                android.util.Log.d("GSYVideoPlayerHelper", "视频播放器启动成功")
            } catch (e: Exception) {
                android.util.Log.e("GSYVideoPlayerHelper", "播放失败: ${e.message}", e)
                e.printStackTrace()
                player.visibility = View.GONE
                isPlaying = false
            }
        } ?: android.util.Log.e("GSYVideoPlayerHelper", "videoPlayer 为 null，无法播放")
    }

    /**
     * 格式化电话号码,去除空格和其他非数字字符
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9]"), "")
    }

    /**
     * 从 Room 数据库获取指定电话号码绑定的彩铃
     */
    fun getRingtoneByPhoneNumber(context: Context, phoneNumber: String, onResult: (RingVideo?) -> Unit) {
        thread {
            try {
                val db = RingVideoDatabase.getInstance()
                val normalizedNumber = normalizePhoneNumber(phoneNumber)
                android.util.Log.d("GSYVideoPlayerHelper", "查询号码: $normalizedNumber (原始: $phoneNumber)")
                
                val ringtoneId = db.ringtonePhoneBindingDao().getRingtoneIdByPhone(normalizedNumber)
                android.util.Log.d("GSYVideoPlayerHelper", "查询到的彩铃ID: $ringtoneId")
                
                if (ringtoneId != null) {
                    val ringVideo = db.ringVideoDao().getByIdSync(ringtoneId)
                    android.util.Log.d("GSYVideoPlayerHelper", "查询到的视频: ${ringVideo?.videoName}, URI: ${ringVideo?.videoUri}")
                    videoPlayer?.post {
                        onResult(ringVideo)
                    }
                } else {
                    android.util.Log.d("GSYVideoPlayerHelper", "该号码未绑定彩铃")
                    videoPlayer?.post {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GSYVideoPlayerHelper", "查询彩铃失败: ${e.message}", e)
                e.printStackTrace()
                videoPlayer?.post {
                    onResult(null)
                }
            }
        }
    }

    /**
     * 根据电话号码播放绑定的彩铃视频
     */
    fun playRingtoneByPhoneNumber(
        context: Context,
        phoneNumber: String,
        onVideoPlaying: ((Boolean) -> Unit)? = null
    ) {
        android.util.Log.d("GSYVideoPlayerHelper", "准备播放彩铃，号码: $phoneNumber")
        getRingtoneByPhoneNumber(context, phoneNumber) { ringVideo ->
            if (ringVideo != null && !ringVideo.videoUri.isNullOrEmpty()) {
                android.util.Log.d("GSYVideoPlayerHelper", "开始播放彩铃: ${ringVideo.videoName}")
                startPlaying(ringVideo.videoUri)
                onVideoPlaying?.invoke(true)
            } else {
                android.util.Log.d("GSYVideoPlayerHelper", "没有找到彩铃或视频URI为空")
                videoPlayer?.visibility = View.GONE
                onVideoPlaying?.invoke(false)
            }
        }
    }

    /**
     * 播放彩铃视频 - 仅播放绑定的彩铃
     */
    fun playRingtoneVideo(
        context: Context,
        packageName: String,
        phoneNumber: String? = null,
        onVideoPlaying: ((Boolean) -> Unit)? = null
    ) {
        if (!phoneNumber.isNullOrEmpty()) {
            playRingtoneByPhoneNumber(context, phoneNumber) { isPlaying ->
                onVideoPlaying?.invoke(isPlaying)
            }
        } else {
            videoPlayer?.visibility = View.GONE
            onVideoPlaying?.invoke(false)
        }
    }

    /**
     * 停止播放并隐藏视频
     */
    fun stopPlaying() {
        videoPlayer?.let { player ->
            if (isPlaying) {
                player.onVideoReset()
            }
            player.visibility = View.GONE
            isPlaying = false
        }
    }

    /**
     * 暂停播放
     */
    fun pausePlaying() {
        videoPlayer?.onVideoPause()
        isPlaying = false
    }

    /**
     * 恢复播放
     */
    fun resumePlaying() {
        videoPlayer?.onVideoResume()
        isPlaying = true
    }

    /**
     * 释放资源
     */
    fun release() {
        videoPlayer?.let { player ->
            if (isPlaying) {
                player.onVideoReset()
            }
            player.visibility = View.GONE
        }
        videoPlayer = null
        currentContext = null
        isPlaying = false
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean {
        return isPlaying
    }

    /**
     * 设置视频可见性
     */
    fun setVisibility(visibility: Int) {
        videoPlayer?.visibility = visibility
    }
}
