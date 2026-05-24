package com.u2tzjtne.telephonehelper.ui.activity

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.databinding.ActivityRingVideoManageBinding
import com.u2tzjtne.telephonehelper.db.RingVideo
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.adapter.RingVideoAdapter
import com.u2tzjtne.telephonehelper.util.BadgeRule
import com.u2tzjtne.telephonehelper.util.RingtoneBadgeRuleStore
import com.u2tzjtne.telephonehelper.util.RingtoneVideoComposer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.InputStream

class RingVideoManageActivity : BaseActivity() {

    private val binding: ActivityRingVideoManageBinding by lazy {
        ActivityRingVideoManageBinding.inflate(layoutInflater)
    }

    private val adapter: RingVideoAdapter by lazy {
        RingVideoAdapter(
            onPreviewClick = ::previewVideo,
            onDeleteClick = ::deleteVideo,
        )
    }

    private var pendingUploadTask: PendingRingtoneUploadTask? = null
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        loadVideos()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnUploadVideo.setOnClickListener {
            if (!isProcessing) {
                openVideoPicker()
            }
        }
        binding.rvVideoList.layoutManager = LinearLayoutManager(this)
        binding.rvVideoList.adapter = adapter
        updateProcessingState(false)
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, REQUEST_PICK_VIDEO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PICK_VIDEO -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uri = data?.data ?: return
                    handlePickedVideo(uri, data.flags)
                }
            }

            REQUEST_PREVIEW_BADGE_RULE -> {
                handlePreviewResult(resultCode, data)
            }
        }
    }

    private fun handlePickedVideo(uri: Uri, flags: Int) {
        updateProcessingState(true, "正在缓存原视频…")
        val videoName = queryDisplayName(uri)
        val mimeType = contentResolver.getType(uri)
        val fileSize = queryFileSize(uri)
        val duration = queryVideoDuration(uri)
        Single.fromCallable {
            tryPersistablePermission(uri, flags)
            val cachedFile = copyImportedVideoToCache(uri, videoName)
            releasePersistablePermission(uri.toString())
            PendingRingtoneUploadTask(
                sourceUriValue = cachedFile.absolutePath,
                videoName = videoName,
                mimeType = mimeType,
                fileSize = cachedFile.length().takeIf { it > 0 } ?: fileSize,
                duration = duration,
            )
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ task ->
                pendingUploadTask = task
                updateProcessingState(false)
                showRuleChoiceDialog()
            }, {
                releasePersistablePermission(uri.toString())
                updateProcessingState(false)
                Toast.makeText(this, "缓存原视频失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun showRuleChoiceDialog() {
        val task = pendingUploadTask ?: return
        val rules = RingtoneBadgeRuleStore.listRules(this)
        val items = buildList {
            add("不使用规则，直接保存原视频")
            rules.forEach { rule ->
                add("${rule.name}（${rule.countAdded()}个图标）")
            }
        }
        AlertDialog.Builder(this)
            .setTitle("选择图标规则")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == 0) {
                    saveVideoRecord(task = task)
                } else {
                    openRulePreview(rules[which - 1], task)
                }
            }
            .setOnCancelListener {
                clearPendingUpload(deleteSourceFile = true)
            }
            .show()
    }

    private fun openRulePreview(rule: BadgeRule, task: PendingRingtoneUploadTask) {
        val snapshot = RingtoneBadgeRuleStore.serializeRule(RingtoneBadgeRuleStore.cloneRule(rule))
        val intent = Intent(this, BadgeRuleEditorActivity::class.java).apply {
            putExtra(BadgeRuleEditorActivity.EXTRA_PREVIEW_MODE, true)
            putExtra(BadgeRuleEditorActivity.EXTRA_RULE_NAME, rule.name)
            putExtra(BadgeRuleEditorActivity.EXTRA_RULE_SNAPSHOT, snapshot)
            putExtra(BadgeRuleEditorActivity.EXTRA_PREVIEW_VIDEO_URI, task.sourceUriValue)
        }
        startActivityForResult(intent, REQUEST_PREVIEW_BADGE_RULE)
    }

    private fun handlePreviewResult(resultCode: Int, data: Intent?) {
        val task = pendingUploadTask ?: return
        if (resultCode != Activity.RESULT_OK) {
            clearPendingUpload(deleteSourceFile = true)
            return
        }
        val rule = RingtoneBadgeRuleStore.deserializeRule(
            data?.getStringExtra(BadgeRuleEditorActivity.EXTRA_RESULT_RULE_SNAPSHOT)
        )
        val previewCanvasWidth = data?.getStringExtra(BadgeRuleEditorActivity.EXTRA_RESULT_CANVAS_WIDTH)?.toIntOrNull() ?: 0
        val previewCanvasHeight = data?.getStringExtra(BadgeRuleEditorActivity.EXTRA_RESULT_CANVAS_HEIGHT)?.toIntOrNull() ?: 0
        if (rule == null || rule.countAdded() == 0) {
            Toast.makeText(this, "规则里没有可烧录图标，已按原视频保存", Toast.LENGTH_SHORT).show()
            saveVideoRecord(task = task)
            return
        }
        composeVideo(task, rule, previewCanvasWidth, previewCanvasHeight)
    }

    private fun composeVideo(task: PendingRingtoneUploadTask, rule: BadgeRule, previewCanvasWidth: Int, previewCanvasHeight: Int) {
        updateProcessingState(true, "正在准备烧录任务…")
        RingtoneVideoComposer(this).compose(
            sourcePath = task.sourceUriValue,
            rule = rule,
            previewCanvasWidth = previewCanvasWidth,
            previewCanvasHeight = previewCanvasHeight,
            callback = object : RingtoneVideoComposer.Callback {
            override fun onStart() {
                updateProcessingState(true, "正在烧录图标…")
            }

            override fun onProgress(progress: Int) {
                updateProcessingState(true, "正在烧录图标…$progress%")
            }

            override fun onSuccess(result: RingtoneVideoComposer.ComposeResult) {
                saveVideoRecord(
                    task = task,
                    playbackUriValue = result.outputFile.absolutePath,
                    mimeType = result.mimeType,
                    fileSize = result.outputSizeBytes,
                    duration = result.durationMs,
                    rule = rule,
                    onPersistFailed = {
                        result.outputFile.delete()
                    }
                )
            }

            override fun onError(message: String) {
                updateProcessingState(false)
                clearPendingUpload(deleteSourceFile = true)
                Toast.makeText(this@RingVideoManageActivity, message, Toast.LENGTH_SHORT).show()
            }

            override fun onCancel() {
                updateProcessingState(false)
                clearPendingUpload(deleteSourceFile = true)
                Toast.makeText(this@RingVideoManageActivity, "已取消图标烧录", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveVideoRecord(
        task: PendingRingtoneUploadTask,
        playbackUriValue: String? = null,
        mimeType: String? = task.mimeType,
        fileSize: Long = task.fileSize,
        duration: Long = task.duration,
        rule: BadgeRule? = null,
        onPersistFailed: (() -> Unit)? = null,
    ) {
        val actualPlaybackUri = playbackUriValue ?: task.sourceUriValue
        Single.fromCallable {
            val dao = RingVideoDatabase.getInstance().ringVideoDao()
            val ringVideo = buildRingVideo(
                task = task,
                sourceUriValue = task.sourceUriValue,
                playbackUriValue = actualPlaybackUri,
                mimeType = mimeType,
                fileSize = fileSize,
                duration = duration,
                rule = rule
            )
            ringVideo.isSelected = dao.getCount() == 0
            dao.insertAndGetId(ringVideo)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ rowId ->
                updateProcessingState(false)
                if (rowId > 0) {
                    clearPendingUpload(deleteSourceFile = false)
                    Toast.makeText(this, if (rule != null) "彩铃视频已烧录并保存" else "彩铃视频已保存", Toast.LENGTH_SHORT).show()
                    loadVideos()
                } else {
                    onPersistFailed?.invoke()
                    clearPendingUpload(deleteSourceFile = true)
                    Toast.makeText(this, "该彩铃视频已经上传过了", Toast.LENGTH_SHORT).show()
                }
            }, {
                updateProcessingState(false)
                onPersistFailed?.invoke()
                clearPendingUpload(deleteSourceFile = true)
                Toast.makeText(this, "保存彩铃视频失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun buildRingVideo(
        task: PendingRingtoneUploadTask,
        sourceUriValue: String,
        playbackUriValue: String,
        mimeType: String?,
        fileSize: Long,
        duration: Long,
        rule: BadgeRule?
    ): RingVideo {
        val ringVideo = RingVideo()
        ringVideo.videoUri = playbackUriValue
        ringVideo.sourceVideoUri = sourceUriValue
        ringVideo.playbackVideoUri = playbackUriValue
        ringVideo.videoName = task.videoName
        ringVideo.mimeType = mimeType
        ringVideo.fileSize = fileSize
        ringVideo.duration = duration
        ringVideo.createdAt = System.currentTimeMillis()
        ringVideo.hasBakedBadge = rule != null
        ringVideo.appliedBadgeRuleName = rule?.name
        ringVideo.appliedBadgeRuleSnapshot = RingtoneBadgeRuleStore.serializeRule(rule)
        return ringVideo
    }

    private fun loadVideos() {
        Single.fromCallable {
            val db = RingVideoDatabase.getInstance()
            val dao = db.ringVideoDao()
            val list = dao.getAllSync()
            val migrated = migrateLegacyVideoRecords(dao, list)
            if (migrated) dao.getAllSync() else list
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                adapter.submitList(list)
                renderList(list)
            }, {
                adapter.submitList(emptyList())
                renderList(emptyList())
                Toast.makeText(this, "读取彩铃视频列表失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun renderList(list: List<RingVideo>) {
        val currentVideo = list.firstOrNull { it.isSelected }
        binding.tvCurrentVideo.text = if (currentVideo != null) {
            buildString {
                append("当前彩铃：")
                append(currentVideo.videoName ?: "未命名视频")
                if (currentVideo.hasBakedBadge && !currentVideo.appliedBadgeRuleName.isNullOrBlank()) {
                    append(" · ")
                    append(currentVideo.appliedBadgeRuleName)
                }
            }
        } else {
            "当前还没有设置彩铃视频"
        }
        binding.tvVideoCount.text = "已上传 ${list.size} 个彩铃视频"
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvVideoList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun deleteVideo(item: RingVideo) {
        Completable.fromAction {
            val db = RingVideoDatabase.getInstance()
            db.runInTransaction {
                val dao = db.ringVideoDao()
                dao.deleteByIdSync(item.id)
            if (item.isSelected) {
                val latestId = dao.getLatestIdSync()
                if (latestId != null) {
                    dao.clearSelectedSync()
                    dao.setSelectedSync(latestId)
                    }
                }
            }
            deleteLocalFiles(item)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Toast.makeText(this, "已删除彩铃视频", Toast.LENGTH_SHORT).show()
                loadVideos()
            }, {
                Toast.makeText(this, "删除彩铃视频失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun previewVideo(item: RingVideo) {
        try {
            val uri = resolvePreviewUri(item.resolvedPlaybackUri)
            if (uri == null) {
                Toast.makeText(this, "暂时无法预览该视频", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType?.takeIf { it.isNotBlank() } ?: "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "暂时无法预览该视频", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolvePreviewUri(pathOrUri: String?): Uri? {
        if (pathOrUri.isNullOrBlank()) {
            return null
        }
        val file = File(pathOrUri)
        return if (file.exists()) {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } else {
            Uri.parse(pathOrUri)
        }
    }

    private fun updateProcessingState(processing: Boolean, message: String = "正在处理视频…") {
        isProcessing = processing
        binding.btnUploadVideo.isEnabled = !processing
        binding.loadingPanel.visibility = if (processing) View.VISIBLE else View.GONE
        binding.tvLoading.text = message
    }

    private fun clearPendingUpload(deleteSourceFile: Boolean) {
        val task = pendingUploadTask
        if (deleteSourceFile && task != null) {
            File(task.sourceUriValue).takeIf { it.exists() }?.delete()
        }
        pendingUploadTask = null
    }

    private fun deleteLocalFiles(item: RingVideo) {
        linkedSetOf(
            item.resolvedSourceUri,
            item.resolvedPlaybackUri
        ).forEach { path ->
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun tryPersistablePermission(uri: Uri, flags: Int) {
        try {
            val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (takeFlags != 0) {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        } catch (_: Exception) {
        }
    }

    private fun releasePersistablePermission(uriValue: String?) {
        if (uriValue.isNullOrBlank()) return
        try {
            val uri = Uri.parse(uriValue)
            contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        }
        return uri.lastPathSegment ?: "未命名视频"
    }

    private fun queryFileSize(uri: Uri): Long {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex)
                }
            }
        }
        return 0L
    }

    private fun queryVideoDuration(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }

    private fun copyImportedVideoToCache(uri: Uri, displayName: String): File {
        val outputFile = createCacheVideoFile("ringtone_video_imports", displayName)
        contentResolver.openInputStream(uri).use { input ->
            if (input == null) {
                throw IllegalStateException("无法读取导入视频")
            }
            input.copyToFile(outputFile)
        }
        return outputFile
    }

    private fun migrateLegacyVideoRecords(
        dao: com.u2tzjtne.telephonehelper.db.RingVideoDao,
        list: List<RingVideo>
    ): Boolean {
        var changed = false
        list.forEach { item ->
            val playbackPath = item.resolvedPlaybackUri
            if (playbackPath.isNullOrBlank() || isManagedCachePath(playbackPath)) {
                return@forEach
            }
            val migratedFile = migrateLegacyPathToCache(playbackPath, item.videoName)
                ?: return@forEach
            val updated = item.apply {
                videoUri = migratedFile.absolutePath
                playbackVideoUri = migratedFile.absolutePath
                if (sourceVideoUri.isNullOrBlank() || !isManagedCachePath(sourceVideoUri)) {
                    sourceVideoUri = migratedFile.absolutePath
                }
                fileSize = migratedFile.length().takeIf { it > 0 } ?: fileSize
            }
            dao.updateSync(updated)
            changed = true
        }
        return changed
    }

    private fun migrateLegacyPathToCache(pathOrUri: String, videoName: String?): File? {
        return runCatching {
            val sourceFile = File(pathOrUri)
            val targetFile = createCacheVideoFile(
                folderName = "ringtone_video_imports",
                displayName = videoName ?: sourceFile.name.ifBlank { "ringtone_video.mp4" }
            )
            if (sourceFile.exists()) {
                sourceFile.inputStream().use { input ->
                    input.copyToFile(targetFile)
                }
            } else {
                contentResolver.openInputStream(Uri.parse(pathOrUri)).use { input ->
                    if (input == null) {
                        return null
                    }
                    input.copyToFile(targetFile)
                }
            }
            targetFile
        }.getOrNull()
    }

    private fun createCacheVideoFile(folderName: String, displayName: String): File {
        val extension = displayName.substringAfterLast('.', "").ifBlank { "mp4" }
        val safeBaseName = displayName.substringBeforeLast('.').ifBlank { "ringtone_video" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val cacheDir = File(cacheDir, folderName).apply {
            if (!exists()) {
                mkdirs()
            }
        }
        return File(cacheDir, "${safeBaseName}_${System.currentTimeMillis()}.$extension")
    }

    private fun isManagedCachePath(path: String?): Boolean {
        if (path.isNullOrBlank()) {
            return false
        }
        val file = File(path)
        return file.absolutePath.startsWith(cacheDir.absolutePath)
    }

    private fun InputStream.copyToFile(target: File) {
        target.outputStream().use { output ->
            copyTo(output)
        }
    }

    private data class PendingRingtoneUploadTask(
        val sourceUriValue: String,
        val videoName: String,
        val mimeType: String?,
        val fileSize: Long,
        val duration: Long,
    )

    companion object {
        private const val REQUEST_PICK_VIDEO = 1001
        private const val REQUEST_PREVIEW_BADGE_RULE = 1002
    }
}
