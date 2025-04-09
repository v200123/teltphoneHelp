package com.u2tzjtne.telephonehelper.ui.activity

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.toColorInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.databinding.NewCallActivityBinding
import com.u2tzjtne.telephonehelper.db.AppDatabase
import com.u2tzjtne.telephonehelper.db.CallRecord
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback
import com.u2tzjtne.telephonehelper.ui.activity.AddCallRecordActivity.Companion.formatWithSpaces
import com.u2tzjtne.telephonehelper.util.MediaPlayerHelper
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import com.u2tzjtne.telephonehelper.util.ToastUtils
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import io.reactivex.MaybeObserver
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class newCallActivity : BaseActivity() {
    private var isSpeakerOn: Boolean = false
    private var number: String = ""
    private val bind by lazy { NewCallActivityBinding.inflate(layoutInflater) }
    private val mStatusObserver: MutableLiveData<Int> = MutableLiveData()
    private val callRecord = CallRecord()
    private val TAG = "TAG"

    private var isJingYin = false
    private var isLuYin = false
    private var isMiantiYin = false

    companion object {
        const val GUADUAN = 9
        const val PLAY_RING = 2 //响铃
        const val CONNECTED_STATUS = 3
        const val HOLD_ON_PHONE = 4
        const val WAIT_FINISH = 5
        const val FINISH = 6
        const val PLAY_NO_RESPONSE_SOUND = 7
        const val CALL_END = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        bind.tvAction5.setText("录音")
        getNumberData()
        //未接通
        updateCallTip(false)
        callRecord.startTime = System.currentTimeMillis()
        callRecord.phoneNumber = number
        callRecord.callType = 0
//        PhoneNumberUtils.getProvince(number,object : getLocalCallback{
//            override fun result(bean: PhoneLocalBean) {
//                if(bean.province != bean.city)
//                    callRecord.attribution = bean.province + bean.city
//                else  callRecord.attribution = bean.province
//                callRecord.operator = bean.carrier
//                bind.tvNewCallNumberLocal.setText(callRecord.attribution + " " + callRecord.operator)
//            }
//        })

        bind.tvNewCallNumber.setText(callRecord.phoneNumber.formatWithSpaces())

        bind.llDialSwitch.setOnClickListener {

            if(isSpeakerOn){
                isSpeakerOn = !isSpeakerOn
                MediaPlayerHelper.getInstance().switchAudioOutput(this, isSpeakerOn)
                bind.ivDialSwitch.setTextColor(Color.WHITE)
            }else{
                isSpeakerOn = !isSpeakerOn
                MediaPlayerHelper.getInstance().switchAudioOutput(this, isSpeakerOn)
                bind.ivDialSwitch.setTextColor("#13A8E1".toColorInt())
            }


        }



        bind.llAction0.setOnClickListener {
            lifecycleScope.cancel()
            GlobalScope.launch(Dispatchers.Default) {
                Log.e(TAG, "updateAction: 开始26秒的延时" )
                delay(15 * 1000)
                mStatusObserver.postValue(PLAY_NO_RESPONSE_SOUND)

            }
        }

        mStatusObserver.observe(this) {
            when (it) {
                GUADUAN ->{
                    lifecycleScope.cancel()
                    MediaPlayerHelper.getInstance().stopAudio()
                    GlobalScope.launch(Dispatchers.Default) {
                        MediaPlayerHelper.getInstance().playCallSound(this@newCallActivity)
                        delay(5_000)
                        MediaPlayerHelper.getInstance().stopAudio()

                        MediaPlayerHelper.getInstance().playGuaduanSound(this@newCallActivity)
                        delay(47_200)
                        mStatusObserver.postValue(CALL_END)
                    }
                }

                CONNECTED_STATUS -> {

                    MediaPlayerHelper.getInstance().stopAudio()
                    callRecord.isConnected = true
                    callRecord.connectedTime = System.currentTimeMillis()
                    updateCallTip(true)
                    bind.tvNewCallStatus.text = ""
                    updateAction()
                }

                PLAY_RING -> {
                    bind.llAction4.setOnClickListener {

                        mStatusObserver.value = GUADUAN

                    }
                    MediaPlayerHelper.getInstance().playCallSound(this)
                }

                PLAY_NO_RESPONSE_SOUND -> {
                    MediaPlayerHelper.getInstance().playNoResponseSound(this)
                    lifecycleScope.launch(Dispatchers.Default) {
                        delay(23200)
                        mStatusObserver.postValue(CALL_END)
                    }
                }

                WAIT_FINISH -> {
                    Log.e(TAG, "onCreate:WAIT_FINISH " )
                    MediaPlayerHelper.getInstance().stopAudio()
                    GlobalScope.launch(Dispatchers.Default) {
                        delay(1000)
                        mStatusObserver.postValue(FINISH)
                    }
                }

                FINISH -> {
                    updateHangUpColor()
                    mStatusObserver.postValue(CALL_END)

                }

                CALL_END -> {
                    updateHangUpColor()

                    GlobalScope.launch(Dispatchers.Default) {
                        delay(1000)
                        withContext(Dispatchers.Main) {
                            hangUp("通话结束", true)
                            finish()
                        }

                    }

                }
            }
        }
        lifecycleScope.launch(Dispatchers.Default) {

            delay(1000)
            withContext(Dispatchers.Main) {
                bind.tvNewCallNumberLocal.setCompoundDrawables(null,
                    null,
                    AppCompatResources.getDrawable(
                        this@newCallActivity,
                        R.drawable.ic_hd
                    )!!.apply { setBounds(0, 0, 105, 105)
                              setTint(getColor(R.color.white_50))
                              },
                    null
                )
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            async {
                delay(2_000)
                mStatusObserver.postValue(PLAY_RING)
            }
            async {
                delay(10.seconds)

                mStatusObserver.postValue(CONNECTED_STATUS)
            }

        }
        setBackGround()

        MediaPlayerHelper.getInstance().switchAudioOutput(this, isSpeakerOn)


        bind.llDialSpeaker.setOnClickListener {
            hangUp("通话结束", true)
        }

        bind.ivDialHangUp.setOnClickListener {
            hangUp("正在挂断...", false)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        hangUp("正在挂断...", true)
    }

    override fun onDestroy() {
        super.onDestroy()

        MediaPlayerHelper.getInstance().stopAudio()
        bind.cmCallTime.stop()
    }


    private fun updateHangUpColor() {
        bind.ivDialSwitch.alpha = 0.5f
        bind.ivDialHangUp.alpha = 0.5f
        bind.ivDialSpeaker.setAlpha(0.5f)
    }

    private fun hangUp(text: String, needSave: Boolean) {
        if (needSave) {
            saveCallRecord()
        }
        updateCallTip(false)
        bind.tvNewCallStatus.setText(text)
        mStatusObserver.value = WAIT_FINISH
    }

    private fun saveCallRecord() {
        callRecord.endTime = System.currentTimeMillis()
        AppDatabase.getInstance().callRecordModel()
            .insert(callRecord)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
    }



    @SuppressLint("UseCompatTextViewDrawableApis")
    private fun updateAction() {
        bind.tvAction0.setTextColor(Color.WHITE)
        bind.tvAction0.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        bind.tvAction1.setTextColor(Color.WHITE)
        bind.tvAction1.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        bind.tvAction3.setTextColor(Color.WHITE)
        bind.tvAction3.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        bind.tvAction4.setTextColor(Color.WHITE)
        bind.tvAction4.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        bind.llAction4.setOnClickListener(null)


        bind.llAction3.setOnClickListener {
            if (isJingYin) {
                bind.tvAction3.setTextColor(Color.WHITE)
                bind.tvAction3.compoundDrawableTintList =
                    ColorStateList.valueOf(Color.WHITE)
            } else {
                bind.tvAction3.setTextColor("#13A8E1".toColorInt())
                bind.tvAction3.compoundDrawableTintList =
                    ColorStateList.valueOf("#13A8E1".toColorInt())
            }
            isJingYin = !isJingYin
        }

        bind.llAction5.setOnClickListener {
            if(isLuYin)
            {
                bind.tvAction5.setTextColor(Color.WHITE)
                bind.tvAction5.stop()
                bind.tvAction5.setText("录音")
                bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
            }
            else{
                bind.tvAction5.setTextColor("#13A8E1".toColorInt())
                bind.tvAction5.start()
                bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf("#13A8E1".toColorInt())
            }
            isLuYin = !isLuYin
        }




    }

    private fun updateCallTip(isConnected: Boolean) {
        if (isConnected) {
            bind.cmCallTime.setVisibility(View.VISIBLE)
            //设置初始值（重置）
            bind.cmCallTime.setBase(SystemClock.elapsedRealtime())
            bind.cmCallTime.start()
            bind.tvNewCallStatus.setVisibility(View.GONE)
        } else {
            bind.tvNewCallStatus.setVisibility(View.VISIBLE)
            bind.cmCallTime.setVisibility(View.GONE)
            bind.cmCallTime.stop()
        }
    }

    private fun getNumberData() {
        number = intent.getStringExtra("phoneNumber") ?: ""

        AppDatabase.getInstance().callRecordModel()
            .getByNumber(number)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : MaybeObserver<CallRecord?> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onSuccess(oldCallRecord: CallRecord) {
                    if ( oldCallRecord.attribution == null || oldCallRecord.attribution=="null" || oldCallRecord.attribution.isBlank() || oldCallRecord.attribution == "未知") {
                        PhoneNumberUtils.getProvince(
                            number
                        ) { bean ->
                            if(bean.province != bean.city)
                                callRecord.attribution = bean.province + bean.city
                            else  callRecord.attribution = bean.province
                            callRecord.operator = bean.carrier
                            bind.tvNewCallNumberLocal.text = callRecord.attribution + " " + callRecord.operator
                        }
                    } else {
                        bind.tvNewCallNumberLocal.text = oldCallRecord.attribution + " " + oldCallRecord.operator
                        callRecord.attribution = oldCallRecord.attribution
                        callRecord.operator = oldCallRecord.operator
                    }
                }

                override fun onError(e: Throwable) {
                    Log.d(TAG, "onError: 走了onError流程")

                }

                override fun onComplete() {
                    Log.d(TAG, "onComplete: 走了onComplete流程")
                    if(callRecord.attribution==null || callRecord.attribution=="null" || callRecord.attribution.isBlank() || callRecord.attribution == "未知") {
                        PhoneNumberUtils.getProvince(
                            number
                        ) { bean ->
                            if (bean.province != bean.city)
                                callRecord.attribution = bean.province + bean.city
                            else callRecord.attribution = bean.province
                            callRecord.operator = bean.carrier
                            bind.tvNewCallNumberLocal.text =
                                callRecord.attribution + " " + callRecord.operator
                        }
                    }
                }
            })

    }

    private fun setBackGround() {
        AndPermission.with(this)
            .runtime()
            .permission(Permission.READ_EXTERNAL_STORAGE)
            .onDenied { data: List<String?>? ->
                ToastUtils.s("请授予权限后再试！")
                finish()
            }
            .onGranted { data: List<String?>? ->
                // 获取WallpaperManager实例
                val wallpaperManager =
                    WallpaperManager.getInstance(this)
                // 获取当前的壁纸
                val wallpaperDrawable = wallpaperManager.drawable
                // 将Drawable转换为Bitmap
                val wallpaperBitmap =
                    (wallpaperDrawable as BitmapDrawable?)!!.bitmap
                // 设置背景
                bind.root.setBackground(BitmapDrawable(resources, wallpaperBitmap))
            }.start()
    }


}