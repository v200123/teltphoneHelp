package com.u2tzjtne.telephonehelper.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityAddCallRecordBinding
import com.u2tzjtne.telephonehelper.db.AppDatabase
import com.u2tzjtne.telephonehelper.db.CallRecord
import com.u2tzjtne.telephonehelper.db.CustomPhoneLocation
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback
import com.u2tzjtne.telephonehelper.util.ClipboardUtils
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import kotlin.random.Random


class AddCallRecordActivity : BaseActivity() {
    private val binding: ActivityAddCallRecordBinding by lazy { ActivityAddCallRecordBinding.inflate(layoutInflater) }
    private val callRecord = CallRecord()

    companion object {
        fun String.formatWithSpaces(): String {
            val digits = this.filter { it.isDigit() }
            return when {
                digits.isEmpty() -> ""
                digits.length <= 3 -> digits
                digits.length == 12 -> digits.substring(0, 4) + " " + digits.substring(4, 8) + " " + digits.substring(8, 12)
                digits.length <= 7 -> digits.substring(0, 3) + " " + digits.substring(3)
                else -> digits.substring(0, 3) + " " + digits.substring(3, 7) + " " + digits.substring(7)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        // 返回按钮点击事件
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 设置按钮点击事件
        binding.ivSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 粘贴按钮点击事件
        binding.btnPaste.setOnClickListener {
            val clipboardText = ClipboardUtils.getText(this)
            if (!clipboardText.isNullOrEmpty()) {
                // 过滤出纯数字
                val phoneNumber = clipboardText.filter { it.isDigit() }
                if (phoneNumber.isNotEmpty()) {
                    binding.etPhoneNumber.setText(phoneNumber)
                } else {
                    Toast.makeText(this, "剪贴板中没有有效的手机号", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rgPhoneType.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.call_type_out) {
                callRecord.callType = 0
            } else {
                callRecord.callType = 1
            }
        }
        binding.button.setOnClickListener {
            val calendar: Calendar = Calendar.getInstance()
            val year: Int = calendar.get(Calendar.YEAR)
            val month: Int = calendar.get(Calendar.MONTH)
            val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
            val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
            val minute: Int = calendar.get(Calendar.MINUTE)

            val datePickerDialog = DatePickerDialog(
                this,
                { view, year, month, dayOfMonth -> // 更新时间选择器的时间
                    val timePickerDialog = TimePickerDialog(
                        this@AddCallRecordActivity,
                        { view, hourOfDay, minute -> // 处理日期和时间选择后的操作，例如更新UI或执行其他任务
                            val dateTime = String.format(
                                "%04d-%02d-%02d %02d:%02d",
                                year,
                                month + 1,
                                dayOfMonth,
                                hourOfDay,
                                minute
                            )
                            Log.d("DateTimePicker", "Selected date and time: $dateTime")
                            binding.button.setText(dateTime)
                            val localDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                            val epochSecond =
                                localDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
                            callRecord.startTime = epochSecond
                            callRecord.connectedTime = epochSecond
                        }, hour, minute, true
                    )
                    timePickerDialog.show()
                }, year, month, day
            )

            datePickerDialog.show()
        }

        // 保存自定义归属地按钮
        binding.btnSaveCustomLocation.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val province = binding.etProvince.text.toString().trim()
            val city = binding.etCity.text.toString().trim()
            val carrier = binding.etCarrier.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "请先输入手机号码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (province.isEmpty() && city.isEmpty() && carrier.isEmpty()) {
                Toast.makeText(this, "请至少填写一项归属地信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 保存完整号码（统一去掉空白字符）
            val fullPhoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)

            val customLocation = CustomPhoneLocation(fullPhoneNumber, province, city, carrier)
            AppDatabase.getInstance().customPhoneLocationModel()
                .insert(customLocation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "自定义归属地保存成功", Toast.LENGTH_SHORT).show()
                }, { error ->
                    Toast.makeText(this, "保存失败: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AddCallRecord", "保存自定义归属地失败", error)
                })
        }

        // 查看已保存的自定义归属地
        binding.btnViewCustomLocation.setOnClickListener {
            startActivity(Intent(this, CustomLocationListActivity::class.java))
        }

        binding.submit.setOnClickListener {

            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (binding.etPhoneCallTime.text == null) {
                Toast.makeText(this, "请输入通话时间", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val callTime = binding.etPhoneCallTime.text.toString().toLong()

            if (callTime != 0L) {
                callRecord.isConnected = true
            } else {

                if (callRecord.callType == 1) {
                    callRecord.callNumber = Random.nextInt(5, 13)
                }
                callRecord.isConnected = false
            }
            callRecord.endTime = callRecord.startTime + (callTime * 1000)

            PhoneNumberUtils.getProvince(phoneNumber, object : getLocalCallback {
                override fun result(bean: PhoneLocalBean) {
                    if (bean.province != bean.city)
                        callRecord.attribution = bean.province + bean.city
                    else callRecord.attribution = bean.province
                    callRecord.operator = bean.carrier
                }
            })
            callRecord.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)

            AppDatabase.getInstance().callRecordModel()
                .insert(callRecord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
            finish()
        }

    }
}
