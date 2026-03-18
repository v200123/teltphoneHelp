package com.u2tzjtne.telephonehelper.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityCustomLocationListBinding
import com.u2tzjtne.telephonehelper.db.AppDatabase
import com.u2tzjtne.telephonehelper.db.CustomPhoneLocation
import com.u2tzjtne.telephonehelper.ui.adapter.CustomLocationAdapter
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * 自定义归属地列表管理界面
 */
class CustomLocationListActivity : BaseActivity() {
    
    private val binding: ActivityCustomLocationListBinding by lazy { 
        ActivityCustomLocationListBinding.inflate(layoutInflater) 
    }
    
    private val adapter = CustomLocationAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupListeners()
        loadData()
    }
    
    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        // 长按删除
        adapter.setOnItemLongClickListener { location ->
            showDeleteDialog(location)
        }
    }
    
    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // 清空所有
        binding.btnClearAll.setOnClickListener {
            showClearAllDialog()
        }
    }
    
    private fun loadData() {
        AppDatabase.getInstance().customPhoneLocationModel()
            .getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { list ->
                    adapter.setData(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                },
                { error ->
                    Toast.makeText(this, "加载失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
    }
    
    private fun showDeleteDialog(location: CustomPhoneLocation) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除 ${location.phone} 的自定义归属地吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteLocation(location)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("清空确认")
            .setMessage("确定要清空所有自定义归属地吗？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                clearAllLocations()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteLocation(location: CustomPhoneLocation) {
        AppDatabase.getInstance().customPhoneLocationModel()
            .delete(location)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                    PhoneNumberUtils.clearCustomCache()
                    loadData()
                },
                { error ->
                    Toast.makeText(this, "删除失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
    }
    
    private fun clearAllLocations() {
        AppDatabase.getInstance().customPhoneLocationModel()
            .deleteAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Toast.makeText(this, "已清空所有自定义归属地", Toast.LENGTH_SHORT).show()
                    PhoneNumberUtils.clearCustomCache()
                    loadData()
                },
                { error ->
                    Toast.makeText(this, "清空失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
    }
}
