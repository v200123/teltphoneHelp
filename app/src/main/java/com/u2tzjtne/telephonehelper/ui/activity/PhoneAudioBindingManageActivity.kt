package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityPhoneAudioBindingManageBinding
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.adapter.PhoneAudioBindingAdapter
import com.u2tzjtne.telephonehelper.util.PhoneDialAudioBindingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneAudioBindingManageActivity : BaseActivity() {

    private val binding: ActivityPhoneAudioBindingManageBinding by lazy {
        ActivityPhoneAudioBindingManageBinding.inflate(layoutInflater)
    }

    private val adapter: PhoneAudioBindingAdapter by lazy {
        PhoneAudioBindingAdapter(onDeleteClick = ::deleteBinding)
    }

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
        binding.tvClearAll.setOnClickListener { showClearAllConfirm() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    PhoneDialAudioBindingHelper.getAllBindings()
                }
                adapter.submitList(list)
                updateEmptyView(list.isEmpty(), list.size)
            } catch (_: Exception) {
                Toast.makeText(this@PhoneAudioBindingManageActivity, getString(R.string.settings_clear_phone_binding_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean, count: Int) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPhoneList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvPhoneCount.text = if (isEmpty) {
            getString(R.string.settings_phone_binding_count_zero)
        } else {
            getString(R.string.settings_phone_binding_count, count)
        }
    }

    private fun deleteBinding(item: PhoneDialAudioBindingHelper.PhoneAudioBinding) {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_clear_phone_binding_title)
            .setMessage(getString(R.string.settings_phone_binding_delete_message, formatPhoneNumber(item.phoneNumber), item.mode.label))
            .setPositiveButton(R.string.settings_confirm) { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            clearBindingInternal(item.phoneNumber)
                        }
                        Toast.makeText(this@PhoneAudioBindingManageActivity, getString(R.string.settings_clear_phone_binding_success), Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (_: Exception) {
                        Toast.makeText(this@PhoneAudioBindingManageActivity, getString(R.string.settings_clear_phone_binding_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.settings_cancel, null)
            .show()
    }

    private fun showClearAllConfirm() {
        AlertDialog.Builder(this)
            .setTitle(R.string.phone_manage_clear_all)
            .setMessage(R.string.settings_phone_binding_clear_all_message)
            .setPositiveButton(R.string.settings_confirm) { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            PhoneDialAudioBindingHelper.clearAllBindings()
                            val db = RingVideoDatabase.getInstance()
                            db.phoneRingtoneAssignmentDao().deleteAll().blockingAwait()
                            db.ringtonePhoneBindingDao().clearAll()
                        }
                        Toast.makeText(this@PhoneAudioBindingManageActivity, getString(R.string.settings_phone_binding_clear_all_success), Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (_: Exception) {
                        Toast.makeText(this@PhoneAudioBindingManageActivity, getString(R.string.settings_clear_phone_binding_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.settings_cancel, null)
            .show()
    }

    private fun clearBindingInternal(phoneNumber: String) {
        PhoneDialAudioBindingHelper.clearBindings(phoneNumber)
        val db = RingVideoDatabase.getInstance()
        db.phoneRingtoneAssignmentDao().deleteByPhoneNumber(phoneNumber)
        db.ringtonePhoneBindingDao().deleteByPhoneNumber(phoneNumber)
    }

    private fun formatPhoneNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
            else -> digits
        }
    }
}
