package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.databinding.ItemMusicBinding
import com.u2tzjtne.telephonehelper.db.MusicFile
import com.u2tzjtne.telephonehelper.util.DateUtils

class MusicAdapter(
    private val onPreviewClick: (MusicFile) -> Unit,
    private val onDeleteClick: (MusicFile) -> Unit,
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private val data = mutableListOf<MusicFile>()

    fun submitList(list: List<MusicFile>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class MusicViewHolder(private val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MusicFile) {
            binding.tvMusicName.text = item.audioName?.takeIf { it.isNotBlank() } ?: "未命名音乐"
            binding.tvMusicPath.text = item.audioUri ?: ""

            val metaParts = mutableListOf<String>()
            if (item.duration > 0) {
                metaParts.add("时长 ${item.formattedDuration}")
            }
            if (item.fileSize > 0) {
                metaParts.add("大小 ${item.formattedFileSize}")
            }
            if (item.createdAt > 0) {
                metaParts.add("上传 ${DateUtils.convertTimestamp(item.createdAt, false)}")
            }
            binding.tvMusicMeta.text = metaParts.joinToString(" · ")

            binding.btnPreview.setOnClickListener { onPreviewClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
