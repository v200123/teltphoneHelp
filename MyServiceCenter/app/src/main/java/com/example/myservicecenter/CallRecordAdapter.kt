package com.example.myservicecenter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class CallRecordAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RECORD = 1
        private const val TYPE_FOOTER = 2
    }

    private val items = mutableListOf<CallRecord>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    private var outgoingPackageInfo: String = ""
    private var customSelfRegion: String = ""
    private var warmTipText: String = ""

    fun setData(data: List<CallRecord>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun setOutgoingPackageInfo(packageInfo: String) {
        outgoingPackageInfo = packageInfo
        notifyDataSetChanged()
    }

    fun setCustomSelfRegion(region: String) {
        customSelfRegion = region.trim()
        notifyDataSetChanged()
    }

    fun setWarmTip(text: String) {
        warmTipText = text
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FOOTER) {
            FooterViewHolder(inflater.inflate(R.layout.item_call_record_footer, parent, false))
        } else {
            RecordViewHolder(inflater.inflate(R.layout.item_call_record, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RecordViewHolder) {
            holder.bind(items[position], dateFormat, outgoingPackageInfo, customSelfRegion)
        } else if (holder is FooterViewHolder) {
            holder.bind(warmTipText)
        }
    }

    override fun getItemCount(): Int {
        val footerCount = if (warmTipText.isBlank()) 0 else 1
        return items.size + footerCount
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < items.size) TYPE_RECORD else TYPE_FOOTER
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val ivDirection: ImageView = itemView.findViewById(R.id.iv_direction)
        private val tvPhone: TextView = itemView.findViewById(R.id.tv_phone)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvAttribution: TextView = itemView.findViewById(R.id.tv_attribution)
        private val tvPackage: TextView = itemView.findViewById(R.id.tv_package)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)
        private val tvBillSeconds: TextView = itemView.findViewById(R.id.tv_bill_seconds)
        private val tvFee: TextView = itemView.findViewById(R.id.tv_fee)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)

        fun bind(
            record: CallRecord,
            dateFormat: SimpleDateFormat,
            outgoingPackageInfo: String,
            customSelfRegion: String
        ) {
            val context = itemView.context
            val billSeconds = calculateBillSeconds(record)
            val billedMinutes = calculateBilledMinutes(billSeconds)
            val isIncoming = record.callType == 1
            val packageText = buildPackageText(isIncoming, outgoingPackageInfo)

            tvTitle.text = if (isIncoming) {
                context.getString(R.string.record_voice_hd_incoming)
            } else {
                context.getString(R.string.record_voice_hd_outgoing)
            }
            ivDirection.setImageResource(
                if (isIncoming) R.drawable.icon_detail_module_called
                else R.drawable.icon_detail_module_call
            )
            tvPhone.text = record.phoneNumber ?: "--"
            tvTime.text = if (record.startTime > 0) {
                dateFormat.format(Date(record.startTime))
            } else {
                "--"
            }
            tvDuration.text = formatDuration(billSeconds)
            tvAttribution.text = customSelfRegion.ifBlank {
                record.attribution ?: context.getString(R.string.record_unknown_location)
            }
            tvPackage.isVisible = packageText.isNotBlank()
            tvPackage.text = packageText
            tvType.text = if (isIncoming) {
                context.getString(R.string.record_type_incoming_domestic)
            } else {
                context.getString(R.string.record_type_outgoing_local)
            }
            tvBillSeconds.text = billedMinutes.toString()
            tvFee.text = context.getString(R.string.record_fee_free)
            tvStatus.text = if (record.isConnected) {
                context.getString(R.string.record_connected)
            } else {
                context.getString(R.string.record_missed)
            }
        }

        private fun calculateBillSeconds(record: CallRecord): Int {
            val start = if (record.connectedTime > 0) record.connectedTime else record.startTime
            val end = record.endTime
            if (start <= 0 || end <= 0 || end < start) {
                return 0
            }
            return max(0L, (end - start) / 1000L).toInt()
        }

        private fun calculateBilledMinutes(totalSeconds: Int): Int {
            if (totalSeconds <= 0) {
                return 0
            }
            return (totalSeconds + 59) / 60
        }

        private fun formatDuration(totalSeconds: Int): String {
            if (totalSeconds <= 0) {
                return "00分00秒"
            }
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d分%02d秒", minutes, seconds)
        }

        private fun buildPackageText(isIncoming: Boolean, outgoingPackageInfo: String): String {
            if (isIncoming) {
                return ""
            }
            return outgoingPackageInfo.trim()
        }
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWarmTip: TextView = itemView.findViewById(R.id.tvWarmTip)

        fun bind(text: String) {
            tvWarmTip.text = text
        }
    }
}

