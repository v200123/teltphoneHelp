package com.u2tzjtne.telephonehelper.ui.activity

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import com.u2tzjtne.telephonehelper.databinding.ActivityCallRecordingDetailBinding
import com.u2tzjtne.telephonehelper.db.AppDatabase
import com.u2tzjtne.telephonehelper.db.CallRecord
import com.u2tzjtne.telephonehelper.util.DateUtils
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

class CallRecordingDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityCallRecordingDetailBinding
    private var callRecordId: Int = -1
    private var callRecord: CallRecord? = null
    private var currentAudioPath: String? = null
    private var currentRealDurationMs: Long = 0L
    private var mediaPlayer: MediaPlayer? = null

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
        binding.btnPickAudio.setOnClickListener { openAudioPicker() }
        binding.btnPlayAudio.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                stopAudioPlayback()
            } else {
                playCurrentAudio()
            }
        }
        binding.btnSave.setOnClickListener { saveRecordingInfo() }
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
        binding.tvCallNumber.text = "号码：${record.phoneNumber}"
        binding.tvCallTime.text = "通话时间：${DateUtils.convertTimestamp(record.startTime, false)}"
        binding.tvCallStatus.text = "通话状态：${buildCallStatus(record)}"

        currentAudioPath = record.recordingPath
        currentRealDurationMs = if (!currentAudioPath.isNullOrBlank()) {
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

    private fun buildCallStatus(record: CallRecord): String {
        if (record.isConnected) {
            val duration = DateUtils.getCallDuration(record.endTime - record.connectedTime)
            return if (record.callType == 0) {
                "呼出，已接通（$duration）"
            } else {
                "呼入，已接通（$duration）"
            }
        }
        return if (record.callType == 1) {
            "呼入，响铃${record.callNumber}声"
        } else {
            "呼出，未接通"
        }
    }

    private fun updateRecordingSection() {
        val durationMs = getCurrentDisplayDurationMs()
        val hasFile = !currentAudioPath.isNullOrBlank()
        val hasDuration = durationMs > 0L

        binding.tvRecordingStatus.text = when {
            hasFile && hasDuration -> "录音状态：已配置本地录音"
            hasFile -> "录音状态：已选择本地录音文件"
            hasDuration -> "录音状态：仅设置了伪造时长"
            else -> "录音状态：暂无录音"
        }
        binding.tvRecordingDuration.text = if (hasDuration) {
            "录音时长：${DateUtils.getCallDuration(durationMs)}"
        } else {
            "录音时长：暂无"
        }
        binding.tvRecordingFile.text = if (hasFile) {
            "录音文件：${resolveDisplayName(currentAudioPath)}"
        } else {
            "录音文件：未选择"
        }
        binding.tvRecordingTip.text = when {
            hasFile -> "你可以直接播放当前录音，也可以重新上传文件或修改伪造时长。"
            hasDuration -> "当前没有实际音频文件，只展示一个伪造的录音时长。"
            else -> "当前通话记录还没有录音，点击下方按钮即可增加录音文件。"
        }
        binding.btnPickAudio.text = if (hasFile) "更换本地录音文件" else "增加录音文件"
        binding.btnPlayAudio.visibility = if (hasFile) View.VISIBLE else View.GONE
        if (binding.btnPlayAudio.visibility == View.VISIBLE && mediaPlayer?.isPlaying != true) {
            binding.btnPlayAudio.text = "播放当前录音"
        }
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
        currentAudioPath = uri.toString()
        currentRealDurationMs = resolveAudioDuration(uri)
        if (currentRealDurationMs > 0L && binding.etFakeDuration.text?.toString()?.trim().isNullOrEmpty()) {
            binding.etFakeDuration.setText((currentRealDurationMs / 1000).toString())
        }
        stopAudioPlayback()
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

    private fun playCurrentAudio() {
        val audioPath = currentAudioPath
        if (audioPath.isNullOrBlank()) {
            showToast("当前没有可播放的录音文件")
            return
        }
        try {
            stopAudioPlayback()
            val uri = buildUri(audioPath)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@CallRecordingDetailActivity, uri)
                setOnCompletionListener {
                    stopAudioPlayback()
                }
                prepare()
                start()
            }
            binding.btnPlayAudio.text = "停止播放"
        } catch (e: Exception) {
            stopAudioPlayback()
            showToast("录音文件无法播放")
        }
    }

    private fun stopAudioPlayback() {
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
        if (::binding.isInitialized) {
            binding.btnPlayAudio.text = "播放当前录音"
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

    private fun resolveDisplayName(path: String?): String {
        if (path.isNullOrBlank()) {
            return "未选择"
        }
        return try {
            val uri = buildUri(path)
            if ("content" == uri.scheme) {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && columnIndex >= 0) {
                        return cursor.getString(columnIndex)
                    }
                }
            }
            val file = if ("file" == uri.scheme) {
                File(uri.path ?: path)
            } else {
                File(path)
            }
            if (file.name.isNotBlank()) file.name else path
        } catch (_: Exception) {
            path
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
