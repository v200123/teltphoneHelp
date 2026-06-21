package com.example.myservicecenter

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "service_center_prefs"
    private const val KEY_OUTGOING_PACKAGE_INFO = "outgoing_package_info"
    private const val KEY_CUSTOM_PHONE_NUMBER = "custom_phone_number"
    private const val KEY_CUSTOM_DISPLAY_NAME = "custom_display_name"
    private const val KEY_CUSTOM_CALL_TYPE_OUTGOING = "custom_call_type_outgoing"
    private const val KEY_CUSTOM_CALL_TYPE_INCOMING = "custom_call_type_incoming"
    private const val KEY_CUSTOM_STAR_LEVEL = "custom_star_level"
    private const val KEY_CUSTOM_SELF_REGION = "custom_self_region"
    private const val KEY_CACHED_CALL_RECORDS = "cached_call_records"
    private const val KEY_MINE_STAT_COUPON = "mine_stat_coupon"
    private const val KEY_MINE_STAT_DATA = "mine_stat_data"
    private const val KEY_MINE_STAT_BALANCE = "mine_stat_balance"
    private const val KEY_MINE_STAT_BEAN = "mine_stat_bean"
    private const val KEY_WEBVIEW_HOME_DATA = "webview_home_data"
    private const val KEY_WEBVIEW_HOME_BALANCE = "webview_home_balance"
    private const val KEY_WEBVIEW_HOME_CALL_MINUTES = "webview_home_call_minutes"
    private const val KEY_WEBVIEW_HOME_PENDING_RIGHTS = "webview_home_pending_rights"
    private const val KEY_WEBVIEW_FIXED_FEE_LIST_JSON = "webview_fixed_fee_list_json"

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

    fun getCustomDisplayName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_DISPLAY_NAME, "")
            .orEmpty()
    }

    fun setCustomDisplayName(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_DISPLAY_NAME, value.trim())
            .apply()
    }

    fun getCustomOutgoingCallType(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_CALL_TYPE_OUTGOING, "")
            .orEmpty()
    }

    fun setCustomOutgoingCallType(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_CALL_TYPE_OUTGOING, value.trim())
            .apply()
    }

    fun getCustomIncomingCallType(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_CALL_TYPE_INCOMING, "")
            .orEmpty()
    }

    fun setCustomIncomingCallType(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_CALL_TYPE_INCOMING, value.trim())
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

    // 我的页面统计数值
    fun getMineStatCoupon(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MINE_STAT_COUPON, "5")
            .orEmpty()
    }

    fun setMineStatCoupon(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MINE_STAT_COUPON, value.trim())
            .apply()
    }

    fun getMineStatData(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MINE_STAT_DATA, "42.92")
            .orEmpty()
    }

    fun setMineStatData(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MINE_STAT_DATA, value.trim())
            .apply()
    }

    fun getMineStatBalance(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MINE_STAT_BALANCE, "724.48")
            .orEmpty()
    }

    fun setMineStatBalance(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MINE_STAT_BALANCE, value.trim())
            .apply()
    }

    fun getMineStatBean(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MINE_STAT_BEAN, "1833")
            .orEmpty()
    }

    fun setMineStatBean(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MINE_STAT_BEAN, value.trim())
            .apply()
    }

    fun getWebViewHomeData(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBVIEW_HOME_DATA, "19.19")
            .orEmpty()
    }

    fun setWebViewHomeData(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBVIEW_HOME_DATA, value.trim())
            .apply()
    }

    fun getWebViewHomeBalance(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBVIEW_HOME_BALANCE, "547.58")
            .orEmpty()
    }

    fun setWebViewHomeBalance(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBVIEW_HOME_BALANCE, value.trim())
            .apply()
    }

    fun getWebViewHomeCallMinutes(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBVIEW_HOME_CALL_MINUTES, "200")
            .orEmpty()
    }

    fun setWebViewHomeCallMinutes(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBVIEW_HOME_CALL_MINUTES, value.trim())
            .apply()
    }

    fun getWebViewHomePendingRights(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBVIEW_HOME_PENDING_RIGHTS, "0")
            .orEmpty()
    }

    fun setWebViewHomePendingRights(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBVIEW_HOME_PENDING_RIGHTS, value.trim())
            .apply()
    }

    fun getWebViewFixedFeeListJson(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(
                KEY_WEBVIEW_FIXED_FEE_LIST_JSON,
                """
                [
                  {
                    "name": "自由选套餐8元档（语音版）",
                    "cycle": "2026-06-01至2026-06-21",
                    "fee": 8.00,
                    "description": ""
                  }
                ]
                """.trimIndent()
            )
            .orEmpty()
    }

    fun setWebViewFixedFeeListJson(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBVIEW_FIXED_FEE_LIST_JSON, value.trim())
            .apply()
    }
}
