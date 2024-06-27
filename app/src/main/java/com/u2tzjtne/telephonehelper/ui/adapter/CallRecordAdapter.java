package com.u2tzjtne.telephonehelper.ui.adapter;

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
            //通话时长
            String duration = DateUtils.getCallDuration(callRecord.endTime - callRecord.connectedTime);
            viewHolder.setText(R.id.tv_status, "呼出" + duration);
        } else {
            viewHolder.setText(R.id.tv_status, "未接通");
        }
        String time = DateUtils.convertTimestamp(callRecord.endTime,false);
        viewHolder.setText(R.id.tv_call_date, time);
        viewHolder.getView(R.id.iv_history).setOnClickListener(v -> {
            //启动到历史记录界面
            HistoryActivity.start(getContext(), callRecord.phoneNumber);
        });
    }
}
