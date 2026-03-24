package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityNoRingtonePhoneManageBinding
import com.u2tzjtne.telephonehelper.db.NoRingtonePhone
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.adapter.NoRingtonePhoneAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * 不显示彩铃号码管理界面
 * 
 * 功能：
 * 1. 添加不显示彩铃的号码
 * 2. 查看已添加的号码列表
 * 3. 删除号码
 * 4. 清空所有号码
 */
class NoRingtonePhoneManageActivity : BaseActivity() {

    private val binding: ActivityNoRingtonePhoneManageBinding by lazy {
        ActivityNoRingtonePhoneManageBinding.inflate(layoutInflater)
    }

    private val adapter: NoRingtonePhoneAdapter by lazy {
        NoRingtonePhoneAdapter(onDeleteClick = ::deletePhone)
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

        // 添加号码按钮
        binding.btnAddPhone.setOnClickListener {
            showAddPhoneDialog()
        }

        // 清空全部
        binding.tvClearAll.setOnClickListener {
            showClearAllConfirm()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    RingVideoDatabase.getInstance().noRingtonePhoneDao().getAll()
                }
                adapter.submitList(list)
                updateEmptyView(list.isEmpty(), list.size)
            } catch (e: Exception) {
                Toast.makeText(this@NoRingtonePhoneManageActivity, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean, count: Int = 0) {
        if (isEmpty) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            binding.rvPhoneList.visibility = android.view.View.GONE
            binding.tvPhoneCount.text = "已添加 0 个号码"
        } else {
            binding.tvEmpty.visibility = android.view.View.GONE
            binding.rvPhoneList.visibility = android.view.View.VISIBLE
            binding.tvPhoneCount.text = "已添加 $count 个号码"
        }
    }

    private fun showAddPhoneDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_phone, null)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhoneNumber)

        AlertDialog.Builder(this)
            .setTitle("添加不显示彩铃的号码")
            .setMessage("该号码拨打电话时将不会播放彩铃视频")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val phoneNumber = etPhone.text.toString().trim()
                if (isValidPhoneNumber(phoneNumber)) {
                    addPhone(phoneNumber)
                } else {
                    Toast.makeText(this, "请输入正确的手机号码", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addPhone(phoneNumber: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val normalizedPhone = phoneNumber.replace(Regex("[^0-9]"), "")
                    val noRingtonePhone = NoRingtonePhone(normalizedPhone)
                    RingVideoDatabase.getInstance().noRingtonePhoneDao().insert(noRingtonePhone)
                }
                Toast.makeText(this@NoRingtonePhoneManageActivity, "添加成功", Toast.LENGTH_SHORT).show()
                loadData()
            } catch (e: Exception) {
                Toast.makeText(this@NoRingtonePhoneManageActivity, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePhone(phone: NoRingtonePhone) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除号码 ${formatPhoneNumber(phone.phoneNumber)} 吗？")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RingVideoDatabase.getInstance().noRingtonePhoneDao().deleteById(phone.id)
                        }
                        Toast.makeText(this@NoRingtonePhoneManageActivity, "删除成功", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(this@NoRingtonePhoneManageActivity, "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearAllConfirm() {
        AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空所有不显示彩铃的号码吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RingVideoDatabase.getInstance().noRingtonePhoneDao().deleteAll()
                        }
                        Toast.makeText(this@NoRingtonePhoneManageActivity, "已清空所有号码", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(this@NoRingtonePhoneManageActivity, "清空失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.replace(Regex("[^0-9]"), "").matches(Regex("^1[3-9]\\d{9}$"))
    }

    private fun formatPhoneNumber(phone: String?): String {
        if (phone.isNullOrEmpty()) return ""
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
            else -> digits
        }
    }
}
