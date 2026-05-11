package com.u2tzjtne.telephonehelper.util

import android.content.Context
import android.net.Uri
import com.u2tzjtne.telephonehelper.db.MusicFile
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import kotlin.concurrent.thread

/**
 * 音乐库播放辅助
 */
object MusicPlaybackHelper {

    private const val PREFS_NAME = "music_playback_prefs"
    private const val KEY_LAST_PLAYED_MUSIC_ID = "key_last_played_music_id"

    fun interface MusicPlaybackCallback {
        fun onResult(didStart: Boolean, musicFile: MusicFile?)
    }

    private fun getPlaybackPreferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    private fun getNextMusic(context: Context): MusicFile? {
        val allMusics = RingVideoDatabase.getInstance().musicFileDao().getAllSync()
        if (allMusics.isEmpty()) {
            return null
        }

        val prefs = getPlaybackPreferences(context)
        val lastPlayedMusicId = prefs.getInt(KEY_LAST_PLAYED_MUSIC_ID, -1)
        val lastPlayedIndex = allMusics.indexOfFirst { it.id == lastPlayedMusicId }
        val nextIndex = if (lastPlayedIndex >= 0) {
            (lastPlayedIndex + 1) % allMusics.size
        } else {
            0
        }
        val nextMusic = allMusics[nextIndex]

        prefs.edit().putInt(KEY_LAST_PLAYED_MUSIC_ID, nextMusic.id).apply()
        return nextMusic
    }

    @JvmStatic
    fun playSelectedMusic(context: Context, onResult: MusicPlaybackCallback? = null) {
        thread {
            try {
                val nextMusic = getNextMusic(context)
                if (nextMusic?.audioUri.isNullOrBlank()) {
                    postResult(context, false, null, onResult)
                    return@thread
                }
                val didStart = MediaPlayerHelper.getInstance()
                    .playUri(context, Uri.parse(nextMusic.audioUri), true, null)
                postResult(context, didStart, if (didStart) nextMusic else null, onResult)
            } catch (_: Exception) {
                postResult(context, false, null, onResult)
            }
        }
    }

    private fun postResult(
        context: Context,
        didStart: Boolean,
        musicFile: MusicFile?,
        onResult: MusicPlaybackCallback?,
    ) {
        if (onResult == null) {
            return
        }
        if (context is android.app.Activity) {
            context.runOnUiThread {
                onResult.onResult(didStart, musicFile)
            }
        } else {
            onResult.onResult(didStart, musicFile)
        }
    }
}
