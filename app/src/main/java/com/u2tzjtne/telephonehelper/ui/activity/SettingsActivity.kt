package com.u2tzjtne.telephonehelper.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.u2tzjtne.telephonehelper.databinding.ActivitySettingsBinding
import com.u2tzjtne.telephonehelper.util.CallVibrationSettings
import com.u2tzjtne.telephonehelper.util.ToastUtils

/**
 * 设置界面
 *
 * 功能入口：
 * 1. 上传/管理彩铃视频
 * 2. 不显示彩铃的号码管理
 * 3. 通话接通/挂断震动时长
 */
class SettingsActivity : BaseActivity() {
    private val binding: ActivitySettingsBinding by lazy { ActivitySettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
    }

    override fun onResume() {
        super.onResume()
        refreshVibrationDuration()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnManageRingVideo.setOnClickListener {
            startActivity(Intent(this, RingVideoManageActivity::class.java))
        }

        binding.btnNoRingtonePhone.setOnClickListener {
            startActivity(Intent(this, NoRingtonePhoneManageActivity::class.java))
        }

        binding.btnCallVibrationDuration.setOnClickListener {
            showVibrationDurationDialog()
        }

        refreshVibrationDuration()
    }

    private fun refreshVibrationDuration() {
        binding.tvCallVibrationDuration.text =
            CallVibrationSettings.formatDurationText(CallVibrationSettings.getDurationMs())
    }

    private fun showVibrationDurationDialog() {
        val editText = EditText(this).apply {
            hint = "请输入0-2000之间的毫秒数"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(CallVibrationSettings.getDurationMs().toString())
            setSelection(text?.length ?: 0)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("设置通话震动时长")
            .setMessage("范围 0-2000ms，设置为 0 表示关闭通话震动")
            .setView(editText)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val inputValue = editText.text?.toString()?.trim().orEmpty()
                if (inputValue.isEmpty()) {
                    ToastUtils.s("请输入震动时长")
                    return@setOnClickListener
                }

                val durationMs = inputValue.toIntOrNull()
                if (durationMs == null) {
                    ToastUtils.s("请输入有效的数字")
                    return@setOnClickListener
                }

                if (durationMs !in CallVibrationSettings.MIN_DURATION_MS..CallVibrationSettings.MAX_DURATION_MS) {
                    ToastUtils.s("请输入0-2000之间的数值")
                    return@setOnClickListener
                }

                CallVibrationSettings.saveDurationMs(durationMs)
                refreshVibrationDuration()
                ToastUtils.s("通话震动时长已更新")
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
