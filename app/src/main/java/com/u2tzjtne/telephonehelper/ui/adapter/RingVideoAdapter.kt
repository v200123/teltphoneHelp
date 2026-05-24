package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.databinding.ItemRingVideoBinding
import com.u2tzjtne.telephonehelper.db.RingVideo
import com.u2tzjtne.telephonehelper.util.DateUtils

class RingVideoAdapter(
    private val onPreviewClick: (RingVideo) -> Unit,
    private val onDeleteClick: (RingVideo) -> Unit,
) : RecyclerView.Adapter<RingVideoAdapter.RingVideoViewHolder>() {

    private val data = mutableListOf<RingVideo>()

    fun submitList(list: List<RingVideo>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RingVideoViewHolder {
        val binding = ItemRingVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RingVideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RingVideoViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class RingVideoViewHolder(private val binding: ItemRingVideoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RingVideo) {
            binding.tvVideoName.text = item.videoName?.takeIf { it.isNotBlank() } ?: "未命名视频"
            binding.tvVideoName.isSelected = true
            binding.tvVideoPath.text = buildString {
                append("播放：")
                append(item.resolvedPlaybackUri ?: "")
                val sourceUri = item.resolvedSourceUri
                if (!sourceUri.isNullOrBlank() && sourceUri != item.resolvedPlaybackUri) {
                    append("\n原始：")
                    append(sourceUri)
                }
            }

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
            if (item.hasBakedBadge) {
                metaParts.add("已烧录${item.appliedBadgeRuleName?.let { " ${it}" } ?: ""}")
            } else {
                metaParts.add("原视频")
            }
            binding.tvVideoMeta.text = metaParts.joinToString(" · ")

            binding.btnPreview.setOnClickListener { onPreviewClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
