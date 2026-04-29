package com.example.myservicecenter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class CallRecordAdapter : BaseQuickAdapter<CallRecord, BaseViewHolder>(R.layout.item_call_record) {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    private var outgoingPackageInfo: String = ""
    private var customSelfRegion: String = ""
    private var warmTipText: String = ""
    private var footerView: View? = null
    private var attachedContext: Context? = null

    fun setData(data: List<CallRecord>) {
        setList(data)
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
        updateFooterView()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedContext = recyclerView.context
        updateFooterView()
    }

    override fun convert(holder: BaseViewHolder, item: CallRecord) {
        val context = holder.itemView.context
        val billSeconds = calculateBillSeconds(item)
        val billedMinutes = calculateBilledMinutes(billSeconds)
        val isIncoming = item.callType == 1
        val packageText = buildPackageText(isIncoming, outgoingPackageInfo)

        holder.setText(
            R.id.tv_title,
            if (isIncoming) context.getString(R.string.record_voice_hd_incoming)
            else context.getString(R.string.record_voice_hd_outgoing)
        )
        holder.setText(R.id.tv_phone, item.phoneNumber ?: "--")
        holder.setText(
            R.id.tv_time,
            if (item.startTime > 0) dateFormat.format(Date(item.startTime)) else "--"
        )
        holder.setText(R.id.tv_duration, formatDuration(billSeconds))
        holder.setText(
            R.id.tv_attribution,
            customSelfRegion.ifBlank { item.attribution ?: context.getString(R.string.record_unknown_location) }
        )
        holder.setText(
            R.id.tv_type,
            if (isIncoming) context.getString(R.string.record_type_incoming_domestic)
            else context.getString(R.string.record_type_outgoing_local)
        )
        holder.setText(R.id.tv_bill_seconds, billedMinutes.toString())
        holder.setText(R.id.tv_fee, context.getString(R.string.record_fee_free))
        holder.setText(
            R.id.tv_status,
            if (item.isConnected) context.getString(R.string.record_connected)
            else context.getString(R.string.record_missed)
        )

        holder.getView<ImageView>(R.id.iv_direction).setImageResource(
            if (isIncoming) R.drawable.icon_detail_module_called
            else R.drawable.icon_detail_module_call
        )
        holder.getView<TextView>(R.id.tv_package).apply {
            text = packageText
            isVisible = packageText.isNotBlank()
        }
    }

    private fun updateFooterView() {
        val context = attachedContext ?: return
        if (warmTipText.isBlank()) {
            footerView?.let {
                removeFooterView(it)
                footerView = null
            }
            return
        }

        val currentFooter = footerView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_call_record_footer, null, false)
            .also {
                addFooterView(it)
                footerView = it
            }
        currentFooter.findViewById<TextView>(R.id.tvWarmTip).text = warmTipText
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
