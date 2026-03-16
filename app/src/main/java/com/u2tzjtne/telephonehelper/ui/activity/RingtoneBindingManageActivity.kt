package com.u2tzjtne.telephonehelper.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityRingtoneBindingManageBinding
import com.u2tzjtne.telephonehelper.db.RingVideo
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.db.RingtonePhoneBinding
import com.u2tzjtne.telephonehelper.ui.adapter.RingtoneBindingAdapter
import com.u2tzjtne.telephonehelper.ui.adapter.SelectRingtoneAdapter
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * 彩铃号码绑定管理界面
 * 
 * 功能：
 * 1. 选择要管理的彩铃
 * 2. 查看该彩铃绑定的所有号码
 * 3. 添加单个号码绑定
 * 4. 批量导入号码绑定
 * 5. 删除号码绑定
 */
class RingtoneBindingManageActivity : BaseActivity() {

    private val binding: ActivityRingtoneBindingManageBinding by lazy {
        ActivityRingtoneBindingManageBinding.inflate(layoutInflater)
    }

    private val adapter: RingtoneBindingAdapter by lazy {
        RingtoneBindingAdapter(onDeleteClick = ::deleteBinding)
    }

    private var currentRingtone: RingVideo? = null
    private var allRingtones: List<RingVideo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        
        // 检查是否有传入的彩铃ID
        val ringtoneId = intent.getIntExtra("ringtone_id", -1)
        if (ringtoneId > 0) {
            // 有指定彩铃，加载并选中
            loadRingtoneAndSelect(ringtoneId)
        } else {
            // 无指定彩铃，显示选择对话框
            showSelectRingtoneDialog()
        }
    }
    
    /**
     * 加载指定ID的彩铃并选中
     */
    private fun loadRingtoneAndSelect(ringtoneId: Int) {
        Single.fromCallable {
            val dao = RingVideoDatabase.getInstance().ringVideoDao()
            val list = dao.getAllSync()
            val ringtone = list.firstOrNull { it.id == ringtoneId }
            Pair(list, ringtone)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (list, ringtone) ->
                allRingtones = list
                if (ringtone != null) {
                    selectRingtone(ringtone)
                } else {
                    Toast.makeText(this, "未找到指定的彩铃", Toast.LENGTH_SHORT).show()
                    showSelectRingtoneDialog()
                }
            }, {
                Toast.makeText(this, "加载彩铃列表失败", Toast.LENGTH_SHORT).show()
                finish()
            })
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        
        binding.rvPhoneList.layoutManager = LinearLayoutManager(this)
        binding.rvPhoneList.adapter = adapter

        // 点击当前彩铃区域可以切换彩铃
        binding.tvCurrentRingtone.setOnClickListener {
            showSelectRingtoneDialog()
        }

        // 添加单个号码
        binding.btnAddBinding.setOnClickListener {
            addSingleBinding()
        }

        // 批量导入
        binding.tvBatchImport.setOnClickListener {
            showBatchImportDialog()
        }

        // 清空全部
        binding.tvClearAll.setOnClickListener {
            clearAllBindings()
        }

        // 输入框实时监听
        binding.etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 自动格式化号码
                s?.let {
                    val formatted = formatPhoneInput(it.toString())
                    if (formatted != it.toString()) {
                        binding.etPhoneNumber.setText(formatted)
                        binding.etPhoneNumber.setSelection(formatted.length)
                    }
                }
            }
        })
    }

    /**
     * 显示选择彩铃对话框
     */
    private fun showSelectRingtoneDialog() {
        Single.fromCallable {
            RingVideoDatabase.getInstance().ringVideoDao().getAllSync()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                allRingtones = list
                if (list.isEmpty()) {
                    Toast.makeText(this, "请先上传彩铃视频", Toast.LENGTH_LONG).show()
                    finish()
                    return@subscribe
                }
                showRingtoneSelectionDialog(list)
            }, {
                Toast.makeText(this, "加载彩铃列表失败", Toast.LENGTH_SHORT).show()
                finish()
            })
    }

    /**
     * 显示彩铃选择对话框
     */
    private fun showRingtoneSelectionDialog(list: List<RingVideo>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_ringtone, null)
        val rvList = dialogView.findViewById<RecyclerView>(R.id.rvRingtoneList)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvEmpty)

        rvList.layoutManager = LinearLayoutManager(this)
        
        val selectAdapter = SelectRingtoneAdapter { ringtone ->
            selectRingtone(ringtone)
        }
        rvList.adapter = selectAdapter
        selectAdapter.submitList(list)
        selectAdapter.setSelectedId(currentRingtone?.id ?: -1)

        // 查询每个彩铃的绑定数量
        loadBindCounts { counts ->
            selectAdapter.setBindCounts(counts)
        }

        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        rvList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(currentRingtone != null)
            .show()
    }

    /**
     * 选择彩铃
     */
    private fun selectRingtone(ringtone: RingVideo) {
        currentRingtone = ringtone
        updateUI()
        loadBindings()
    }

    /**
     * 更新界面显示
     */
    private fun updateUI() {
        val ringtone = currentRingtone
        if (ringtone != null) {
            binding.tvCurrentRingtone.text = ringtone.videoName ?: "未命名视频"
            binding.tvCurrentRingtone.textSize = 16f
        } else {
            binding.tvCurrentRingtone.text = "请选择一个彩铃"
            binding.tvCurrentRingtone.textSize = 14f
        }
    }

    /**
     * 加载当前彩铃的绑定数量
     */
    private fun loadBindCounts(callback: (Map<Int, Int>) -> Unit) {
        Completable.fromAction {
            val db = RingVideoDatabase.getInstance()
            val counts = mutableMapOf<Int, Int>()
            allRingtones.forEach { ringtone ->
                val count = db.ringtonePhoneBindingDao().getBoundPhoneCount(ringtone.id)
                counts[ringtone.id] = count
            }
            runOnUiThread {
                callback(counts)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * 加载当前彩铃绑定的号码列表
     */
    private fun loadBindings() {
        val ringtoneId = currentRingtone?.id ?: return

        Single.fromCallable {
            RingVideoDatabase.getInstance().ringtonePhoneBindingDao().getByRingtoneId(ringtoneId)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                adapter.submitList(list)
                binding.tvBindCount.text = "已绑定 ${list.size} 个号码"
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.rvPhoneList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }, {
                Toast.makeText(this, "加载绑定列表失败", Toast.LENGTH_SHORT).show()
            })
    }

    /**
     * 格式化电话号码，去除所有非数字字符
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9]"), "")
    }

    /**
     * 添加单个号码绑定
     */
    private fun addSingleBinding() {
        val phoneNumber = normalizePhoneNumber(binding.etPhoneNumber.text.toString())
        
        if (!isValidPhoneNumber(phoneNumber)) {
            Toast.makeText(this, "请输入正确的手机号码", Toast.LENGTH_SHORT).show()
            return
        }

        val ringtoneId = currentRingtone?.id
        if (ringtoneId == null) {
            Toast.makeText(this, "请先选择彩铃", Toast.LENGTH_SHORT).show()
            return
        }

        Completable.fromAction {
            val db = RingVideoDatabase.getInstance()
            // 检查号码是否已绑定其他彩铃
            val existingId = db.ringtonePhoneBindingDao().getRingtoneIdByPhone(phoneNumber)
            if (existingId != null && existingId != ringtoneId) {
                // 号码已绑定其他彩铃，先解绑
                db.ringtonePhoneBindingDao().deleteByPhoneNumber(phoneNumber)
            }
            // 绑定到新彩铃
            db.ringtonePhoneBindingDao().insert(RingtonePhoneBinding(phoneNumber, ringtoneId))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show()
                binding.etPhoneNumber.setText("")
                loadBindings()
            }, {
                Toast.makeText(this, "添加失败：${it.message}", Toast.LENGTH_SHORT).show()
            })
    }

    /**
     * 显示批量导入对话框
     */
    private fun showBatchImportDialog() {
        val ringtoneId = currentRingtone?.id
        if (ringtoneId == null) {
            Toast.makeText(this, "请先选择彩铃", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_batch_import, null)
        val etNumbers = dialogView.findViewById<EditText>(R.id.etPhoneNumbers)
        val tvResult = dialogView.findViewById<TextView>(R.id.tvParseResult)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("导入", null)
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnShowListener {
            val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnPositive.setOnClickListener {
                val numbers = parsePhoneNumbers(etNumbers.text.toString())
                if (numbers.isEmpty()) {
                    Toast.makeText(this, "没有识别到有效号码", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                batchImportNumbers(numbers)
                dialog.dismiss()
            }
        }

        // 实时解析
        etNumbers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val numbers = parsePhoneNumbers(s.toString())
                tvResult.text = "识别到 ${numbers.size} 个有效号码"
                tvResult.setTextColor(if (numbers.isNotEmpty()) 0xFF4CAF50.toInt() else 0xFFFF5722.toInt())
            }
        })

        dialog.show()
    }

    /**
     * 解析手机号码
     */
    private fun parsePhoneNumbers(text: String): List<String> {
        val regex = Regex("1[3-9]\\d{9}")
        return regex.findAll(text).map { it.value }.distinct().toList()
    }

    /**
     * 批量导入号码
     */
    private fun batchImportNumbers(numbers: List<String>) {
        val ringtoneId = currentRingtone?.id ?: return

        Completable.fromAction {
            val db = RingVideoDatabase.getInstance()
            val dao = db.ringtonePhoneBindingDao()
            
            numbers.forEach { phoneNumber ->
                // 检查号码是否已绑定其他彩铃
                val existingId = dao.getRingtoneIdByPhone(phoneNumber)
                if (existingId != null && existingId != ringtoneId) {
                    // 号码已绑定其他彩铃，先解绑
                    dao.deleteByPhoneNumber(phoneNumber)
                }
                // 绑定到新彩铃
                dao.insert(RingtonePhoneBinding(phoneNumber, ringtoneId))
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Toast.makeText(this, "成功导入 ${numbers.size} 个号码", Toast.LENGTH_SHORT).show()
                loadBindings()
            }, {
                Toast.makeText(this, "导入失败：${it.message}", Toast.LENGTH_SHORT).show()
            })
    }

    /**
     * 删除单个绑定
     */
    private fun deleteBinding(item: RingtonePhoneBinding) {
        AlertDialog.Builder(this)
            .setTitle("确认解绑")
            .setMessage("确定要解绑号码 ${formatPhoneNumber(item.phoneNumber)} 吗？")
            .setPositiveButton("确定") { _, _ ->
                Completable.fromAction {
                    RingVideoDatabase.getInstance().ringtonePhoneBindingDao().delete(item)
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "解绑成功", Toast.LENGTH_SHORT).show()
                        loadBindings()
                    }, {
                        Toast.makeText(this, "解绑失败", Toast.LENGTH_SHORT).show()
                    })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 清空所有绑定
     */
    private fun clearAllBindings() {
        val ringtoneId = currentRingtone?.id
        if (ringtoneId == null) {
            Toast.makeText(this, "请先选择彩铃", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空该彩铃绑定的所有号码吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                Completable.fromAction {
                    RingVideoDatabase.getInstance().ringtonePhoneBindingDao().deleteByRingtoneId(ringtoneId)
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "已清空所有绑定", Toast.LENGTH_SHORT).show()
                        loadBindings()
                    }, {
                        Toast.makeText(this, "清空失败", Toast.LENGTH_SHORT).show()
                    })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 验证手机号
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    /**
     * 格式化手机号输入（添加空格）
     */
    private fun formatPhoneInput(input: String): String {
        val digits = input.replace(Regex("[^0-9]"), "")
        return when {
            digits.length <= 3 -> digits
            digits.length <= 7 -> "${digits.substring(0, 3)} ${digits.substring(3)}"
            digits.length <= 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
            else -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7, 11)}"
        }
    }

    /**
     * 格式化手机号显示
     */
    private fun formatPhoneNumber(phone: String?): String {
        if (phone.isNullOrEmpty()) return ""
        return when {
            phone.length == 11 -> "${phone.substring(0, 3)} ${phone.substring(3, 7)} ${phone.substring(7)}"
            else -> phone
        }
    }
}