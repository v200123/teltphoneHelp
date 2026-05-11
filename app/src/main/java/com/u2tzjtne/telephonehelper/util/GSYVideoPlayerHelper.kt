package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.u2tzjtne.telephonehelper.db.PhoneRingtoneAssignment
import com.u2tzjtne.telephonehelper.db.RingVideo
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.widget.EmptyControlVideo
import java.io.File
import kotlin.concurrent.thread

/**
 * Helper for ringtone video playback.
 * In ringtone library mode, each new call advances to the next item in the library.
 */
class GSYVideoPlayerHelper private constructor() {

    private var videoPlayer: EmptyControlVideo? = null
    private var currentContext: Context? = null
    private var isPlaying = false

    companion object {
        private const val PREFS_NAME = "ringtone_playback_prefs"
        private const val KEY_LAST_PLAYED_RINGTONE_ID = "key_last_played_ringtone_id"

        @Volatile
        private var instance: GSYVideoPlayerHelper? = null

        @JvmStatic
        fun getInstance(): GSYVideoPlayerHelper {
            return instance ?: synchronized(this) {
                instance ?: GSYVideoPlayerHelper().also { instance = it }
            }
        }
    }

    fun init(player: EmptyControlVideo) {
        android.util.Log.d("GSYVideoPlayerHelper", "init player")
        videoPlayer = player
        currentContext = player.context
    }

    fun setBackground(bitmap: Bitmap) {
        videoPlayer?.background = BitmapDrawable(currentContext?.resources, bitmap)
    }

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
                android.util.Log.e("GSYVideoPlayerHelper", "play failed: ${e.message}", e)
                player.visibility = View.GONE
                isPlaying = false
            }
        }
    }

    fun startPlaying(packageName: String, resId: Int) {
        val uri = Uri.parse("android.resource://$packageName/$resId")
        startPlaying(uri)
    }

    fun startPlaying(videoPath: String) {
        android.util.Log.d("GSYVideoPlayerHelper", "startPlaying path=$videoPath")
        videoPlayer?.let { player ->
            try {
                val file = File(videoPath)
                val uri = if (file.exists()) {
                    android.util.Log.d("GSYVideoPlayerHelper", "use file uri")
                    Uri.fromFile(file)
                } else {
                    android.util.Log.d("GSYVideoPlayerHelper", "parse raw uri")
                    Uri.parse(videoPath)
                }

                android.util.Log.d("GSYVideoPlayerHelper", "resolved uri=$uri")

                GSYVideoOptionBuilder()
                    .setUrl(uri.toString())
                    .setCacheWithPlay(true)
                    .setLooping(true)
                    .build(player)

                player.visibility = View.VISIBLE
                player.startPlayLogic()
                isPlaying = true
                android.util.Log.d("GSYVideoPlayerHelper", "player started")
            } catch (e: Exception) {
                android.util.Log.e("GSYVideoPlayerHelper", "play failed: ${e.message}", e)
                player.visibility = View.GONE
                isPlaying = false
            }
        } ?: android.util.Log.e("GSYVideoPlayerHelper", "videoPlayer is null")
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9]"), "")
    }

    private fun isNoRingtonePhone(phoneNumber: String): Boolean {
        return try {
            RingVideoDatabase.getInstance().noRingtonePhoneDao().isNoRingtonePhone(phoneNumber)
        } catch (e: Exception) {
            android.util.Log.e("GSYVideoPlayerHelper", "no-ringtone check failed: ${e.message}")
            false
        }
    }

    private fun getPlaybackPreferences() =
        currentContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Reuses an existing phone assignment first; otherwise assigns the next ringtone in sequence.
     */
    @Synchronized
    private fun getOrAssignRingtone(phoneNumber: String): RingVideo? {
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        val db = RingVideoDatabase.getInstance()

        if (isNoRingtonePhone(normalizedNumber)) {
            android.util.Log.d("GSYVideoPlayerHelper", "phone is excluded: $normalizedNumber")
            return null
        }

        val allRingtones = db.ringVideoDao().getAllSync()
        if (allRingtones.isEmpty()) {
            android.util.Log.d("GSYVideoPlayerHelper", "ringtone library is empty")
            return null
        }

        val existingAssignment = db.phoneRingtoneAssignmentDao().getByPhoneNumber(normalizedNumber)
        if (existingAssignment != null) {
            val assignedRingtone = db.ringVideoDao().getByIdSync(existingAssignment.ringtoneId)
            if (assignedRingtone != null && !assignedRingtone.videoUri.isNullOrBlank()) {
                android.util.Log.d(
                    "GSYVideoPlayerHelper",
                    "reuse assigned ringtone for $normalizedNumber: ${assignedRingtone.videoName} (id=${assignedRingtone.id})"
                )
                return assignedRingtone
            }
        }

        val prefs = getPlaybackPreferences()
        val lastPlayedRingtoneId = prefs?.getInt(KEY_LAST_PLAYED_RINGTONE_ID, -1) ?: -1
        val lastPlayedIndex = allRingtones.indexOfFirst { it.id == lastPlayedRingtoneId }
        val nextIndex = if (lastPlayedIndex >= 0) {
            (lastPlayedIndex + 1) % allRingtones.size
        } else {
            0
        }
        val nextRingtone = allRingtones[nextIndex]

        android.util.Log.d(
            "GSYVideoPlayerHelper",
            "sequential ringtone for $normalizedNumber: ${nextRingtone.videoName} (id=${nextRingtone.id}, index=$nextIndex)"
        )

        prefs?.edit()?.putInt(KEY_LAST_PLAYED_RINGTONE_ID, nextRingtone.id)?.apply()

        val assignment = PhoneRingtoneAssignment(normalizedNumber, nextRingtone.id).apply {
            if (existingAssignment != null) {
                id = existingAssignment.id
            }
        }
        db.phoneRingtoneAssignmentDao().insert(assignment)

        return nextRingtone
    }

    fun playRingtoneByPhoneNumber(
        context: Context,
        phoneNumber: String,
        onVideoPlaying: ((Boolean) -> Unit)? = null
    ) {
        android.util.Log.d("GSYVideoPlayerHelper", "prepare ringtone for phone=$phoneNumber")

        if (currentContext == null) {
            currentContext = context.applicationContext
        }

        thread {
            try {
                val ringVideo = getOrAssignRingtone(phoneNumber)

                videoPlayer?.post {
                    if (ringVideo != null && !ringVideo.videoUri.isNullOrEmpty()) {
                        android.util.Log.d("GSYVideoPlayerHelper", "start ringtone=${ringVideo.videoName}")
                        startPlaying(ringVideo.videoUri)
                        onVideoPlaying?.invoke(true)
                    } else {
                        android.util.Log.d("GSYVideoPlayerHelper", "skip ringtone playback")
                        videoPlayer?.visibility = View.GONE
                        onVideoPlaying?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GSYVideoPlayerHelper", "ringtone playback failed: ${e.message}", e)
                videoPlayer?.post {
                    videoPlayer?.visibility = View.GONE
                    onVideoPlaying?.invoke(false)
                }
            }
        }
    }

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

    fun stopPlaying() {
        videoPlayer?.let { player ->
            if (isPlaying) {
                player.onVideoReset()
            }
            player.visibility = View.GONE
            isPlaying = false
        }
    }

    fun pausePlaying() {
        videoPlayer?.onVideoPause()
        isPlaying = false
    }

    fun resumePlaying() {
        videoPlayer?.onVideoResume()
        isPlaying = true
    }

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

    fun isPlaying(): Boolean {
        return isPlaying
    }

    fun setVisibility(visibility: Int) {
        videoPlayer?.visibility = visibility
    }
}
