package com.u2tzjtne.telephonehelper.util

object CallVibrationSettings {
    private const val KEY_CALL_VIBRATION_DURATION_MS = "key_call_vibration_duration_ms"
    const val DEFAULT_DURATION_MS = 230
    const val MIN_DURATION_MS = 0
    const val MAX_DURATION_MS = 2000

    fun getDurationMs(): Int {
        val storedValue = SPUtils.getInt(KEY_CALL_VIBRATION_DURATION_MS, DEFAULT_DURATION_MS)
        return storedValue.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
    }

    fun saveDurationMs(durationMs: Int) {
        SPUtils.putInt(KEY_CALL_VIBRATION_DURATION_MS, durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS))
    }

    fun formatDurationText(durationMs: Int): String {
        return "${durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)}ms"
    }
}
