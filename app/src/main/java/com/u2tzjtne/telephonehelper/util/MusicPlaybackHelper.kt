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

    fun interface MusicPlaybackCallback {
        fun onResult(didStart: Boolean, musicFile: MusicFile?)
    }

    @JvmStatic
    fun playSelectedMusic(context: Context, onResult: MusicPlaybackCallback? = null) {
        thread {
            try {
                val selectedMusic = RingVideoDatabase.getInstance().musicFileDao().getSelectedSync()
                if (selectedMusic?.audioUri.isNullOrBlank()) {
                    postResult(context, false, null, onResult)
                    return@thread
                }
                val didStart = MediaPlayerHelper.getInstance()
                    .playUri(context, Uri.parse(selectedMusic.audioUri), true, null)
                postResult(context, didStart, if (didStart) selectedMusic else null, onResult)
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
