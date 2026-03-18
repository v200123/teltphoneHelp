package com.u2tzjtne.telephonehelper.ui.activity

import android.Manifest
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
import com.u2tzjtne.telephonehelper.db.Recording
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback
import com.u2tzjtne.telephonehelper.ui.activity.AddCallRecordActivity.Companion.formatWithSpaces
import com.u2tzjtne.telephonehelper.util.AudioRecorderHelper
import com.u2tzjtne.telephonehelper.util.GSYVideoPlayerHelper
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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    // 录音工具类
    private val audioRecorderHelper = AudioRecorderHelper.getInstance()
    // 临时存储本次通话的所有录音信息（等待通话记录保存后关联）
    private val pendingRecordings = mutableListOf<AudioRecorderHelper.RecordingInfo>()

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

        bind.tvNewCallNumber.setText(callRecord.phoneNumber.formatWithSpaces())

        // 初始化 GSY 视频播放器
        GSYVideoPlayerHelper.getInstance().init(bind.videoRingtone)

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
            // 取消自动接通的协程，但保留其他协程
            lifecycleScope.coroutineContext.cancelChildren()
            
            // 启动无人接听倒计时（15秒后触发）
            GlobalScope.launch(Dispatchers.Default) {
                Log.e(TAG, "updateAction: 开始15秒的无人接听倒计时" )
                delay(15 * 1000)
                mStatusObserver.postValue(PLAY_NO_RESPONSE_SOUND)
            }
        }

        mStatusObserver.observe(this) {
            when (it) {
                GUADUAN ->{
                    lifecycleScope.cancel()
                    // 停止录音并保存
                    stopAndSaveRecording()
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
                    // 接通后停止播放彩铃视频
                    GSYVideoPlayerHelper.getInstance().stopPlaying()
                    // 恢复正常界面样式
                    updateUIForRingtoneVideo(false)

                    MediaPlayerHelper.getInstance().stopAudio()
                    callRecord.isConnected = true
                    callRecord.connectedTime = System.currentTimeMillis()
                    updateCallTip(true)
                    bind.tvNewCallStatus.text = ""
                    updateAction()
                }

                PLAY_RING -> {
                    Log.d(TAG, "PLAY_RING 状态触发，准备播放彩铃")
                    bind.llAction4.setOnClickListener {
                        mStatusObserver.value = GUADUAN
                    }
                    // 开始播放彩铃视频（根据号码查找绑定的彩铃）
                    // 如果有视频在播放，则不播放拨号等待音
                    GSYVideoPlayerHelper.getInstance().playRingtoneVideo(this, packageName, number) { isVideoPlaying ->
                        Log.d(TAG, "彩铃播放回调: isVideoPlaying=$isVideoPlaying")
                        if (!isVideoPlaying) {
                            // 没有视频时播放拨号等待音
                            Log.d(TAG, "没有彩铃，播放拨号等待音")
                            MediaPlayerHelper.getInstance().playCallSound(this)
                        } else {
                            // 有彩铃视频播放时，调整界面样式
                            Log.d(TAG, "彩铃正在播放，更新UI")
                            updateUIForRingtoneVideo(true)
                        }
                    }
                }

                PLAY_NO_RESPONSE_SOUND -> {
                    // 停止彩铃视频播放
                    GSYVideoPlayerHelper.getInstance().stopPlaying()
                    // 恢复正常界面样式
                    updateUIForRingtoneVideo(false)
                    
                    MediaPlayerHelper.getInstance().playNoResponseSound(this)
                    lifecycleScope.launch(Dispatchers.Default) {
                        delay(23200)
                        mStatusObserver.postValue(CALL_END)
                    }
                }

                WAIT_FINISH -> {
                    Log.e(TAG, "onCreate:WAIT_FINISH " )
                    MediaPlayerHelper.getInstance().stopAudio()
                    // 停止录音
                    stopAndSaveRecording()
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

        // 确保停止录音
        stopAndSaveRecording()
        
        // 释放 GSY 视频播放器资源
        GSYVideoPlayerHelper.getInstance().release()
        
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
        
        // 如果有录音，设置第一个录音路径到 callRecord（向后兼容）
        if (pendingRecordings.isNotEmpty()) {
            callRecord.recordingPath = pendingRecordings[0].filePath
            callRecord.recordingStartTime = pendingRecordings[0].startTime
            callRecord.recordingEndTime = pendingRecordings[0].endTime
        }
        
        AppDatabase.getInstance().callRecordModel()
            .insert(callRecord)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onComplete() {
                    Log.d(TAG, "通话记录保存成功")
                    // 获取刚插入的通话记录ID，然后保存录音记录
                    saveRecordingRecords()
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "通话记录保存失败: ${e.message}")
                }
            })
    }

    /**
     * 保存所有录音记录到 Recording 表
     */
    private fun saveRecordingRecords() {
        if (pendingRecordings.isEmpty()) return
        
        // 查询刚插入的通话记录获取ID
        AppDatabase.getInstance().callRecordModel()
            .getByNumber(number)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(object : MaybeObserver<CallRecord?> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(savedCallRecord: CallRecord) {
                    // 为每条录音创建 Recording 记录
                    pendingRecordings.forEach { info ->
                        val recording = Recording().apply {
                            callRecordId = savedCallRecord.id
                            filePath = info.filePath
                            startTime = info.startTime
                            endTime = info.endTime
                            duration = info.getDuration()
                            format = "m4a"
                            createdAt = System.currentTimeMillis()
                            // 获取文件大小
                            fileSize = try {
                                File(info.filePath).length()
                            } catch (e: Exception) {
                                0L
                            }
                        }
                        
                        AppDatabase.getInstance().recordingModel()
                            .insert(recording)
                            .subscribeOn(Schedulers.io())
                            .subscribe()
                    }
                    Log.d(TAG, "已保存 ${pendingRecordings.size} 条录音记录")
                    pendingRecordings.clear()
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "查询通话记录失败: ${e.message}")
                }

                override fun onComplete() {
                    Log.d(TAG, "未找到通话记录")
                }
            })
    }

    /**
     * 开始录音
     */
    private fun startRecording() {
        // 检查并请求录音权限
        AndPermission.with(this)
            .runtime()
            .permission(Permission.RECORD_AUDIO)
            .onDenied { 
                ToastUtils.s("需要录音权限才能使用录音功能")
                // 重置录音按钮状态
                isLuYin = false
                bind.tvAction5.setTextColor(Color.WHITE)
                bind.tvAction5.stop()
                bind.tvAction5.setText("录音")
                bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
            }
            .onGranted { 
                // 开始录音
                val recordingPath = audioRecorderHelper.startRecording(number)
                if (recordingPath != null) {
                    Log.d(TAG, "录音开始: $recordingPath")
                    ToastUtils.s("开始录音")
                } else {
                    ToastUtils.s("录音启动失败")
                    // 重置状态
                    isLuYin = false
                    bind.tvAction5.setTextColor(Color.WHITE)
                    bind.tvAction5.stop()
                    bind.tvAction5.setText("录音")
                    bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
                }
            }.start()
    }

    /**
     * 停止录音并将录音信息添加到待保存列表
     */
    private fun stopAndSaveRecording() {
        if (audioRecorderHelper.isCurrentlyRecording()) {
            val recordingInfo = audioRecorderHelper.stopRecording()
            recordingInfo?.let { info ->
                Log.d(TAG, "录音停止: ${info.filePath}, 时长: ${info.getFormattedDuration()}")
                // 将录音信息添加到待保存列表
                pendingRecordings.add(info)
            }
        }
    }

    /**
     * 停止录音（用户手动停止）
     */
    private fun stopRecordingByUser() {
        if (audioRecorderHelper.isCurrentlyRecording()) {
            val recordingInfo = audioRecorderHelper.stopRecording()
            recordingInfo?.let { info ->
                Log.d(TAG, "录音停止: ${info.filePath}, 时长: ${info.getFormattedDuration()}")
                // 将录音信息添加到待保存列表
                pendingRecordings.add(info)
                ToastUtils.s("录音已停止，共 ${pendingRecordings.size} 条录音")
            }
        }
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

        // 录音按钮点击事件
        bind.llAction5.setOnClickListener {
            if (isLuYin) {
                // 停止录音
                bind.tvAction5.setTextColor(Color.WHITE)
                bind.tvAction5.stop()
                bind.tvAction5.setText("录音")
                bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
                stopRecordingByUser()
            } else {
                // 开始录音
                bind.tvAction5.setTextColor("#13A8E1".toColorInt())
                bind.tvAction5.setBase(SystemClock.elapsedRealtime())
                bind.tvAction5.start()
                bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf("#13A8E1".toColorInt())
                startRecording()
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
                // 设置根布局背景
                bind.root.setBackground(BitmapDrawable(resources, wallpaperBitmap))
            }.start()
    }

    /**
     * 根据是否播放彩铃视频更新界面样式
     * @param isPlayingVideo 是否正在播放彩铃视频
     */
    private fun updateUIForRingtoneVideo(isPlayingVideo: Boolean) {
        if (isPlayingVideo) {
            // 播放彩铃视频时的界面调整
            // 1. 隐藏半透明黑色遮罩层（视频本身就是背景）
            bind.tvNewCallStatus.text = "正在等待对方接听电话"
            bind.tvAICallStatus.visibility = View.GONE
            bind.tvNewCallPlayingRing.visibility = View.VISIBLE
            // 2. 可选：隐藏头像（视频中有更好的视觉效果）
             bind.ivNewCallHead.visibility = View.GONE
            bind.tvNewCallNumberLocal.setCompoundDrawables(null,
                null,
                null,
                null
            )
            // 3. 或者让头像更小、更透明
//            bind.ivNewCallHead.alpha = 0.3f
            
        } else {
            // 恢复正常界面
            // 1. 恢复半透明黑色遮罩层
            bind.tvNewCallPlayingRing.visibility = View.GONE
            bind.tvNewCallStatus.text = "正在拨号中"
            // 2. 恢复头像显示
            bind.ivNewCallHead.visibility = View.VISIBLE
            bind.tvAICallStatus.visibility = View.VISIBLE

//            bind.ivNewCallHead.alpha = 1.0f
        }
    }


}
