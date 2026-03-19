package com.u2tzjtne.telephonehelper.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.SoundPool;
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

import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.util.XPopupUtils;
import com.u2tzjtne.telephonehelper.R;
import com.u2tzjtne.telephonehelper.db.AppDatabase;
import com.u2tzjtne.telephonehelper.db.CallRecord;
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean;
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback;
import com.u2tzjtne.telephonehelper.ui.adapter.CallRecordAdapter;
import com.u2tzjtne.telephonehelper.ui.adapter.FilterResultAdapter;
import com.u2tzjtne.telephonehelper.ui.dialog.CopyPhoneNumberDialog;
import com.u2tzjtne.telephonehelper.ui.dialog.ThemeSwitchDialog;
import com.u2tzjtne.telephonehelper.util.ClipboardUtils;
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;
import com.u2tzjtne.telephonehelper.util.StatusBarUtils;

import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

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
    LinearLayout llTab;
    TextView tvTabCall;
    TextView tvTabContact;
    TextView tvTabBusiness;
    View llSettings;
    View llSearch;
    View llSwitch;
    View llActionButtons;
    RecyclerView rvFilterResult;
    FilterResultAdapter filterAdapter;
    List<CallRecord> filterData;
    String currentPrefix = ""; // 当前筛选前缀
    MediaPlayer mediaPlayer;
    SoundPool soundPool ;
    int soundId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requestAppPermissionsIfNeeded();

    }


    @Override
    public void onResume() {
        super.onResume();
//        mediaPlayer  = MediaPlayer.create(this, R.raw.bohao);
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5) // 最大同时播放数
                .build();
        soundId = soundPool.load(this, R.raw.bohao, 1);
        StatusBarUtils.setDarkStatusBar(this);
        hideNumber(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                runOnUiThread(() -> {
                    String text = ClipboardUtils.getText(MainActivity.this);
                    if(text!=null&&text.startsWith("1"))
                    {
                        llDialRoot.setVisibility(View.VISIBLE);
                        new XPopup.Builder(MainActivity.this).hasShadowBg(false).isCenterHorizontal(true)
                                .popupWidth(XPopupUtils.getScreenWidth(MainActivity.this) - 200)
                                .hasStatusBar(true)
                                .isLightStatusBar(true)
                                .isDestroyOnDismiss(true).atView(findViewById(R.id.ll_bohaopan)).asCustom(new CopyPhoneNumberDialog(MainActivity.this, text, new Function0<Unit>() {
                            @Override
                            public Unit invoke() {
                                llNumber.setVisibility(View.VISIBLE);
                                tvDialNumber.setVisibility(View.VISIBLE);
                                tvDialNumber.setText(AddCallRecordActivity.Companion.formatWithSpaces(text) );
                                if (text.length() >= 8) {
                                    showActionPage(true);
                                    if (text.length() == 8) {
                                        setLocation(text + " 0000");
                                    }else {
                                        setLocation(text);
                                    }
                                }
                                ClipboardUtils.clearFirstClipboard(MainActivity.this);
                                return null;
                            }
                        })).show();
                    }
                });

            }
        }).start();

        getData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer!=null) {
            mediaPlayer.release();
        }
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
                    soundPool.play(soundId, 1, 1, 1, 0, 1);
                    TextView textView = (TextView) childView;
                    String currentText;
                    if (tvDialNumber.getText() != null) {
                        currentText = tvDialNumber.getText().toString();
                    } else {
                        return;
                    }
                    String pureNumber = currentText.replace(" ", "") + textView.getText().toString();
                    // 从输入第一位就开始筛选
                    showActionPage(!pureNumber.isEmpty());
                    tvDialNumber.setText(AddCallRecordActivity.Companion.formatWithSpaces(pureNumber));

                    // 获取纯数字号码进行筛选
                    filterByPrefix(pureNumber);
                    if (pureNumber.length() == 7) {
                        setLocation(pureNumber + "0000");
                    } else if (pureNumber.length() > 5) {
                        setLocation(pureNumber);
                    }

                }
            }
        }
    };

    private void showActionPage(boolean isShow) {
        if (isShow) {
            //显示操作菜单（包含筛选列表或原有按钮）
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
            // 隐藏筛选列表，显示原有按钮
            rvFilterResult.setVisibility(View.GONE);
            llActionButtons.setVisibility(View.VISIBLE);
            // 清空筛选数据
            filterData.clear();
            currentPrefix = "";
            filterAdapter.setHighlightPrefix("");
            filterAdapter.notifyDataSetChanged();
        }
    }

    private void setLocation(String number) {
//        ToastUtils.s(number);
        PhoneNumberUtils.getProvince(number, new getLocalCallback() {
            @Override
            public void result(PhoneLocalBean bean) {
                String attribution ;
                String operator ;
                if(!bean.getProvince().equals(bean.getCity()) )
                    attribution = bean.getProvince() + bean.getCity();
                else  attribution = bean.getProvince();
                operator = bean.getCarrier();
                tvLocation.setText(attribution + " " + operator);
            }
        });

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
        tvTabCall = findViewById(R.id.tv_tab_call);
        tvTabContact = findViewById(R.id.tv_tab_contact);
        tvTabBusiness = findViewById(R.id.tv_tab_business);
        llSettings= findViewById(R.id.ll_settings);
        llSearch= findViewById(R.id.ll_search);
        llSwitch= findViewById(R.id.ll_switch);
        llActionButtons= findViewById(R.id.ll_action_buttons);
        rvFilterResult= findViewById(R.id.rv_filter_result);

        // 设置 Tab 点击事件
        tvTabCall.setOnClickListener(v -> switchTab(0));
        tvTabContact.setOnClickListener(v -> switchTab(1));
        tvTabBusiness.setOnClickListener(v -> switchTab(2));

        findViewById(R.id.iv_dial_show).setOnClickListener(this);
        findViewById(R.id.ll_dial_hide).setOnClickListener(this);
        findViewById(R.id.ll_dial_call).setOnClickListener(this);
        findViewById(R.id.ll_dial_delete).setOnClickListener(this);
        
        // 设置按钮 - 添加通话记录
        llSettings.setOnClickListener(view -> {
            startActivity(new Intent(this, AddCallRecordActivity.class));
        });

        rvCallRecord.setLayoutManager(new LinearLayoutManager(this));
        //初始化筛选结果列表
        rvFilterResult.setLayoutManager(new LinearLayoutManager(this));
        filterData = new java.util.ArrayList<>();
        filterAdapter = new FilterResultAdapter(filterData);
        rvFilterResult.setAdapter(filterAdapter);
        filterAdapter.setOnItemClickListener((adapter1, view, position) -> {
            callPhone(filterData.get(position).phoneNumber);
        });
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

    /**
     * 切换 Tab
     * @param index 0-通话, 1-联系人, 2-营业厅
     */
    private void switchTab(int index) {
        // 重置所有 Tab 颜色
        tvTabCall.setTextColor(getResources().getColor(R.color.textSecondary));
        tvTabContact.setTextColor(getResources().getColor(R.color.textSecondary));
        tvTabBusiness.setTextColor(getResources().getColor(R.color.textSecondary));
        
        // 设置选中 Tab 颜色
        switch (index) {
            case 0:
                tvTabCall.setTextColor(getResources().getColor(R.color.dialGreen));
                // 通话 Tab，正常显示
                break;
            case 1:
                tvTabContact.setTextColor(getResources().getColor(R.color.dialGreen));
                // 联系人 Tab，显示主题切换对话框
                new XPopup.Builder(this)
                        .asCustom(new ThemeSwitchDialog(this))
                        .show();
                break;
            case 2:
                tvTabBusiness.setTextColor(getResources().getColor(R.color.dialGreen));
                // 营业厅 Tab，暂无功能
                break;
        }
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
        // 清空筛选数据
        filterData.clear();
        currentPrefix = "";
        filterAdapter.setHighlightPrefix("");
        filterAdapter.notifyDataSetChanged();
    }

    private void deleteNumber() {
        if (!TextUtils.isEmpty(tvDialNumber.getText())) {
            String pureNumber = tvDialNumber.getText().toString().replace(" ", "");
            if (pureNumber.isEmpty()) {
                hideNumber(true);
                return;
            }
            String afterText = pureNumber.substring(0, pureNumber.length() - 1);
            if (afterText.isEmpty()) {
                hideNumber(true);
            } else {
                tvDialNumber.setText(AddCallRecordActivity.Companion.formatWithSpaces(afterText));
                // 更新筛选结果
                filterByPrefix(afterText);
                showActionPage(true);
                if (afterText.length() == 7) {
                    setLocation(afterText + "0000");
                } else if (afterText.length() > 5) {
                    setLocation(afterText);
                } else {
                    tvLocation.setText("");
                }
            }
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

    /**
     * 根据号码前缀筛选历史记录
     * @param prefix 号码前缀
     */
    @SuppressLint("CheckResult")
    private void filterByPrefix(String prefix) {
        // 保存当前筛选前缀用于高亮显示
        currentPrefix = prefix != null ? prefix : "";
        
        if (TextUtils.isEmpty(prefix)) {
            // 没有输入，显示原有按钮
            updateFilterUI(null);
            return;
        }
        AppDatabase.getInstance().callRecordModel()
                .getByPrefixGroup(prefix)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateFilterUI);
    }

    /**
     * 更新筛选结果UI
     * @param data 筛选结果数据
     */
    private void updateFilterUI(List<CallRecord> data) {
        if (data == null || data.isEmpty()) {
            // 没有筛选结果，显示原有按钮
            rvFilterResult.setVisibility(View.GONE);
            llActionButtons.setVisibility(View.VISIBLE);
        } else {
            // 有筛选结果，显示列表
            filterData.clear();
            filterData.addAll(data);
            // 更新高亮前缀
            filterAdapter.setHighlightPrefix(currentPrefix);
            filterAdapter.notifyDataSetChanged();
            rvFilterResult.setVisibility(View.VISIBLE);
            llActionButtons.setVisibility(View.GONE);
        }
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
//            case R.id.ll_dial_call:
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
