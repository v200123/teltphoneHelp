package com.example.myservicecenter

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myservicecenter.databinding.ActivityMainBinding
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        private const val READ_CALL_RECORDS_PERMISSION =
            "com.u2tzjtne.telephonehelper.permission.READ_CALL_RECORDS"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CallRecordAdapter

    private var allRecords: List<CallRecord> = emptyList()
    private var topBarExpandedColor: Int = Color.TRANSPARENT
    private var topBarCollapsedColor: Int = Color.TRANSPARENT
    private var pageOpenTimeMillis: Long = 0L
    private val tipDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageOpenTimeMillis = System.currentTimeMillis()
        enableEdgeToEdgeStatusBar()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        applyWindowInsets()
        bindAppBarColorTransition()
    }

    override fun onResume() {
        super.onResume()
        syncOutgoingPackageInfo()
        syncCustomSelfRegion()
        applyCustomNumberInfo()
        loadCallRecords()
    }

    private fun initViews() {
        topBarExpandedColor = ContextCompat.getColor(this, R.color.panel_blue_start)
        topBarCollapsedColor = ContextCompat.getColor(this, R.color.panel_blue_start)

        adapter = CallRecordAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.topBarContainer.setBackgroundColor(topBarExpandedColor)

        applyCustomNumberInfo()
        binding.tvSummaryHint.text = getString(R.string.summary_hint_default)
        binding.btnSearch.setOnClickListener {
            applyFilter()
        }
        binding.btnMore.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.etSearchPhone.doAfterTextChanged {
            if (it.isNullOrBlank()) {
                applyFilter()
            }
        }
        binding.swipeRefresh.setOnRefreshListener {
            loadCallRecords()
        }
        syncOutgoingPackageInfo()
        syncCustomSelfRegion()
        updateWarmTip()
        binding.btnCallAnalysis.setOnClickListener {
            Toast.makeText(this, getString(R.string.call_analysis_tip), Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindAppBarColorTransition() {
        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val collapsed = abs(verticalOffset) > 8
            binding.topBarContainer.setBackgroundColor(
                if (collapsed) topBarCollapsedColor else topBarExpandedColor
            )
        })
    }

    private fun syncOutgoingPackageInfo() {
        adapter.setOutgoingPackageInfo(AppPreferences.getOutgoingPackageInfo(this))
    }

    private fun syncCustomSelfRegion() {
        adapter.setCustomSelfRegion(AppPreferences.getCustomSelfRegion(this))
    }

    private fun applyCustomNumberInfo() {
        val customNumber = AppPreferences.getCustomPhoneNumber(this).trim()
        val starLevel = AppPreferences.getCustomStarLevel(this).coerceIn(1, 5)
//        binding.tvSummarySubtitle.text = "${starLevel}星用户"
        val starIcon = AppCompatResources.getDrawable(this, getStarLevelIconRes(starLevel))
        binding.tvSummarySubtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(starIcon, null, null, null)
        if (customNumber.isEmpty() && binding.tvSummaryPhone.text.isNullOrBlank()) {
            binding.tvSummaryPhone.text = "--"
        } else if (customNumber.isNotEmpty()) {
            binding.tvSummaryPhone.text = maskPhoneNumber(customNumber)
        }
        updateWarmTip()
    }

    private fun updateWarmTip() {
        val customNumber = AppPreferences.getCustomPhoneNumber(this).trim()
        val maskedNumber = if (customNumber.isBlank()) "--" else maskPhoneNumber(customNumber)
        val openTimeText = tipDateFormat.format(Date(pageOpenTimeMillis))
        adapter.setWarmTip(getString(R.string.warm_tip_template, maskedNumber, openTimeText))
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
    private fun enableEdgeToEdgeStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true
    }

    private fun applyWindowInsets() {
        val topBarStart = binding.topBarContainer.paddingStart
        val topBarTop = binding.topBarContainer.paddingTop
        val topBarEnd = binding.topBarContainer.paddingEnd
        val topBarBottom = binding.topBarContainer.paddingBottom
        val listStart = binding.recyclerView.paddingStart
        val listTop = binding.recyclerView.paddingTop
        val listEnd = binding.recyclerView.paddingEnd
        val listBottom = binding.recyclerView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.topBarContainer.updatePadding(
                left = topBarStart,
                top = topBarTop + systemBars.top,
                right = topBarEnd,
                bottom = topBarBottom
            )
            binding.recyclerView.updatePadding(
                left = listStart,
                top = listTop,
                right = listEnd,
                bottom = listBottom + systemBars.bottom
            )
            binding.topBarContainer.post {
                val topBarHeight = binding.topBarContainer.height
                binding.appBarLayout.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    topMargin = topBarHeight
                }
            }

            insets
        }
    }

    private fun loadCallRecords() {
        if (!hasProviderPermission()) {
            allRecords = emptyList()
            applyFilter()
           return
        }

        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            val records = queryCallRecords()
            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = false
                allRecords = records
                updateSummary(records)
                applyFilter()
            }
        }
    }

    private fun hasProviderPermission(): Boolean {
        return checkSelfPermission(READ_CALL_RECORDS_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun applyFilter() {
        val keyword = binding.etSearchPhone.text?.toString()?.trim().orEmpty()
        val filteredRecords = if (keyword.isEmpty()) {
            allRecords
        } else {
            allRecords.filter { it.phoneNumber.orEmpty().contains(keyword) }
        }

        if (filteredRecords.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.setData(filteredRecords)
        }
    }

    private fun updateSummary(records: List<CallRecord>) {
        val latestRecord = records.firstOrNull()
        val customNumber = AppPreferences.getCustomPhoneNumber(this).trim()
        binding.tvSummaryPhone.text = if (customNumber.isNotEmpty()) {
            maskPhoneNumber(customNumber)
        } else {
            latestRecord?.phoneNumber?.let { maskPhoneNumber(it) } ?: "--"
        }
        binding.tvSummaryHint.text = if (records.isEmpty()) {
            getString(R.string.empty_call_records).replace("\n", " ")
        } else {
            val attribution = latestRecord?.attribution.orEmpty()
            val operator = latestRecord?.operator.orEmpty()
            listOf(attribution, operator)
                .filter { it.isNotBlank() }
                .joinToString(" 路 ")
                .ifBlank { getString(R.string.summary_hint_default) }
        }
    }

    private fun maskPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.length < 7) {
            return phoneNumber
        }
        return buildString {
            append(phoneNumber.take(3))
            append("****")
            append(phoneNumber.takeLast(4))
        }
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
                    list.add(
                        CallRecord(
                            id = if (idIndex >= 0) cursor.getLong(idIndex) else 0,
                            phoneNumber = if (phoneIndex >= 0) cursor.getString(phoneIndex) else null,
                            attribution = if (attributionIndex >= 0) cursor.getString(attributionIndex) else null,
                            operator = if (operatorIndex >= 0) cursor.getString(operatorIndex) else null,
                            startTime = if (startTimeIndex >= 0) cursor.getLong(startTimeIndex) else 0,
                            connectedTime = if (connectedTimeIndex >= 0) cursor.getLong(connectedTimeIndex) else 0,
                            endTime = if (endTimeIndex >= 0) cursor.getLong(endTimeIndex) else 0,
                            isConnected = if (isConnectedIndex >= 0) cursor.getInt(isConnectedIndex) == 1 else false,
                            callNumber = if (callNumberIndex >= 0) cursor.getInt(callNumberIndex) else 0,
                            callType = if (callTypeIndex >= 0) cursor.getInt(callTypeIndex) else 0,
                            recordingPath = if (recordingPathIndex >= 0) cursor.getString(recordingPathIndex) else null,
                            recordingStartTime = if (recordingStartTimeIndex >= 0) cursor.getLong(recordingStartTimeIndex) else 0,
                            recordingEndTime = if (recordingEndTimeIndex >= 0) cursor.getLong(recordingEndTimeIndex) else 0
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "${e.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        // 某些设备上的内容提供者可能不严格遵循 sortOrder，这里在客户端兜底按时间正序（最新在下）。
        return list.sortedBy { record ->
            when {
                record.startTime > 0 -> record.startTime
                record.connectedTime > 0 -> record.connectedTime
                else -> record.endTime
            }
        }
    }
}

