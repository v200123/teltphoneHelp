package com.u2tzjtne.telephonehelper.ui.activity

import android.R.color.transparent
import android.annotation.SuppressLint
import android.content.Context
import android.app.WallpaperManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

import com.u2tzjtne.telephonehelper.util.CallVibrationSettings
import com.u2tzjtne.telephonehelper.util.PhoneNumberFormatUtils
import com.u2tzjtne.telephonehelper.util.GSYVideoPlayerHelper
import com.u2tzjtne.telephonehelper.util.MediaPlayerHelper
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import com.u2tzjtne.telephonehelper.util.ToastUtils
import com.zackratos.ultimatebarx.ultimatebarx.statusBarOnly
import io.reactivex.MaybeObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class newCallActivity : BaseActivity() {

    // ===================== 通话状态枚举 =====================
    /**
     * 通话状态机：
     *
     * DIALING  ──(2s)──► RINGING ──(8s)──► CONNECTED（自动接通）
     *                         │
     *                    [用户触发]
     *                    ├── 静音按钮 ──► NO_ANSWER（等待15s → 无人接听音 → 自动结束）
     *                    └── 暂停按钮 ──► BUSY（忙线音 → 自动结束）
     *
     * CONNECTED ──[挂断]──► HUNG_UP（延迟3.2s finish）
     * RINGING   ──[挂断]──► HUNG_UP
     * DIALING   ──[挂断]──► HUNG_UP
     */
    enum class CallState {
        /** 拨打中：从启动到真正振铃前，显示"正在拨号" */
        DIALING,
        /** 响铃中：对方已振铃，可能播放彩铃，等待接听 */
        RINGING,
        /** 通话中：已接通，计时开始，全功能可用 */
        CONNECTED,
        /** 忙音：对方正在通话中，播放忙线音后自动结束 */
        BUSY,
        /** 无人接听：等待超时后触发，播放无人接听音后自动结束 */
        NO_ANSWER,
        /** 挂断：用户主动挂断或通话结束，延迟后finish */
        HUNG_UP
    }

    // ===================== 成员变量 =====================
    private var isSpeakerOn: Boolean = false
    private var number: String = ""
    private val bind by lazy { NewCallActivityBinding.inflate(layoutInflater) }

    /** 通话状态 LiveData，所有状态变更必须通过此发布 */
    private val callStateLD: MutableLiveData<CallState> = MutableLiveData()

    private val callRecord = CallRecord()
    private val TAG = "newCallActivity"

    private var isJingYin = false
    private var isLuYin = false
    private var hasRingtone = false  // 是否有彩铃
    private var ringtoneDuration: Long = 0L  // 彩铃视频时长（毫秒）

    // 录音开始时间（仅用于计时，不实际录音）
    private var recordingStartTime: Long = 0L

    // 协程 Job
    private var ringJob: Job? = null          // 延迟进入RINGING状态
    private var autoConnectJob: Job? = null   // 延迟自动接通
    private var noAnswerWaitJob: Job? = null  // 无人接听等待计时
    private var noAnswerAudioJob: Job? = null // 无人接听音播放计时
    private var finishJob: Job? = null        // 延迟finish

    companion object {
        // 时间常量（毫秒）
        private const val RING_DELAY_MILLIS = 2_000L           // 拨号→振铃 延迟
        private const val AUTO_CONNECT_DELAY_MILLIS = 10_000L  // 振铃→自动接通 延迟
        private const val NO_ANSWER_TRIGGER_MILLIS = 15_000L   // 触发无人接听的等待时间
        private const val NO_ANSWER_AUDIO_MILLIS = 23_200L     // 无人接听音时长
        private const val FINISH_DELAY_MILLIS = 1_000L         // 挂断后延迟finish
        private const val BUSY_AUDIO_RES_NAME = "audio_busy"
    }

    // ===================== 生命周期 =====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        statusBarOnly {
            transparent()
        }
        getNumberData()
        initCallRecord()
        initUI()
        initGSYVideo()
        observeCallState()
        setBackGround()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        MediaPlayerHelper.getInstance().prepareCallAudio(this, isSpeakerOn)

        // iv_dial_hang_up 无论任何状态均响应挂断，永久绑定且不可被覆盖
        bind.ivDialHangUp.setOnClickListener { dispatchHangUp(userInitiated = true) }

        // 启动通话流程：进入拨打中状态
        callStateLD.value = CallState.DIALING
    }

    override fun onBackPressed() {
        dispatchHangUp(userInitiated = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllJobs()
        stopRecordingTimer()
        GSYVideoPlayerHelper.getInstance().release()
        MediaPlayerHelper.getInstance().stopAudio()
        MediaPlayerHelper.getInstance().releaseCallAudio(this)
        bind.cmCallTime.stop()
    }

    // ===================== 状态观察 & 分发 =====================

    /**
     * 监听通话状态，集中分发到各处理函数
     */
    private fun observeCallState() {
        callStateLD.observe(this) { state ->
            Log.d(TAG, "通话状态变更 → $state")
            when (state) {
                CallState.DIALING    -> onStateDialing()
                CallState.RINGING    -> onStateRinging()
                CallState.CONNECTED  -> onStateConnected()
                CallState.BUSY       -> onStateBusy()
                CallState.NO_ANSWER  -> onStateNoAnswer()
                CallState.HUNG_UP    -> onStateHungUp()
            }
        }
    }

    // ===================== 各状态处理 =====================

    /**
     * 状态：拨打中
     * - UI：显示"正在拨号"，计时器隐藏
     * - 音频：无
     * - 按钮：action区灰显（仅预连接模式），键盘按钮不可用
     * - 调度：2s后进入RINGING
     */
    private fun onStateDialing() {
        // UI：根据是否有彩铃显示不同文字
        bind.tvNewCallStatus.text = if (hasRingtone) "正在等待对方接听电话" else "正在拨号"
        showCallStatus(connected = false)
        setActionAreaEnabled(false)
        setBottomBarMode(dialMode = false)

        // 预连接阶段的按钮交互：静音=触发无人接听流程，暂停=触发忙线流程
        setupPreConnectActions()

        // 调度：2s → RINGING，10s → CONNECTED
        ringJob = lifecycleScope.launch {
            delay(RING_DELAY_MILLIS)
            callStateLD.postValue(CallState.RINGING)
        }
        autoConnectJob = lifecycleScope.launch {
            delay(AUTO_CONNECT_DELAY_MILLIS)
            callStateLD.postValue(CallState.CONNECTED)
        }
    }

    /**
     * 状态：响铃中
     * - UI：显示"对方已振铃"，可能切换为彩铃视频UI
     * - 音频：播放彩铃视频 或 拨号等待音（循环）
     * - 按钮：预连接模式保持不变（可触发未接/忙线）
     * 
     * 注意：如果播放彩铃，会根据彩铃时长调整自动接通时间，确保至少播放一次
     */
    private fun onStateRinging() {
        if (callRecord.isConnected) return
        // 尝试播放彩铃视频，否则播放拨号等待音
        GSYVideoPlayerHelper.getInstance().playRingtoneVideo(this, packageName, number) { isVideoPlaying ->
            Log.d(TAG, "彩铃回调: isVideoPlaying=$isVideoPlaying")
            hasRingtone = isVideoPlaying
            if (isVideoPlaying) {
                updateUIForRingtoneVideo(true)
                bind.tvNewCallStatus.text = "正在等待对方接听电话"
                // 获取彩铃时长并调整自动接通时间，确保至少播放一次
                adjustAutoConnectTimeForRingtone()
            } else {
                updateUIForRingtoneVideo(false)
                bind.tvNewCallStatus.text = "正在拨号"
                MediaPlayerHelper.getInstance().playCallSound(this)
            }
        }
    }

    /**
     * 根据彩铃时长调整自动接通时间，确保彩铃至少完整播放一次
     * 
     * 逻辑：
     * - 如果彩铃时长大于 10 秒，使用彩铃时长作为自动接通时间
     * - 如果彩铃时长小于等于 10 秒，保持原有的 10 秒自动接通
     * - 最多等待 60 秒（避免彩铃过长导致等待太久）
     */
    private fun adjustAutoConnectTimeForRingtone() {
        thread {
            try {
                // 从数据库获取当前号码分配的彩铃信息
                val normalizedNumber = number.replace(Regex("[^0-9]"), "")
                val db = com.u2tzjtne.telephonehelper.db.RingVideoDatabase.getInstance()
                val assignedRingtoneId = db.phoneRingtoneAssignmentDao().getRingtoneIdByPhone(normalizedNumber)
                
                if (assignedRingtoneId != null) {
                    val ringVideo = db.ringVideoDao().getByIdSync(assignedRingtoneId)
                    ringVideo?.let {
                        ringtoneDuration = it.duration
                        Log.d(TAG, "彩铃时长: ${ringtoneDuration}ms")
                        
                        // 彩铃时长大于 10 秒且小于 60 秒时，调整自动接通时间
                        val newDelay = when {
                            ringtoneDuration > 60_000L -> 60_000L  // 最长等待 60 秒
                            ringtoneDuration > AUTO_CONNECT_DELAY_MILLIS -> ringtoneDuration
                            else -> AUTO_CONNECT_DELAY_MILLIS  // 保持原有 10 秒
                        }
                        
                        if (newDelay > AUTO_CONNECT_DELAY_MILLIS) {
                            // 取消原来的自动接通任务，重新设置
                            autoConnectJob?.cancel()
                            autoConnectJob = lifecycleScope.launch {
                                delay(newDelay)
                                if (!callRecord.isConnected && finishJob?.isActive != true) {
                                    Log.d(TAG, "彩铃已至少播放一次，自动接通")
                                    callStateLD.postValue(CallState.CONNECTED)
                                }
                            }
                            Log.d(TAG, "自动接通时间已调整为: ${newDelay}ms，确保彩铃至少播放一次")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取彩铃时长失败: ${e.message}")
                // 失败时使用默认的自动接通时间
            }
        }
    }

    /**
     * 状态：通话中（已接通）
     * - UI：隐藏状态文字，显示计时器，号码缩小，HD图标
     * - 音频：停止彩铃/等待音
     * - 计时器：开始
     * - 按钮：全功能可用（静音/录音/键盘/免提）
     */
    private fun onStateConnected() {
        if (callRecord.isConnected) return
        callRecord.isConnected = true
        callRecord.connectedTime = System.currentTimeMillis()
        vibrateOnce()

        // 取消预连接调度（若是自动接通则autoConnectJob已触发，若是手动则需取消其余）
        cancelPreConnectJobs()
        stopRingtonePlayback()

        // UI：号码文字缩小
        bind.tvNewCallPlayingRing.visibility = View.GONE
        bind.tvNewCallNumber.apply {
            setTextColor(getColor(R.color.white_70))
            textSize = 20.0f
        }
        bind.tvNewCallNumberLocal.compoundDrawablePadding = 15
        // HD 图标
        bind.tvNewCallNumberLocal.setCompoundDrawables(
            null, null,
            AppCompatResources.getDrawable(this, R.drawable.incall_callcard_speech_hd)!!.apply {
                setBounds(0, 0, 48, 48)
                setTint(getColor(R.color.white_70))
            },
            null
        )

        // 计时器
        showCallStatus(connected = true)

        // 按钮：切换为通话中全功能模式
        setActionAreaEnabled(true)
        setupConnectedActions()
        setBottomBarMode(dialMode = false)
    }

    /**
     * 状态：忙音
     * - 触发条件：预连接阶段用户点击"暂停通话"按钮（模拟对方忙线拒接）
     * - UI：显示"对方正在通话中"
     * - 音频：先播完一次拨号音，再播忙线音，完成后自动挂断
     * - 按钮：全部禁用
     */
    private fun onStateBusy() {
        if (callRecord.isConnected) return
        cancelPreConnectJobs()
        stopRingtonePlayback()
        setActionAreaEnabled(false)
//        setBottomBarDisabled()

        // 根据是否有彩铃显示不同文字
        bind.tvNewCallStatus.text =/* if (hasRingtone) "正在等待对方接听电话" else*/ "正在拨号"
        showCallStatus(connected = false)

        // 先播完一次拨号音，再播忙线音
        playBusyThenFinish()
    }

    /**
     * 状态：无人接听
     * - 触发条件：预连接阶段用户点击"静音"按钮 或 超时自动触发
     * - UI：显示"暂时无人接听"
     * - 音频：播放无人接听音（23.2s后自动结束）
     * - 按钮：全部禁用
     */
    private fun onStateNoAnswer() {
        if (callRecord.isConnected) return

        cancelPreConnectJobs()
        stopRingtonePlayback()
        setActionAreaEnabled(false)
//        setBottomBarDisabled()

        // 根据是否有彩铃显示不同文字
        bind.tvNewCallStatus.text = if (hasRingtone) "正在拨号" else "正在拨号"
        showCallStatus(connected = false)

        MediaPlayerHelper.getInstance().playNoResponseSound(this)

//        noAnswerAudioJob = lifecycleScope.launch {
//            delay(NO_ANSWER_AUDIO_MILLIS)
//            endCallAndFinish(statusText = "正在拨号", needSave = true, delayMillis = 0L)
//        }
    }

    /**
     * 状态：挂断
     * - 触发条件：用户点击挂断按钮 或 通话结束
     * - UI：显示挂断文字，所有控件半透明禁用
     * - 音频：播放挂断音
     * - 计时器：停止
     * - 延迟：3.2s后finish
     */
    private fun onStateHungUp() {
        // 防止重复触发
    }

    // ===================== 核心挂断逻辑 =====================

    /**
     * 用户主动挂断入口
     */
    private fun dispatchHangUp(userInitiated: Boolean) {
        val statusText = if (callRecord.isConnected) "通话结束" else "正在挂断..."
        endCallAndFinish(statusText = statusText, needSave = userInitiated, delayMillis = FINISH_DELAY_MILLIS)
    }

    /**
     * 对方挂断入口（被动挂断）
     * - 状态文字显示"对方已挂断"
     * - 保存通话记录
     */
    private fun dispatchRemoteHangUp() {
        val statusText = if (callRecord.isConnected) "通话结束" else "正在挂断..."
        endCallAndFinish(statusText = statusText, needSave = true, delayMillis = FINISH_DELAY_MILLIS)
    }

    /**
     * 结束通话并延迟关闭页面（防重入）
     *
     * @param statusText     状态栏显示文字
     * @param needSave       是否保存通话记录
     * @param delayMillis    finish前延迟时间（ms）
     */
    private fun endCallAndFinish(statusText: String, needSave: Boolean, delayMillis: Long) {
        // 防重入保护：已在进行中则直接return
        if (finishJob?.isActive == true) return

        cancelAllJobs()
        stopRecordingTimer()
        stopRingtonePlayback()

        // 音频：播放挂断音
        vibrateOnce()
        MediaPlayerHelper.getInstance().playGuaduanSound(this)

        // UI
        bind.tvNewCallStatus.text = statusText
        bind.tvAICallStatus.visibility = View.GONE
        showCallStatus(connected = false)
        bind.cmCallTime.stop()
        setBottomBarDisabled()
        setActionAreaEnabled(false)
        // 通话结束：将6个功能图标和文字统一设为白色（alpha 已由 setActionAreaEnabled 控制半透明）
        val white = ColorStateList.valueOf(Color.WHITE)
        listOf(bind.tvAction0, bind.tvAction1, bind.tvAction2,
               bind.tvAction3, bind.tvAction4).forEach { tv ->
            tv.setTextColor(Color.WHITE)
            tv.compoundDrawableTintList = white
        }
        bind.tvAction5.setTextColor(Color.WHITE)
        listOf(bind.ivAction0, bind.ivAction1,
               bind.ivAction3, bind.ivAction4).forEach { iv ->
            iv.imageTintList = white
        }

        if (needSave) {
            saveCallRecord()
        }

        finishJob = lifecycleScope.launch {
            delay(delayMillis)
            finish()
        }
    }

    // ===================== 忙线辅助 =====================

    private fun vibrateOnce() {
        try {
            val vibrationDurationMs = CallVibrationSettings.getDurationMs()
            if (vibrationDurationMs <= 0) return

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        vibrationDurationMs.toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationDurationMs.toLong())
            }
        } catch (e: Exception) {
            Log.w(TAG, "震动执行失败: ${e.message}")
        }
    }

    private fun playBusyThenFinish() {
        val busyStarted = MediaPlayerHelper.getInstance().playBusySound(this, BUSY_AUDIO_RES_NAME) {
            runOnUiThread {
                endCallAndFinish(statusText = "通话结束", needSave = true, delayMillis = 0L)
            }
        }
        if (!busyStarted) {
            Log.w(TAG, "未找到忙线音资源：$BUSY_AUDIO_RES_NAME")
            endCallAndFinish(statusText = "对方正在通话中", needSave = true, delayMillis = 0L)
        }
    }

    // ===================== 按钮配置 =====================

    /**
     * 预连接阶段按钮配置：
     * - 静音按钮 → 触发无人接听流程（用户不等待了）
     * - 暂停按钮 → 触发忙线拒接流程
     * - 其他按钮 → 不可用
     */
    private fun setupPreConnectActions() {
        bind.llAction0.setOnClickListener(null)
        bind.llAction1.setOnClickListener(null)
        bind.llAction2.setOnClickListener(null)
        bind.llAction5.setOnClickListener(null)

        // 静音：触发无人接听等待
        bind.llAction3.setOnClickListener {
            if (!callRecord.isConnected && finishJob?.isActive != true) {
                cancelPreConnectJobs()
                bind.tvNewCallStatus.text =if(hasRingtone)"正在等待对方接听电话" else "正在拨号"
                noAnswerWaitJob = lifecycleScope.launch {
                    delay(NO_ANSWER_TRIGGER_MILLIS)
                    callStateLD.postValue(CallState.NO_ANSWER)
                }
            }
        }

        // 暂停通话：触发忙线流程
        bind.llAction4.setOnClickListener {
            if (!callRecord.isConnected && finishJob?.isActive != true) {
                callStateLD.postValue(CallState.BUSY)
            }
        }
    }

    /**
     * 通话中按钮配置：全功能可用
     */
    @SuppressLint("UseCompatTextViewDrawableApis")
    private fun setupConnectedActions() {
        // 接通后：视频模式、添加通话、静音、暂停通话 图标和文字变白色
        listOf(
            bind.tvAction0, bind.tvAction1,
            bind.tvAction3, bind.tvAction4
        ).forEach { tv ->
            tv.setTextColor(Color.WHITE)
            tv.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        }
        listOf(
            bind.ivAction0, bind.ivAction1,
            bind.ivAction3, bind.ivAction4
        ).forEach { iv ->
            iv.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
        // 笔记和录音保持常亮（已在 XML 中配置为 callTextPrimary，此处确保不受影响）
        bind.tvAction2.setTextColor(Color.WHITE)
        bind.tvAction5.setTextColor(Color.WHITE)

        // 视频模式：暂不实现，保留点击空操作
        bind.llAction0.setOnClickListener(null)
        // 添加通话：暂不实现
        bind.llAction1.setOnClickListener(null)
        // 笔记：暂不实现
        bind.llAction2.setOnClickListener(null)

        // 静音（已接通阶段）
        bind.llAction3.setOnClickListener {
            toggleMute()
        }

        // 暂停通话（已接通阶段）：切换键盘展开
        bind.llAction4.setOnClickListener {
            toggleDialPad()
        }

        // 录音
        bind.llAction5.setOnClickListener {
            toggleRecording()
        }

        // 免提
        bind.llDialSwitch.setOnClickListener {
            switchSpeaker()
        }

        // 键盘按钮：触发对方挂断流程
        bind.llDialSpeaker.setOnClickListener {
            dispatchRemoteHangUp()
        }

        // 挂断按钮（ll容器）
        bind.llDialHangUp.setOnClickListener {
            dispatchHangUp(userInitiated = true)
        }
        // ivDialHangUp 已在 onCreate 中永久绑定，此处不重复设置
    }

    // ===================== UI 更新辅助 =====================

    /**
     * 切换计时器 / 状态文字显示
     */
    private fun showCallStatus(connected: Boolean) {
        if (connected) {
            bind.cmCallTime.visibility = View.VISIBLE
            bind.cmCallTime.base = SystemClock.elapsedRealtime()
            bind.cmCallTime.start()
            bind.tvNewCallStatus.visibility = View.GONE
        } else {
            bind.tvNewCallStatus.visibility = View.VISIBLE
            bind.cmCallTime.visibility = View.GONE
            bind.cmCallTime.stop()
        }
    }

    /**
     * 设置底部操作栏（免提/挂断/键盘）为禁用状态（半透明）
     */
    private fun setBottomBarDisabled() {
        bind.ivDialSwitch.alpha = 0.5f
        bind.ivDialHangUp.alpha = 0.5f
        bind.ivDialSpeaker.alpha = 0.5f
        bind.llDialSwitch.setOnClickListener(null)
        bind.llDialHangUp.setOnClickListener(null)
        bind.llDialSpeaker.setOnClickListener(null)
        // ivDialHangUp 不清除监听，始终保持可点击挂断
    }

    /**
     * 设置底部按钮为通话模式（非键盘展开模式）
     */
    private fun setBottomBarMode(dialMode: Boolean) {
        // dialMode=true 代表键盘展开状态，暂保留扩展
        bind.ivDialSwitch.alpha = 1.0f
        bind.ivDialHangUp.alpha = 1.0f
        bind.ivDialSpeaker.alpha = 1.0f
        bind.llDialHangUp.setOnClickListener { dispatchHangUp(userInitiated = true) }
    }

    /**
     * 设置 action 功能区（6宫格）是否可用（通过 alpha 反映）
     * 笔记（action_2）和录音（action_5）始终保持常亮，不受状态影响
     */
    private fun setActionAreaEnabled(enabled: Boolean) {
//        val alpha = if (enabled) 1.0f else 0.5f
        val alpha =1.0f
        // 视频模式、添加通话、静音、暂停通话 跟随状态
        bind.llAction0.alpha = alpha
        bind.llAction1.alpha = alpha
        bind.llAction3.alpha = alpha
        bind.llAction4.alpha = alpha
        // 笔记和录音始终常亮
        bind.llAction2.alpha = 1.0f
        bind.llAction5.alpha = 1.0f
    }

    /**
     * 根据是否播放彩铃视频更新界面
     */
    private fun updateUIForRingtoneVideo(isPlayingVideo: Boolean) {
        if (isPlayingVideo) {
            bind.tvNewCallStatus.text = "正在等待对方接听电话"
            bind.tvAICallStatus.visibility = View.GONE
            bind.tvNewCallPlayingRing.visibility = View.VISIBLE
            bind.ivNewCallHead.visibility = View.GONE
        } else {
            bind.ivNewCallHead.visibility = View.VISIBLE
            bind.tvAICallStatus.visibility = View.VISIBLE
            bind.tvNewCallPlayingRing.visibility = View.GONE
        }
    }

    // ===================== 功能操作 =====================

    /**
     * 切换静音
     */
    private fun toggleMute() {
        isJingYin = !isJingYin
        val color = if (isJingYin) "#13A8E1".toColorInt() else Color.WHITE
        bind.tvAction3.setTextColor(color)
        bind.tvAction3.compoundDrawableTintList = ColorStateList.valueOf(color)
        bind.ivAction3.imageTintList = ColorStateList.valueOf(color)
    }

    /**
     * 切换键盘面板显示（ll_dial_root）
     */
    private fun toggleDialPad() {
        val isVisible = bind.llDialRoot.visibility == View.VISIBLE
        bind.llDialRoot.visibility = if (isVisible) View.GONE else View.VISIBLE
    }

    /**
     * 切换录音（仅计时，不实际录音）
     */
    private fun toggleRecording() {
        if (isLuYin) {
            // 停止计时
            isLuYin = false
            bind.tvAction5.setTextColor(Color.WHITE)
            bind.tvAction5.stop()
            bind.tvAction5.text = "录音"
            bind.tvAction5.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        } else {
            // 开始计时（仅模拟录音计时，不申请权限，不实际录音）
            isLuYin = true
            recordingStartTime = SystemClock.elapsedRealtime()
            bind.tvAction5.setTextColor("#13A8E1".toColorInt())
            bind.tvAction5.base = recordingStartTime
            bind.tvAction5.start()
            bind.tvAction5.compoundDrawableTintList =
                ColorStateList.valueOf("#13A8E1".toColorInt())
        }
    }

    /**
     * 切换免提/听筒
     */
    private fun switchSpeaker() {
        isSpeakerOn = !isSpeakerOn
        MediaPlayerHelper.getInstance().switchAudioOutput(this, isSpeakerOn)
        val color = if (isSpeakerOn) "#13A8E1".toColorInt() else Color.WHITE
        bind.ivDialSwitch.setTextColor(color)
    }

    // ===================== 音频控制 =====================

    private fun stopRingtonePlayback() {
        GSYVideoPlayerHelper.getInstance().stopPlaying()
        MediaPlayerHelper.getInstance().stopAudio()
        updateUIForRingtoneVideo(false)
    }

    // ===================== 协程调度管理 =====================

    private fun cancelPreConnectJobs() {
        ringJob?.cancel()
        autoConnectJob?.cancel()
        noAnswerWaitJob?.cancel()
        noAnswerAudioJob?.cancel()
    }

    private fun cancelAllJobs() {
        cancelPreConnectJobs()
        finishJob?.cancel()
    }

    // ===================== 录音计时 =====================

    private fun stopRecordingTimer() {
        if (isLuYin) {
            isLuYin = false
            bind.tvAction5.stop()
        }
    }

    // ===================== 数据存储 =====================

    private fun saveCallRecord() {
        callRecord.endTime = System.currentTimeMillis()
        // 注：不再保存录音文件路径，因为只进行计时模拟，不实际录音

        AppDatabase.getInstance().callRecordModel()
            .insert(callRecord)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.CompletableObserver {
                override fun onSubscribe(d: Disposable) {}
                override fun onComplete() {
                    Log.d(TAG, "通话记录保存成功")
                }
                override fun onError(e: Throwable) {
                    Log.e(TAG, "通话记录保存失败: ${e.message}")
                }
            })
    }

    // ===================== 初始化 =====================

    private fun initCallRecord() {
        callRecord.startTime = System.currentTimeMillis()
        callRecord.phoneNumber = number
        callRecord.callType = 0
    }

    private fun initUI() {
        bind.tvNewCallNumber.text = PhoneNumberFormatUtils.formatWithSpaces(callRecord.phoneNumber)
        bind.tvAction5.text = "录音"
        // 初始状态：挂断按钮可用，其他底部按钮可用
        bind.llDialHangUp.setOnClickListener { dispatchHangUp(userInitiated = true) }
        // ivDialHangUp 已在 onCreate 中永久绑定，此处不重复设置
        bind.llDialSwitch.setOnClickListener { switchSpeaker() }
        // 键盘按钮：触发对方挂断流程（预连接和通话中均可触发）
        bind.llDialSpeaker.setOnClickListener { dispatchRemoteHangUp() }
    }

    private fun initGSYVideo() {
        GSYVideoPlayerHelper.getInstance().init(bind.videoRingtone)
    }

    private fun getNumberData() {
        number = intent.getStringExtra("phoneNumber") ?: ""
        AppDatabase.getInstance().callRecordModel()
            .getByNumber(number)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : MaybeObserver<CallRecord?> {
                override fun onSubscribe(d: Disposable) {}
                override fun onSuccess(oldCallRecord: CallRecord) {
                    if (oldCallRecord.attribution.isNullOrBlank() ||
                        oldCallRecord.attribution == "null" ||
                        oldCallRecord.attribution == "未知"
                    ) {
                        fetchAndSetAttribution()
                    } else {
                        bind.tvNewCallNumberLocal.text =
                            "${oldCallRecord.attribution} ${oldCallRecord.operator}"
                        callRecord.attribution = oldCallRecord.attribution
                        callRecord.operator = oldCallRecord.operator
                    }
                }
                override fun onError(e: Throwable) {
                    Log.d(TAG, "getNumberData onError")
                    fetchAndSetAttribution()
                }
                override fun onComplete() {
                    Log.d(TAG, "getNumberData onComplete")
                    if (callRecord.attribution.isNullOrBlank() ||
                        callRecord.attribution == "null" ||
                        callRecord.attribution == "未知"
                    ) {
                        fetchAndSetAttribution()
                    }
                }
            })
    }

    private fun fetchAndSetAttribution() {
        PhoneNumberUtils.getProvince(number) { bean ->
            val attribution = if (bean.province != bean.city) {
                bean.province + bean.city
            } else {
                bean.province
            }
            callRecord.attribution = attribution
            callRecord.operator = bean.carrier
            bind.tvNewCallNumberLocal.text = "$attribution ${bean.carrier}"
        }
    }

    private fun setBackGround() {
        if (!hasReadStoragePermission()) {
            ToastUtils.s("存储权限未开启，当前将使用默认通话背景")
            return
        }
        val wallpaperManager = WallpaperManager.getInstance(this)
        val wallpaperDrawable = wallpaperManager.drawable
        val wallpaperBitmap = (wallpaperDrawable as BitmapDrawable).bitmap
        bind.root.background = BitmapDrawable(resources, wallpaperBitmap)
    }
}
