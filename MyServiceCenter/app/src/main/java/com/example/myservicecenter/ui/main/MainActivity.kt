package com.example.myservicecenter.ui.main

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myservicecenter.AppPreferences
import com.example.myservicecenter.CallDetailFragment
import com.example.myservicecenter.CallRecord
import com.example.myservicecenter.CallRecordCacheDatabase
import com.example.myservicecenter.CallRecordContract
import com.example.myservicecenter.ui.detail.PackageDetailWebViewFragment
import com.example.myservicecenter.R
import com.example.myservicecenter.SettingsActivity
import com.example.myservicecenter.databinding.ActivityMainBinding
import com.example.myservicecenter.toCachedEntity
import com.example.myservicecenter.toCallRecord
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var topBarExpandedColor: Int = Color.TRANSPARENT
    private var topBarCollapsedColor: Int = Color.TRANSPARENT
    private var pageScrollThresholdPx: Int = 0
    private var lastTopBarCollapsedState: Boolean? = null
    private var monthOptions: List<YearMonth> = emptyList()
    private var selectedMonth: YearMonth? = null
    private val monthZoneId: ZoneId = ZoneId.systemDefault()
    private var allRecords: List<CallRecord> = emptyList()
    private val callRecordCacheDao by lazy {
        CallRecordCacheDatabase.getInstance(applicationContext).callRecordCacheDao()
    }
    private lateinit var pagerAdapter: DetailPagerAdapter

    companion object {
        private const val READ_CALL_RECORDS_PERMISSION =
            "com.u2tzjtne.telephonehelper.permission.READ_CALL_RECORDS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        applyWindowInsets()
        bindAppBarColorTransition()
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return
        applyCustomNumberInfo()
        if (allRecords.isNotEmpty()) {
            updateSummary(allRecords)
        }
    }

    private fun initViews() {
        topBarExpandedColor = Color.TRANSPARENT
        topBarCollapsedColor = Color.WHITE
        pageScrollThresholdPx = dpToPx(72)
        binding.topBarContainer.setBackgroundColor(topBarExpandedColor)
        binding.topBarContainer.bringToFront()

        binding.btnMore.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.ivBack.setOnClickListener  { finish() }
        applyCustomNumberInfo()

        setupMonthSelector()
        setupDetailTabsAndPager()
        loadCallRecords()
    }

    fun updateTopBarForPageScroll(scrollY: Int) {
        val collapsed = scrollY >= pageScrollThresholdPx
        if (lastTopBarCollapsedState == collapsed) {
            return
        }
        lastTopBarCollapsedState = collapsed
        binding.topBarContainer.setBackgroundColor(
            if (collapsed) topBarCollapsedColor else topBarExpandedColor
        )
    }

    private fun setupDetailTabsAndPager() {
        val pages = listOf(
            getString(R.string.tab_package),
            getString(R.string.tab_call_detail),
            getString(R.string.tab_sms_detail),
            getString(R.string.tab_data_detail),
            getString(R.string.tab_vas_deduction_record)
        )
        pagerAdapter = DetailPagerAdapter(this, pages)
        binding.viewPagerDetail.adapter = pagerAdapter
        binding.viewPagerDetail.isUserInputEnabled = false

//        val tabLayout = binding.tabLayoutDetail
//        for (index in 0 until tabLayout.tabCount) {
//            val tab = tabLayout.getTabAt(index) ?: continue
//            tab.customView = createDetailTabView(tab.text?.toString().orEmpty(), tab.isSelected)
//        }
//        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//                updateDetailTabStyle(tab, true)
//                binding.viewPagerDetail.setCurrentItem(tab.position, true)
//                notifyCallDetailMonthChanged()
//            }
//            override fun onTabUnselected(tab: TabLayout.Tab) {
//                updateDetailTabStyle(tab, false)
//            }
//            override fun onTabReselected(tab: TabLayout.Tab) {
//                updateDetailTabStyle(tab, true)
//                notifyCallDetailMonthChanged()
//            }
//        })
//        binding.viewPagerDetail.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                if (tabLayout.selectedTabPosition != position) {
//                    tabLayout.getTabAt(position)?.select()
//                }
//                notifyCallDetailMonthChanged()
//            }
//        })
    }

    private fun createDetailTabView(title: String, selected: Boolean): TextView {
        return TextView(this).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (selected) R.color.text_primary else R.color.text_muted
                )
            )
        }
    }

    private fun updateDetailTabStyle(tab: TabLayout.Tab, selected: Boolean) {
        val tabTextView = tab.customView as? TextView ?: return
        tabTextView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        tabTextView.setTextColor(
            ContextCompat.getColor(
                this,
                if (selected) R.color.text_primary else R.color.text_muted
            )
        )
    }

    private fun setupMonthSelector() {
        val currentMonth = YearMonth.now()
        monthOptions = (0..11).map { currentMonth.minusMonths(it.toLong()) }
        selectedMonth = monthOptions.firstOrNull()
//        binding.monthContainer.removeAllViews()
//        monthOptions.forEachIndexed { index, yearMonth ->
//            binding.monthContainer.addView(createMonthView(yearMonth, yearMonth == selectedMonth, index > 0))
//        }
    }

    private fun createMonthView(yearMonth: YearMonth, selected: Boolean, addStartMargin: Boolean): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), dpToPx(58)).apply {
                if (addStartMargin) marginStart = dpToPx(4)
            }
            gravity = Gravity.CENTER
            minLines = 2
            maxLines = 2
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            text = "${yearMonth.monthValue}月\n${yearMonth.year}"
            background = if (selected) ContextCompat.getDrawable(context, R.drawable.bg_month_selected) else null
            setOnClickListener {
                selectedMonth = yearMonth
                refreshMonthSelection()
                notifyCallDetailMonthChanged()
            }
        }
    }

    private fun refreshMonthSelection() {
//        monthOptions.forEachIndexed { index, yearMonth ->
//            val monthView = binding.monthContainer.getChildAt(index) as? TextView ?: return@forEachIndexed
//            val isSelected = yearMonth == selectedMonth
//            monthView.text = "${yearMonth.monthValue}月\n${yearMonth.year}"
//            monthView.background = if (isSelected) ContextCompat.getDrawable(this, R.drawable.bg_month_selected) else null
//        }
    }

    private fun applyCustomNumberInfo() {
//        val customNumber = AppPreferences.getCustomPhoneNumber(this).trim()
//        val starLevel = AppPreferences.getCustomStarLevel(this).coerceIn(1, 5)
//        val starIcon = AppCompatResources.getDrawable(this, getStarLevelIconRes(starLevel))
//        binding.tvSummarySubtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(starIcon, null, null, null)
//        binding.tvSummaryPhone.text = if (customNumber.isNotEmpty()) maskPhoneNumber(customNumber) else "--"
//        binding.tvSummaryName.text = getSummaryDisplayName()
//        binding.tvSummaryHint.text = getString(R.string.summary_hint_default)
    }

    private fun getSummaryDisplayName(): String {
        val rawName = AppPreferences.getCustomDisplayName(this).trim()
            .ifBlank { getString(R.string.summary_name_default) }
        return formatSummaryDisplayName(rawName)
    }

    private fun formatSummaryDisplayName(rawName: String): String {
        val normalized = rawName.trim()
        if (normalized.isBlank()) {
            return getString(R.string.summary_name_default)
        }
        return normalized.take(1) + "*"
    }

    private fun getStarLevelIconRes(starLevel: Int): Int {
        return when (starLevel.coerceIn(1, 5)) {
            1 -> R.drawable.userinfo_icon_one_level_new
            2 -> R.drawable.userinfo_icon_two_level_new
            3 -> R.drawable.userinfo_icon_three_level_new
            4 -> R.drawable.userinfo_icon_four_level_new
            else -> R.drawable.userinfo_icon_five_level_new
        }
    }

    private fun maskPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.length < 7) return phoneNumber
        return buildString {
            append(phoneNumber.take(3))
            append("****")
            append(phoneNumber.takeLast(4))
        }
    }

    private fun bindAppBarColorTransition() {
//        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
//            val collapsed = abs(verticalOffset) > 8
//            binding.topBarContainer.setBackgroundColor(if (collapsed) topBarCollapsedColor else topBarExpandedColor)
//        })
    }

    private fun applyWindowInsets() {
        val topBarStart = binding.topBarContainer.paddingStart
        val topBarTop = binding.topBarContainer.paddingTop
        val topBarEnd = binding.topBarContainer.paddingEnd
        val topBarBottom = binding.topBarContainer.paddingBottom
//        val appBarContentStart = binding.appBarContentContainer.paddingStart
//        val appBarContentEnd = binding.appBarContentContainer.paddingEnd
//        val appBarContentBottom = binding.appBarContentContainer.paddingBottom

//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            binding.topBarContainer.updatePadding(
//                left = topBarStart,
//                top = topBarTop + systemBars.top,
//                right = topBarEnd,
//                bottom = topBarBottom
//            )
//            binding.topBarContainer.post {
//                binding.appBarContentContainer.updatePadding(
//                    left = appBarContentStart,
//                    top = binding.topBarContainer.height,
//                    right = appBarContentEnd,
//                    bottom = appBarContentBottom
//                )
//                binding.topBarContainer.bringToFront()
//            }
//            insets
//        }
    }

    private fun dpToPx(valueDp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            valueDp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    fun showCallAnalysisTip() {
        Toast.makeText(this, getString(R.string.call_analysis_tip), Toast.LENGTH_SHORT).show()
    }

    fun loadCallRecords() {
        if (!hasProviderPermission()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val cachedRecords = loadCachedCallRecords()
                withContext(Dispatchers.Main) {
                    allRecords = cachedRecords
                    updateMonthOptions(cachedRecords)
                    updateSummary(cachedRecords)
                    pagerAdapter.callDetailFragment.submitRecords(cachedRecords)
                    pagerAdapter.callDetailFragment.setRefreshing(false)
                    notifyCallDetailMonthChanged()
                }
            }
            return
        }

        pagerAdapter.callDetailFragment.setRefreshing(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val providerRecords = queryCallRecords()
            if (providerRecords.isNotEmpty()) {
                callRecordCacheDao.replaceAll(providerRecords.map { it.toCachedEntity() })
            }
            val records = if (providerRecords.isNotEmpty()) providerRecords else loadCachedCallRecords()
            withContext(Dispatchers.Main) {
                allRecords = records
                updateMonthOptions(records)
                updateSummary(records)
                pagerAdapter.callDetailFragment.submitRecords(records)
                pagerAdapter.callDetailFragment.setRefreshing(false)
                notifyCallDetailMonthChanged()
            }
        }
    }

    private fun notifyCallDetailMonthChanged() {
        pagerAdapter.callDetailFragment.setSelectedMonth(selectedMonth)
    }

    private fun hasProviderPermission(): Boolean {
        return checkSelfPermission(READ_CALL_RECORDS_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun loadCachedCallRecords(): List<CallRecord> {
        return callRecordCacheDao.getAll().map { it.toCallRecord() }
    }

    private fun updateMonthOptions(records: List<CallRecord>) {
//        val recordMonths = records
//            .mapNotNull { getRecordYearMonth(it) }
//            .distinct()
//            .sortedDescending()
//        val availableMonths = mergeMonthOptions(recordMonths)
//        selectedMonth = when {
//            availableMonths.isEmpty() -> null
//            selectedMonth in availableMonths -> selectedMonth
//            else -> availableMonths.first()
//        }
//        monthOptions = availableMonths
//        binding.monthContainer.removeAllViews()
//        availableMonths.forEachIndexed { index, yearMonth ->
//            binding.monthContainer.addView(createMonthView(yearMonth, yearMonth == selectedMonth, index > 0))
//        }
    }

    private fun mergeMonthOptions(recordMonths: List<YearMonth>): List<YearMonth> {
        val currentMonth = YearMonth.now()
        val fallback = (0..11).map { currentMonth.minusMonths(it.toLong()) }
        return (recordMonths + fallback).distinct().sortedDescending()
    }

    private fun getRecordYearMonth(record: CallRecord): YearMonth? {
        val timestamp = when {
            record.startTime > 0 -> record.startTime
            record.connectedTime > 0 -> record.connectedTime
            record.endTime > 0 -> record.endTime
            else -> 0L
        }
        if (timestamp <= 0L) return null
        return Instant.ofEpochMilli(timestamp).atZone(monthZoneId).toLocalDate().let { date ->
            YearMonth.of(date.year, date.monthValue)
        }
    }

    private fun updateSummary(records: List<CallRecord>) {
//        val latestRecord = records.firstOrNull()
//        val customNumber = AppPreferences.getCustomPhoneNumber(this).trim()
//        binding.tvSummaryPhone.text = if (customNumber.isNotEmpty()) {
//            maskPhoneNumber(customNumber)
//        } else {
//            latestRecord?.phoneNumber?.let { maskPhoneNumber(it) } ?: "--"
//        }
//        binding.tvSummaryName.text = getSummaryDisplayName()
//        binding.tvSummaryHint.text = if (records.isEmpty()) {
//            getString(R.string.empty_call_records).replace("\n", " ")
//        } else {
//            val attribution = latestRecord?.attribution.orEmpty()
//            val operator = latestRecord?.operator.orEmpty()
//            listOf(attribution, operator).filter { it.isNotBlank() }.joinToString(" 路 ")
//                .ifBlank { getString(R.string.summary_hint_default) }
//        }
    }

    private suspend fun queryCallRecords(): List<CallRecord> {
        val list = mutableListOf<CallRecord>()
        val projection = arrayOf(
            CallRecordContract.CallRecord.COLUMN_ID,
            CallRecordContract.CallRecord.COLUMN_PHONE_NUMBER,
            CallRecordContract.CallRecord.COLUMN_ATTRIBUTION,
            CallRecordContract.CallRecord.COLUMN_OPERATOR,
            CallRecordContract.CallRecord.COLUMN_START_TIME,
            CallRecordContract.CallRecord.COLUMN_CONNECTED_TIME,
            CallRecordContract.CallRecord.COLUMN_END_TIME,
            CallRecordContract.CallRecord.COLUMN_IS_CONNECTED,
            CallRecordContract.CallRecord.COLUMN_CALL_NUMBER,
            CallRecordContract.CallRecord.COLUMN_CALL_TYPE,
            CallRecordContract.CallRecord.COLUMN_RECORDING_PATH,
            CallRecordContract.CallRecord.COLUMN_RECORDING_START_TIME,
            CallRecordContract.CallRecord.COLUMN_RECORDING_END_TIME
        )

        try {
            contentResolver.query(
                CallRecordContract.CallRecord.CONTENT_URI,
                projection,
                null,
                null,
                CallRecordContract.CallRecord.DEFAULT_SORT_ORDER
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_ID)
                val phoneIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_PHONE_NUMBER)
                val attributionIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_ATTRIBUTION)
                val operatorIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_OPERATOR)
                val startTimeIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_START_TIME)
                val connectedTimeIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_CONNECTED_TIME)
                val endTimeIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_END_TIME)
                val isConnectedIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_IS_CONNECTED)
                val callNumberIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_CALL_NUMBER)
                val callTypeIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_CALL_TYPE)
                val recordingPathIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_RECORDING_PATH)
                val recordingStartTimeIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_RECORDING_START_TIME)
                val recordingEndTimeIndex = cursor.getColumnIndex(CallRecordContract.CallRecord.COLUMN_RECORDING_END_TIME)

                while (cursor.moveToNext()) {
                    val record = CallRecord(
                        id = if (idIndex >= 0) cursor.getLong(idIndex) else 0,
                        phoneNumber = if (phoneIndex >= 0) cursor.getString(phoneIndex) else null,
                        attribution = if (attributionIndex >= 0) cursor.getString(attributionIndex) else null,
                        operator = if (operatorIndex >= 0) cursor.getString(operatorIndex) else null,
                        startTime = if (startTimeIndex >= 0) cursor.getLong(startTimeIndex) else 0,
                        connectedTime = if (connectedTimeIndex >= 0) cursor.getLong(
                            connectedTimeIndex
                        ) else 0,
                        endTime = if (endTimeIndex >= 0) cursor.getLong(endTimeIndex) else 0,
                        isConnected = if (isConnectedIndex >= 0) cursor.getInt(isConnectedIndex) == 1 else false,
                        callNumber = if (callNumberIndex >= 0) cursor.getInt(callNumberIndex) else 0,
                        callType = if (callTypeIndex >= 0) cursor.getInt(callTypeIndex) else 0,
                        recordingPath = if (recordingPathIndex >= 0) cursor.getString(
                            recordingPathIndex
                        ) else null,
                        recordingStartTime = if (recordingStartTimeIndex >= 0) cursor.getLong(
                            recordingStartTimeIndex
                        ) else 0,
                        recordingEndTime = if (recordingEndTimeIndex >= 0) cursor.getLong(
                            recordingEndTimeIndex
                        ) else 0
                    )
                    if (record.isConnected) list.add(record)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        return list.sortedBy { record ->
            when {
                record.startTime > 0 -> record.startTime
                record.connectedTime > 0 -> record.connectedTime
                else -> record.endTime
            }
        }
    }
}

private class DetailPagerAdapter(
    activity: AppCompatActivity,
    private val pages: List<String>
) : FragmentStateAdapter(activity) {
    val packageDetailFragment = PackageDetailWebViewFragment()
    val callDetailFragment = CallDetailFragment()
    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> packageDetailFragment
            1 -> callDetailFragment
            else -> ModulePlaceholderFragment.newInstance(pages[position])
        }
    }
}
