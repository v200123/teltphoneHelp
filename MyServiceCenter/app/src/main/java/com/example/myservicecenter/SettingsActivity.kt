package com.example.myservicecenter

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.myservicecenter.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        applyWindowInsets()
    }

    private fun initViews() {
        binding.etOutgoingPackage.setText(AppPreferences.getOutgoingPackageInfo(this))
        binding.etCustomNumber.setText(AppPreferences.getCustomPhoneNumber(this))
        binding.etCustomStar.setText(AppPreferences.getCustomStarLevel(this).toString())
        binding.etCustomRegion.setText(AppPreferences.getCustomSelfRegion(this))
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener {
            AppPreferences.setOutgoingPackageInfo(this, binding.etOutgoingPackage.text?.toString().orEmpty())
            AppPreferences.setCustomPhoneNumber(this, binding.etCustomNumber.text?.toString().orEmpty())
            val starLevel = binding.etCustomStar.text?.toString()?.toIntOrNull()?.coerceIn(1, 5) ?: 5
            AppPreferences.setCustomStarLevel(this, starLevel)
            AppPreferences.setCustomSelfRegion(this, binding.etCustomRegion.text?.toString().orEmpty())
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
