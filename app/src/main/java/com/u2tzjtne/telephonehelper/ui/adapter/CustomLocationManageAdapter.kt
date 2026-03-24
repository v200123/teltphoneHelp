package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.databinding.ItemCustomLocationManageBinding
import com.u2tzjtne.telephonehelper.db.CustomPhoneLocation

/**
 * 自定义归属地管理列表适配器
 */
class CustomLocationManageAdapter(
    private val onEditClick: (CustomPhoneLocation) -> Unit,
    private val onDeleteClick: (CustomPhoneLocation) -> Unit
) : ListAdapter<CustomPhoneLocation, CustomLocationManageAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomLocationManageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCustomLocationManageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomPhoneLocation) {
            binding.tvPhoneNumber.text = formatPhoneNumber(item.phone)
            
            val locationInfo = buildString {
                if (!item.province.isNullOrEmpty()) {
                    append(item.province)
                }
                if (!item.city.isNullOrEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append(item.city)
                }
                if (!item.carrier.isNullOrEmpty()) {
                    if (isNotEmpty()) append(" | ")
                    append(item.carrier)
                }
            }
            binding.tvLocationInfo.text = locationInfo.ifEmpty { "未设置归属地信息" }
            
            binding.ivEdit.setOnClickListener { onEditClick(item) }
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
    }

    class DiffCallback : DiffUtil.ItemCallback<CustomPhoneLocation>() {
        override fun areItemsTheSame(oldItem: CustomPhoneLocation, newItem: CustomPhoneLocation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CustomPhoneLocation, newItem: CustomPhoneLocation): Boolean {
            return oldItem.phone == newItem.phone &&
                    oldItem.province == newItem.province &&
                    oldItem.city == newItem.city &&
                    oldItem.carrier == newItem.carrier
        }
    }
}
