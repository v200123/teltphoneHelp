package com.u2tzjtne.telephonehelper.ui.activity;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.u2tzjtne.telephonehelper.R;
import com.u2tzjtne.telephonehelper.db.AppDatabase;
import com.u2tzjtne.telephonehelper.db.CallRecord;
import com.u2tzjtne.telephonehelper.event.ClipboardEvent;
import com.u2tzjtne.telephonehelper.ui.adapter.CallRecordAdapter;
import com.u2tzjtne.telephonehelper.util.ClipboardUtils;
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;
import com.u2tzjtne.telephonehelper.util.StatusBarUtils;
import com.u2tzjtne.telephonehelper.util.ToastUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @author u2tzjtne
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    TextView tvDialNumber;

    ImageView llDialDelete;

    RecyclerView rvCallRecord;

    ImageView ivDialShow;

    LinearLayout llDial1;

    LinearLayout llDial2;

    LinearLayout llDial3;

    LinearLayout llDial4;

    LinearLayout llDial5;

    LinearLayout llDial6;

    LinearLayout llDial7;

    LinearLayout llDial8;

    LinearLayout llDial9;

    LinearLayout llDialX;

    LinearLayout llDial0;

    LinearLayout llDialY;

    LinearLayout llDialRoot;

    LinearLayout llNumber;

    TextView tvLocation;

    EditText etSearch;

    View llAction;
    View ivBack;
    View llTab;
    View llSettings;
    View llSearch;
    View llSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        StatusBarUtils.setDarkStatusBar(this);
        hideNumber(true);
        getData();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
//            String text = ClipboardUtils.getText(this);
//            if (!TextUtils.isEmpty(text)) {
//                ToastUtils.s(text);
//            }
        }
    }

    private final View.OnClickListener dialClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            llNumber.setVisibility(View.VISIBLE);
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                View childView = viewGroup.getChildAt(0);
                if (childView instanceof TextView) {
                    TextView textView = (TextView) childView;
                    String currentText;
                    if (tvDialNumber.getText() != null) {
                        currentText = tvDialNumber.getText().toString();
                    } else {
                        return;
                    }
                    showActionPage(currentText.length() >= 3);
                    if (currentText.length() == 3 || currentText.length() == 8) {
                        currentText = currentText + " ";
                    }
                    currentText = currentText + textView.getText().toString();
                    tvDialNumber.setText(currentText);
//                    if (currentText.length() == 13) {
//                        setLocation(currentText);
//                    }
                    if (currentText.length() == 8) {
                        setLocation(currentText + " 0000");
                    }
                }
            }
        }
    };

    private void showActionPage(boolean isShow) {
        if (isShow) {
            //显示操作菜单
            llAction.setVisibility(View.VISIBLE);
            rvCallRecord.setVisibility(View.GONE);
            llTab.setVisibility(View.GONE);
            llSwitch.setVisibility(View.GONE);
            llSettings.setVisibility(View.GONE);
            llSearch.setVisibility(View.GONE);
        } else {
            //显示通话记录
            llAction.setVisibility(View.GONE);
            rvCallRecord.setVisibility(View.VISIBLE);
            llTab.setVisibility(View.VISIBLE);
            llSwitch.setVisibility(View.VISIBLE);
            llSettings.setVisibility(View.VISIBLE);
            llSearch.setVisibility(View.VISIBLE);
        }
    }

    private void setLocation(String number) {
//        ToastUtils.s(number);
        String province = PhoneNumberUtils.getProvince(number);
        if (TextUtils.isEmpty(province)) {
            return;
        }
        String operator = PhoneNumberUtils.getOperator(number);
        if (!TextUtils.isEmpty(operator)) {
            province = province + "  " + operator;
        }
        tvLocation.setText(province);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {

        tvDialNumber= findViewById(R.id.tv_dial_number);
        llDialDelete= findViewById(R.id.ll_dial_delete);
        rvCallRecord= findViewById(R.id.rv_call_record);
        ivDialShow= findViewById(R.id.iv_dial_show);
        llDial1= findViewById(R.id.ll_dial_1);
        llDial2= findViewById(R.id.ll_dial_2);
        llDial3= findViewById(R.id.ll_dial_3);
        llDial4= findViewById(R.id.ll_dial_4);
        llDial5= findViewById(R.id.ll_dial_5);
        llDial6= findViewById(R.id.ll_dial_6);
        llDial7= findViewById(R.id.ll_dial_7);
        llDial8= findViewById(R.id.ll_dial_8);
        llDial9= findViewById(R.id.ll_dial_9);
        llDialX= findViewById(R.id.ll_dial_x);
        llDial0= findViewById(R.id.ll_dial_0);
        llDialY= findViewById(R.id.ll_dial_y);
        llDialRoot= findViewById(R.id.ll_dial_root);
        llNumber= findViewById(R.id.ll_number);
        tvLocation= findViewById(R.id.tv_location);
        etSearch= findViewById(R.id.et_search);
        llAction= findViewById(R.id.ll_action);
        ivBack= findViewById(R.id.iv_back);
        llTab= findViewById(R.id.ll_tab);
        llSettings= findViewById(R.id.ll_settings);
        llSearch= findViewById(R.id.ll_search);
        llSwitch= findViewById(R.id.ll_switch);


        findViewById(R.id.iv_dial_show).setOnClickListener(this);
        findViewById(R.id.ll_dial_hide).setOnClickListener(this);
        findViewById(R.id.ll_dial_call).setOnClickListener(this);
        findViewById(R.id.ll_dial_delete).setOnClickListener(this);





        rvCallRecord.setLayoutManager(new LinearLayoutManager(this));
        //触摸隐藏拨号盘
        rvCallRecord.setOnTouchListener((view, motionEvent) -> {
            llDialRoot.setVisibility(View.GONE);
            return false;
        });
        llDial1.setOnClickListener(dialClickListener);
        llDial2.setOnClickListener(dialClickListener);
        llDial3.setOnClickListener(dialClickListener);
        llDial4.setOnClickListener(dialClickListener);
        llDial5.setOnClickListener(dialClickListener);
        llDial6.setOnClickListener(dialClickListener);
        llDial7.setOnClickListener(dialClickListener);
        llDial8.setOnClickListener(dialClickListener);
        llDial9.setOnClickListener(dialClickListener);
        llDial0.setOnClickListener(dialClickListener);
        llDialX.setOnClickListener(dialClickListener);
        llDialY.setOnClickListener(dialClickListener);
        //长按删除号码
        llDialDelete.setOnLongClickListener(view -> {
            hideNumber(true);
            return false;
        });
        ivBack.setOnClickListener(view -> {
            showActionPage(false);
        });
    }

    private void callPhone(String number) {
//        hideNumber(true);
        if(number.isEmpty()){
            return;
        }
        CallActivity.start(this, number);
    }

    //隐藏号码显示
    private void hideNumber(boolean isHide) {
        tvLocation.setText("");
        tvDialNumber.setText("");
        if (isHide) {
            llNumber.setVisibility(View.GONE);
        } else {
            llNumber.setVisibility(View.VISIBLE);
        }
        showActionPage(false);
    }

    private void deleteNumber() {
        if (!TextUtils.isEmpty(tvDialNumber.getText())) {
            String currentText = tvDialNumber.getText().toString();
            String afterText = currentText.substring(0, currentText.length() - 1);
            tvDialNumber.setText(afterText);
            if (afterText.length() < 1) {
                hideNumber(true);
            }
            showActionPage(tvDialNumber.getText().length() >= 3);
        } else {
            hideNumber(true);
        }
    }

    private String getAndCheckNumber() {
        if (tvDialNumber.getText() == null) {
            return null;
        }
        return tvDialNumber.getText().toString();
    }

    @SuppressLint("CheckResult")
    private void getData() {
        AppDatabase.getInstance().callRecordModel()
                .getAllByGroup()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setData);
    }

    private void setData(List<CallRecord> data) {
        if (data == null) {
            return;
        }
        //if (data.size() == 0) {
        //    etSearch.setHint("搜索联系人");
        //} else {
        //    etSearch.setHint("搜索" + data.size() + "位联系人");
        //}
        CallRecordAdapter adapter = new CallRecordAdapter(data);
        rvCallRecord.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter1, view, position) -> {
            callPhone(data.get(position).phoneNumber);
        });
        //删除通话记录
        adapter.setOnItemLongClickListener((adapter12, view, position) -> {
            AppDatabase.getInstance()
                    .callRecordModel()
                    .deleteByNumber(data.get(position).phoneNumber)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
            adapter.remove(position);
            return false;
        });
    }

//    @OnClick({R.id.iv_dial_show, R.id.ll_dial_hide, R.id.ll_dial_call, R.id.ll_dial_delete})
//    public void onViewClicked(View view) {
//        switch (view.getId()) {
//            case R.id.iv_dial_show:
//                llDialRoot.setVisibility(View.VISIBLE);
//                break;
//            case R.id.ll_dial_hide:
//                llDialRoot.setVisibility(View.GONE);
//                break;
//            case R.id.ll_dial_call:
//                callPhone(getAndCheckNumber());
//                break;
//            case R.id.ll_dial_delete:
//                deleteNumber();
//                break;
//            default:
//                break;
//        }
//    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_dial_show:
                llDialRoot.setVisibility(View.VISIBLE);
                break;
            case R.id.ll_dial_hide:
                llDialRoot.setVisibility(View.GONE);
                break;
            case R.id.ll_dial_call:
                callPhone(getAndCheckNumber());
                break;
            case R.id.ll_dial_delete:
                deleteNumber();
                break;
            default:
                break;
        }
    }
}