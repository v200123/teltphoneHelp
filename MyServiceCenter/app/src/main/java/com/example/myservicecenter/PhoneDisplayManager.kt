package com.example.myservicecenter

import android.content.Context
import com.example.myservicecenter.core.AppPreferences

/**
 * App 内所有电话号码的唯一展示入口。
 *
 * 页面只应调用 [display]，不直接读取号码或自行脱敏；以后调整号码来源、默认值或
 * 脱敏规则时，只需要修改此处。
 */
object PhoneDisplayManager {
    /** 当前由“号码信息自定义”统一管理的原始号码。 */
    fun managedPhone(context: Context, fallback: String = ""): String =
        AppPreferences.getCustomPhoneNumber(context).trim().ifBlank { fallback.trim() }

    /** 所有号码编辑界面的统一保存入口。 */
    fun updateManagedPhone(context: Context, value: String) {
        AppPreferences.setCustomPhoneNumber(context, value)
    }

    /** 当前号码的统一展示文本，默认按 3-4-4 规则脱敏。 */
    fun display(context: Context, fallback: String = "", masked: Boolean = true): String {
        val phone = managedPhone(context, fallback)
        if (phone.isBlank()) return "--"
        return if (masked) mask(phone) else phone
    }

    private fun mask(phone: String): String {
        if (phone.length < 7) return phone
        return buildString {
            append(phone.take(3))
            append("****")
            append(phone.takeLast(4))
        }
    }
}
