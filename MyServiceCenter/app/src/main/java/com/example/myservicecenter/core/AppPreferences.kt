package com.example.myservicecenter.core

import android.content.Context
import android.net.Uri

object AppPreferences {
    // Preference keys
    private const val PREFS_NAME = "service_center_prefs"
    private const val KEY_OUTGOING_PACKAGE_INFO = "outgoing_package_info"
    private const val KEY_INCOMING_PACKAGE_INFO = "incoming_package_info"
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
    private const val KEY_WEBVIEW_HOME_CALL_MINUTES = "webview_home_call_minutes"
    private const val KEY_WEBVIEW_HOME_POINTS = "webview_home_points"
    private const val KEY_WEBVIEW_HOME_PENDING_RIGHTS = "webview_home_pending_rights"
    private const val KEY_WEBVIEW_FIXED_FEE_LIST_JSON = "webview_fixed_fee_list_json"
    private const val KEY_HOME_CURRENT_DEVICE_IMAGE_URI = "home_current_device_image_uri"
    private const val KEY_HOME_CURRENT_DEVICE_VIDEO_URI = "home_current_device_video_uri"
    private const val KEY_HOME_CURRENT_DEVICE_MEDIA_TYPE = "home_current_device_media_type"
    private const val KEY_CODE_TABLE_ITEM_PREFIX = "code_table_item_"
    private const val KEY_CODE_TABLE_BOTTOM_IMAGE_URI = "code_table_bottom_image_uri"
    private const val KEY_CODE_TABLE_BOTTOM_VIDEO_URI = "code_table_bottom_video_uri"
    private const val KEY_CODE_TABLE_BOTTOM_MEDIA_TYPE = "code_table_bottom_media_type"
    private const val KEY_HOME_SEARCH_TEXT = "home_search_text"
    private const val KEY_HOME_TOP_ACTION_PREFIX = "home_top_action_"

    const val HOME_DEVICE_MEDIA_TYPE_IMAGE = "image"
    const val HOME_DEVICE_MEDIA_TYPE_VIDEO = "video"
    const val CODE_TABLE_BOTTOM_MEDIA_TYPE_IMAGE = "image"
    const val CODE_TABLE_BOTTOM_MEDIA_TYPE_VIDEO = "video"

    data class CodeTableItemConfig(
        val tip: String = "",
        val tipVisible: Boolean = false,
        val value: String = "",
        val unit: String = "",
        val title: String = "",
        val buttonText: String = ""
    )

    const val TOP_ACTION_BADGE_NONE = 0
    const val TOP_ACTION_BADGE_DOT = 1
    const val TOP_ACTION_BADGE_TEXT = 2

    data class HomeTopActionConfig(
        val title: String,
        val badgeStyle: Int,
        val badgeText: String
    )

    fun getHomeTopActionConfig(context: Context, index: Int): HomeTopActionConfig {
        val default = when (index) {
            0 -> HomeTopActionConfig("签到有礼", TOP_ACTION_BADGE_DOT, "")
            else -> HomeTopActionConfig("消息", TOP_ACTION_BADGE_TEXT, "80")
        }
        val prefix = "$KEY_HOME_TOP_ACTION_PREFIX$index"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return HomeTopActionConfig(
            title = prefs.getString("${prefix}_title", default.title).orEmpty(),
            badgeStyle = prefs.getInt("${prefix}_badge_style", default.badgeStyle),
            badgeText = prefs.getString("${prefix}_badge_text", default.badgeText).orEmpty()
        )
    }

    fun setHomeTopActionConfig(context: Context, index: Int, config: HomeTopActionConfig) {
        val prefix = "$KEY_HOME_TOP_ACTION_PREFIX$index"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("${prefix}_title", config.title.trim())
            .putInt("${prefix}_badge_style", config.badgeStyle)
            .putString("${prefix}_badge_text", config.badgeText.trim())
            .apply()
    }

    fun resetHomeTopActionConfig(context: Context, index: Int) {
        val prefix = "$KEY_HOME_TOP_ACTION_PREFIX$index"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove("${prefix}_title")
            .remove("${prefix}_badge_style")
            .remove("${prefix}_badge_text")
            .apply()
    }

    fun getCodeTableItemConfig(context: Context, index: Int): CodeTableItemConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultButtonText = when (index) {
            0 -> "充流量"
            1 -> "充话费"
            2 -> "办套餐"
            3 -> "去使用"
            else -> ""
        }
        val default = if (index == 3) CodeTableItemConfig(
            tip = "sdfsdf", tipVisible = true, value = "20", unit = "GB", title = "5G",
            buttonText = defaultButtonText
        ) else {
            CodeTableItemConfig(buttonText = defaultButtonText)
        }
        val prefix = "$KEY_CODE_TABLE_ITEM_PREFIX$index"
        return CodeTableItemConfig(
            tip = prefs.getString("${prefix}_tip", default.tip).orEmpty(),
            tipVisible = prefs.getBoolean("${prefix}_tip_visible", default.tipVisible),
            value = prefs.getString("${prefix}_value", default.value).orEmpty(),
            unit = prefs.getString("${prefix}_unit", default.unit).orEmpty(),
            title = prefs.getString("${prefix}_title", default.title).orEmpty(),
            buttonText = prefs.getString("${prefix}_button", default.buttonText).orEmpty()
        )
    }

    fun setCodeTableItemConfig(context: Context, index: Int, config: CodeTableItemConfig) {
        val prefix = "$KEY_CODE_TABLE_ITEM_PREFIX$index"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("${prefix}_tip", config.tip.trim())
            .putBoolean("${prefix}_tip_visible", config.tipVisible)
            .putString("${prefix}_value", config.value.trim())
            .putString("${prefix}_unit", config.unit.trim())
            .putString("${prefix}_title", config.title.trim())
            .putString("${prefix}_button", config.buttonText.trim())
            .apply()
    }

    fun resetCodeTableItemConfigs(context: Context) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        repeat(8) { index ->
            val prefix = "$KEY_CODE_TABLE_ITEM_PREFIX$index"
            editor.remove("${prefix}_tip")
                .remove("${prefix}_tip_visible")
                .remove("${prefix}_value")
                .remove("${prefix}_unit")
                .remove("${prefix}_title")
                .remove("${prefix}_button")
        }
        editor.apply()
    }

    fun resetCodeTableItemConfig(context: Context, index: Int) {
        val prefix = "$KEY_CODE_TABLE_ITEM_PREFIX$index"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove("${prefix}_tip")
            .remove("${prefix}_tip_visible")
            .remove("${prefix}_value")
            .remove("${prefix}_unit")
            .remove("${prefix}_title")
            .remove("${prefix}_button")
            .apply()
    }

    fun getCodeTableBottomImageUri(context: Context): Uri? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CODE_TABLE_BOTTOM_IMAGE_URI, null)
            .orEmpty()
            .takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
    }

    fun setCodeTableBottomImageUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CODE_TABLE_BOTTOM_IMAGE_URI, uri.toString())
            .apply()
    }

    fun getCodeTableBottomVideoUri(context: Context): Uri? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CODE_TABLE_BOTTOM_VIDEO_URI, null)
            .orEmpty().takeIf { it.isNotBlank() }?.let(Uri::parse)

    fun setCodeTableBottomVideoUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CODE_TABLE_BOTTOM_VIDEO_URI, uri.toString())
            .putString(KEY_CODE_TABLE_BOTTOM_MEDIA_TYPE, CODE_TABLE_BOTTOM_MEDIA_TYPE_VIDEO)
            .apply()
    }

    fun setCodeTableBottomImageMedia(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CODE_TABLE_BOTTOM_IMAGE_URI, uri.toString())
            .putString(KEY_CODE_TABLE_BOTTOM_MEDIA_TYPE, CODE_TABLE_BOTTOM_MEDIA_TYPE_IMAGE)
            .apply()
    }

    fun getCodeTableBottomMediaType(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CODE_TABLE_BOTTOM_MEDIA_TYPE, CODE_TABLE_BOTTOM_MEDIA_TYPE_IMAGE)
            .orEmpty()

    fun getHomeSearchText(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOME_SEARCH_TEXT, "")
            .orEmpty()

    fun setHomeSearchText(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_HOME_SEARCH_TEXT, value.trim())
            .apply()
    }

    // Basic user info and custom display settings
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

    fun getIncomingPackageInfo(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_INCOMING_PACKAGE_INFO, "")
            .orEmpty()
    }

    fun setIncomingPackageInfo(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_INCOMING_PACKAGE_INFO, value.trim())
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

    // 首页 / WebView 摘要数据
    // 注：流量、余额复用上面的 MineStat 系列，不再单独存储。
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

    fun getWebViewHomePoints(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBVIEW_HOME_POINTS, "1929")
            .orEmpty()
    }

    fun setWebViewHomePoints(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBVIEW_HOME_POINTS, value.trim())
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

    // 详单页面 JSON 缓存
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

    /** “我的设备”卡片中用户选择的本地图片。使用可持久化的 document Uri 保存。 */
    fun getHomeCurrentDeviceImageUri(context: Context): Uri? {
        val rawUri = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOME_CURRENT_DEVICE_IMAGE_URI, null)
            .orEmpty()
        return rawUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

    fun setHomeCurrentDeviceImageUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOME_CURRENT_DEVICE_IMAGE_URI, uri.toString())
            .apply()
    }

    /** “我的设备”卡片当前展示的媒体类型。 */
    fun getHomeCurrentDeviceMediaType(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOME_CURRENT_DEVICE_MEDIA_TYPE, HOME_DEVICE_MEDIA_TYPE_IMAGE)
            .orEmpty()
            .ifBlank { HOME_DEVICE_MEDIA_TYPE_IMAGE }
    }

    fun setHomeCurrentDeviceMediaType(context: Context, type: String) {
        val normalized = when (type) {
            HOME_DEVICE_MEDIA_TYPE_VIDEO -> HOME_DEVICE_MEDIA_TYPE_VIDEO
            else -> HOME_DEVICE_MEDIA_TYPE_IMAGE
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOME_CURRENT_DEVICE_MEDIA_TYPE, normalized)
            .apply()
    }

    /** “我的设备”卡片中用户选择的本地视频。使用可持久化的 document Uri 保存。 */
    fun getHomeCurrentDeviceVideoUri(context: Context): Uri? {
        val rawUri = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOME_CURRENT_DEVICE_VIDEO_URI, null)
            .orEmpty()
        return rawUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

    fun setHomeCurrentDeviceVideoUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOME_CURRENT_DEVICE_VIDEO_URI, uri.toString())
            .apply()
    }

}
