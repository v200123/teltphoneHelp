package com.u2tzjtne.telephonehelper.util

import android.os.Handler
import android.os.Looper
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import kotlin.concurrent.thread

object CallPromptSettings {

    private val mainHandler = Handler(Looper.getMainLooper())

    enum class PromptType(
        val value: Int,
        val settingsTitle: String,
        val addDialogTitle: String,
        val addDialogMessage: String,
        val statusText: String,
        val rawName: String
    ) {
        POWER_OFF(
            value = 1,
            settingsTitle = "\u5173\u673a\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogTitle = "\u6dfb\u52a0\u5173\u673a\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogMessage = "\u8be5\u53f7\u7801\u62e8\u6253\u65f6\u5c06\u64ad\u653e\u5173\u673a\u63d0\u793a\u97f3",
            statusText = "\u5bf9\u65b9\u5df2\u5173\u673a",
            rawName = "audio_power_off"
        ),
        EMPTY_NUMBER(
            value = 2,
            settingsTitle = "\u7a7a\u53f7\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogTitle = "\u6dfb\u52a0\u7a7a\u53f7\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogMessage = "\u8be5\u53f7\u7801\u62e8\u6253\u65f6\u5c06\u64ad\u653e\u7a7a\u53f7\u63d0\u793a\u97f3",
            statusText = "\u60a8\u62e8\u6253\u7684\u662f\u7a7a\u53f7",
            rawName = "audio_empty_number"
        ),
        BUSY(
            value = 3,
            settingsTitle = "\u7528\u6237\u6b63\u5fd9\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogTitle = "\u6dfb\u52a0\u7528\u6237\u6b63\u5fd9\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogMessage = "\u8be5\u53f7\u7801\u62e8\u6253\u65f6\u5c06\u64ad\u653e\u7528\u6237\u6b63\u5fd9\u63d0\u793a\u97f3",
            statusText = "\u7528\u6237\u6b63\u5fd9",
            rawName = "audio_user_busy"
        ),
        UNREACHABLE(
            value = 4,
            settingsTitle = "\u65e0\u6cd5\u63a5\u901a\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogTitle = "\u6dfb\u52a0\u65e0\u6cd5\u63a5\u901a\u63d0\u793a\u97f3\u53f7\u7801",
            addDialogMessage = "\u8be5\u53f7\u7801\u62e8\u6253\u65f6\u5c06\u5148\u9759\u97f3\u7ea63\u79d2\uff0c\u518d\u64ad\u653e\u65e0\u6cd5\u63a5\u901a\u63d0\u793a\u97f3",
            statusText = "\u5bf9\u65b9\u6682\u65f6\u65e0\u6cd5\u63a5\u901a",
            rawName = "audio_busy"
        );

        val triggerDelayMillis: Long
            get() = if (this == UNREACHABLE) 3_000L else 1_000L

        val shouldPlayNormalDialBeforePrompt: Boolean
            get() = false

        companion object {
            fun fromValue(value: Int): PromptType {
                return entries.firstOrNull { it.value == value } ?: POWER_OFF
            }
        }
    }

    fun interface MatchCallback {
        fun onResult(promptType: PromptType?)
    }

    @JvmStatic
    fun resolvePromptTypeAsync(phoneNumber: String?, callback: MatchCallback) {
        val normalized = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalized.isEmpty()) {
            callback.onResult(null)
            return
        }
        thread {
            val match = try {
                RingVideoDatabase.getInstance()
                    .callPromptPhoneDao()
                    .getByPhoneNumber(normalized)
                    ?.let { PromptType.fromValue(it.promptType) }
            } catch (_: Exception) {
                null
            }
            mainHandler.post {
                callback.onResult(match)
            }
        }
    }
}
