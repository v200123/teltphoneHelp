package com.u2tzjtne.telephonehelper.util

import android.os.Handler
import android.os.Looper
import com.u2tzjtne.telephonehelper.db.AutoHangUpRule
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import kotlin.concurrent.thread

object AutoHangUpSettings {

    const val MIN_DELAY_SECONDS = 1
    const val MAX_DELAY_SECONDS = 3600

    private val mainHandler = Handler(Looper.getMainLooper())

    fun interface MatchCallback {
        fun onResult(rule: AutoHangUpRule?)
    }

    @JvmStatic
    fun resolveRuleAsync(phoneNumber: String?, callback: MatchCallback) {
        val normalized = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalized.isEmpty()) {
            callback.onResult(null)
            return
        }
        thread {
            val match = try {
                RingVideoDatabase.getInstance()
                    .autoHangUpRuleDao()
                    .getByPhoneNumber(normalized)
            } catch (_: Exception) {
                null
            }
            mainHandler.post {
                callback.onResult(match)
            }
        }
    }
}
