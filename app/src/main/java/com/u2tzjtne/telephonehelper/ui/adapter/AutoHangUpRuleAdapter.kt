package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.databinding.ItemAutoHangUpRuleBinding
import com.u2tzjtne.telephonehelper.db.AutoHangUpRule

class AutoHangUpRuleAdapter(
    private val onEditClick: (AutoHangUpRule) -> Unit,
    private val onDeleteClick: (AutoHangUpRule) -> Unit,
) : ListAdapter<AutoHangUpRule, AutoHangUpRuleAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAutoHangUpRuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAutoHangUpRuleBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AutoHangUpRule) {
            binding.tvPhoneNumber.text = formatPhoneNumber(item.phoneNumber)
            binding.tvDelaySeconds.text = "接通后 ${item.hangUpDelaySeconds} 秒挂断"
            binding.root.setOnClickListener { onEditClick(item) }
            binding.ivDelete.setOnClickListener { onDeleteClick(item) }
        }

        private fun formatPhoneNumber(phone: String): String {
            val digits = phone.replace(Regex("[^0-9]"), "")
            return when {
                digits.length == 11 -> "${digits.substring(0, 3)} ${digits.substring(3, 7)} ${digits.substring(7)}"
                else -> digits
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AutoHangUpRule>() {
        override fun areItemsTheSame(oldItem: AutoHangUpRule, newItem: AutoHangUpRule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AutoHangUpRule, newItem: AutoHangUpRule): Boolean {
            return oldItem.id == newItem.id &&
                oldItem.phoneNumber == newItem.phoneNumber &&
                oldItem.hangUpDelaySeconds == newItem.hangUpDelaySeconds &&
                oldItem.addTime == newItem.addTime
        }
    }
}
