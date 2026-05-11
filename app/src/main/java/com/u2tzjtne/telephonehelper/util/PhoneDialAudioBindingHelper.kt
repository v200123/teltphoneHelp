package com.u2tzjtne.telephonehelper.util

/**
 * 号码拨打音模式绑定
 * 首次拨打某个号码时，记录当时使用的模式；后续继续复用该模式。
 */
object PhoneDialAudioBindingHelper {

    private const val KEY_MODE_PREFIX = "key_phone_dial_audio_mode_"
    private const val KEY_MUSIC_ID_PREFIX = "key_phone_music_id_"

    data class PhoneAudioBinding(
        val phoneNumber: String,
        val mode: CallDialAudioSettings.DialAudioMode,
    )

    @JvmStatic
    fun resolveMode(phoneNumber: String): CallDialAudioSettings.DialAudioMode {
        val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        val currentMode = CallDialAudioSettings.getMode()
        if (normalizedNumber.isBlank()) {
            return currentMode
        }
        val storedValue = SPUtils.getInt(modeKey(normalizedNumber), Int.MIN_VALUE)
        if (storedValue != Int.MIN_VALUE) {
            return CallDialAudioSettings.DialAudioMode.fromValue(storedValue)
        }
        if (getAssignedMusicId(normalizedNumber) > 0) {
            saveMode(normalizedNumber, CallDialAudioSettings.DialAudioMode.MUSIC_LIBRARY)
            return CallDialAudioSettings.DialAudioMode.MUSIC_LIBRARY
        }
        saveMode(normalizedNumber, currentMode)
        return currentMode
    }

    @JvmStatic
    fun saveMode(phoneNumber: String, mode: CallDialAudioSettings.DialAudioMode) {
        val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank()) {
            return
        }
        SPUtils.putInt(modeKey(normalizedNumber), mode.value)
    }

    @JvmStatic
    fun getAssignedMusicId(phoneNumber: String): Int {
        val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank()) {
            return -1
        }
        return SPUtils.getInt(musicIdKey(normalizedNumber), -1)
    }

    @JvmStatic
    fun saveAssignedMusicId(phoneNumber: String, musicId: Int) {
        val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank() || musicId <= 0) {
            return
        }
        SPUtils.putInt(musicIdKey(normalizedNumber), musicId)
    }

    @JvmStatic
    fun clearBindings(phoneNumber: String) {
        val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank()) {
            return
        }
        SPUtils.remove(modeKey(normalizedNumber))
        SPUtils.remove(musicIdKey(normalizedNumber))
    }

    @JvmStatic
    fun getAllBindings(): List<PhoneAudioBinding> {
        return SPUtils.getAll()
            .mapNotNull { (key, value) ->
                if (!key.startsWith(KEY_MODE_PREFIX)) {
                    return@mapNotNull null
                }
                val phoneNumber = key.removePrefix(KEY_MODE_PREFIX)
                val modeValue = value as? Int ?: return@mapNotNull null
                if (phoneNumber.isBlank()) {
                    return@mapNotNull null
                }
                PhoneAudioBinding(phoneNumber, CallDialAudioSettings.DialAudioMode.fromValue(modeValue))
            }
            .sortedByDescending { it.phoneNumber }
    }

    @JvmStatic
    fun clearAllBindings() {
        val allKeys = SPUtils.getAll().keys
        allKeys.forEach { key ->
            if (key.startsWith(KEY_MODE_PREFIX) || key.startsWith(KEY_MUSIC_ID_PREFIX)) {
                SPUtils.remove(key)
            }
        }
    }

    private fun modeKey(phoneNumber: String): String = KEY_MODE_PREFIX + phoneNumber

    private fun musicIdKey(phoneNumber: String): String = KEY_MUSIC_ID_PREFIX + phoneNumber
}
