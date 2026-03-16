package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.db.RingtonePhoneBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 彩铃号码绑定列表适配器
 */
class RingtoneBindingAdapter(
    private val onDeleteClick: (RingtonePhoneBinding) -> Unit
) : RecyclerView.Adapter<RingtoneBindingAdapter.ViewHolder>() {

    private val dataList = mutableListOf<RingtonePhoneBinding>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun submitList(list: List<RingtonePhoneBinding>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ringtone_binding, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemCount(): Int = dataList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPhoneNumber: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        private val tvBindTime: TextView = itemView.findViewById(R.id.tvBindTime)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

        fun bind(item: RingtonePhoneBinding) {
            tvPhoneNumber.text = formatPhoneNumber(item.phoneNumber)
            tvBindTime.text = dateFormat.format(Date(item.bindTime))
            ivDelete.setOnClickListener { onDeleteClick(item) }
        }

        private fun formatPhoneNumber(phone: String?): String {
            if (phone.isNullOrEmpty()) return ""
            // 格式化为 138 1234 5678
            return when {
                phone.length == 11 -> "${phone.substring(0, 3)} ${phone.substring(3, 7)} ${phone.substring(7)}"
                else -> phone
            }
        }
    }
}