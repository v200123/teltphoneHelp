package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityCustomLocationManageBinding
import com.u2tzjtne.telephonehelper.db.AppDatabase
import com.u2tzjtne.telephonehelper.db.CustomPhoneLocation
import com.u2tzjtne.telephonehelper.ui.adapter.CustomLocationManageAdapter
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import androidx.lifecycle.lifecycleScope
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * 自定义归属地管理界面
 * 
 * 功能：
 * 1. 添加自定义归属地
 * 2. 查看已保存的归属地列表
 * 3. 编辑归属地
 * 4. 删除归属地
 * 5. 清空所有归属地
 */
class CustomLocationManageActivity : BaseActivity() {

    private val binding: ActivityCustomLocationManageBinding by lazy {
        ActivityCustomLocationManageBinding.inflate(layoutInflater)
    }

    private val adapter: CustomLocationManageAdapter by lazy {
        CustomLocationManageAdapter(
            onEditClick = ::showEditDialog,
            onDeleteClick = ::deleteLocation
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        loadData()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }

        binding.rvLocationList.layoutManager = LinearLayoutManager(this)
        binding.rvLocationList.adapter = adapter

        // 添加归属地按钮
        binding.btnAddLocation.setOnClickListener {
            showAddLocationDialog()
        }

        // 清空全部
        binding.tvClearAll.setOnClickListener {
            showClearAllConfirm()
        }
    }

    private fun loadData() {
        AppDatabase.getInstance().customPhoneLocationModel().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                adapter.submitList(list)
                updateEmptyView(list.isEmpty(), list.size)
            }, {
                Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun updateEmptyView(isEmpty: Boolean, count: Int = 0) {
        if (isEmpty) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            binding.rvLocationList.visibility = android.view.View.GONE
            binding.tvLocationCount.text = "已添加 0 个归属地"
        } else {
            binding.tvEmpty.visibility = android.view.View.GONE
            binding.rvLocationList.visibility = android.view.View.VISIBLE
            binding.tvLocationCount.text = "已添加 $count 个归属地"
        }
    }

    private fun showAddLocationDialog() {
        showLocationDialog(null)
    }

    private fun showEditDialog(location: CustomPhoneLocation) {
        showLocationDialog(location)
    }

    private fun showLocationDialog(location: CustomPhoneLocation?) {
        val isEdit = location != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_location, null)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhoneNumber)
        val etProvince = dialogView.findViewById<EditText>(R.id.etProvince)
        val etCity = dialogView.findViewById<EditText>(R.id.etCity)
        val etCarrier = dialogView.findViewById<EditText>(R.id.etCarrier)

        if (isEdit) {
            etPhone.setText(location!!.phone)
            etProvince.setText(location.province)
            etCity.setText(location.city)
            etCarrier.setText(location.carrier)
            etPhone.isEnabled = false // 编辑时不能修改号码
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "编辑归属地" else "添加自定义归属地")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val phoneNumber = etPhone.text.toString().trim()
                val province = etProvince.text.toString().trim()
                val city = etCity.text.toString().trim()
                val carrier = etCarrier.text.toString().trim()

                if (phoneNumber.isEmpty()) {
                    Toast.makeText(this, "请输入手机号码", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (province.isEmpty() && city.isEmpty() && carrier.isEmpty()) {
                    Toast.makeText(this, "请至少填写一项归属地信息", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val normalizedPhone = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
                val customLocation = CustomPhoneLocation(
                    normalizedPhone,
                    province,
                    city,
                    carrier
                )
                
                if (isEdit) {
                    customLocation.id = location!!.id
                    customLocation.createTime = location.createTime
                }
                
                saveLocation(customLocation)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveLocation(location: CustomPhoneLocation) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance().customPhoneLocationModel().insert(location)
                }
                Toast.makeText(this@CustomLocationManageActivity, "保存成功", Toast.LENGTH_SHORT).show()
                loadData()
            } catch (e: Exception) {
                Toast.makeText(this@CustomLocationManageActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteLocation(location: CustomPhoneLocation) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除号码 ${formatPhoneNumber(location.phone)} 的自定义归属地吗？")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            AppDatabase.getInstance().customPhoneLocationModel().deleteById(location.id)
                        }
                        Toast.makeText(this@CustomLocationManageActivity, "删除成功", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(this@CustomLocationManageActivity, "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearAllConfirm() {
        AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空所有自定义归属地吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            AppDatabase.getInstance().customPhoneLocationModel().deleteAll()
                        }
                        Toast.makeText(this@CustomLocationManageActivity, "已清空所有归属地", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(this@CustomLocationManageActivity, "清空失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
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
