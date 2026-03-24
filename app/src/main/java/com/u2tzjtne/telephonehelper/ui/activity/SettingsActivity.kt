package com.u2tzjtne.telephonehelper.ui.activity

import android.content.Intent
import android.os.Bundle
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivitySettingsBinding

/**
 * 设置界面
 * 
 * 功能入口：
 * 1. 上传/管理彩铃视频
 * 2. 不显示彩铃的号码管理
 */
class SettingsActivity : BaseActivity() {
    private val binding: ActivitySettingsBinding by lazy { ActivitySettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 返回按钮点击事件
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 彩铃视频管理
        binding.btnManageRingVideo.setOnClickListener {
            startActivity(Intent(this, RingVideoManageActivity::class.java))
        }

        // 不显示彩铃的号码管理
        binding.btnNoRingtonePhone.setOnClickListener {
            startActivity(Intent(this, NoRingtonePhoneManageActivity::class.java))
        }
    }
}
