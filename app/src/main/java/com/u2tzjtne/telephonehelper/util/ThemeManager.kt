package com.u2tzjtne.telephonehelper.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.u2tzjtne.telephonehelper.R

/**
 * 主题管理器
 * 负责白天/黑夜主题的切换和持久化
 *
 * 使用方式：
 * 1. 在 Application 的 onCreate 中调用 ThemeManager.init(this)
 * 2. 在 Activity 的 onCreate 中调用 ThemeManager.applyTheme(this)
 * 3. 切换主题时调用 ThemeManager.toggleTheme(activity)
 */
object ThemeManager {

    private const val PREF_NAME = "theme_preferences"
    private const val KEY_THEME_MODE = "theme_mode"

    // 主题模式
    const val MODE_LIGHT = 0      // 白天模式
    const val MODE_DARK = 1       // 黑夜模式
    const val MODE_SYSTEM = 2     // 跟随系统

    private var sharedPreferences: SharedPreferences? = null

    /**
     * 初始化主题管理器
     * 建议在 Application.onCreate 中调用
     */
    @JvmStatic
    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE
            )
        }
        applyThemeMode(getCurrentMode())
    }

    /**
     * 获取 SharedPreferences
     */
    private fun getPrefs(): SharedPreferences {
        return sharedPreferences
            ?: throw IllegalStateException("ThemeManager not initialized. Call init() first.")
    }

    /**
     * 获取当前主题模式
     */
    @JvmStatic
    fun getCurrentMode(): Int {
        return getPrefs().getInt(KEY_THEME_MODE, MODE_SYSTEM)
    }

    /**
     * 是否是黑夜模式
     */
    @JvmStatic
    fun isDarkMode(): Boolean {
        return when (getCurrentMode()) {
            MODE_DARK -> true
            MODE_LIGHT -> false
            else -> false // 系统模式需要检查
        }
    }

    /**
     * 是否为跟随系统模式
     */
    @JvmStatic
    fun isFollowSystem(): Boolean {
        return getCurrentMode() == MODE_SYSTEM
    }

    /**
     * 设置主题模式
     */
    @JvmStatic
    fun setThemeMode(mode: Int) {
        getPrefs().edit().putInt(KEY_THEME_MODE, mode).apply()
        applyThemeMode(mode)
    }

    /**
     * 应用主题模式到系统
     */
    private fun applyThemeMode(mode: Int) {
        when (mode) {
            MODE_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            MODE_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            MODE_SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }

    /**
     * 应用主题到 Activity
     * 在 Activity.onCreate 中调用，调用时机要在 super.onCreate 之后，setContentView 之前
     */
    @JvmStatic
    fun applyTheme(activity: Activity) {
        when (getCurrentMode()) {
            MODE_LIGHT -> activity.setTheme(R.style.AppTheme_Light)
            MODE_DARK -> activity.setTheme(R.style.AppTheme_Dark)
            else -> {
                // 系统模式，让系统自动处理
            }
        }
    }

    /**
     * 切换主题（白天/黑夜切换）
     * 会重新创建 Activity 以应用新主题
     */
    @JvmStatic
    fun toggleTheme(activity: Activity) {
        val newMode = if (getCurrentMode() == MODE_DARK) MODE_LIGHT else MODE_DARK
        setThemeMode(newMode)
        // 重新创建 Activity 以应用新主题
        activity.recreate()
    }

    /**
     * 切换到白天模式
     */
    @JvmStatic
    fun setLightMode(activity: Activity) {
        if (getCurrentMode() != MODE_LIGHT) {
            setThemeMode(MODE_LIGHT)
            activity.recreate()
        }
    }

    /**
     * 切换到黑夜模式
     */
    @JvmStatic
    fun setDarkMode(activity: Activity) {
        if (getCurrentMode() != MODE_DARK) {
            setThemeMode(MODE_DARK)
            activity.recreate()
        }
    }

    /**
     * 切换到跟随系统模式
     */
    @JvmStatic
    fun setFollowSystem(activity: Activity) {
        if (getCurrentMode() != MODE_SYSTEM) {
            setThemeMode(MODE_SYSTEM)
            activity.recreate()
        }
    }
}
