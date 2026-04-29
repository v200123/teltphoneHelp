package com.example.myservicecenter

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "service_center_prefs"
    private const val KEY_OUTGOING_PACKAGE_INFO = "outgoing_package_info"
    private const val KEY_CUSTOM_PHONE_NUMBER = "custom_phone_number"
    private const val KEY_CUSTOM_STAR_LEVEL = "custom_star_level"
    private const val KEY_CUSTOM_SELF_REGION = "custom_self_region"
    private const val KEY_CACHED_CALL_RECORDS = "cached_call_records"

    fun getOutgoingPackageInfo(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OUTGOING_PACKAGE_INFO, "")
            .orEmpty()
    }

    fun setOutgoingPackageInfo(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_OUTGOING_PACKAGE_INFO, value.trim())
            .apply()
    }

    fun getCustomPhoneNumber(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_PHONE_NUMBER, "")
            .orEmpty()
    }

    fun setCustomPhoneNumber(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_PHONE_NUMBER, value.trim())
            .apply()
    }

    fun getCustomStarLevel(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_CUSTOM_STAR_LEVEL, 5)
    }

    fun setCustomStarLevel(context: Context, value: Int) {
        val normalized = value.coerceIn(1, 5)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CUSTOM_STAR_LEVEL, normalized)
            .apply()
    }

    fun getCustomSelfRegion(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_SELF_REGION, "")
            .orEmpty()
    }

    fun setCustomSelfRegion(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_SELF_REGION, value.trim())
            .apply()
    }
}
