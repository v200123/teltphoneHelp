package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityPhoneAudioBindingManageBinding
import com.u2tzjtne.telephonehelper.ui.adapter.BadgeRuleBindingAdapter
import com.u2tzjtne.telephonehelper.util.PhoneBadgeRuleBinding
import com.u2tzjtne.telephonehelper.util.RingtoneBadgeRuleStore

class BadgeRuleBindingManageActivity : BaseActivity() {

    private val binding: ActivityPhoneAudioBindingManageBinding by lazy {
        ActivityPhoneAudioBindingManageBinding.inflate(layoutInflater)
    }

    private val adapter: BadgeRuleBindingAdapter by lazy {
        BadgeRuleBindingAdapter(onDeleteClick = ::deleteBinding)
    }
    private var allBindings: List<PhoneBadgeRuleBinding> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun initView() {
        binding.tvTitle.text = getString(R.string.settings_badge_rule_binding)
        binding.tvDesc.text = getString(R.string.settings_badge_rule_binding_desc)
        binding.etSearchPhone.hint = getString(R.string.settings_phone_binding_search_hint)
        binding.tvEmpty.text = getString(R.string.settings_badge_rule_binding_empty)
        binding.ivBack.setOnClickListener { finish() }
        binding.rvPhoneList.layoutManager = LinearLayoutManager(this)
        binding.rvPhoneList.adapter = adapter
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
        allBindings = RingtoneBadgeRuleStore.getAllPhoneRuleBindings(this)
        applyFilter(binding.etSearchPhone.text?.toString().orEmpty())
    }

    private fun applyFilter(keyword: String) {
        val normalizedKeyword = keyword.replace(Regex("[^0-9]"), "")
        val trimmedKeyword = keyword.trim()
        val filteredList = if (normalizedKeyword.isEmpty() && trimmedKeyword.isEmpty()) {
            allBindings
        } else {
            allBindings.filter { item ->
                val phoneMatched = normalizedKeyword.isNotEmpty() &&
                    item.phoneNumber.replace(Regex("[^0-9]"), "").contains(normalizedKeyword)
                val ruleMatched = trimmedKeyword.isNotEmpty() && item.ruleName.contains(trimmedKeyword, ignoreCase = true)
                phoneMatched || ruleMatched
            }
        }
        adapter.submitList(filteredList)
        updateEmptyView(filteredList.isEmpty(), allBindings.size, filteredList.size, trimmedKeyword.isNotEmpty())
    }

    private fun updateEmptyView(isEmpty: Boolean, totalCount: Int, matchCount: Int, hasKeyword: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPhoneList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvPhoneCount.text = if (totalCount == 0) {
            getString(R.string.settings_badge_rule_binding_count_zero)
        } else if (hasKeyword) {
            getString(R.string.settings_badge_rule_binding_count_with_match, totalCount, matchCount)
        } else {
            getString(R.string.settings_badge_rule_binding_count, totalCount)
        }
        if (isEmpty) {
            binding.tvEmpty.text = if (hasKeyword) {
                getString(R.string.settings_phone_binding_search_empty)
            } else {
                getString(R.string.settings_badge_rule_binding_empty)
            }
        }
    }

    private fun deleteBinding(item: PhoneBadgeRuleBinding) {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_badge_rule_binding)
            .setMessage(getString(R.string.settings_badge_rule_binding_delete_message, formatPhoneNumber(item.phoneNumber)))
            .setPositiveButton(R.string.settings_confirm) { _, _ ->
                RingtoneBadgeRuleStore.clearPhoneRuleBinding(this, item.phoneNumber)
                Toast.makeText(this, getString(R.string.settings_badge_rule_binding_clear_success), Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton(R.string.settings_cancel, null)
            .show()
    }

    private fun showClearAllConfirm() {
        AlertDialog.Builder(this)
            .setTitle(R.string.phone_manage_clear_all)
            .setMessage(R.string.settings_badge_rule_binding_clear_all_message)
            .setPositiveButton(R.string.settings_confirm) { _, _ ->
                RingtoneBadgeRuleStore.clearAllPhoneRuleBindings(this)
                Toast.makeText(this, getString(R.string.settings_badge_rule_binding_clear_all_success), Toast.LENGTH_SHORT).show()
                loadData()
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
