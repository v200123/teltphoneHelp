package com.example.myservicecenter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.myservicecenter.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var selectedTabIndex: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#DCEFFE")
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        applyWindowInsets()
    }

    private fun initViews() {
        binding.tabHome.setOnClickListener { selectTab(0) }
        binding.tabVideo.setOnClickListener { selectTab(1) }
        binding.tabEquity.setOnClickListener { selectTab(2) }
        binding.tabMine.setOnClickListener { selectTab(3) }
        binding.btnOpenDetail.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        Glide.with(this)
            .load(R.drawable.tab_mine_select_v2)
            .placeholder(android.R.drawable.sym_def_app_icon)
            .error(android.R.drawable.sym_def_app_icon)
            .transform(CircleCrop())
            .into(binding.ivAvatar)
        selectTab(3)
    }

    private fun selectTab(index: Int) {
        selectedTabIndex = index
        updatePage(index)
        updateBottomTabs()
    }

    private fun updatePage(index: Int) {
        if (index == 3) {
            binding.mineScroll.visibility = View.VISIBLE
            binding.tvPlaceholder.visibility = View.GONE
            return
        }
        binding.mineScroll.visibility = View.GONE
        binding.tvPlaceholder.visibility = View.VISIBLE
        binding.tvPlaceholder.text = getString(R.string.module_placeholder_with_name, getTabTitle(index))
    }

    private fun updateBottomTabs() {
        updateSingleTab(
            selected = selectedTabIndex == 0,
            iconSelected = R.drawable.tab_home_select_v2,
            iconUnselected = R.drawable.tab_home_unselect_v2,
            iconView = binding.ivTabHome,
            textView = binding.tvTabHome
        )
        updateSingleTab(
            selected = selectedTabIndex == 1,
            iconSelected = R.drawable.tab_discovery_select_v2,
            iconUnselected = R.drawable.tab_discovery_unselect_v2,
            iconView = binding.ivTabVideo,
            textView = binding.tvTabVideo
        )
        updateSingleTab(
            selected = selectedTabIndex == 2,
            iconSelected = R.drawable.tab_equity_select_v2,
            iconUnselected = R.drawable.tab_equity_default_v2,
            iconView = binding.ivTabEquity,
            textView = binding.tvTabEquity
        )
        updateSingleTab(
            selected = selectedTabIndex == 3,
            iconSelected = R.drawable.tab_mine_select_v2,
            iconUnselected = R.drawable.tab_mine_unselect_v2,
            iconView = binding.ivTabMine,
            textView = binding.tvTabMine
        )
    }

    private fun updateSingleTab(
        selected: Boolean,
        iconSelected: Int,
        iconUnselected: Int,
        iconView: android.widget.ImageView,
        textView: android.widget.TextView
    ) {
        iconView.setImageResource(if (selected) iconSelected else iconUnselected)
        textView.setTextColor(
            ContextCompat.getColor(
                this,
                if (selected) R.color.action_blue else R.color.text_secondary
            )
        )
    }

    private fun getTabTitle(index: Int): String {
        return when (index) {
            0 -> getString(R.string.home_tab_home)
            1 -> getString(R.string.home_tab_video)
            2 -> getString(R.string.home_tab_equity)
            else -> getString(R.string.home_tab_mine)
        }
    }

    private fun applyWindowInsets() {
        val mineStart = binding.mineScroll.paddingStart
        val mineTop = binding.mineScroll.paddingTop
        val mineEnd = binding.mineScroll.paddingEnd
        val mineBottom = binding.mineScroll.paddingBottom

        val tabStart = binding.bottomTabBar.paddingStart
        val tabTop = binding.bottomTabBar.paddingTop
        val tabEnd = binding.bottomTabBar.paddingEnd
        val tabBottom = binding.bottomTabBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.mineScroll.updatePadding(
                left = mineStart,
                top = mineTop + systemBars.top,
                right = mineEnd,
                bottom = mineBottom
            )
            binding.bottomTabBar.updatePadding(
                left = tabStart,
                top = tabTop,
                right = tabEnd,
                bottom = tabBottom + systemBars.bottom
            )
            insets
        }
    }
}
