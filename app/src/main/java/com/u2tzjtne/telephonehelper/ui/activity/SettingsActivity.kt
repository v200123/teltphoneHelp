package com.u2tzjtne.telephonehelper.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.databinding.ActivitySettingsBinding
import com.u2tzjtne.telephonehelper.util.CallDialAudioSettings
import com.u2tzjtne.telephonehelper.util.CallPromptSettings
import com.u2tzjtne.telephonehelper.util.CallVibrationSettings
import com.u2tzjtne.telephonehelper.util.PhoneDialAudioBindingHelper
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import com.u2tzjtne.telephonehelper.util.ToastUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

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
        refreshDialAudioMode()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnManageRingVideo.setOnClickListener {
            startActivity(Intent(this, RingVideoManageActivity::class.java))
        }

        binding.btnManageMusic.setOnClickListener {
            startActivity(Intent(this, MusicManageActivity::class.java))
        }

        binding.btnDialAudioMode.setOnClickListener {
            showDialAudioModeDialog()
        }

        binding.btnClearPhoneBinding.setOnClickListener {
            startActivity(Intent(this, PhoneAudioBindingManageActivity::class.java))
        }

        binding.btnNoRingtonePhone.setOnClickListener {
            startActivity(Intent(this, NoRingtonePhoneManageActivity::class.java))
        }

        binding.btnPowerOffPromptPhone.setOnClickListener {
            CallPromptPhoneManageActivity.start(this, CallPromptSettings.PromptType.POWER_OFF)
        }

        binding.btnEmptyNumberPromptPhone.setOnClickListener {
            CallPromptPhoneManageActivity.start(this, CallPromptSettings.PromptType.EMPTY_NUMBER)
        }

        binding.btnBusyPromptPhone.setOnClickListener {
            CallPromptPhoneManageActivity.start(this, CallPromptSettings.PromptType.BUSY)
        }

        binding.btnCallVibrationDuration.setOnClickListener {
            showVibrationDurationDialog()
        }

        refreshVibrationDuration()
        refreshDialAudioMode()
    }

    private fun refreshVibrationDuration() {
        binding.tvCallVibrationDuration.text =
            CallVibrationSettings.formatDurationText(CallVibrationSettings.getDurationMs())
    }

    private fun refreshDialAudioMode() {
        binding.tvDialAudioMode.text = CallDialAudioSettings.getModeLabel()
    }

    private fun showDialAudioModeDialog() {
        val modes = CallDialAudioSettings.DialAudioMode.entries.toTypedArray()
        val labels = modes.map { it.label }.toTypedArray()
        val checkedIndex = modes.indexOf(CallDialAudioSettings.getMode()).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("选择拨打播放模式")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                CallDialAudioSettings.saveMode(modes[which])
                refreshDialAudioMode()
                ToastUtils.s("拨打播放模式已更新")
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
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

    private fun showClearPhoneBindingDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.settings_clear_phone_binding_hint)
            inputType = InputType.TYPE_CLASS_PHONE
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_clear_phone_binding_title)
            .setMessage(R.string.settings_clear_phone_binding_message)
            .setView(editText)
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_confirm) { _, _ ->
                clearPhoneBinding(editText.text?.toString().orEmpty())
            }
            .show()
    }

    private fun clearPhoneBinding(phoneNumber: String) {
        val normalizedNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.length != 11) {
            ToastUtils.s(getString(R.string.settings_clear_phone_binding_invalid))
            return
        }

        Completable.fromAction {
            PhoneDialAudioBindingHelper.clearBindings(normalizedNumber)
            val db = RingVideoDatabase.getInstance()
            db.phoneRingtoneAssignmentDao().deleteByPhoneNumber(normalizedNumber)
            db.ringtonePhoneBindingDao().deleteByPhoneNumber(normalizedNumber)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                ToastUtils.s(getString(R.string.settings_clear_phone_binding_success))
            }, {
                ToastUtils.s(getString(R.string.settings_clear_phone_binding_failed))
            })
    }
}
