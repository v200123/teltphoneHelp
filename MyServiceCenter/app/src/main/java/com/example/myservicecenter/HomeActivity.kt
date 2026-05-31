package com.example.myservicecenter

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myservicecenter.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var selectedTabIndex: Int = 0
    private lateinit var pagerAdapter: HomePagerAdapter

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
