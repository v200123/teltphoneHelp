package com.example.myservicecenter.ui.main

import com.example.myservicecenter.PhoneDisplayManager
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.myservicecenter.R

import com.example.myservicecenter.core.AppPreferences
import com.example.myservicecenter.data.calllog.CallRecord
import com.example.myservicecenter.data.calllog.CallRecordCacheDatabase
import com.example.myservicecenter.data.calllog.toCachedEntity
import com.example.myservicecenter.data.calllog.toCallRecord
import com.example.myservicecenter.databinding.ActivityMainBinding
import com.example.myservicecenter.ui.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import com.example.myservicecenter.core.CommonWebViewSupport
import com.example.myservicecenter.data.calllog.CallRecordContract
import com.example.myservicecenter.data.sms.SmsDetailDatabase
import com.example.myservicecenter.data.sms.SmsDetailRecordEntity
import kotlin.collections.filter
import kotlin.collections.isNotEmpty
import kotlin.collections.map

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val callRecordCacheDao by lazy {
        CallRecordCacheDatabase.getInstance(applicationContext).callRecordCacheDao()
    }
    private val smsDetailDao by lazy {
        SmsDetailDatabase.getInstance(applicationContext).smsDetailDao()
    }

    private var topBarExpandedColor: Int = Color.TRANSPARENT
    private var pageScrollThresholdPx: Int = 0

    companion object {
        private const val DETAIL_PAGE_URL = "file:///android_asset/new_order_pager.html"
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
        setupWebView()
    }

    private fun initViews() {
        pageScrollThresholdPx = dpToPx(25)
        binding.topBarContainer.setBackgroundColor(topBarExpandedColor)
        binding.topBarContainer.bringToFront()
        binding.rlBasemoduleRightMenuParent.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.titleBackBtn.setOnClickListener {
            finish()
        }
    }

    fun updateTopBarForPageScroll(scrollY: Int) {
        val progress = (scrollY.toFloat() / pageScrollThresholdPx)
            .coerceIn(0f, 1f)
        val alpha = (progress * 255).toInt()
        binding.topBarContainer.setBackgroundColor(Color.argb(alpha, 255, 255, 255))
    }

    fun loadCallRecords() {
        syncPersistedPageState()
    }

    fun showCallAnalysisTip() {
        Toast.makeText(this, getString(R.string.call_analysis_tip), Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setupWebView() {
        binding.webViewPackageDetail.apply {
            setBackgroundColor(Color.WHITE)
            alpha = 0f
            addJavascriptInterface(EditorBridge(), "PackageDetailBridge")
            CommonWebViewSupport.configure(
                webView = this,
                logTag = "PackageDetailWebView",
                onPageFinished = { _, _ ->
                    syncInitialPageStateAndReveal()
                    requestActiveSmsDetailList()
                    bindBasicInfoEditorClick()
                    bindEditorClick()
                }
            )
            loadUrl(buildDetailPageUrl())
        }
    }

    private fun buildDetailPageUrl(): String {
        val phoneNumber = PhoneDisplayManager.managedPhone(this)
        val displayName = AppPreferences.getCustomDisplayName(this).trim()
        val badgeLevel = AppPreferences.getCustomStarLevel(this).toString()
        return DETAIL_PAGE_URL.toUri()
            .buildUpon()
            .appendQueryParameter("phoneNumber", phoneNumber)
            .appendQueryParameter("name", displayName)
            .appendQueryParameter("badgeLevel", badgeLevel)
            .build()
            .toString()
    }

    private fun syncPackageListToPage() {
        val listJson = escapeJsString(AppPreferences.getWebViewFixedFeeListJson(this))
        val script = """
            (function() {
              var records = [];
              try {
                records = JSON.parse('$listJson');
              } catch (e) {
                records = [];
              }
              if (typeof window.renderFixedFeeList === 'function') {
                window.renderFixedFeeList(records);
              }
            })();
        """.trimIndent()
        evaluatePageScript(script)
    }

    /**
     * The bundled page remains hidden until its persisted values have been applied.
     * This prevents its template/sample values from briefly appearing before the
     * JavaScript bridge overwrites them.
     */
    private fun syncInitialPageStateAndReveal() {
        val listJson = escapeJsString(AppPreferences.getWebViewFixedFeeListJson(this))
        val customPhone = PhoneDisplayManager.managedPhone(this)
        val displayName = AppPreferences.getCustomDisplayName(this).trim()
        val badgeLevel = AppPreferences.getCustomStarLevel(this)
        val openedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val script = """
            (function() {
              var records = [];
              try {
                records = JSON.parse('$listJson');
              } catch (e) {
                records = [];
              }
              if (typeof window.renderFixedFeeList === 'function') {
                window.renderFixedFeeList(records);
              }
              var config = {
                phoneNumber: '${escapeJsString(customPhone)}',
                name: '${escapeJsString(displayName)}',
                badgeLevel: '${escapeJsString(badgeLevel.toString())}',
                openedAt: '${escapeJsString(openedAt)}'
              };
              if (typeof window.applyPackageDetailConfig === 'function') {
                window.applyPackageDetailConfig(config);
              }
            })();
        """.trimIndent()
        binding.webViewPackageDetail.evaluateJavascript(script) {
            binding.webViewPackageDetail.alpha = 1f
        }
    }

    private fun syncBasicInfoToPage() {
        val customPhone = PhoneDisplayManager.managedPhone(this)
        val displayName = AppPreferences.getCustomDisplayName(this).trim()
        val badgeLevel = AppPreferences.getCustomStarLevel(this)
        val openedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val script = """
            (function() {
              var config = {
                phoneNumber: '${escapeJsString(customPhone)}',
                name: '${escapeJsString(displayName)}',
                badgeLevel: '${escapeJsString(badgeLevel.toString())}',
                openedAt: '${escapeJsString(openedAt)}'
              };
              if (typeof window.applyPackageDetailConfig === 'function') {
                window.applyPackageDetailConfig(config);
              }
            })();
        """.trimIndent()
        evaluatePageScript(script)
    }

    private fun bindEditorClick() {
        val script = """
            (function() {
              if (window.__fixedFeeEditorBound) return;
              window.__fixedFeeEditorBound = true;
              document.addEventListener('click', function(event) {
                var target = event.target;
                if (!target) return;
                var item = target.closest ? target.closest('.list1-item') : null;
                if (!item) return;
                if (window.PackageDetailBridge && typeof window.PackageDetailBridge.openEditor === 'function') {
                  window.PackageDetailBridge.openEditor();
                }
              }, true);
            })();
        """.trimIndent()
        evaluatePageScript(script)
    }

    private fun bindBasicInfoEditorClick() {
        val script = """
            (function() {
              if (window.__basicInfoEditorBound) return;
              window.__basicInfoEditorBound = true;
              document.addEventListener('click', function(event) {
                var target = event.target;
                if (!target) return;
                var userInfo = target.closest ? target.closest('.userInfo') : null;
                var noticeView = target.closest ? target.closest('.notice-view') : null;
                if (!userInfo && !noticeView) return;
                if (window.PackageDetailBridge && typeof window.PackageDetailBridge.openBasicInfoEditor === 'function') {
                  window.PackageDetailBridge.openBasicInfoEditor();
                }
              }, true);
            })();
        """.trimIndent()
        evaluatePageScript(script)
    }

    private fun syncPersistedPageState() {
        binding.webViewPackageDetail.post {
            val targetUrl = buildDetailPageUrl()
            val currentUrl = binding.webViewPackageDetail.url.orEmpty()
            if (currentUrl != targetUrl) {
                binding.webViewPackageDetail.loadUrl(targetUrl)
                return@post
            }
            syncPackageListToPage()
            syncBasicInfoToPage()
            requestActiveSmsDetailList()
        }
    }

    private fun requestCallDetailListForMonth(year: Int, month: Int) {
        lifecycleScope.launch {
            val listJson = withContext(Dispatchers.IO) {
                buildCallDetailListJson(year, month)
            }
            val script = """
                (function() {
                  var records = [];
                  try {
                    records = JSON.parse('${escapeJsString(listJson)}');
                  } catch (e) {
                    records = [];
                  }
                  if (typeof window.renderCallDetailList === 'function') {
                    window.renderCallDetailList(records, { year: $year, month: $month });
                  }
                })();
            """.trimIndent()
            evaluatePageScript(script)
        }
    }

    private fun requestSmsDetailListForMonth(year: Int, month: Int) {
        lifecycleScope.launch {
            val listJson = withContext(Dispatchers.IO) {
                buildSmsDetailListJson(year, month)
            }
            val script = """
                (function() {
                  var records = [];
                  try {
                    records = JSON.parse('${escapeJsString(listJson)}');
                  } catch (e) {
                    records = [];
                  }
                  if (typeof window.renderSmsDetailList === 'function') {
                    window.renderSmsDetailList(records, { year: $year, month: $month });
                  }
                })();
            """.trimIndent()
            evaluatePageScript(script)
        }
    }

    private fun requestActiveSmsDetailList() {
        val script = """
            (function() {
              if (typeof window.requestCurrentSmsDetailData === 'function') {
                window.requestCurrentSmsDetailData();
              }
            })();
        """.trimIndent()
        evaluatePageScript(script)
    }

    private fun showBasicInfoEditorDialog() {
        val phoneInput = EditText(this).apply {
            hint = "请输入手机号"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(PhoneDisplayManager.managedPhone(this@MainActivity))
            maxLines = 1
        }
        val nameInput = EditText(this).apply {
            hint = "请输入姓名"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            setText(AppPreferences.getCustomDisplayName(this@MainActivity))
            maxLines = 1
        }
        val starInput = EditText(this).apply {
            hint = "请输入等级（1-5）"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(AppPreferences.getCustomStarLevel(this@MainActivity).toString())
            maxLines = 1
        }

        listOf(phoneInput, nameInput, starInput).forEachIndexed { index, editText ->
            editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            editText.setTextColor(Color.parseColor("#333333"))
            editText.setPadding(dpToPx(12), 0, dpToPx(12), 0)
            editText.background = getDrawable(R.drawable.bg_search_input)
            editText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            ).apply {
                if (index > 0) {
                    topMargin = dpToPx(10)
                }
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0)
            addView(phoneInput)
            addView(nameInput)
            addView(starInput)
        }

        AlertDialog.Builder(this)
            .setTitle("设置详单基本信息")
            .setMessage("点击顶部信息区或温馨提示即可再次打开。手机号会自动脱敏，温馨提示号码与手机号保持一致。")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val phone = phoneInput.text?.toString().orEmpty().trim()
                val name = nameInput.text?.toString().orEmpty().trim()
                val starLevel = starInput.text?.toString()?.toIntOrNull()?.coerceIn(1, 5) ?: 3
                PhoneDisplayManager.updateManagedPhone(this, phone)
                AppPreferences.setCustomDisplayName(this, name)
                AppPreferences.setCustomStarLevel(this, starLevel)
                syncPersistedPageState()
                Toast.makeText(this, "详单基本信息已更新", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showEditorDialog() {
        val currentRecord = readFixedFeeRecord()
        val nameInput = EditText(this).apply {
            hint = "请输入套餐名称"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentRecord.name)
            maxLines = 2
        }
        val feeInput = EditText(this).apply {
            hint = "请输入金额"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(currentRecord.fee)
            maxLines = 1
        }

        listOf(nameInput, feeInput).forEachIndexed { index, editText ->
            editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            editText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index > 0) {
                    topMargin = dpToPx(12)
                }
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0)
            addView(nameInput)
            addView(feeInput)
        }

        AlertDialog.Builder(this)
            .setTitle("设置套餐及固定费用详单数据")
            .setMessage("点击套餐内容即可再次打开。这里只修改套餐名称和费用。")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val packageName = nameInput.text?.toString().orEmpty().trim()
                val feeText = feeInput.text?.toString().orEmpty().trim()
                try {
                    val savedJson = buildFixedFeeListJson(packageName, feeText)
                    AppPreferences.setWebViewFixedFeeListJson(this, savedJson)
                    syncPackageListToPage()
                    Toast.makeText(this, "套餐详单数据已更新", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(this, "保存失败：${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun readFixedFeeRecord(): FixedFeeRecord {
        return try {
            val array = JSONArray(AppPreferences.getWebViewFixedFeeListJson(this))
            val first = if (array.length() > 0) array.optJSONObject(0) else null
            FixedFeeRecord(
                name = first?.optString("name").orEmpty().ifBlank { "自由选套餐8元档（语音版）" },
                fee = first?.opt("fee")?.toString().orEmpty().ifBlank { "8.00" }
            )
        } catch (_: Exception) {
            FixedFeeRecord(
                name = "自由选套餐8元档（语音版）",
                fee = "8.00"
            )
        }
    }

    private fun buildFixedFeeListJson(packageName: String, feeText: String): String {
        val existingJson = AppPreferences.getWebViewFixedFeeListJson(this)
        val array = try {
            JSONArray(existingJson)
        } catch (_: Exception) {
            JSONArray()
        }
        val first = if (array.length() > 0) {
            array.optJSONObject(0) ?: JSONObject()
        } else {
            JSONObject()
        }
        first.put("name", packageName.ifBlank { "自由选套餐8元档（语音版）" })
        first.put("fee", feeText.toDoubleOrNull() ?: 8.0)
        if (!first.has("description")) {
            first.put("description", "")
        }
        if (array.length() > 0) {
            array.put(0, first)
        } else {
            array.put(first)
        }
        return array.toString()
    }

    private suspend fun buildCallDetailListJson(year: Int, month: Int): String {
        val records = loadCallDetailRecords(year, month)
        val result = JSONArray()
        records.forEach { record ->
            result.put(record.toWebCallDetailJson())
        }
        return result.toString()
    }

    private suspend fun buildSmsDetailListJson(year: Int, month: Int): String {
        val (monthStartMillis, nextMonthStartMillis) = buildMonthRange(year, month)
        val records = smsDetailDao.getByMonth(monthStartMillis, nextMonthStartMillis)
        val result = JSONArray()
        records.forEach { record ->
            result.put(record.toWebSmsDetailJson())
        }
        return result.toString()
    }

    private suspend fun loadCallDetailRecords(year: Int, month: Int): List<CallRecord> {
        val providerRecords = if (hasProviderPermission()) {
            queryCallRecords(year, month)
        } else {
            emptyList()
        }
        if (providerRecords.isNotEmpty()) {
            callRecordCacheDao.replaceAll(providerRecords.map { it.toCachedEntity() })
            return providerRecords
        }
        return filterCallRecordsByMonth(
            callRecordCacheDao.getAll().map { it.toCallRecord() },
            year,
            month
        )
    }

    private fun hasProviderPermission(): Boolean {
        return checkSelfPermission(READ_CALL_RECORDS_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun queryCallRecords(year: Int, month: Int): List<CallRecord> {
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
        val (monthStartMillis, nextMonthStartMillis) = buildMonthRange(year, month)
        val selection = """
            (
              (${CallRecordContract.CallRecord.COLUMN_START_TIME} >= ? AND ${CallRecordContract.CallRecord.COLUMN_START_TIME} < ?)
              OR
              (${CallRecordContract.CallRecord.COLUMN_START_TIME} <= 0 AND ${CallRecordContract.CallRecord.COLUMN_CONNECTED_TIME} >= ? AND ${CallRecordContract.CallRecord.COLUMN_CONNECTED_TIME} < ?)
              OR
              (${CallRecordContract.CallRecord.COLUMN_START_TIME} <= 0 AND ${CallRecordContract.CallRecord.COLUMN_CONNECTED_TIME} <= 0 AND ${CallRecordContract.CallRecord.COLUMN_END_TIME} >= ? AND ${CallRecordContract.CallRecord.COLUMN_END_TIME} < ?)
            )
            AND ${CallRecordContract.CallRecord.COLUMN_IS_CONNECTED} = ?
        """.trimIndent()
        val selectionArgs = arrayOf(
            monthStartMillis.toString(),
            nextMonthStartMillis.toString(),
            monthStartMillis.toString(),
            nextMonthStartMillis.toString(),
            monthStartMillis.toString(),
            nextMonthStartMillis.toString(),
            "1"
        )
        try {
            contentResolver.query(
                CallRecordContract.CallRecord.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
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
                        connectedTime = if (connectedTimeIndex >= 0) cursor.getLong(connectedTimeIndex) else 0,
                        endTime = if (endTimeIndex >= 0) cursor.getLong(endTimeIndex) else 0,
                        isConnected = if (isConnectedIndex >= 0) cursor.getInt(isConnectedIndex) == 1 else false,
                        callNumber = if (callNumberIndex >= 0) cursor.getInt(callNumberIndex) else 0,
                        callType = if (callTypeIndex >= 0) cursor.getInt(callTypeIndex) else 0,
                        recordingPath = if (recordingPathIndex >= 0) cursor.getString(recordingPathIndex) else null,
                        recordingStartTime = if (recordingStartTimeIndex >= 0) cursor.getLong(recordingStartTimeIndex) else 0,
                        recordingEndTime = if (recordingEndTimeIndex >= 0) cursor.getLong(recordingEndTimeIndex) else 0
                    )
                    if (record.isConnected) {
                        list.add(record)
                    }
                }
            }
        } catch (error: Exception) {
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, error.message.orEmpty(), Toast.LENGTH_LONG).show()
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

    private fun buildMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val startCalendar = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = startCalendar.timeInMillis
            add(Calendar.MONTH, 1)
        }
        return startCalendar.timeInMillis to endCalendar.timeInMillis
    }

    private fun filterCallRecordsByMonth(records: List<CallRecord>, year: Int, month: Int): List<CallRecord> {
        if (records.isEmpty()) {
            return emptyList()
        }
        return records.filter { record ->
            val timestamp = resolveDisplayTimestamp(record)
            if (timestamp <= 0L) {
                false
            } else {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp
                calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) + 1 == month
            }
        }
    }

    private fun CallRecord.toWebCallDetailJson(): JSONObject {
        val isIncoming = callType == 1
        val billSeconds = calculateBillSeconds(this)
        // 被叫不参与计费，无论通话时长多少，详单均显示 0 分钟。
        val billedMinutes = if (isIncoming) 0 else calculateBilledMinutes(billSeconds)
        val displayTimestamp = resolveDisplayTimestamp(this)
        val customRegion = AppPreferences.getCustomSelfRegion(this@MainActivity).trim()
        val outgoingPackage = AppPreferences.getOutgoingPackageInfo(this@MainActivity).trim()
        val incomingPackage = AppPreferences.getIncomingPackageInfo(this@MainActivity).trim()
        val customOutgoingType = AppPreferences.getCustomOutgoingCallType(this@MainActivity).trim()
        val customIncomingType = AppPreferences.getCustomIncomingCallType(this@MainActivity).trim()
        val title = if (isIncoming) {
            getString(R.string.record_voice_hd_incoming)
        } else {
            getString(R.string.record_voice_hd_outgoing)
        }
        val communicationType = if (isIncoming) {
            customIncomingType.ifBlank { getString(R.string.record_type_incoming_domestic) }
        } else {
            customOutgoingType.ifBlank { getString(R.string.record_type_outgoing_local) }
        }
        // 套餐文案由设置控制；没有填写时保持空白，不显示模板默认文案。
        val packageName = if (isIncoming) incomingPackage else outgoingPackage
        val location = customRegion.ifBlank {
            attribution ?: operator ?: getString(R.string.record_unknown_location)
        }
        return JSONObject().apply {
            put("callType", title)
            put("phoneNumber", phoneNumber.orEmpty())
            put("time", formatCallRecordTime(displayTimestamp))
            put("startTimeMillis", displayTimestamp)
            put("duration", formatDuration(billSeconds))
            put("location", location)
            put("packageName", packageName)
            put("communicationType", communicationType)
            put("billingMinutes", billedMinutes.toString())
            put("fee", "0.00")
        }
    }

    private fun SmsDetailRecordEntity.toWebSmsDetailJson(): JSONObject {
        return JSONObject().apply {
            put("direction", direction)
            put("phoneNumber", phoneNumber)
            put("messageType", messageType)
            put("location", location)
            put("time", formatCallRecordTime(timestampMillis))
            put("timestampMillis", timestampMillis)
            put("packageName", packageName)
            put("fee", fee)
        }
    }

    private fun evaluatePageScript(script: String) {
        binding.webViewPackageDetail.evaluateJavascript(script, null)
    }

    private fun resolveDisplayTimestamp(record: CallRecord): Long {
        return when {
            record.startTime > 0 -> record.startTime
            record.connectedTime > 0 -> record.connectedTime
            record.endTime > 0 -> record.endTime
            else -> 0L
        }
    }

    private fun calculateBillSeconds(record: CallRecord): Int {
        val start = if (record.connectedTime > 0) record.connectedTime else record.startTime
        val end = record.endTime
        if (start <= 0 || end <= 0 || end < start) {
            return 0
        }
        return ((end - start) / 1000L).coerceAtLeast(0L).toInt()
    }

    private fun calculateBilledMinutes(totalSeconds: Int): Int {
        if (totalSeconds <= 0) {
            return 0
        }
        return (totalSeconds + 59) / 60
    }

    private fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds <= 0) {
            return "00秒"
        }
        if (totalSeconds < 60) {
            return String.format(Locale.getDefault(), "%02d秒", totalSeconds)
        }
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d分%02d秒", minutes, seconds)
    }

    private fun formatCallRecordTime(timestamp: Long): String {
        if (timestamp <= 0L) {
            return "--"
        }
        return SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun escapeJsString(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun dpToPx(valueDp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            valueDp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onResume() {
        super.onResume()
        binding.webViewPackageDetail.onResume()
        syncPersistedPageState()
    }

    override fun onPause() {
        binding.webViewPackageDetail.onPause()
        super.onPause()
    }

    inner class EditorBridge {
        @JavascriptInterface
        fun openEditor() {
            runOnUiThread { showEditorDialog() }
        }

        @JavascriptInterface
        fun openBasicInfoEditor() {
            runOnUiThread { showBasicInfoEditorDialog() }
        }

        @JavascriptInterface
        fun requestCallDetailMonth(yearText: String?, monthText: String?) {
            val year = yearText?.toIntOrNull() ?: return
            val month = monthText?.toIntOrNull() ?: return
            if (month !in 1..12) {
                return
            }
            runOnUiThread {
                requestCallDetailListForMonth(year, month)
            }
        }

        @JavascriptInterface
        fun requestSmsDetailMonth(yearText: String?, monthText: String?) {
            val year = yearText?.toIntOrNull() ?: return
            val month = monthText?.toIntOrNull() ?: return
            if (month !in 1..12) {
                return
            }
            runOnUiThread {
                requestSmsDetailListForMonth(year, month)
            }
        }

        @JavascriptInterface
        fun onPageScroll(scrollYText: String?) {
            val scrollY = scrollYText?.toIntOrNull() ?: 0
            runOnUiThread {
//                Log.d("onPageScroll", "onPageScroll：${scrollY}")
                updateTopBarForPageScroll(scrollY)
            }
        }
    }

    private data class FixedFeeRecord(
        val name: String,
        val fee: String
    )
}
