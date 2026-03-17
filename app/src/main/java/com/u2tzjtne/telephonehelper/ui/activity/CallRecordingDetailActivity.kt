package com.u2tzjtne.telephonehelper.ui.activity

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.u2tzjtne.telephonehelper.databinding.ActivityCallRecordingDetailBinding
import com.u2tzjtne.telephonehelper.db.AppDatabase
import com.u2tzjtne.telephonehelper.db.CallRecord
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean
import com.u2tzjtne.telephonehelper.util.DateUtils
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.ihxq.projects.pna.ISP
import me.ihxq.projects.pna.PhoneNumberLookup
import java.io.File

class CallRecordingDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityCallRecordingDetailBinding
    private var callRecordId: Int = -1
    private var callRecord: CallRecord? = null
    private var currentAudioPath: String? = null
    private var currentRealDurationMs: Long = 0L
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null
    private val phoneNumberLookup = PhoneNumberLookup()

    companion object {
        private const val EXTRA_CALL_RECORD_ID = "callRecordId"
        private const val REQUEST_PICK_AUDIO = 1001

        fun start(context: Context, callRecordId: Int) {
            val intent = Intent(context, CallRecordingDetailActivity::class.java)
            intent.putExtra(EXTRA_CALL_RECORD_ID, callRecordId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallRecordingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callRecordId = intent.getIntExtra(EXTRA_CALL_RECORD_ID, -1)
        if (callRecordId <= 0) {
            showToast("未找到对应的通话记录")
            finish()
            return
        }

        initView()
        loadCallRecord()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnCall.setOnClickListener { onCallClicked() }
        binding.btnSms.setOnClickListener { onSmsClicked() }
        binding.layoutCallNote.setOnClickListener { onCallNoteClicked() }
        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnPickAudio.setOnClickListener { openAudioPicker() }
        binding.btnSave.setOnClickListener { saveRecordingInfo() }

        binding.seekbarRecording.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    val duration = mediaPlayer?.duration ?: 0
                    val position = (duration * progress / 100f).toInt()
                    mediaPlayer?.seekTo(position)
                    updateCurrentTime(position)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun onCallClicked() {
        val phoneNumber = callRecord?.phoneNumber ?: return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        startActivity(intent)
    }

    private fun onSmsClicked() {
        val phoneNumber = callRecord?.phoneNumber ?: return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
        startActivity(intent)
    }

    private fun onCallNoteClicked() {
        showToast("通话笔记功能开发中")
    }

    private fun loadCallRecord() {
        Maybe.fromCallable {
            AppDatabase.getInstance().callRecordModel().getById(callRecordId)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ record ->
                callRecord = record
                bindCallRecord(record)
            }, {
                showToast("读取通话记录失败")
                finish()
            }, {
                showToast("通话记录不存在")
                finish()
            })
    }

    private fun bindCallRecord(record: CallRecord) {
        // 设置通话时间（跨天显示日期，当天显示时间）
        binding.tvCallTime.text = formatCallTime(record.startTime)
        
        // 设置通话状态
        binding.tvCallStatus.text = buildCallStatusText(record)

        // 设置电话号码（不带空格）
        binding.tvCallNumber.text = record.phoneNumber.replace(" ", "")



        // 加载录音信息
        // 修复：空字符串和 "null" 字符串都应该被视为无效
        currentAudioPath = record.recordingPath?.takeIf { it.isNotBlank() && it != "null" }
        currentRealDurationMs = if (currentAudioPath != null) {
            resolveAudioDuration(buildUri(currentAudioPath!!))
        } else {
            0L
        }

        val storedDurationMs = getStoredDurationMs(record)
        when {
            storedDurationMs > 0L -> binding.etFakeDuration.setText((storedDurationMs / 1000).toString())
            currentRealDurationMs > 0L -> binding.etFakeDuration.setText((currentRealDurationMs / 1000).toString())
            else -> binding.etFakeDuration.setText("")
        }
        updateRecordingSection()
    }

    private fun formatCallTime(timestamp: Long): String {
        val instant = java.time.Instant.ofEpochMilli(timestamp)
        val dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.of("Asia/Shanghai"))
        val today = java.time.LocalDate.now()
        val date = dateTime.toLocalDate()
        
        return if (date.equals(today)) {
            // 当天，只显示时:分
            dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            // 跨天，显示完整日期时间：3月17日 16:47:38
            dateTime.format(java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm:ss"))
        }
    }

    private fun buildLocationText(phoneNumber: String): String {
        val cleanNumber = phoneNumber.replace(" ", "")
        if (cleanNumber.length < 7) {
            return "手机  |  ..."
        }

        return try {
            val info = phoneNumberLookup.lookup(cleanNumber)
            info.map { phoneInfo ->
                val attribution = phoneInfo.attribution
                val location = if (attribution.province == attribution.city) {
                    attribution.province
                } else {
                    attribution.province + attribution.city
                }
                val carrier = phoneInfo.isp.cnName.replace("中国", "")
                "手机  |  $location $carrier  |  ..."
            }.orElse("手机  |  ...")
        } catch (e: Exception) {
            "手机  |  ..."
        }
    }

    private fun buildCallStatusText(record: CallRecord): String {
        val typeText = if (record.callType == 0) "呼出" else "呼入"
        
        if (record.isConnected) {
            val duration = record.endTime - record.connectedTime
            val durationText = formatDurationSimple(duration)
            return "$typeText$durationText"
        }
        
        return if (record.callType == 1) {
            val ringCount = (record.endTime - record.startTime) / 3000 // 约3秒一声
            "$typeText 响铃${ringCount}声"
        } else {
            "$typeText 未接通"
        }
    }

    private fun formatDurationSimple(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) {
            "${minutes}分${seconds}秒"
        } else {
            "${seconds}秒"
        }
    }

    private fun updateRecordingSection() {
        val durationMs = getCurrentDisplayDurationMs()
        // 修复：空字符串和 "null" 字符串都应该被视为无效
        val hasFile = !currentAudioPath.isNullOrBlank() && currentAudioPath != "null"
        val hasDuration = durationMs > 0L
        // 有录音文件或有录音时长（包括伪造），都认为已有录音
        val hasRecording = hasFile || hasDuration

        // 更新录音时长显示
        binding.tvRecordingDuration.text = formatDurationTime(durationMs)

        // 更新播放控制区域的可见性
        if (hasRecording) {
            // 有录音：显示播放器，隐藏上传和伪造区域
            binding.layoutRecordingPlayer.visibility = View.VISIBLE
            binding.tvCurrentTime.visibility = View.VISIBLE
            binding.tvNoRecording.visibility = View.GONE
            binding.btnPlayPause.visibility = if (hasFile) View.VISIBLE else View.GONE
            // 隐藏上传和伪造区域
            binding.btnPickAudio.visibility = View.GONE
            binding.layoutFakeDuration.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
        } else {
            // 无录音无时长
            binding.layoutRecordingPlayer.visibility = View.GONE
            binding.tvCurrentTime.visibility = View.GONE
            binding.tvNoRecording.visibility = View.VISIBLE
            // 显示上传和伪造区域
            binding.btnPickAudio.visibility = View.VISIBLE
            binding.layoutFakeDuration.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE
        }

        // 重置播放状态
        binding.seekbarRecording.progress = 0
        binding.tvCurrentTime.text = "00:00"
        updatePlayPauseIcon(false)

        // 更新按钮文本
        binding.btnPickAudio.text = if (hasFile) "更换本地录音文件" else "添加录音文件"
    }

    private fun formatDurationTime(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val totalSeconds = (durationMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            pauseAudio()
        } else {
            playAudio()
        }
    }

    private fun playAudio() {
        val audioPath = currentAudioPath
        if (audioPath.isNullOrBlank()) {
            showToast("当前没有可播放的录音文件")
            return
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@CallRecordingDetailActivity, buildUri(audioPath))
                    setOnCompletionListener {
                        onAudioCompleted()
                    }
                    prepare()
                }
            }

            mediaPlayer?.start()
            isPlaying = true
            updatePlayPauseIcon(true)
            startSeekBarUpdate()
        } catch (e: Exception) {
            stopAudioPlayback()
            showToast("录音文件无法播放")
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                updatePlayPauseIcon(false)
                stopSeekBarUpdate()
            }
        }
    }

    private fun onAudioCompleted() {
        isPlaying = false
        updatePlayPauseIcon(false)
        stopSeekBarUpdate()
        binding.seekbarRecording.progress = 0
        binding.tvCurrentTime.text = "00:00"
        mediaPlayer?.seekTo(0)
    }

    private fun startSeekBarUpdate() {
        stopSeekBarUpdate()
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val duration = player.duration
                        val position = player.currentPosition
                        val progress = (position * 100f / duration).toInt()
                        binding.seekbarRecording.progress = progress
                        updateCurrentTime(position)
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun stopSeekBarUpdate() {
        updateSeekBarRunnable?.let {
            handler.removeCallbacks(it)
        }
        updateSeekBarRunnable = null
    }

    private fun updateCurrentTime(positionMs: Int) {
        val totalSeconds = positionMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.tvCurrentTime.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        // 可以根据播放状态切换不同的图标
        // binding.btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause_gray else R.drawable.ic_play_gray)
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_PICK_AUDIO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_AUDIO || resultCode != RESULT_OK) {
            return
        }
        val uri = data?.data ?: return
        val takeFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (takeFlags != 0) {
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: SecurityException) {
            }
        }
        
        stopAudioPlayback()
        currentAudioPath = uri.toString()
        currentRealDurationMs = resolveAudioDuration(uri)
        
        if (currentRealDurationMs > 0L && binding.etFakeDuration.text?.toString()?.trim().isNullOrEmpty()) {
            binding.etFakeDuration.setText((currentRealDurationMs / 1000).toString())
        }
        updateRecordingSection()
    }

    private fun saveRecordingInfo() {
        val record = callRecord ?: return
        val fakeDurationMs = getFakeDurationMs()
        val storedDurationMs = getStoredDurationMs(record)
        val finalDurationMs = when {
            fakeDurationMs > 0L -> fakeDurationMs
            currentRealDurationMs > 0L -> currentRealDurationMs
            storedDurationMs > 0L -> storedDurationMs
            else -> 0L
        }
        val finalPath = currentAudioPath?.takeIf { it.isNotBlank() }

        if (finalPath.isNullOrBlank() && finalDurationMs <= 0L) {
            showToast("请先上传录音文件，或至少填写一个假的录音时长")
            return
        }

        val startTime = if (finalDurationMs > 0L) {
            when {
                record.connectedTime > 0L -> record.connectedTime
                record.startTime > 0L -> record.startTime
                else -> System.currentTimeMillis()
            }
        } else {
            0L
        }
        val endTime = if (finalDurationMs > 0L) startTime + finalDurationMs else 0L

        AppDatabase.getInstance().callRecordModel()
            .updateRecordingPath(record.id, finalPath, startTime, endTime)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                record.recordingPath = finalPath
                record.recordingStartTime = startTime
                record.recordingEndTime = endTime
                setResult(RESULT_OK)
                showToast("录音信息已保存")
                stopAudioPlayback()
                updateRecordingSection()
            }, {
                showToast("保存失败，请稍后重试")
            })
    }

    private fun stopAudioPlayback() {
        stopSeekBarUpdate()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (_: Exception) {
            }
            try {
                it.reset()
            } catch (_: Exception) {
            }
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
        if (::binding.isInitialized) {
            updatePlayPauseIcon(false)
        }
    }

    private fun getFakeDurationMs(): Long {
        val seconds = binding.etFakeDuration.text?.toString()?.trim()?.toLongOrNull() ?: 0L
        return if (seconds > 0L) seconds * 1000 else 0L
    }

    private fun getCurrentDisplayDurationMs(): Long {
        val fakeDurationMs = getFakeDurationMs()
        if (fakeDurationMs > 0L) {
            return fakeDurationMs
        }
        if (currentRealDurationMs > 0L) {
            return currentRealDurationMs
        }
        val record = callRecord ?: return 0L
        return getStoredDurationMs(record)
    }

    private fun getStoredDurationMs(record: CallRecord): Long {
        val duration = record.recordingEndTime - record.recordingStartTime
        return if (duration > 0L) duration else 0L
    }

    private fun resolveAudioDuration(uri: Uri): Long {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun buildUri(path: String): Uri {
        val lowerCasePath = path.lowercase()
        return when {
            lowerCasePath.startsWith("content://") || lowerCasePath.startsWith("file://") || lowerCasePath.startsWith("android.resource://") -> Uri.parse(path)
            else -> Uri.fromFile(File(path))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        stopAudioPlayback()
        super.onDestroy()
    }
}
