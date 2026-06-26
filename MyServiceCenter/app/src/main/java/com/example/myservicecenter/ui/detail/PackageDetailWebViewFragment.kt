package com.example.myservicecenter.ui.detail

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myservicecenter.AppPreferences
import com.example.myservicecenter.CallRecord
import com.example.myservicecenter.CallRecordCacheDatabase
import com.example.myservicecenter.CallRecordContract
import com.example.myservicecenter.CommonWebViewSupport
import com.example.myservicecenter.R
import com.example.myservicecenter.databinding.FragmentPackageDetailWebviewBinding
import com.example.myservicecenter.ui.main.MainActivity
import com.example.myservicecenter.toCallRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.forEach

class PackageDetailWebViewFragment : Fragment(R.layout.fragment_package_detail_webview) {
    companion object {
        private const val TAG = "PackageDetailFragment"
        private const val DETAIL_PAGE_URL = "https://www.lastcoffee.top:8200/merged_order_tabs.html"
        private const val READ_CALL_RECORDS_PERMISSION =
            "com.u2tzjtne.telephonehelper.permission.READ_CALL_RECORDS"
    }

    private var _binding: FragmentPackageDetailWebviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        _binding = FragmentPackageDetailWebviewBinding.bind(view)
        setupWebView()
    }

    // WebView setup
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setupWebView() {
//        binding.progressPackageDetail.visibility = View.VISIBLE
        binding.webViewPackageDetail.apply {
            setBackgroundColor(Color.WHITE)
            addJavascriptInterface(EditorBridge(), "PackageDetailBridge")
            CommonWebViewSupport.configure(
                webView = this,
                logTag = "PackageDetailWebView",
                onPageCommitVisible = { _, _ ->
//                    binding.progressPackageDetail.visibility = View.GONE
                },
                onPageFinished = { _, _ ->
//                    binding.progressPackageDetail.visibility = View.GONE
                    syncPackageListToPage()
                    syncBasicInfoToPage()
                    bindBasicInfoEditorClick()
                    bindEditorClick()
                }
            )
            loadUrl(buildDetailPageUrl())
        }
    }

    // Page state and sync
    private fun buildDetailPageUrl(): String {
        val context = context ?: return DETAIL_PAGE_URL
        val phoneNumber = AppPreferences.getCustomPhoneNumber(context).trim()
        val displayName = AppPreferences.getCustomDisplayName(context).trim()
        val badgeLevel = AppPreferences.getCustomStarLevel(context).toString()
        return Uri.parse(DETAIL_PAGE_URL)
            .buildUpon()
            .appendQueryParameter("phoneNumber", phoneNumber)
            .appendQueryParameter("name", displayName)
            .appendQueryParameter("badgeLevel", badgeLevel)
            .build()
            .toString()
    }

    private fun syncPackageListToPage() {
        val context = context ?: return
        val listJson = escapeJsString(AppPreferences.getWebViewFixedFeeListJson(context))
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

    private fun syncBasicInfoToPage() {
        val context = context ?: return
        val customPhone = AppPreferences.getCustomPhoneNumber(context).trim()
        val displayPhone = if (customPhone.isNotBlank()) customPhone else ""
        val displayName = AppPreferences.getCustomDisplayName(context).trim()
        val badgeLevel = AppPreferences.getCustomStarLevel(context)
        val openedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val script = """
            (function() {
              var config = {
                phoneNumber: '${escapeJsString(displayPhone)}',
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

    private fun syncPersistedPageState() {
        _binding?.webViewPackageDetail?.post {
            val targetUrl = buildDetailPageUrl()
            val currentUrl = _binding?.webViewPackageDetail?.url.orEmpty()
            if (currentUrl != targetUrl) {
                _binding?.webViewPackageDetail?.loadUrl(targetUrl)
                return@post
            }
            syncPackageListToPage()
            syncBasicInfoToPage()
        }
    }

    private fun requestCallDetailListForMonth(year: Int, month: Int) {
        val context = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val listJson = withContext(Dispatchers.IO) {
                buildCallDetailListJson(context, year, month)
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

    // Page click bindings
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

    // Native editor dialogs
    private fun showBasicInfoEditorDialog() {
        val context = context ?: return
        val phoneInput = EditText(context).apply {
            hint = "请输入手机号"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(AppPreferences.getCustomPhoneNumber(context))
            maxLines = 1
        }
        val nameInput = EditText(context).apply {
            hint = "请输入姓名"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            setText(AppPreferences.getCustomDisplayName(context))
            maxLines = 1
        }
        val starInput = EditText(context).apply {
            hint = "请输入等级（1-5）"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(AppPreferences.getCustomStarLevel(context).toString())
            maxLines = 1
        }

        listOf(phoneInput, nameInput, starInput).forEachIndexed { index, editText ->
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            editText.setTextColor(Color.parseColor("#333333"))
            editText.setPadding(dpToPx(12), 0, dpToPx(12), 0)
            editText.background = context.getDrawable(R.drawable.bg_search_input)
            editText.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            ).apply {
                if (index > 0) {
                    topMargin = dpToPx(10)
                }
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0)
            addView(phoneInput)
            addView(nameInput)
            addView(starInput)
        }

        AlertDialog.Builder(context)
            .setTitle("设置详单基本信息")
            .setMessage("点击顶部信息区或温馨提示即可再次打开。手机号会自动脱敏，温馨提示号码与手机号保持一致。")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val phone = phoneInput.text?.toString().orEmpty().trim()
                val name = nameInput.text?.toString().orEmpty().trim()
                val starLevel = starInput.text?.toString()?.toIntOrNull()?.coerceIn(1, 5) ?: 3
                AppPreferences.setCustomPhoneNumber(context, phone)
                AppPreferences.setCustomDisplayName(context, name)
                AppPreferences.setCustomStarLevel(context, starLevel)
                syncPersistedPageState()
                Toast.makeText(context, "详单基本信息已更新", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showEditorDialog() {
        val context = context ?: return
        val currentRecord = readFixedFeeRecord(context)
        val nameInput = EditText(context).apply {
            hint = "请输入套餐名称"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentRecord.name)
            maxLines = 2
        }
        val feeInput = EditText(context).apply {
            hint = "请输入金额"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(currentRecord.fee)
            maxLines = 1
        }

        listOf(nameInput, feeInput).forEachIndexed { index, editText ->
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            editText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index > 0) {
                    topMargin = dpToPx(12)
                }
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0)
            addView(nameInput)
            addView(feeInput)
        }

        AlertDialog.Builder(context)
            .setTitle("设置套餐及固定费用详单数据")
            .setMessage("点击套餐内容即可再次打开。这里只修改套餐名称和费用。")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val packageName = nameInput.text?.toString().orEmpty().trim()
                val feeText = feeInput.text?.toString().orEmpty().trim()
                try {
                    val savedJson = buildFixedFeeListJson(
                        context = context,
                        packageName = packageName,
                        feeText = feeText
                    )
                    AppPreferences.setWebViewFixedFeeListJson(context, savedJson)
                    syncPackageListToPage()
                    Toast.makeText(context, "套餐详单数据已更新", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(context, "保存失败：${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    // Fixed-fee editor persistence
    private fun readFixedFeeRecord(context: Context): FixedFeeRecord {
        return try {
            val array = JSONArray(AppPreferences.getWebViewFixedFeeListJson(context))
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

    private fun buildFixedFeeListJson(
        context: Context,
        packageName: String,
        feeText: String
    ): String {
        val existingJson = AppPreferences.getWebViewFixedFeeListJson(context)
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

    private suspend fun buildCallDetailListJson(context: Context, year: Int, month: Int): String {
        val records = loadCallDetailRecords(context, year, month)
        val result = JSONArray()
        records.forEach { record ->
            result.put(record.toWebCallDetailJson(context))
        }
        return result.toString()
    }

    private suspend fun loadCallDetailRecords(context: Context, year: Int, month: Int): List<CallRecord> {
        val providerRecords = if (hasProviderPermission(context)) {
            queryCallRecords(context, year, month)
        } else {
            emptyList()
        }
        if (providerRecords.isNotEmpty()) {
            withContext(Dispatchers.Main){
                Toast.makeText(context,"读取到${providerRecords.size}条数据", Toast.LENGTH_SHORT).show()
            }
            return providerRecords
        }
        return filterCallRecordsByMonth(
            CallRecordCacheDatabase.getInstance(context).callRecordCacheDao()
            .getAll()
            .map { it.toCallRecord() },
            year,
            month
        )
    }

    private fun hasProviderPermission(context: Context): Boolean {
        return context.checkSelfPermission(READ_CALL_RECORDS_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun queryCallRecords(context: Context, year: Int, month: Int): List<CallRecord> {
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
            context.contentResolver.query(
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
                    if (record.isConnected) {
                        list.add(record)
                    }
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "queryCallRecords failed: ${error.message}")
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

    private fun CallRecord.toWebCallDetailJson(context: Context): JSONObject {
        val isIncoming = callType == 1
        val billSeconds = calculateBillSeconds(this)
        val billedMinutes = calculateBilledMinutes(billSeconds)
        val displayTimestamp = resolveDisplayTimestamp(this)
        val customRegion = AppPreferences.getCustomSelfRegion(context).trim()
        val outgoingPackage = AppPreferences.getOutgoingPackageInfo(context).trim()
        val customOutgoingType = AppPreferences.getCustomOutgoingCallType(context).trim()
        val customIncomingType = AppPreferences.getCustomIncomingCallType(context).trim()
        val title = if (isIncoming) {
            context.getString(R.string.record_voice_hd_incoming)
        } else {
            context.getString(R.string.record_voice_hd_outgoing)
        }
        val communicationType = if (isIncoming) {
            customIncomingType.ifBlank { context.getString(R.string.record_type_incoming_domestic) }
        } else {
            customOutgoingType.ifBlank { context.getString(R.string.record_type_outgoing_local) }
        }
        val packageName = outgoingPackage.ifBlank { "标准资费" }
        val location = customRegion.ifBlank {
            attribution ?: operator ?: context.getString(R.string.record_unknown_location)
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

    private data class FixedFeeRecord(
        val name: String,
        val fee: String
    )

    // Utilities
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
            return "0秒"
        }
        if (totalSeconds < 60) {
            return "${totalSeconds}秒"
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

    // Fragment lifecycle
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        _binding?.webViewPackageDetail?.onResume()
        syncPersistedPageState()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            (activity as? MainActivity)?.updateTopBarForPageScroll(0)
        }
        if (!hidden) {
            syncPersistedPageState()
        }
    }

    override fun onPause() {
        (activity as? MainActivity)?.updateTopBarForPageScroll(0)
        Log.d(TAG, "onPause")
        _binding?.webViewPackageDetail?.onPause()
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    // JS bridge
    inner class EditorBridge {
        @JavascriptInterface
        fun openEditor() {
            activity?.runOnUiThread { showEditorDialog() }
        }

        @JavascriptInterface
        fun openBasicInfoEditor() {
            activity?.runOnUiThread { showBasicInfoEditorDialog() }
        }

        @JavascriptInterface
        fun requestCallDetailMonth(yearText: String?, monthText: String?) {
            val year = yearText?.toIntOrNull() ?: return
            val month = monthText?.toIntOrNull() ?: return
            if (month !in 1..12) {
                return
            }
            activity?.runOnUiThread {
                requestCallDetailListForMonth(year, month)
            }
        }

        @JavascriptInterface
        fun onPageScroll(scrollYText: String?) {
            val scrollY = scrollYText?.toIntOrNull() ?: 0
            activity?.runOnUiThread {
                (activity as? MainActivity)?.updateTopBarForPageScroll(scrollY)
            }
        }
    }
}
