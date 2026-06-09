package com.example.myservicecenter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myservicecenter.databinding.ActivityHomeBinding
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var selectedTabIndex: Int = 0
    private lateinit var pagerAdapter: HomePagerAdapter
    private var hasCheckedAppUpdate = false
    private var pendingInstallApkFile: File? = null
    private var pendingForceUpdate = false
    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val apkFile = pendingInstallApkFile ?: return@registerForActivityResult
            if (AppUpdateManager.canRequestPackageInstalls(this)) {
                AppUpdateManager.installApk(this, apkFile)
                pendingInstallApkFile = null
                pendingForceUpdate = false
            } else if (pendingForceUpdate) {
                showInstallPermissionDialog(apkFile, true)
            } else {
                Toast.makeText(this, getString(R.string.update_install_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.parseColor("#DCEFFE"),
                Color.parseColor("#DCEFFE")
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        applyWindowInsets()
        checkAppUpdateOnLaunch()
    }

    private fun initViews() {
        pagerAdapter = HomePagerAdapter(this)
        binding.viewPagerHome.adapter = pagerAdapter
        binding.viewPagerHome.offscreenPageLimit = 4
        binding.viewPagerHome.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (selectedTabIndex != position) {
                    selectedTabIndex = position
                    updateBottomTabs()
                }
            }
        })
        binding.tabHome.setOnClickListener { selectTab(0) }
        binding.tabVideo.setOnClickListener { selectTab(1) }
        binding.tabEquity.setOnClickListener { selectTab(2) }
        binding.tabMine.setOnClickListener { selectTab(3) }
        selectTab(0, false)
    }

    private fun selectTab(index: Int, smoothScroll: Boolean = true) {
        selectedTabIndex = index
        updateBottomTabs()
        if (binding.viewPagerHome.currentItem != index) {
            binding.viewPagerHome.setCurrentItem(index, smoothScroll)
        }
    }

    private fun updateBottomTabs() {
        updateSingleTab(
            selected = selectedTabIndex == 0,
            iconSelected = R.drawable.tab_home_select_v2,
            iconUnselected = R.drawable.tab_home_unselect_v2,
            iconView = binding.tabHome
        )
        updateSingleTab(
            selected = selectedTabIndex == 1,
            iconSelected = R.drawable.tab_discovery_select_v2,
            iconUnselected = R.drawable.tab_discovery_unselect_v2,
            iconView = binding.tabVideo
        )
        updateSingleTab(
            selected = selectedTabIndex == 2,
            iconSelected = R.drawable.tab_equity_select_v2,
            iconUnselected = R.drawable.tab_equity_default_v2,
            iconView = binding.tabEquity
        )
        updateSingleTab(
            selected = selectedTabIndex == 3,
            iconSelected = R.drawable.tab_mine_select_v2,
            iconUnselected = R.drawable.tab_mine_unselect_v2,
            iconView = binding.tabMine
        )
    }

    private fun updateSingleTab(
        selected: Boolean,
        iconSelected: Int,
        iconUnselected: Int,
        iconView: android.widget.ImageView
    ) {
        iconView.setImageResource(if (selected) iconSelected else iconUnselected)
    }

    private fun applyWindowInsets() {
        val pagerStart = binding.viewPagerHome.paddingStart
        val pagerTop = binding.viewPagerHome.paddingTop
        val pagerEnd = binding.viewPagerHome.paddingEnd
        val pagerBottom = binding.viewPagerHome.paddingBottom

        val bottomTabBarMarginBottom =
            (binding.bottomTabBar.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.viewPagerHome.updatePadding(
                left = pagerStart,
                top = pagerTop + systemBars.top,
                right = pagerEnd,
                bottom = pagerBottom
            )
            binding.bottomTabBar.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                bottomMargin = bottomTabBarMarginBottom + systemBars.bottom
            }
            insets
        }
    }

    private fun checkAppUpdateOnLaunch() {
        if (hasCheckedAppUpdate) return
        hasCheckedAppUpdate = true

        // 首页每次冷启动时发起一次版本检测，避免重复弹窗打断当前会话。
        lifecycleScope.launch {
            val updateResult = withContext(Dispatchers.IO) {
                runCatching { AppUpdateManager.checkForUpdate(this@HomeActivity) }
                    .onFailure { Log.e("HomeActivity", "启动版本检测失败", it) }
                    .getOrNull()
            }
            if (updateResult?.needUpdate == true) {
                showUpdateDialog(updateResult)
            }
        }
    }

    private fun showUpdateDialog(updateResult: UpdateCheckResult) {
        val dialogMessage = buildString {
            append(getString(R.string.update_dialog_version_label, updateResult.latestVersion ?: "--"))
            if (!updateResult.updateLog.isNullOrBlank()) {
                append("\n\n")
                append(getString(R.string.update_dialog_log_label))
                append("\n")
                append(updateResult.updateLog)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (updateResult.forceUpdate) {
                    getString(R.string.update_dialog_title_force)
                } else {
                    getString(R.string.update_dialog_title)
                }
            )
            .setMessage(dialogMessage)
            .setCancelable(!updateResult.forceUpdate)
            .setPositiveButton(R.string.update_dialog_confirm, null)
            .apply {
                if (!updateResult.forceUpdate) {
                    setNegativeButton(R.string.update_dialog_cancel, null)
                }
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialog.dismiss()
                startUpdateDownload(updateResult)
            }
        }
        dialog.show()
    }

    private fun startUpdateDownload(updateResult: UpdateCheckResult) {
        pendingForceUpdate = updateResult.forceUpdate
        // 下载阶段单独弹一个不可取消的进度提示，避免用户重复点击更新按钮。
        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.update_downloading_title)
            .setMessage(getString(R.string.update_downloading_message))
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            val downloadResult = withContext(Dispatchers.IO) {
                runCatching {
                    AppUpdateManager.downloadApk(this@HomeActivity, updateResult) { progress ->
                        runOnUiThread {
                            if (progressDialog.isShowing) {
                                progressDialog.setMessage(
                                    getString(R.string.update_downloading_progress, progress)
                                )
                            }
                        }
                    }
                }
            }
            if (progressDialog.isShowing) {
                progressDialog.dismiss()
            }

            downloadResult.onSuccess { apkFile ->
                pendingInstallApkFile = apkFile
                if (AppUpdateManager.canRequestPackageInstalls(this@HomeActivity)) {
                    AppUpdateManager.installApk(this@HomeActivity, apkFile)
                    pendingInstallApkFile = null
                    pendingForceUpdate = false
                } else {
                    showInstallPermissionDialog(apkFile, updateResult.forceUpdate)
                }
            }.onFailure { throwable ->
                Toast.makeText(
                    this@HomeActivity,
                    getString(
                        R.string.update_download_failed,
                        throwable.message ?: "未知错误"
                    ),
                    Toast.LENGTH_LONG
                ).show()
                if (updateResult.forceUpdate) {
                    showUpdateDialog(updateResult)
                }
            }
        }
    }

    private fun showInstallPermissionDialog(apkFile: File, forceUpdate: Boolean) {
        pendingInstallApkFile = apkFile
        pendingForceUpdate = forceUpdate
        // Android 8.0+ 安装外部 APK 前需要单独授权“安装未知来源应用”。
        AlertDialog.Builder(this)
            .setTitle(R.string.update_install_permission_title)
            .setMessage(R.string.update_install_permission_message)
            .setCancelable(!forceUpdate)
            .setPositiveButton(R.string.update_install_permission_confirm) { _, _ ->
                installPermissionLauncher.launch(
                    AppUpdateManager.createInstallPermissionIntent(this)
                )
            }
            .apply {
                if (!forceUpdate) {
                    setNegativeButton(R.string.update_dialog_cancel, null)
                }
            }
            .show()
    }
}

private class HomePagerAdapter(
    activity: AppCompatActivity
) : FragmentStateAdapter(activity) {
    private val pageTitles = listOf(
        activity.getString(R.string.home_tab_home),
        activity.getString(R.string.home_tab_video),
        activity.getString(R.string.home_tab_equity)
    )

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return if (position == 3) {
            HomeMineFragment()
        } else {
            ModulePlaceholderFragment.newInstance(pageTitles[position])
        }
    }
}
