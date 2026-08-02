package com.u2tzjtne.telephonehelper.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
    private static final String TAG = "HistoryActivity";


    TextView tvNumber;


    TextView tvAttribution;


    RecyclerView rvCallRecord;

    private String number;
    private List<CallRecord> currentRecords;

    public static void start(Context context, String phoneNumber) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).openHistoryDetail(phoneNumber);
            return;
        }
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


        tvNumber.setText(PhoneNumberUtils.formatPhoneNumber(number));


        PhoneNumberUtils.getProvince(number, new getLocalCallback() {
            @Override
            public void result(PhoneLocalBean bean) {
                String attribution;
                String operator = safeText(bean.getCarrier());
                if (!safeText(bean.getProvince()).equals(safeText(bean.getCity())))
                    attribution = safeText(bean.getProvince()) + safeText(bean.getCity());
                else attribution = safeText(bean.getProvince());
                tvAttribution.setText(attribution + " " + operator);
                saveResolvedLocationIfNeeded(attribution, operator);

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
                .getByNumberMuti(number)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setData);
    }

    private void setData(List<CallRecord> data) {
        currentRecords = data;
        CallHistoryAdapter adapter = new CallHistoryAdapter(data);
        rvCallRecord.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter1, view, position) -> {
            CallRecord callRecord = data.get(position);
            CallRecordingDetailActivity.Companion.start(this, callRecord.id);
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
//        StatusBarUtils.setDarkStatusBar(this);
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

    @Override
    public void finish() {
        setResult(Activity.RESULT_OK);
        super.finish();
    }

    private void saveResolvedLocationIfNeeded(String attribution, String operator) {
        String safeAttribution = safeText(attribution).trim();
        String safeOperator = safeText(operator).trim();
        if (!isResolvedLocationValue(safeAttribution) && !isResolvedLocationValue(safeOperator)) {
            return;
        }
        if (currentRecords == null || currentRecords.isEmpty()) {
            return;
        }
        AppDatabase.getInstance().callRecordModel()
                .getByNumberMuti(number)
                .subscribeOn(Schedulers.io())
                .subscribe(records -> {
                    boolean updated = false;
                    for (CallRecord record : records) {
                        String recordAttribution = safeText(record.attribution).trim();
                        String recordOperator = safeText(record.operator).trim();
                        boolean needsAttribution = !isResolvedLocationValue(recordAttribution) && isResolvedLocationValue(safeAttribution);
                        boolean needsOperator = !isResolvedLocationValue(recordOperator) && isResolvedLocationValue(safeOperator);
                        if (!needsAttribution && !needsOperator) {
                            continue;
                        }
                        if (needsAttribution) {
                            record.attribution = safeAttribution;
                        }
                        if (needsOperator) {
                            record.operator = safeOperator;
                        }
                        AppDatabase.getInstance().callRecordModel().update(record).blockingAwait();
                        updated = true;
                    }
                    if (updated) {
                        Log.d(TAG, "已补写归属地到通话记录: " + number);
                    }
                }, error -> Log.w(TAG, "补写归属地失败: " + error.getMessage()));
    }

    private boolean isResolvedLocationValue(String value) {
        return !TextUtils.isEmpty(value) && !"null".equalsIgnoreCase(value) && !"未知".equals(value);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
