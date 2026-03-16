package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.db.RingVideo

/**
 * 选择彩铃对话框适配器
 */
class SelectRingtoneAdapter(
    private val onItemClick: (RingVideo) -> Unit
) : RecyclerView.Adapter<SelectRingtoneAdapter.ViewHolder>() {

    private val dataList = mutableListOf<RingVideo>()
    private var selectedId: Int = -1
    private var bindCounts: Map<Int, Int> = emptyMap()

    fun submitList(list: List<RingVideo>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    fun setSelectedId(id: Int) {
        selectedId = id
        notifyDataSetChanged()
    }

    fun setBindCounts(counts: Map<Int, Int>) {
        bindCounts = counts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_ringtone, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemCount(): Int = dataList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val llRoot: LinearLayout = itemView.findViewById(R.id.llRoot)
        private val rbSelect: RadioButton = itemView.findViewById(R.id.rbSelect)
        private val tvVideoName: TextView = itemView.findViewById(R.id.tvVideoName)
        private val tvVideoInfo: TextView = itemView.findViewById(R.id.tvVideoInfo)
        private val tvBindCount: TextView = itemView.findViewById(R.id.tvBindCount)

        fun bind(item: RingVideo) {
            tvVideoName.text = item.videoName ?: "未命名视频"
            tvVideoInfo.text = "${item.getFormattedFileSize()} | ${item.getFormattedDuration()}"
            
            // 显示绑定数量
            val count = bindCounts[item.id] ?: 0
            tvBindCount.text = "已绑定 ${count} 个"
            tvBindCount.visibility = if (count > 0) View.VISIBLE else View.GONE
            
            // 设置选中状态
            rbSelect.isChecked = item.id == selectedId
            
            // 点击事件
            llRoot.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}