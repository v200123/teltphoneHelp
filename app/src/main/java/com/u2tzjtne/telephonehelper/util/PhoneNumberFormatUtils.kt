package com.u2tzjtne.telephonehelper.util

/**
 * 电话号码格式化工具类
 */
object PhoneNumberFormatUtils {

    /**
     * 将电话号码格式化为带空格的形式
     * 例如：13812345678 -> 138 1234 5678
     */
    @JvmStatic
    fun formatWithSpaces(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val digits = text.filter { it.isDigit() }
        return when {
            digits.isEmpty() -> ""
            digits.length <= 3 -> digits
            digits.length == 12 -> digits.substring(0, 4) + " " + digits.substring(4, 8) + " " + digits.substring(8, 12)
            digits.length <= 7 -> digits.substring(0, 3) + " " + digits.substring(3)
            else -> digits.substring(0, 3) + " " + digits.substring(3, 7) + " " + digits.substring(7)
        }
    }
}
