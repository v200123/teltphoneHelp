package com.example.myservicecenter.ui.calllog

import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.myservicecenter.R
import com.example.myservicecenter.data.calllog.CallRecordContract
import com.example.myservicecenter.databinding.ActivityProviderCallRecordsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProviderCallRecordsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProviderCallRecordsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true
        binding = ActivityProviderCallRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadRecords() }
        loadRecords()
    }

    private fun loadRecords() {
        binding.tvStatus.text = "正在读取 ${CallRecordContract.CallRecord.CONTENT_URI}…"
        binding.recordsContainer.removeAllViews()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { queryProviderRows() }
            binding.tvStatus.text = result.status
            result.rows.forEachIndexed { index, row -> addRecordView(index + 1, row) }
        }
    }

    private fun queryProviderRows(): ProviderResult {
        return try {
            val rows = mutableListOf<String>()
            val cursor = contentResolver.query(
                CallRecordContract.CallRecord.CONTENT_URI,
                null,
                null,
                null,
                CallRecordContract.CallRecord.DEFAULT_SORT_ORDER
            ) ?: return ProviderResult(emptyList(), "Provider 未返回游标，请确认电话助手已安装并启用。")
            cursor.use {
                while (it.moveToNext()) rows += cursorToText(it)
            }
            ProviderResult(rows, "读取成功：${rows.size} 条记录（显示 Provider 原始字段）。")
        } catch (error: SecurityException) {
            ProviderResult(emptyList(), "没有读取权限：${error.message.orEmpty()}")
        } catch (error: Exception) {
            ProviderResult(emptyList(), "读取失败：${error.javaClass.simpleName}：${error.message.orEmpty()}")
        }
    }

    private fun cursorToText(cursor: Cursor): String = buildString {
        for (index in 0 until cursor.columnCount) {
            append(cursor.getColumnName(index)).append(" = ")
            append(
                when (cursor.getType(index)) {
                    Cursor.FIELD_TYPE_NULL -> "null"
                    Cursor.FIELD_TYPE_BLOB -> "<二进制数据 ${cursor.getBlob(index)?.size ?: 0} 字节>"
                    else -> cursor.getString(index).orEmpty()
                }
            )
            if (index < cursor.columnCount - 1) append('\n')
        }
    }

    private fun addRecordView(position: Int, content: String) {
        binding.recordsContainer.addView(TextView(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            background = getDrawable(R.drawable.bg_search_input)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextColor(getColor(R.color.text_primary))
            textSize = 13f
            text = "记录 #$position\n$content"
        })
    }

    private fun applyWindowInsets() {
        val padding = binding.topBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            binding.topBar.updatePadding(top = padding + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private data class ProviderResult(val rows: List<String>, val status: String)
}
