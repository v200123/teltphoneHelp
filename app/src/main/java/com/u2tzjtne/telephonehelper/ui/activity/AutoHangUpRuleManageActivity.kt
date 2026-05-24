package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityAutoHangUpRuleManageBinding
import com.u2tzjtne.telephonehelper.db.AutoHangUpRule
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.adapter.AutoHangUpRuleAdapter
import com.u2tzjtne.telephonehelper.util.AutoHangUpSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoHangUpRuleManageActivity : BaseActivity() {

    private val binding: ActivityAutoHangUpRuleManageBinding by lazy {
        ActivityAutoHangUpRuleManageBinding.inflate(layoutInflater)
    }

    private val adapter: AutoHangUpRuleAdapter by lazy {
        AutoHangUpRuleAdapter(
            onEditClick = ::showEditRuleDialog,
            onDeleteClick = ::deleteRule
        )
    }

    private var allRules: List<AutoHangUpRule> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        loadData()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.rvPhoneList.layoutManager = LinearLayoutManager(this)
        binding.rvPhoneList.adapter = adapter
        binding.tvAddRule.setOnClickListener { showAddRuleDialog() }
        binding.tvClearAll.setOnClickListener { showClearAllConfirm() }
        binding.etSearchPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString().orEmpty())
            }
        })
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    RingVideoDatabase.getInstance().autoHangUpRuleDao().getAll()
                }
                allRules = list
                applyFilter(binding.etSearchPhone.text?.toString().orEmpty())
            } catch (_: Exception) {
                Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilter(keyword: String) {
        val normalizedKeyword = keyword.replace(Regex("[^0-9]"), "")
        val filteredList = if (normalizedKeyword.isEmpty()) {
            allRules
        } else {
            allRules.filter { item ->
                item.phoneNumber.replace(Regex("[^0-9]"), "").contains(normalizedKeyword)
            }
        }
        adapter.submitList(filteredList)
        updateEmptyView(filteredList.isEmpty(), filteredList.size)
    }

    private fun updateEmptyView(isEmpty: Boolean, count: Int) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPhoneList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvPhoneCount.text = if (isEmpty) {
            getString(R.string.settings_auto_hang_up_count_zero)
        } else {
            getString(R.string.settings_auto_hang_up_count, count)
        }
    }

    private fun showAddRuleDialog() {
        showRuleDialog()
    }

    private fun showEditRuleDialog(rule: AutoHangUpRule) {
        showRuleDialog(rule)
    }

    private fun showRuleDialog(existingRule: AutoHangUpRule? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_auto_hang_up_rule, null)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhoneNumber)
        val etSeconds = dialogView.findViewById<EditText>(R.id.etDelaySeconds)
        existingRule?.let {
            etPhone.setText(formatPhoneNumber(it.phoneNumber))
            etSeconds.setText(it.hangUpDelaySeconds.toString())
            etPhone.setSelection(etPhone.text?.length ?: 0)
            etSeconds.setSelection(etSeconds.text?.length ?: 0)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existingRule == null) R.string.settings_auto_hang_up_add_title else R.string.settings_auto_hang_up_edit_title)
            .setMessage(if (existingRule == null) R.string.settings_auto_hang_up_add_message else R.string.settings_auto_hang_up_edit_message)
            .setView(dialogView)
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_confirm, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val phoneNumber = etPhone.text?.toString().orEmpty().trim()
                val secondsText = etSeconds.text?.toString().orEmpty().trim()
                val normalizedPhone = phoneNumber.replace(Regex("[^0-9]"), "")
                val delaySeconds = secondsText.toIntOrNull()

                if (!normalizedPhone.matches(Regex("^1[3-9]\\d{9}$"))) {
                    Toast.makeText(this, getString(R.string.settings_clear_phone_binding_invalid), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (delaySeconds == null || delaySeconds !in AutoHangUpSettings.MIN_DELAY_SECONDS..AutoHangUpSettings.MAX_DELAY_SECONDS) {
                    Toast.makeText(this, getString(R.string.settings_auto_hang_up_invalid_seconds), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (existingRule == null) {
                    addRule(normalizedPhone, delaySeconds)
                } else {
                    updateRule(existingRule, normalizedPhone, delaySeconds)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun addRule(phoneNumber: String, delaySeconds: Int) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RingVideoDatabase.getInstance()
                        .autoHangUpRuleDao()
                        .insert(AutoHangUpRule(phoneNumber, delaySeconds))
                }
                Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_add_success), Toast.LENGTH_SHORT).show()
                loadData()
            } catch (e: Exception) {
                Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_add_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRule(oldRule: AutoHangUpRule, phoneNumber: String, delaySeconds: Int) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val dao = RingVideoDatabase.getInstance().autoHangUpRuleDao()
                    dao.deleteById(oldRule.id)
                    dao.insert(AutoHangUpRule(phoneNumber, delaySeconds))
                }
                Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_edit_success), Toast.LENGTH_SHORT).show()
                loadData()
            } catch (e: Exception) {
                Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_add_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteRule(rule: AutoHangUpRule) {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_clear_phone_binding_title)
            .setMessage(getString(R.string.settings_auto_hang_up_delete_message, formatPhoneNumber(rule.phoneNumber), rule.hangUpDelaySeconds))
            .setPositiveButton(R.string.settings_confirm) { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RingVideoDatabase.getInstance().autoHangUpRuleDao().deleteById(rule.id)
                        }
                        Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_delete_success), Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (_: Exception) {
                        Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.settings_cancel, null)
            .show()
    }

    private fun showClearAllConfirm() {
        AlertDialog.Builder(this)
            .setTitle(R.string.phone_manage_clear_all)
            .setMessage(R.string.settings_auto_hang_up_clear_all_message)
            .setPositiveButton(R.string.settings_confirm) { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RingVideoDatabase.getInstance().autoHangUpRuleDao().deleteAll()
                        }
                        Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_clear_all_success), Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (_: Exception) {
                        Toast.makeText(this@AutoHangUpRuleManageActivity, getString(R.string.settings_auto_hang_up_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.settings_cancel, null)
            .show()
    }

    private fun formatPhoneNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
            else -> digits
        }
    }
}
