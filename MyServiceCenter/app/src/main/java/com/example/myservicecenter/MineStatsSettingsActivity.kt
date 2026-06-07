package com.example.myservicecenter

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.myservicecenter.databinding.ActivityMineStatsSettingsBinding

class MineStatsSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMineStatsSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true

        binding = ActivityMineStatsSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        applyWindowInsets()
    }

    private fun initViews() {
        binding.etCoupon.setText(AppPreferences.getMineStatCoupon(this))
        binding.etData.setText(AppPreferences.getMineStatData(this))
        binding.etBalance.setText(AppPreferences.getMineStatBalance(this))
        binding.etBean.setText(AppPreferences.getMineStatBean(this))

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener {
            AppPreferences.setMineStatCoupon(this, binding.etCoupon.text?.toString().orEmpty())
            AppPreferences.setMineStatData(this, binding.etData.text?.toString().orEmpty())
            AppPreferences.setMineStatBalance(this, binding.etBalance.text?.toString().orEmpty())
            AppPreferences.setMineStatBean(this, binding.etBean.text?.toString().orEmpty())
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
