package com.u2tzjtne.telephonehelper.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.u2tzjtne.telephonehelper.R;
import com.u2tzjtne.telephonehelper.db.AppDatabase;
import com.u2tzjtne.telephonehelper.db.CallRecord;
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean;
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback;
import com.u2tzjtne.telephonehelper.ui.adapter.CallHistoryAdapter;
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;
import com.u2tzjtne.telephonehelper.util.StatusBarUtils;

import java.util.List;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @author u2tzjtne
 */
public class HistoryActivity extends BaseActivity implements View.OnClickListener {


    TextView tvNumber;


    TextView tvAttribution;


    RecyclerView rvCallRecord;

    private String number;

    public static void start(Context context, String phoneNumber) {
        Intent intent = new Intent(context, HistoryActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        getData();
        initView();
    }

    private void initView() {
        tvNumber = findViewById(R.id.tv_number);
        tvAttribution = findViewById(R.id.tv_attribution);
        rvCallRecord = findViewById(R.id.rv_list);

        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_call).setOnClickListener(this);


        tvNumber.setText(number);

        PhoneNumberUtils.getProvince(number, new getLocalCallback() {
            @Override
            public void result(PhoneLocalBean bean) {
                String attribution ;
                String operator ;
                if(!bean.getProvince().equals(bean.getCity()) )
                    attribution = bean.getProvince() + bean.getCity();
                else  attribution = bean.getProvince();
                operator = bean.getCarrier();
                tvAttribution.setText(attribution + " " + operator);

            }
        });
        rvCallRecord.setLayoutManager(new LinearLayoutManager(this));
    }

    private void getData() {
        number = getIntent().getStringExtra("phoneNumber");
    }

    @SuppressLint("CheckResult")
    private void getCallData() {
        AppDatabase.getInstance().callRecordModel()
                .getByNumber(number)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setData);
    }

    private void setData(List<CallRecord> data) {
        CallHistoryAdapter adapter = new CallHistoryAdapter(data);
        rvCallRecord.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter1, view, position) -> {
            CallActivity.start(this, number);
        });
        adapter.setOnItemLongClickListener((adapter12, view, position) -> {
            AppDatabase.getInstance()
                    .callRecordModel()
                    .delete(data.get(position))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
            adapter.remove(position);
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatusBarUtils.setDarkStatusBar(this);
        //刷新通话记录
        getCallData();
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.iv_call:
                CallActivity.start(this, number);
                break;
            default:
                break;
        }
    }
}