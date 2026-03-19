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
import com.u2tzjtne.telephonehelper.db.Recording
import com.u2tzjtne.telephonehelper.ui.activity.AddCallRecordActivity.Companion.formatWithSpaces
import com.u2tzjtne.telephonehelper.util.AudioRecorderHelper
import com.u2tzjtne.telephonehelper.util.GSYVideoPlayerHelper
import com.u2tzjtne.telephonehelper.util.MediaPlayerHelper
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import com.u2tzjtne.telephonehelper.util.ToastUtils
import io.reactivex.MaybeObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class newCallActivity : BaseActivity() {
    private var isSpeakerOn: Boolean = false
    private var number: String = ""
    private val bind by lazy { NewCallActivityBinding.inflate(layoutInflater) }
    private val mStatusObserver: MutableLiveData<Int> = MutableLiveData()
    private val callRecord = CallRecord()
    private val TAG = "newCallActivity"

    private var isJingYin = false
    private var isLuYin = false

    // 录音工具类
    private val audioRecorderHelper = AudioRecorderHelper.getInstance()
    // 临时存储本次通话的所有录音信息（等待通话记录保存后关联）
    private val pendingRecordings = mutableListOf<AudioRecorderHelper.RecordingInfo>()

    private var playRingJob: Job? = null
    private var autoConnectJob: Job? = null
    private var noResponseJob: Job? = null
    private var finishJob: Job? = null
    private var isEnding = false

    companion object {
        const val PLAY_RING = 2 // 响铃
        const val CONNECTED_STATUS = 3
        const val PLAY_NO_RESPONSE_SOUND = 7

        private const val RING_DELAY_MILLIS = 2_000L
        private const val AUTO_CONNECT_DELAY_MILLIS = 10_000L
        private const val NO_RESPONSE_DELAY_MILLIS = 15_000L
        private const val NO_RESPONSE_AUDIO_DURATION_MILLIS = 23_200L
        private const val FINISH_DELAY_MILLIS = 1_000L
        private const val BUSY_AUDIO_RES_NAME = "audio_busy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        bind.tvAction5.text = "录音"
        configurePreConnectActions()
        getNumberData()

        // 未接通
        updateCallTip(false)
        callRecord.startTime = System.currentTimeMillis()
        callRecord.phoneNumber = number
        callRecord.callType = 0
        bind.tvNewCallNumber.text = callRecord.phoneNumber.formatWithSpaces()

        // 初始化 GSY 视频播放器
        GSYVideoPlayerHelper.getInstance().init(bind.videoRingtone)

        bind.llDialSwitch.setOnClickListener {
            switchSpeaker()
        }

        mStatusObserver.observe(this) { status ->
            when (status) {
                PLAY_RING -> handlePlayRing()
                CONNECTED_STATUS -> handleConnectedStatus()
                PLAY_NO_RESPONSE_SOUND -> handleNoResponseSound()
            }
        }

        scheduleInitialCallFlow()
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
        hangUp("正在挂断...", true)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllJobs()

        // 确保停止录音
        stopAndSaveRecording()

        // 释放 GSY 视频播放器资源
        GSYVideoPlayerHelper.getInstance().release()

        MediaPlayerHelper.getInstance().stopAudio()
        bind.cmCallTime.stop()
    }

    private fun scheduleInitialCallFlow() {
        playRingJob?.cancel()
        autoConnectJob?.cancel()

        playRingJob = lifecycleScope.launch {
            delay(RING_DELAY_MILLIS)
            mStatusObserver.postValue(PLAY_RING)
        }

        autoConnectJob = lifecycleScope.launch {
            delay(AUTO_CONNECT_DELAY_MILLIS)
            mStatusObserver.postValue(CONNECTED_STATUS)
        }
    }

    private fun cancelPreConnectJobs() {
        playRingJob?.cancel()
        autoConnectJob?.cancel()
        noResponseJob?.cancel()
    }

    private fun cancelAllJobs() {
        cancelPreConnectJobs()
        finishJob?.cancel()
    }

    private fun configurePreConnectActions() {
        bind.llAction0.setOnClickListener(null)
        bind.llAction3.setOnClickListener {
            startMissedCallFlow()
        }
        bind.llAction4.setOnClickListener {
            startRejectCallFlow()
        }
        bind.llAction5.setOnClickListener(null)
    }

    private fun handlePlayRing() {
        if (isEnding || callRecord.isConnected) return

        Log.d(TAG, "PLAY_RING 状态触发，准备播放彩铃")
        bind.tvNewCallStatus.text = "对方已振铃"

        // 开始播放彩铃视频（根据号码查找绑定的彩铃）
        // 如果有视频在播放，则不播放拨号等待音
        GSYVideoPlayerHelper.getInstance().playRingtoneVideo(this, packageName, number) { isVideoPlaying ->
            Log.d(TAG, "彩铃播放回调: isVideoPlaying=$isVideoPlaying")
            if (!isVideoPlaying) {
                Log.d(TAG, "没有彩铃视频，播放拨号等待音")
                updateUIForRingtoneVideo(false)
                bind.tvNewCallStatus.text = "对方已振铃"
                MediaPlayerHelper.getInstance().playCallSound(this)
            } else {
                Log.d(TAG, "彩铃正在播放，更新UI")
                updateUIForRingtoneVideo(true)
            }
        }
    }

    private fun handleConnectedStatus() {
        if (isEnding || callRecord.isConnected) return

        cancelPreConnectJobs()
        callRecord.isConnected = true
        callRecord.connectedTime = System.currentTimeMillis()

        stopRingtonePlayback()
        bind.tvNewCallNumberLocal.setCompoundDrawables(
            null,
            null,
            AppCompatResources.getDrawable(this, R.drawable.ic_hd)!!.apply {
                setBounds(0, 0, 105, 105)
                setTint(getColor(R.color.white_70))
            },
            null
        )
        updateCallTip(true)
        bind.tvNewCallStatus.text = ""
        updateAction()
    }

    private fun handleNoResponseSound() {
        if (isEnding || callRecord.isConnected) return

        stopRingtonePlayback()
        bind.tvNewCallStatus.text = "暂时无人接听"
        MediaPlayerHelper.getInstance().playNoResponseSound(this)

        noResponseJob?.cancel()
        noResponseJob = lifecycleScope.launch {
            delay(NO_RESPONSE_AUDIO_DURATION_MILLIS)
            endCallAndFinish("暂时无人接听", true, 0L)
        }
    }

    private fun startMissedCallFlow() {
        if (isEnding || callRecord.isConnected) return

        cancelPreConnectJobs()
        bind.tvNewCallStatus.text = "等待对方接听"
        noResponseJob = lifecycleScope.launch {
            delay(NO_RESPONSE_DELAY_MILLIS)
            mStatusObserver.postValue(PLAY_NO_RESPONSE_SOUND)
        }
    }

    private fun startRejectCallFlow() {
        if (isEnding || callRecord.isConnected) return

        cancelPreConnectJobs()
        stopRingtonePlayback()
        bind.tvNewCallStatus.text = "对方正在通话中"

        val ringStarted = MediaPlayerHelper.getInstance().playCallSoundOnce(this) {
            runOnUiThread {
                playBusyPromptThenFinish()
            }
        }

        if (!ringStarted) {
            playBusyPromptThenFinish()
        }
    }

    private fun playBusyPromptThenFinish() {
        if (isEnding) return

        val busyStarted = MediaPlayerHelper.getInstance().playBusySound(this, BUSY_AUDIO_RES_NAME) {
            runOnUiThread {
                endCallAndFinish("对方正在通话中", true, 0L)
            }
        }

        if (!busyStarted) {
            Log.w(TAG, "未找到忙线提示音资源：$BUSY_AUDIO_RES_NAME")
            endCallAndFinish("对方正在通话中", true, 0L)
        }
    }

    private fun stopRingtonePlayback() {
        GSYVideoPlayerHelper.getInstance().stopPlaying()
        MediaPlayerHelper.getInstance().stopAudio()
        updateUIForRingtoneVideo(false)
    }

    private fun updateHangUpColor() {
        bind.ivDialSwitch.alpha = 0.5f
        bind.ivDialHangUp.alpha = 0.5f
        bind.ivDialSpeaker.alpha = 0.5f
    }

    private fun hangUp(text: String, needSave: Boolean) {
        endCallAndFinish(text, needSave, FINISH_DELAY_MILLIS)
    }

    private fun endCallAndFinish(statusText: String, needSave: Boolean, finishDelayMillis: Long) {
        if (isEnding) return
        isEnding = true

        cancelAllJobs()
        stopAndSaveRecording()
        stopRingtonePlayback()
        updateHangUpColor()
        updateCallTip(false)
        bind.tvNewCallStatus.text = statusText

        if (needSave) {
            saveCallRecord()
        }

        finishJob = lifecycleScope.launch {
            delay(finishDelayMillis)
            finish()
        }
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
        if (!hasRecordAudioPermission()) {
            ToastUtils.s("需要录音权限才能使用录音功能，请先在首页完成授权")
            resetRecordingButton()
            return
        }

        // 开始录音
        val recordingPath = audioRecorderHelper.startRecording(number)
        if (recordingPath != null) {
            Log.d(TAG, "录音开始: $recordingPath")
            ToastUtils.s("开始录音")
        } else {
            ToastUtils.s("录音启动失败")
            resetRecordingButton()
        }
    }

    private fun resetRecordingButton() {
        isLuYin = false
        bind.tvAction5.setTextColor(Color.WHITE)
        bind.tvAction5.stop()
        bind.tvAction5.text = "录音"
        bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
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

        bind.llAction0.setOnClickListener(null)
        bind.llAction4.setOnClickListener(null)

        bind.llAction3.setOnClickListener {
            toggleMuteAction()
        }

        // 录音按钮点击事件
        bind.llAction5.setOnClickListener {
            if (isLuYin) {
                // 停止录音
                bind.tvAction5.setTextColor(Color.WHITE)
                bind.tvAction5.stop()
                bind.tvAction5.text = "录音"
                bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
                stopRecordingByUser()
            } else {
                // 开始录音
                bind.tvAction5.setTextColor("#13A8E1".toColorInt())
                bind.tvAction5.base = SystemClock.elapsedRealtime()
                bind.tvAction5.start()
                bind.tvAction5.compoundDrawableTintList =
                    ColorStateList.valueOf("#13A8E1".toColorInt())
                startRecording()
            }
            isLuYin = !isLuYin
        }
    }

    private fun toggleMuteAction() {
        if (isJingYin) {
            bind.tvAction3.setTextColor(Color.WHITE)
            bind.tvAction3.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        } else {
            bind.tvAction3.setTextColor("#13A8E1".toColorInt())
            bind.tvAction3.compoundDrawableTintList =
                ColorStateList.valueOf("#13A8E1".toColorInt())
        }
        isJingYin = !isJingYin
    }

    private fun switchSpeaker() {
        if (isSpeakerOn) {
            isSpeakerOn = !isSpeakerOn
            MediaPlayerHelper.getInstance().switchAudioOutput(this, isSpeakerOn)
            bind.ivDialSwitch.setTextColor(Color.WHITE)
        } else {
            isSpeakerOn = !isSpeakerOn
            MediaPlayerHelper.getInstance().switchAudioOutput(this, isSpeakerOn)
            bind.ivDialSwitch.setTextColor("#13A8E1".toColorInt())
        }
    }

    private fun updateCallTip(isConnected: Boolean) {
        if (isConnected) {
            bind.cmCallTime.visibility = View.VISIBLE
            // 设置初始值（重置）
            bind.cmCallTime.base = SystemClock.elapsedRealtime()
            bind.cmCallTime.start()
            bind.tvNewCallStatus.visibility = View.GONE
        } else {
            bind.tvNewCallStatus.visibility = View.VISIBLE
            bind.cmCallTime.visibility = View.GONE
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
                    if (oldCallRecord.attribution == null || oldCallRecord.attribution == "null" || oldCallRecord.attribution.isBlank() || oldCallRecord.attribution == "未知") {
                        PhoneNumberUtils.getProvince(number) { bean ->
                            if (bean.province != bean.city) {
                                callRecord.attribution = bean.province + bean.city
                            } else {
                                callRecord.attribution = bean.province
                            }
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
                    if (callRecord.attribution == null || callRecord.attribution == "null" || callRecord.attribution.isBlank() || callRecord.attribution == "未知") {
                        PhoneNumberUtils.getProvince(number) { bean ->
                            if (bean.province != bean.city) {
                                callRecord.attribution = bean.province + bean.city
                            } else {
                                callRecord.attribution = bean.province
                            }
                            callRecord.operator = bean.carrier
                            bind.tvNewCallNumberLocal.text = callRecord.attribution + " " + callRecord.operator
                        }
                    }
                }
            })
    }

    private fun setBackGround() {
        if (!hasReadStoragePermission()) {
            ToastUtils.s("存储权限未开启，当前将使用默认通话背景")
            return
        }
        // 获取 WallpaperManager 实例
        val wallpaperManager = WallpaperManager.getInstance(this)
        // 获取当前的壁纸
        val wallpaperDrawable = wallpaperManager.drawable
        // 将 Drawable 转换为 Bitmap
        val wallpaperBitmap = (wallpaperDrawable as BitmapDrawable).bitmap
        // 设置根布局背景
        bind.root.background = BitmapDrawable(resources, wallpaperBitmap)
    }

    /**
     * 根据是否播放彩铃视频更新界面样式
     * @param isPlayingVideo 是否正在播放彩铃视频
     */
    private fun updateUIForRingtoneVideo(isPlayingVideo: Boolean) {
        if (isPlayingVideo) {
            bind.tvNewCallStatus.text = "正在等待对方接听电话"
            bind.tvAICallStatus.visibility = View.GONE
            bind.tvNewCallPlayingRing.visibility = View.VISIBLE
            bind.ivNewCallHead.visibility = View.GONE
        } else {
            bind.tvNewCallPlayingRing.visibility = View.GONE
            bind.ivNewCallHead.visibility = View.VISIBLE
            bind.tvAICallStatus.visibility = View.VISIBLE
        }
    }
}
