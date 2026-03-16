package com.u2tzjtne.telephonehelper.ui.activity

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.u2tzjtne.telephonehelper.databinding.ActivityRingVideoManageBinding
import com.u2tzjtne.telephonehelper.db.RingVideo
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.adapter.RingVideoAdapter
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class RingVideoManageActivity : BaseActivity() {

    private val binding: ActivityRingVideoManageBinding by lazy {
        ActivityRingVideoManageBinding.inflate(layoutInflater)
    }

    private val adapter: RingVideoAdapter by lazy {
        RingVideoAdapter(
            onBindPhoneClick = ::openBindPhoneActivity,
            onPreviewClick = ::previewVideo,
            onDeleteClick = ::deleteVideo,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        loadVideos()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnUploadVideo.setOnClickListener { openVideoPicker() }
        binding.rvVideoList.layoutManager = LinearLayoutManager(this)
        binding.rvVideoList.adapter = adapter
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
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            saveVideo(uri, data.flags)
        }
    }

    private fun saveVideo(uri: Uri, flags: Int) {
        tryPersistablePermission(uri, flags)
        Single.fromCallable {
            val dao = RingVideoDatabase.getInstance().ringVideoDao()
            val ringVideo = buildRingVideo(uri)
            ringVideo.isSelected = dao.getCount() == 0
            dao.insertAndGetId(ringVideo)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ rowId ->
                if (rowId > 0) {
                    Toast.makeText(this, "彩铃视频已保存", Toast.LENGTH_SHORT).show()
                    loadVideos()
                } else {
                    Toast.makeText(this, "该彩铃视频已经上传过了", Toast.LENGTH_SHORT).show()
                }
            }, {
                Toast.makeText(this, "保存彩铃视频失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun buildRingVideo(uri: Uri): RingVideo {
        val ringVideo = RingVideo()
        ringVideo.videoUri = uri.toString()
        ringVideo.videoName = queryDisplayName(uri)
        ringVideo.mimeType = contentResolver.getType(uri)
        ringVideo.fileSize = queryFileSize(uri)
        ringVideo.duration = queryVideoDuration(uri)
        ringVideo.createdAt = System.currentTimeMillis()
        return ringVideo
    }

    private fun loadVideos() {
        Single.fromCallable {
            RingVideoDatabase.getInstance().ringVideoDao().getAllSync()
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
            "当前彩铃：${currentVideo.videoName ?: "未命名视频"}"
        } else {
            "当前还没有设置彩铃视频"
        }
        binding.tvVideoCount.text = "已上传 ${list.size} 个彩铃视频"
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvVideoList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * 打开彩铃号码绑定管理界面
     */
    private fun openBindPhoneActivity(item: RingVideo) {
        val intent = Intent(this, RingtoneBindingManageActivity::class.java).apply {
            putExtra("ringtone_id", item.id)
            putExtra("ringtone_name", item.videoName)
        }
        startActivity(intent)
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
            releasePersistablePermission(item.videoUri)
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
            val uri = Uri.parse(item.videoUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType?.takeIf { it.isNotBlank() } ?: "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "暂时无法预览该视频", Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val REQUEST_PICK_VIDEO = 1001
    }
}
