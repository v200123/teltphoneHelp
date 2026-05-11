package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.databinding.ItemPhoneAudioBindingBinding
import com.u2tzjtne.telephonehelper.util.PhoneDialAudioBindingHelper

class PhoneAudioBindingAdapter(
    private val onDeleteClick: (PhoneDialAudioBindingHelper.PhoneAudioBinding) -> Unit,
) : ListAdapter<PhoneDialAudioBindingHelper.PhoneAudioBinding, PhoneAudioBindingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhoneAudioBindingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPhoneAudioBindingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PhoneDialAudioBindingHelper.PhoneAudioBinding) {
            binding.tvPhoneNumber.text = formatPhoneNumber(item.phoneNumber)
            binding.tvPlayType.text = item.mode.label
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

    class DiffCallback : DiffUtil.ItemCallback<PhoneDialAudioBindingHelper.PhoneAudioBinding>() {
        override fun areItemsTheSame(
            oldItem: PhoneDialAudioBindingHelper.PhoneAudioBinding,
            newItem: PhoneDialAudioBindingHelper.PhoneAudioBinding,
        ): Boolean {
            return oldItem.phoneNumber == newItem.phoneNumber
        }

        override fun areContentsTheSame(
            oldItem: PhoneDialAudioBindingHelper.PhoneAudioBinding,
            newItem: PhoneDialAudioBindingHelper.PhoneAudioBinding,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
