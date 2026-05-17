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
            settingsTitle = "关机提示音号码",
            addDialogTitle = "添加关机提示音号码",
            addDialogMessage = "该号码拨打时将播放关机提示音",
            statusText = "正在拨号",
            rawName = "audio_power_off"
        ),
        EMPTY_NUMBER(
            value = 2,
            settingsTitle = "空号提示音号码",
            addDialogTitle = "添加空号提示音号码",
            addDialogMessage = "该号码拨打时将播放空号提示音",
            statusText = "正在拨号",
            rawName = "audio_empty_number"
        ),
        BUSY(
            value = 3,
            settingsTitle = "用户正忙提示音号码",
            addDialogTitle = "添加用户正忙提示音号码",
            addDialogMessage = "该号码拨打时将播放用户正忙提示音",
            statusText = "正在拨号",
            rawName = "audio_user_busy"
        ),
        UNREACHABLE(
            value = 4,
            settingsTitle = "无法接通提示音号码",
            addDialogTitle = "添加无法接通提示音号码",
            addDialogMessage = "该号码拨打时将先静音约3秒，再播放无法接通提示音",
            statusText = "正在拨号",
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
