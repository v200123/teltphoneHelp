package com.example.myservicecenter.ui.sms

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myservicecenter.PhoneDisplayManager
import com.example.myservicecenter.R
import com.example.myservicecenter.data.sms.SmsDetailDatabase
import com.example.myservicecenter.data.sms.SmsDetailRecordEntity

import com.example.myservicecenter.databinding.ActivitySmsDetailManageBinding
import com.example.myservicecenter.databinding.ItemSmsDetailRecordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SmsDetailManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySmsDetailManageBinding
    private val smsDetailDao by lazy {
        SmsDetailDatabase.getInstance(applicationContext).smsDetailDao()
    }
    private val adapter = SmsDetailAdapter(
        onEdit = { record -> showSingleEditorDialog(record) },
        onDelete = { record -> confirmDelete(record) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true

        binding = ActivitySmsDetailManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        applyWindowInsets()
        reloadRecords()
    }

    private fun initViews() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showSingleEditorDialog(null) }
        binding.btnBatchAdd.setOnClickListener { showBatchEditorDialog() }
        binding.btnClearAll.setOnClickListener { confirmClearAll() }
        binding.etSearchPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                reloadRecords()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun reloadRecords() {
        val query = binding.etSearchPhone.text?.toString().orEmpty().trim()
        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                if (query.isBlank()) {
                    smsDetailDao.getAll()
                } else {
                    smsDetailDao.searchByPhone(query.filter(Char::isDigit))
                }
            }
            adapter.submit(records)
            binding.tvCount.text = "共${records.size}条"
        }
    }

    private fun showSingleEditorDialog(record: SmsDetailRecordEntity?) {
        val directionInput = dialogInput("收发方式：接收/发送", record?.direction ?: DEFAULT_DIRECTION)
        val phoneInput = dialogInput("手机号/服务号码", record?.phoneNumber.orEmpty(), InputType.TYPE_CLASS_PHONE)
        val typeInput = dialogInput("类型：短信/彩信/5G消息", record?.messageType ?: DEFAULT_MESSAGE_TYPE)
        val locationInput = dialogInput("地域类型", record?.location ?: DEFAULT_LOCATION)
        val timeInput = dialogInput("发送时间：MM-dd HH:mm:ss", record?.timestampMillis?.let(::formatTime) ?: formatTime(System.currentTimeMillis()))
        val packageInput = dialogInput("套餐名称", record?.packageName ?: DEFAULT_PACKAGE)
        val feeInput = dialogInput("费用", record?.fee ?: DEFAULT_FEE, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val inputs = listOf(directionInput, phoneInput, typeInput, locationInput, timeInput, packageInput, feeInput)

        AlertDialog.Builder(this)
            .setTitle(if (record == null) "新增短/彩信详单" else "编辑短/彩信详单")
            .setView(buildDialogContainer(inputs))
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val phoneNumber = cleanPhone(phoneInput.text?.toString().orEmpty())
                val timestamp = parseTime(timeInput.text?.toString().orEmpty())
                if (phoneNumber.isBlank()) {
                    Toast.makeText(this, "手机号不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (timestamp <= 0L) {
                    Toast.makeText(this, "发送时间格式应为 MM-dd HH:mm:ss", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val saved = SmsDetailRecordEntity(
                    id = record?.id ?: 0,
                    direction = directionInput.text?.toString().orEmpty().trim()
                        .ifBlank { DEFAULT_DIRECTION },
                    phoneNumber = phoneNumber,
                    messageType = typeInput.text?.toString().orEmpty().trim()
                        .ifBlank { DEFAULT_MESSAGE_TYPE },
                    location = locationInput.text?.toString().orEmpty().trim()
                        .ifBlank { DEFAULT_LOCATION },
                    timestampMillis = timestamp,
                    packageName = packageInput.text?.toString().orEmpty().trim()
                        .ifBlank { DEFAULT_PACKAGE },
                    fee = normalizeFee(feeInput.text?.toString().orEmpty()),
                    createdAtMillis = record?.createdAtMillis ?: System.currentTimeMillis()
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    if (record == null) {
                        smsDetailDao.insert(saved)
                    } else {
                        smsDetailDao.update(saved)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SmsDetailManageActivity, "已保存", Toast.LENGTH_SHORT).show()
                        reloadRecords()
                    }
                }
            }
            .show()
    }

    private fun showBatchEditorDialog() {
        val phonesInput = dialogInput("一行一个，也可用逗号/空格分隔", "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE).apply {
            minLines = 5
            maxLines = 8
            setSingleLine(false)
        }
        val directionInput = dialogInput("接收/发送", DEFAULT_DIRECTION)
        val typeInput = dialogInput("短信/彩信/5G消息", DEFAULT_MESSAGE_TYPE)
        val locationInput = dialogInput("例如：内地", DEFAULT_LOCATION)
        val timeInput = dialogInput("MM-dd HH:mm:ss", formatTime(System.currentTimeMillis()))
        val packageInput = dialogInput("例如：标准资费", DEFAULT_PACKAGE)
        val feeInput = dialogInput("例如：0.00", DEFAULT_FEE, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val fields = listOf(
            DialogField("批量手机号", phonesInput),
            DialogField("收发方式", directionInput),
            DialogField("短/彩信类型", typeInput),
            DialogField("地域类型", locationInput),
            DialogField("起始发送时间", timeInput),
            DialogField("套餐名称", packageInput),
            DialogField("费用", feeInput)
        )

        AlertDialog.Builder(this)
            .setTitle("批量上传手机号")
            .setMessage("其他信息统一使用同一组配置；第一条使用起始时间，后续每条随机递增1或2秒。")
            .setView(buildLabeledDialogContainer(fields))
            .setNegativeButton("取消", null)
            .setPositiveButton("生成") { _, _ ->
                val phoneNumbers = parsePhoneList(phonesInput.text?.toString().orEmpty())
                val startTimestamp = parseTime(timeInput.text?.toString().orEmpty())
                if (phoneNumbers.isEmpty()) {
                    Toast.makeText(this, "请至少输入一个手机号", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (startTimestamp <= 0L) {
                    Toast.makeText(this, "起始发送时间格式应为 MM-dd HH:mm:ss", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val now = System.currentTimeMillis()
                var cursorTime = startTimestamp
                val records = phoneNumbers.mapIndexed { index, phone ->
                    if (index > 0) {
                        cursorTime += Random.nextLong(1L, 3L) * 1000L
                    }
                    SmsDetailRecordEntity(
                        direction = directionInput.text?.toString().orEmpty().trim()
                            .ifBlank { DEFAULT_DIRECTION },
                        phoneNumber = phone,
                        messageType = typeInput.text?.toString().orEmpty().trim()
                            .ifBlank { DEFAULT_MESSAGE_TYPE },
                        location = locationInput.text?.toString().orEmpty().trim()
                            .ifBlank { DEFAULT_LOCATION },
                        timestampMillis = cursorTime,
                        packageName = packageInput.text?.toString().orEmpty().trim()
                            .ifBlank { DEFAULT_PACKAGE },
                        fee = normalizeFee(feeInput.text?.toString().orEmpty()),
                        createdAtMillis = now
                    )
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    smsDetailDao.insertAll(records)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SmsDetailManageActivity, "已生成${records.size}条", Toast.LENGTH_SHORT).show()
                        reloadRecords()
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(record: SmsDetailRecordEntity) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定删除 ${PhoneDisplayManager.display(this, record.phoneNumber)} 的这条短/彩信详单吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    smsDetailDao.deleteById(record.id)
                    withContext(Dispatchers.Main) {
                        reloadRecords()
                    }
                }
            }
            .show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle("清空全部")
            .setMessage("确定删除所有短/彩信详单数据吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("清空") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    smsDetailDao.clearAll()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SmsDetailManageActivity, "已清空", Toast.LENGTH_SHORT).show()
                        reloadRecords()
                    }
                }
            }
            .show()
    }

    private fun dialogInput(hintText: String, value: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = type
            setText(value)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(Color.parseColor("#333333"))
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            background = getDrawable(R.drawable.bg_search_input)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            )
        }
    }

    private fun buildDialogContainer(inputs: List<EditText>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0)
            inputs.forEachIndexed { index, input ->
                addView(input)
                if (index < inputs.lastIndex) {
                    (input.layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = dpToPx(10)
                }
            }
        }
    }

    private fun buildLabeledDialogContainer(fields: List<DialogField>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0)
            fields.forEachIndexed { index, field ->
                val label = TextView(this@SmsDetailManageActivity).apply {
                    text = field.label
                    setTextColor(Color.parseColor("#333333"))
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (index > 0) {
                            topMargin = dpToPx(10)
                        }
                        bottomMargin = dpToPx(6)
                    }
                }
                addView(label)
                addView(field.input)
            }
        }
    }

    private fun parsePhoneList(value: String): List<String> {
        return value
            .split(Regex("[,，;；\\s]+"))
            .map(::cleanPhone)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun cleanPhone(value: String): String {
        return value.filter(Char::isDigit)
    }

    private fun normalizeFee(value: String): String {
        return String.format(Locale.getDefault(), "%.2f", value.trim().toDoubleOrNull() ?: 0.0)
    }

    private fun parseTime(value: String): Long {
        val text = value.trim()
        if (text.isBlank()) {
            return 0L
        }
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        return try {
            TIME_FORMAT.parse("$year-$text")?.time ?: 0L
        } catch (_: ParseException) {
            0L
        }
    }

    private fun formatTime(timestampMillis: Long): String {
        return if (timestampMillis > 0L) {
            DISPLAY_TIME_FORMAT.format(Date(timestampMillis))
        } else {
            ""
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

    private fun dpToPx(valueDp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            valueDp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private class SmsDetailAdapter(
        private val onEdit: (SmsDetailRecordEntity) -> Unit,
        private val onDelete: (SmsDetailRecordEntity) -> Unit
    ) : RecyclerView.Adapter<SmsDetailAdapter.ViewHolder>() {

        private val records = mutableListOf<SmsDetailRecordEntity>()

        fun submit(data: List<SmsDetailRecordEntity>) {
            records.clear()
            records.addAll(data)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSmsDetailRecordBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(records[position], onEdit, onDelete)
        }

        override fun getItemCount(): Int = records.size

        class ViewHolder(
            private val binding: ItemSmsDetailRecordBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(
                record: SmsDetailRecordEntity,
                onEdit: (SmsDetailRecordEntity) -> Unit,
                onDelete: (SmsDetailRecordEntity) -> Unit
            ) {
                binding.tvTitle.text = "${record.direction} ${PhoneDisplayManager.display(itemView.context, record.phoneNumber)}"
                binding.tvType.text = record.messageType
                binding.tvTime.text = "${record.location}    ${DISPLAY_TIME_FORMAT.format(Date(record.timestampMillis))}"
                binding.tvPackage.text = "套餐名称：${record.packageName}"
                binding.tvFee.text = "费用：¥${record.fee}"
                binding.btnEdit.setOnClickListener { onEdit(record) }
                binding.btnDelete.setOnClickListener { onDelete(record) }
            }
        }
    }

    companion object {
        private const val DEFAULT_DIRECTION = "接收"
        private const val DEFAULT_MESSAGE_TYPE = "短信"
        private const val DEFAULT_LOCATION = "内地"
        private const val DEFAULT_PACKAGE = "标准资费"
        private const val DEFAULT_FEE = "0.00"
        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        private val DISPLAY_TIME_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    }

    private data class DialogField(
        val label: String,
        val input: EditText
    )
}
