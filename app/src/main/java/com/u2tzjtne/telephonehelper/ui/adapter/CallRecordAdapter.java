package com.u2tzjtne.telephonehelper.ui.adapter;

import android.graphics.Color;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.u2tzjtne.telephonehelper.R;
import com.u2tzjtne.telephonehelper.db.CallRecord;
import com.u2tzjtne.telephonehelper.ui.activity.HistoryActivity;
import com.u2tzjtne.telephonehelper.util.DateUtils;

import java.util.List;

/**
 * @author u2tzjtne@gmail.com
 * @date 2020/6/22
 */
public class CallRecordAdapter extends BaseQuickAdapter<CallRecord, BaseViewHolder> {
    public CallRecordAdapter(List<CallRecord> data) {
        super(R.layout.item_call_record, data);
    }

    @Override
    protected void convert(BaseViewHolder viewHolder, CallRecord callRecord) {
        viewHolder.setText(R.id.tv_call_number, callRecord.phoneNumber.replace(" ",""));
        //判断是否接通
        viewHolder.setText(R.id.tv_attribution, callRecord.attribution + " " + callRecord.operator);
        if (callRecord.isConnected) {
            viewHolder.getView(R.id.iv_flag).setVisibility(View.VISIBLE);
            viewHolder.setTextColor(R.id.tv_call_number, Color.BLACK);
            //通话时长
            String duration = DateUtils.getCallDuration(callRecord.endTime - callRecord.connectedTime);

            if(callRecord.callType==0) {
                viewHolder.setText(R.id.tv_status, "呼出" + duration);
                viewHolder.getView(R.id.iv_flag).setScaleY(1);
            }
            else {
                if(duration.equals("0秒")){
                    viewHolder.setText(R.id.tv_status, "已挂断");
                }else {
                    viewHolder.setText(R.id.tv_status, "呼入" + duration);
                }
                viewHolder.getView(R.id.iv_flag).setScaleY(-1);
            }


        } else {

            if(callRecord.callType==1) {
                viewHolder.setTextColor(R.id.tv_call_number, Color.RED);
                viewHolder.getView(R.id.iv_flag).setVisibility(View.GONE);
                viewHolder.setText(R.id.tv_status, "响铃" + callRecord.callNumber+"声");

            }
            else {
                viewHolder.setText(R.id.tv_status, "未接通");
            }
        }

        String time = DateUtils.convertTimestamp(callRecord.startTime,false);
        viewHolder.setText(R.id.tv_call_date, time);
        viewHolder.getView(R.id.iv_history).setOnClickListener(v -> {
            //启动到历史记录界面
            HistoryActivity.start(getContext(), callRecord.phoneNumber);
        });
    }
}
