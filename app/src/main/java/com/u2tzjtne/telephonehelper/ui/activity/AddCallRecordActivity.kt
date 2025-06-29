package com.u2tzjtne.telephonehelper.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.ActivityAddCallRecordBinding
import com.u2tzjtne.telephonehelper.db.AppDatabase
import com.u2tzjtne.telephonehelper.db.CallRecord
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback
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
            if (this.length > 3)
                return this.replace(Regex("(\\d{3})(\\d{4})(\\d{4})"), "$1 $2 $3")
            else return this
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
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
            callRecord.phoneNumber = phoneNumber.formatWithSpaces()
            AppDatabase.getInstance().callRecordModel()
                .insert(callRecord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
            finish()
        }

    }


}