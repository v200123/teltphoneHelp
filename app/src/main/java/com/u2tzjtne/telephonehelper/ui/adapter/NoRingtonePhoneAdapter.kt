package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.databinding.ItemNoRingtonePhoneBinding
import com.u2tzjtne.telephonehelper.db.NoRingtonePhone

/**
 * 不显示彩铃号码列表适配器
 */
class NoRingtonePhoneAdapter(
    private val onDeleteClick: (NoRingtonePhone) -> Unit
) : ListAdapter<NoRingtonePhone, NoRingtonePhoneAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNoRingtonePhoneBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNoRingtonePhoneBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NoRingtonePhone) {
            binding.tvPhoneNumber.text = formatPhoneNumber(item.phoneNumber)
            binding.tvAddTime.text = formatTime(item.addTime)
            binding.ivDelete.setOnClickListener { onDeleteClick(item) }
        }

        private fun formatPhoneNumber(phone: String?): String {
            if (phone.isNullOrEmpty()) return ""
            val digits = phone.replace(Regex("[^0-9]"), "")
            return when {
                digits.length == 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
                else -> digits
            }
        }

        private fun formatTime(time: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(time))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NoRingtonePhone>() {
        override fun areItemsTheSame(oldItem: NoRingtonePhone, newItem: NoRingtonePhone): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NoRingtonePhone, newItem: NoRingtonePhone): Boolean {
            return oldItem.phoneNumber == newItem.phoneNumber &&
                    oldItem.addTime == newItem.addTime
        }
    }
}
