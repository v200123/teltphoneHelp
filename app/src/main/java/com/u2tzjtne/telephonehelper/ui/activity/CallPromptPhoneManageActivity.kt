package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityNoRingtonePhoneManageBinding
import com.u2tzjtne.telephonehelper.db.CallPromptPhone
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.adapter.CallPromptPhoneAdapter
import com.u2tzjtne.telephonehelper.util.CallPromptSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallPromptPhoneManageActivity : BaseActivity() {

    private val binding: ActivityNoRingtonePhoneManageBinding by lazy {
        ActivityNoRingtonePhoneManageBinding.inflate(layoutInflater)
    }

    private val promptType: CallPromptSettings.PromptType by lazy {
        CallPromptSettings.PromptType.fromValue(
            intent.getIntExtra(
                EXTRA_PROMPT_TYPE,
                CallPromptSettings.PromptType.POWER_OFF.value
            )
        )
    }

    private val adapter: CallPromptPhoneAdapter by lazy {
        CallPromptPhoneAdapter(onDeleteClick = ::deletePhone)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        loadData()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvTitle.text = promptType.settingsTitle
        binding.btnAddPhone.text = "\u6dfb\u52a0\u53f7\u7801"
        binding.tvEmpty.text = "\u6682\u65e0\u8bb0\u5f55\n\u70b9\u51fb\u4e0a\u65b9\u6309\u94ae\u6dfb\u52a0"
        binding.rvPhoneList.layoutManager = LinearLayoutManager(this)
        binding.rvPhoneList.adapter = adapter

        binding.btnAddPhone.setOnClickListener {
            showAddPhoneDialog()
        }

        binding.tvClearAll.setOnClickListener {
            showClearAllConfirm()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    RingVideoDatabase.getInstance().callPromptPhoneDao().getAllByType(promptType.value)
                }
                adapter.submitList(list)
                updateEmptyView(list.isEmpty(), list.size)
            } catch (_: Exception) {
                Toast.makeText(this@CallPromptPhoneManageActivity, "\u52a0\u8f7d\u5931\u8d25", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean, count: Int = 0) {
        if (isEmpty) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            binding.rvPhoneList.visibility = android.view.View.GONE
            binding.tvPhoneCount.text = "\u5df2\u6dfb\u52a0 0 \u4e2a\u53f7\u7801"
        } else {
            binding.tvEmpty.visibility = android.view.View.GONE
            binding.rvPhoneList.visibility = android.view.View.VISIBLE
            binding.tvPhoneCount.text = "\u5df2\u6dfb\u52a0 $count \u4e2a\u53f7\u7801"
        }
    }

    private fun showAddPhoneDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_phone, null)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhoneNumber)

        AlertDialog.Builder(this)
            .setTitle(promptType.addDialogTitle)
            .setMessage(promptType.addDialogMessage)
            .setView(dialogView)
            .setPositiveButton("\u6dfb\u52a0") { _, _ ->
                val phoneNumber = etPhone.text.toString().trim()
                if (isValidPhoneNumber(phoneNumber)) {
                    addPhone(phoneNumber)
                } else {
                    Toast.makeText(this, "\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u7535\u8bdd\u53f7\u7801", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("\u53d6\u6d88", null)
            .show()
    }

    private fun addPhone(phoneNumber: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val normalizedPhone = phoneNumber.replace(Regex("[^0-9]"), "")
                    val item = CallPromptPhone(normalizedPhone, promptType.value)
                    RingVideoDatabase.getInstance().callPromptPhoneDao().insert(item)
                }
                Toast.makeText(this@CallPromptPhoneManageActivity, "\u6dfb\u52a0\u6210\u529f", Toast.LENGTH_SHORT).show()
                loadData()
            } catch (e: Exception) {
                Toast.makeText(this@CallPromptPhoneManageActivity, "\u6dfb\u52a0\u5931\u8d25: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePhone(phone: CallPromptPhone) {
        AlertDialog.Builder(this)
            .setTitle("\u786e\u8ba4\u5220\u9664")
            .setMessage("\u786e\u5b9a\u8981\u5220\u9664\u53f7\u7801 ${formatPhoneNumber(phone.phoneNumber)} \u5417\uff1f")
            .setPositiveButton("\u786e\u5b9a") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RingVideoDatabase.getInstance().callPromptPhoneDao().deleteById(phone.id)
                        }
                        Toast.makeText(this@CallPromptPhoneManageActivity, "\u5220\u9664\u6210\u529f", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (_: Exception) {
                        Toast.makeText(this@CallPromptPhoneManageActivity, "\u5220\u9664\u5931\u8d25", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("\u53d6\u6d88", null)
            .show()
    }

    private fun showClearAllConfirm() {
        AlertDialog.Builder(this)
            .setTitle("\u786e\u8ba4\u6e05\u7a7a")
            .setMessage("\u786e\u5b9a\u8981\u6e05\u7a7a ${promptType.settingsTitle} \u5417\uff1f\u6b64\u64cd\u4f5c\u4e0d\u53ef\u6062\u590d\u3002")
            .setPositiveButton("\u786e\u5b9a") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RingVideoDatabase.getInstance().callPromptPhoneDao().deleteAllByType(promptType.value)
                        }
                        Toast.makeText(this@CallPromptPhoneManageActivity, "\u5df2\u6e05\u7a7a\u6240\u6709\u53f7\u7801", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (_: Exception) {
                        Toast.makeText(this@CallPromptPhoneManageActivity, "\u6e05\u7a7a\u5931\u8d25", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("\u53d6\u6d88", null)
            .show()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.replace(Regex("[^0-9]"), "").matches(Regex("^\\d{3,20}$"))
    }

    private fun formatPhoneNumber(phone: String?): String {
        if (phone.isNullOrEmpty()) return ""
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
            else -> digits
        }
    }

    companion object {
        private const val EXTRA_PROMPT_TYPE = "extra_prompt_type"

        fun start(context: Context, promptType: CallPromptSettings.PromptType) {
            context.startActivity(
                Intent(context, CallPromptPhoneManageActivity::class.java).apply {
                    putExtra(EXTRA_PROMPT_TYPE, promptType.value)
                }
            )
        }
    }
}
