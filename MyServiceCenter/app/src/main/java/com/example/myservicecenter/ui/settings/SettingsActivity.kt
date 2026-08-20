package com.example.myservicecenter.ui.settings

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.appupdater.AppUpdater
import com.example.appupdater.UpdateConfig
import com.example.myservicecenter.BuildConfig
import com.example.myservicecenter.PhoneDisplayManager
import com.example.myservicecenter.R
import com.example.myservicecenter.core.AppPreferences
import com.example.myservicecenter.databinding.ActivitySettingsBinding
import com.example.myservicecenter.ui.calllog.ProviderCallRecordsActivity
import com.example.myservicecenter.ui.sms.SmsDetailManageActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var appUpdater: AppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appUpdater = AppUpdater.register(
            activity = this,
            config = UpdateConfig(baseUrl = BuildConfig.UPDATE_SERVICE_BASE_URL)
        )

        initViews()
        applyWindowInsets()
    }

    private fun initViews() {
        binding.tvCurrentVersion.text = getString(R.string.settings_current_version, BuildConfig.VERSION_NAME)
        binding.btnCheckUpdate.setOnClickListener {
            appUpdater.checkAndShowUserInitiated()
        }
        binding.etOutgoingPackage.setText(AppPreferences.getOutgoingPackageInfo(this))
        binding.etIncomingPackage.setText(AppPreferences.getIncomingPackageInfo(this))
        binding.etCustomNumber.setText(PhoneDisplayManager.managedPhone(this))
        binding.etCustomName.setText(AppPreferences.getCustomDisplayName(this))
        binding.etCallTypeOutgoing.setText(AppPreferences.getCustomOutgoingCallType(this))
        binding.etCallTypeIncoming.setText(AppPreferences.getCustomIncomingCallType(this))
        binding.etCustomStar.setText(AppPreferences.getCustomStarLevel(this).toString())
        binding.etCustomRegion.setText(AppPreferences.getCustomSelfRegion(this))
        binding.etMineCoupon.setText(AppPreferences.getMineStatCoupon(this))
        binding.etMineData.setText(AppPreferences.getMineStatData(this))
        binding.etMineBalance.setText(AppPreferences.getMineStatBalance(this))
        binding.etMineBean.setText(AppPreferences.getMineStatBean(this))
//        binding.etWebHomeData.setText(AppPreferences.getWebViewHomeData(this))
//        binding.etWebHomeBalance.setText(AppPreferences.getWebViewHomeBalance(this))
        binding.etWebHomeCall.setText(AppPreferences.getWebViewHomeCallMinutes(this))
        binding.etWebHomePoints.setText(AppPreferences.getWebViewHomePoints(this))
        binding.etWebHomeRights.setText(AppPreferences.getWebViewHomePendingRights(this))
        binding.btnSmsDetailManage.setOnClickListener {
            startActivity(Intent(this, SmsDetailManageActivity::class.java))
        }
        binding.btnProviderCallRecords.setOnClickListener {
            startActivity(Intent(this, ProviderCallRecordsActivity::class.java))
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener {
            AppPreferences.setOutgoingPackageInfo(this, binding.etOutgoingPackage.text?.toString().orEmpty())
            AppPreferences.setIncomingPackageInfo(this, binding.etIncomingPackage.text?.toString().orEmpty())
            PhoneDisplayManager.updateManagedPhone(this, binding.etCustomNumber.text?.toString().orEmpty())
            AppPreferences.setCustomDisplayName(this, binding.etCustomName.text?.toString().orEmpty())
            AppPreferences.setCustomOutgoingCallType(this, binding.etCallTypeOutgoing.text?.toString().orEmpty())
            AppPreferences.setCustomIncomingCallType(this, binding.etCallTypeIncoming.text?.toString().orEmpty())
            val starLevel = binding.etCustomStar.text?.toString()?.toIntOrNull()?.coerceIn(1, 5) ?: 5
            AppPreferences.setCustomStarLevel(this, starLevel)
            AppPreferences.setCustomSelfRegion(this, binding.etCustomRegion.text?.toString().orEmpty())
            AppPreferences.setMineStatCoupon(this, binding.etMineCoupon.text?.toString().orEmpty())
            AppPreferences.setMineStatData(this, binding.etMineData.text?.toString().orEmpty())
            AppPreferences.setMineStatBalance(this, binding.etMineBalance.text?.toString().orEmpty())
            AppPreferences.setMineStatBean(this, binding.etMineBean.text?.toString().orEmpty())
            AppPreferences.setWebViewHomeCallMinutes(this, binding.etWebHomeCall.text?.toString().orEmpty())
            AppPreferences.setWebViewHomePoints(this, binding.etWebHomePoints.text?.toString().orEmpty())
            AppPreferences.setWebViewHomePendingRights(this, binding.etWebHomeRights.text?.toString().orEmpty())
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun applyWindowInsets() {
        val top = binding.topBar.paddingTop
        val left = binding.topBar.paddingStart
        val right = binding.topBar.paddingEnd
        val bottom = binding.topBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(
                left = left,
                top = top + systemBars.top,
                right = right,
                bottom = bottom
            )
            insets
        }
    }
}
