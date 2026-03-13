package com.u2tzjtne.telephonehelper.ui.adapter;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.u2tzjtne.telephonehelper.R;
import com.u2tzjtne.telephonehelper.db.CallRecord;
import com.u2tzjtne.telephonehelper.ui.activity.HistoryActivity;
import com.u2tzjtne.telephonehelper.util.DateUtils;

import java.util.List;

/**
 * 筛选结果适配器（支持高亮匹配文字）
 * @author u2tzjtne@gmail.com
 */
public class FilterResultAdapter extends BaseQuickAdapter<CallRecord, BaseViewHolder> {
    
    private String highlightPrefix = "";
    private static final String HIGHLIGHT_COLOR = "#03bd5c";

    public FilterResultAdapter(List<CallRecord> data) {
        super(R.layout.item_call_record, data);
    }

    /**
     * 设置需要高亮的前缀
     * @param prefix 前缀字符串
     */
    public void setHighlightPrefix(String prefix) {
        this.highlightPrefix = prefix != null ? prefix : "";
    }

    @Override
    protected void convert(BaseViewHolder viewHolder, CallRecord callRecord) {
        // 设置号码（带高亮）
        String phoneNumber = callRecord.phoneNumber.replace(" ", "");
        
        // 判断是否接通，设置文字颜色
        if (callRecord.isConnected) {
            viewHolder.setTextColor(R.id.tv_call_number, Color.BLACK);
        } else if (callRecord.callType == 1) {
            viewHolder.setTextColor(R.id.tv_call_number, Color.RED);
        }
        
        // 应用高亮效果到号码文本
        applyHighlight(viewHolder, phoneNumber);
        
        // 设置归属地
        viewHolder.setText(R.id.tv_attribution, callRecord.attribution + " " + callRecord.operator);
        if (callRecord.isConnected) {
            viewHolder.getView(R.id.iv_flag).setVisibility(View.VISIBLE);
            // 筛选结果不显示通话时长
            if (callRecord.callType == 0) {
                viewHolder.setText(R.id.tv_status, "呼出");
                viewHolder.getView(R.id.iv_flag).setScaleY(1);
            } else {
                viewHolder.setText(R.id.tv_status, "呼入");
                viewHolder.getView(R.id.iv_flag).setScaleY(-1);
            }
        } else {
            if (callRecord.callType == 1) {
                viewHolder.getView(R.id.iv_flag).setVisibility(View.GONE);
                viewHolder.setText(R.id.tv_status, "响铃");
            } else {
                viewHolder.setText(R.id.tv_status, "未接通");
            }
        }

        // 筛选结果不显示通话时间
        viewHolder.getView(R.id.tv_call_date).setVisibility(View.GONE);
        viewHolder.getView(R.id.iv_history).setOnClickListener(v -> {
            // 启动到历史记录界面
            HistoryActivity.start(getContext(), callRecord.phoneNumber);
        });
    }
    
    /**
     * 应用高亮效果到号码文本
     */
    private void applyHighlight(BaseViewHolder viewHolder, String phoneNumber) {
        if (highlightPrefix.isEmpty()) {
            return;
        }
        
        android.widget.TextView tvNumber = viewHolder.getView(R.id.tv_call_number);
        SpannableString spannableString = new SpannableString(phoneNumber);
        
        // 查找匹配的位置
        String lowerPhoneNumber = phoneNumber.toLowerCase();
        String lowerPrefix = highlightPrefix.toLowerCase();
        int startIndex = lowerPhoneNumber.indexOf(lowerPrefix);
        
        if (startIndex != -1) {
            int endIndex = startIndex + highlightPrefix.length();
            // 确保结束索引不超出字符串长度
            endIndex = Math.min(endIndex, phoneNumber.length());
            
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor(HIGHLIGHT_COLOR));
            spannableString.setSpan(colorSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        tvNumber.setText(spannableString);
    }
}
