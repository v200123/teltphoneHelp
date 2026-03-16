package com.u2tzjtne.telephonehelper.ui.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import com.lxj.xpopup.core.CenterPopupView
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.util.ThemeManager

/**
 * 主题切换对话框
 *
 * 使用方式：
 * ```
 * XPopup.Builder(context)
 *     .asCustom(ThemeSwitchDialog(context))
 *     .show()
 * ```
 */
class ThemeSwitchDialog(context: Context) : CenterPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.dialog_theme_switch
    }

    override fun onCreate() {
        super.onCreate()

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        val tvConfirm = findViewById<TextView>(R.id.tv_confirm)

        // 设置当前选中的主题
        when (ThemeManager.getCurrentMode()) {
            ThemeManager.MODE_LIGHT -> radioGroup.check(R.id.rb_light)
            ThemeManager.MODE_DARK -> radioGroup.check(R.id.rb_dark)
            ThemeManager.MODE_SYSTEM -> radioGroup.check(R.id.rb_system)
        }

        // 监听主题切换
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newMode = when (checkedId) {
                R.id.rb_light -> ThemeManager.MODE_LIGHT
                R.id.rb_dark -> ThemeManager.MODE_DARK
                R.id.rb_system -> ThemeManager.MODE_SYSTEM
                else -> ThemeManager.MODE_SYSTEM
            }
            
            // 如果主题有变化，保存并应用
            if (newMode != ThemeManager.getCurrentMode()) {
                ThemeManager.setThemeMode(newMode)
                // 关闭对话框
                dismiss()
                // 重新创建 Activity 以应用新主题
                (context as? Activity)?.recreate()
            }
        }

        // 确认按钮
        tvConfirm.setOnClickListener {
            dismiss()
        }
    }
}
