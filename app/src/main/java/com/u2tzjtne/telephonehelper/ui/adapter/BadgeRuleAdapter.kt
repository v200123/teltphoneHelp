package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.databinding.ItemBadgeRuleBinding
import com.u2tzjtne.telephonehelper.util.BadgeRule
import com.u2tzjtne.telephonehelper.util.DateUtils
import com.u2tzjtne.telephonehelper.util.RuleSourceType

class BadgeRuleAdapter(
    private val onEditClick: (BadgeRule) -> Unit,
    private val onRenameClick: (BadgeRule) -> Unit,
    private val onDeleteClick: (BadgeRule) -> Unit
) : RecyclerView.Adapter<BadgeRuleAdapter.ViewHolder>() {

    private val data = mutableListOf<BadgeRule>()

    fun submitList(list: List<BadgeRule>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBadgeRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(
        private val binding: ItemBadgeRuleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BadgeRule) {
            binding.tvRuleName.text = item.name
            val source = if (item.sourceType == RuleSourceType.PRESET) "预设" else "自定义"
            val time = DateUtils.convertTimestamp(item.updatedAt, false)
            binding.tvRuleMeta.text = "$source · ${item.countAdded()}个图标 · $time"
            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnRename.setOnClickListener { onRenameClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
