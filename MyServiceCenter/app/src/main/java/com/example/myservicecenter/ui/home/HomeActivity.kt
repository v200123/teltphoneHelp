package com.example.myservicecenter.ui.home
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
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
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.appupdater.AppUpdater
import com.example.appupdater.UpdateConfig
import com.example.myservicecenter.BuildConfig
import com.example.myservicecenter.R
import com.example.myservicecenter.databinding.ActivityHomeBinding
import com.example.myservicecenter.ui.main.ModulePlaceholderFragment
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var appUpdater: AppUpdater
    private var selectedTabIndex: Int = 0
    private lateinit var pagerAdapter: HomePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appUpdater = AppUpdater.register(
            activity = this,
            config = UpdateConfig(baseUrl = BuildConfig.UPDATE_SERVICE_BASE_URL)
        )
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = false
        }
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        applyWindowInsets()
        appUpdater.checkAndShow()
    }

    private fun initViews() {
        pagerAdapter = HomePagerAdapter(this)
        binding.viewPagerHome.adapter = pagerAdapter
        binding.viewPagerHome.isUserInputEnabled = false
        binding.viewPagerHome.offscreenPageLimit = 4
        binding.viewPagerHome.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (selectedTabIndex != position) {
                    selectedTabIndex = position
                    updateBottomTabs()
                }
            }
        })
        binding.customTabIcon01.setOnClickListener { selectTab(0) }
//        binding.tabVideo.setOnClickListener { selectTab(1) }
//        binding.tabEquity.setOnClickListener { selectTab(2) }
        binding.customTabIcon05.setOnClickListener { selectTab(3) }
        Glide.with(this).load("https://img.app.coc.10086.cn/group1/M00/07/79/CtFOBmg3w5aAIvSXAAxdjPeIATU58.webp")
            .into(binding.customTabIcon03)
        Glide.with(this).load("https://res.app.coc.10086.cn/group1/M00/0E/ED/CtFOGGpd8SmAKv5yAAAVQaFRXRs646.png?fmt=webp")
            .into(binding.customTabIcon02)
        Glide.with(this).load("https://res.app.coc.10086.cn/group2/M00/08/F2/CtFOW2jSn9SAQAW_AAAcM6BzEvY854.png?fmt=webp")
            .into(binding.customTabIcon04)
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
            iconView = binding.customTabIcon01
        )

        updateSingleTab(
            selected = selectedTabIndex == 3,
            iconSelected = R.drawable.tab_mine_select_v2,
            iconUnselected = R.drawable.tab_mine_unselect_v2,
            iconView = binding.customTabIcon05
        )
    }
    private fun updateSingleTab(
        selected: Boolean,
        iconSelected: Int,
        iconUnselected: Int,
        iconView: ImageView
    ) {
        iconView.setImageResource(if (selected) iconSelected else iconUnselected)
    }
    private fun applyWindowInsets() {
        val pagerStart = binding.viewPagerHome.paddingStart
        val pagerTop = binding.viewPagerHome.paddingTop
        val pagerEnd = binding.viewPagerHome.paddingEnd
        val pagerBottom = binding.viewPagerHome.paddingBottom

//        val bottomTabBarMarginBottom =
//            (binding.bottomTabBar.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
//
//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            binding.viewPagerHome.updatePadding(
//                left = pagerStart,
//                top = pagerTop,
//                right = pagerEnd,
//                bottom = pagerBottom
//            )
//            binding.bottomTabBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
//                bottomMargin = bottomTabBarMarginBottom + systemBars.bottom
//            }
//            insets
//        }
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
        return when (position) {
            0 -> HomeFragment()
            3 -> HomeMineFragment()
            else -> ModulePlaceholderFragment.newInstance(pageTitles[position])
        }
    }
}
