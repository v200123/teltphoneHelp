package com.u2tzjtne.telephonehelper.util

/**
 * 拨打阶段音频播放设置
 */
object CallDialAudioSettings {

    private const val KEY_DIAL_AUDIO_MODE = "key_dial_audio_mode"

    enum class DialAudioMode(val value: Int, val label: String) {
        NORMAL(0, "普通拨打音"),
        MUSIC_LIBRARY(1, "音乐库"),
        RINGTONE_LIBRARY(2, "彩铃库");

        companion object {
            fun fromValue(value: Int): DialAudioMode {
                return entries.firstOrNull { it.value == value } ?: NORMAL
            }
        }
    }

    @JvmStatic
    fun getMode(): DialAudioMode {
        return DialAudioMode.fromValue(SPUtils.getInt(KEY_DIAL_AUDIO_MODE, DialAudioMode.NORMAL.value))
    }

    @JvmStatic
    fun saveMode(mode: DialAudioMode) {
        SPUtils.putInt(KEY_DIAL_AUDIO_MODE, mode.value)
    }

    @JvmStatic
    fun getModeLabel(): String = getMode().label
}
