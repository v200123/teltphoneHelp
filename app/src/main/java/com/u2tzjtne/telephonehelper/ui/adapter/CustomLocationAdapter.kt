package com.u2tzjtne.telephonehelper.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.u2tzjtne.telephonehelper.R
import com.u2tzjtne.telephonehelper.db.CustomPhoneLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 自定义归属地列表适配器
 */
class CustomLocationAdapter : RecyclerView.Adapter<CustomLocationAdapter.ViewHolder>() {

    private val data = mutableListOf<CustomPhoneLocation>()
    private var onItemLongClickListener: ((CustomPhoneLocation) -> Unit)? = null

    fun setData(list: List<CustomPhoneLocation>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun setOnItemLongClickListener(listener: (CustomPhoneLocation) -> Unit) {
        onItemLongClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvPhone: TextView = view.findViewById(R.id.tv_phone)
        private val tvLocation: TextView = view.findViewById(R.id.tv_location)
        private val tvCarrier: TextView = view.findViewById(R.id.tv_carrier)

        init {
            view.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClickListener?.invoke(data[position])
                }
                true
            }
        }

        fun bind(item: CustomPhoneLocation) {
            // 显示完整号码
            tvPhone.text = item.phone
            
            // 显示归属地
            val location = buildString {
                append(item.province ?: "")
                if (!item.city.isNullOrEmpty() && item.city != item.province) {
                    append(" ")
                    append(item.city)
                }
            }
            tvLocation.text = if (location.isNotBlank()) location else "未设置"
            
            // 显示运营商
            tvCarrier.text = if (!item.carrier.isNullOrEmpty()) item.carrier else ""
        }
    }
}
