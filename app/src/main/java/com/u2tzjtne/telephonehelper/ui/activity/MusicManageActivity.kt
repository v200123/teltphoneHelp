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
import com.u2tzjtne.telephonehelper.databinding.ActivityMusicManageBinding
import com.u2tzjtne.telephonehelper.db.MusicFile
import com.u2tzjtne.telephonehelper.db.RingVideoDatabase
import com.u2tzjtne.telephonehelper.ui.adapter.MusicAdapter
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MusicManageActivity : BaseActivity() {

    private val binding: ActivityMusicManageBinding by lazy {
        ActivityMusicManageBinding.inflate(layoutInflater)
    }

    private val adapter: MusicAdapter by lazy {
        MusicAdapter(
            onPreviewClick = ::previewMusic,
            onDeleteClick = ::deleteMusic,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        loadMusics()
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnUploadMusic.setOnClickListener { openMusicPicker() }
        binding.rvMusicList.layoutManager = LinearLayoutManager(this)
        binding.rvMusicList.adapter = adapter
    }

    private fun openMusicPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        startActivityForResult(intent, REQUEST_PICK_MUSIC)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_MUSIC && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            saveMusic(uri, data.flags)
        }
    }

    private fun saveMusic(uri: Uri, flags: Int) {
        tryPersistablePermission(uri, flags)
        Single.fromCallable {
            val dao = RingVideoDatabase.getInstance().musicFileDao()
            val musicFile = buildMusicFile(uri)
            musicFile.isSelected = dao.getCount() == 0
            dao.insertAndGetId(musicFile)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ rowId ->
                if (rowId > 0) {
                    Toast.makeText(this, "音乐已保存", Toast.LENGTH_SHORT).show()
                    loadMusics()
                } else {
                    Toast.makeText(this, "该音乐已经上传过了", Toast.LENGTH_SHORT).show()
                }
            }, {
                Toast.makeText(this, "保存音乐失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun buildMusicFile(uri: Uri): MusicFile {
        val musicFile = MusicFile()
        musicFile.audioUri = uri.toString()
        musicFile.audioName = queryDisplayName(uri)
        musicFile.mimeType = contentResolver.getType(uri)
        musicFile.fileSize = queryFileSize(uri)
        musicFile.duration = queryAudioDuration(uri)
        musicFile.createdAt = System.currentTimeMillis()
        return musicFile
    }

    private fun loadMusics() {
        Single.fromCallable {
            RingVideoDatabase.getInstance().musicFileDao().getAllSync()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                adapter.submitList(list)
                renderList(list)
            }, {
                adapter.submitList(emptyList())
                renderList(emptyList())
                Toast.makeText(this, "读取音乐列表失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun renderList(list: List<MusicFile>) {
        val currentMusic = list.firstOrNull { it.isSelected }
        binding.tvCurrentMusic.text = if (currentMusic != null) {
            "当前音乐：${currentMusic.audioName ?: "未命名音乐"}"
        } else {
            "当前还没有设置音乐"
        }
        binding.tvMusicCount.text = "已上传 ${list.size} 个音乐文件"
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvMusicList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun deleteMusic(item: MusicFile) {
        Completable.fromAction {
            val db = RingVideoDatabase.getInstance()
            db.runInTransaction {
                val dao = db.musicFileDao()
                dao.deleteByIdSync(item.id)
                if (item.isSelected) {
                    val latestId = dao.getLatestIdSync()
                    if (latestId != null) {
                        dao.clearSelectedSync()
                        dao.setSelectedSync(latestId)
                    }
                }
            }
            releasePersistablePermission(item.audioUri)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Toast.makeText(this, "已删除音乐", Toast.LENGTH_SHORT).show()
                loadMusics()
            }, {
                Toast.makeText(this, "删除音乐失败", Toast.LENGTH_SHORT).show()
            })
    }

    private fun previewMusic(item: MusicFile) {
        try {
            val uri = Uri.parse(item.audioUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType?.takeIf { it.isNotBlank() } ?: "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "暂时无法试听该音乐", Toast.LENGTH_SHORT).show()
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
        return uri.lastPathSegment ?: "未命名音乐"
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

    private fun queryAudioDuration(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (_: Exception) {
            0L
        }
    }

    companion object {
        private const val REQUEST_PICK_MUSIC = 1002
    }
}
