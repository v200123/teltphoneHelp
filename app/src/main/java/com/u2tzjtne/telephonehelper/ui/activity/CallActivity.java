package com.u2tzjtne.telephonehelper.ui.activity;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.u2tzjtne.telephonehelper.R;
import com.u2tzjtne.telephonehelper.db.AppDatabase;
import com.u2tzjtne.telephonehelper.db.CallRecord;
import com.u2tzjtne.telephonehelper.util.MediaPlayerHelper;
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;
import com.u2tzjtne.telephonehelper.util.ToastUtils;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @author u2tzjtne
 */
public class CallActivity extends BaseActivity implements View.OnClickListener {

    TextView tvCallNumber;
    TextView tvAttribution;
    TextView tvCallStatus;
    Chronometer cmCallTime;
    LinearLayout llDialRoot;
    LinearLayout llActionRoot;
    ImageView ivDialSpeaker;

    ImageView ivAction0;

    TextView tvAction0;

    ImageView ivAction1;

    TextView tvAction1;

    ImageView ivAction2;

    TextView tvAction2;

    ImageView ivAction3;

    TextView tvAction3;

    ImageView ivAction4;

    TextView tvAction4;

    ImageView ivAction5;

    TextView tvAction5;

    View llPageRoot;

    View llCallTime;

    LinearLayout llDial1;
    private String number;
    private final CallRecord callRecord = new CallRecord();
    private final int CONNECTED = 1;

    private final int PLAY_RING = 2;

    private final int FINISH = 3;
    private final int WAIT_FINISH = 4;
    private final int PLAY_NO_RESPONSE_SOUND = 5;

    private final int CALL_END = 6;


    public static void start(Context context, String phoneNumber) {
        Intent intent;
        if(context.getPackageName().contains("old"))
            intent = new Intent(context, CallActivity.class);
        else{
            intent=  new Intent(context, newCallActivity.class);
        }
        intent.putExtra("phoneNumber", phoneNumber);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        getData();
        initView();
        //10秒后自动接通
        handler.sendEmptyMessageDelayed(CONNECTED, 10 * 1000);
        //2秒后 显示对方振铃
        handler.sendEmptyMessageDelayed(PLAY_RING, 2 * 1000);
        setBackGround();
        MediaPlayerHelper.getInstance().switchAudioOutput(CallActivity.this, isSpeakerOn);
    }


    private void updateHangUpColor(){
        findViewById(R.id.iv_dial_switch).setAlpha(0.5f);
        findViewById(R.id.iv_dial_hang_up).setAlpha(0.5f);
        ivDialSpeaker.setAlpha(0.5f);
    }



    private void setBackGround() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .onDenied(data -> {
                    ToastUtils.s("请授予权限后再试！");
                    finish();
                })
                .onGranted(data -> {
                    // 获取WallpaperManager实例
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                    // 获取当前的壁纸
                    Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                    // 将Drawable转换为Bitmap
                    Bitmap wallpaperBitmap = ((BitmapDrawable) wallpaperDrawable).getBitmap();
                    // 设置背景
                    llPageRoot.setBackground(new BitmapDrawable(getResources(), wallpaperBitmap));
                }).start();
    }

    private void initView() {

        tvCallNumber = findViewById(R.id.tv_call_number);
        tvAttribution = findViewById(R.id.tv_attribution);
        tvCallStatus = findViewById(R.id.tv_call_status);
        cmCallTime = findViewById(R.id.cm_call_time);
        llDialRoot = findViewById(R.id.ll_dial_root);
        llActionRoot = findViewById(R.id.ll_action_root);
        ivDialSpeaker = findViewById(R.id.iv_dial_speaker);
        ivAction0 = findViewById(R.id.iv_action_0);
        tvAction0 = findViewById(R.id.tv_action_0);
        ivAction1 = findViewById(R.id.iv_action_1);
        tvAction1 = findViewById(R.id.tv_action_1);
        ivAction2 = findViewById(R.id.iv_action_2);

        tvAction2 = findViewById(R.id.tv_action_2);
        ivAction3 = findViewById(R.id.iv_action_3);
        tvAction3 = findViewById(R.id.tv_action_3);
        ivAction4 = findViewById(R.id.iv_action_4);
        tvAction4 = findViewById(R.id.tv_action_4);
        ivAction5 = findViewById(R.id.iv_action_5);
        tvAction5 = findViewById(R.id.tv_action_5);
        llPageRoot = findViewById(R.id.ll_page_root);
        llCallTime = findViewById(R.id.ll_call_time);
        llDial1 = findViewById(R.id.ll_dial_1);

        findViewById(R.id.ll_dial_switch).setOnClickListener(this);
        findViewById(R.id.ll_dial_hang_up).setOnClickListener(this);
        findViewById(R.id.ll_dial_speaker).setOnClickListener(this);
        findViewById(R.id.ll_action_0).setOnClickListener(this);
        findViewById(R.id.ll_action_3).setOnClickListener(this);
        findViewById(R.id.ll_action_4).setOnClickListener(this);


        //未接通
        updateCallTip(false);
        callRecord.startTime = System.currentTimeMillis();
        callRecord.phoneNumber = number;
        callRecord.attribution = PhoneNumberUtils.getProvince(number);
        callRecord.operator = PhoneNumberUtils.getOperator(number);
        tvAttribution.setText(callRecord.attribution + " " + callRecord.operator);
        tvCallNumber.setText(callRecord.phoneNumber);
    }

    private void getData() {
        number = getIntent().getStringExtra("phoneNumber");
    }

    /**
     * 更新通话提示
     */
    private void updateCallTip(boolean isConnected) {
        if (isConnected) {
            llCallTime.setVisibility(View.VISIBLE);
            //设置初始值（重置）
            cmCallTime.setBase(SystemClock.elapsedRealtime());
            cmCallTime.start();
            tvCallStatus.setVisibility(View.GONE);
        } else {
            tvCallStatus.setVisibility(View.VISIBLE);
            llCallTime.setVisibility(View.GONE);
            cmCallTime.stop();
        }
    }


    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CONNECTED:
                    MediaPlayerHelper.getInstance().stopAudio();
                    callRecord.isConnected = true;
                    callRecord.connectedTime = System.currentTimeMillis();
                    updateCallTip(true);
                    updateAction();
                    break;
                case PLAY_RING:
                    tvCallStatus.setText("对方已振铃");
                    MediaPlayerHelper.getInstance().playCallSound(CallActivity.this);
                    break;
                case PLAY_NO_RESPONSE_SOUND:
                    MediaPlayerHelper.getInstance().playNoResponseSound(CallActivity.this);
                    handler.sendEmptyMessageDelayed(CALL_END, 23200);
                    break;
                case WAIT_FINISH:
                    handler.sendEmptyMessageDelayed(FINISH, 1000);
                    break;

                case FINISH:
                    updateHangUpColor();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    finish();
                case CALL_END:
                    hangUp("通话结束", true);
                    break;
            }
        }
    };

    /**
     * 保存通话记录
     */
    private void saveCallRecord() {
        callRecord.endTime = System.currentTimeMillis();
        AppDatabase.getInstance().callRecordModel()
                .insert(callRecord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }


    /**
     * 挂断
     */
    private void hangUp(String text, Boolean needSave) {
        if (needSave) {
            saveCallRecord();
        }
        updateCallTip(false);
        tvCallStatus.setText(text);
        handler.sendEmptyMessageDelayed(WAIT_FINISH, 0);
    }

    /**
     * 拨号盘
     */
    private void switchDial() {
        llDialRoot.setVisibility(llDialRoot.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        llActionRoot.setVisibility(llDialRoot.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }


    private boolean isSpeakerOn = false;
    private boolean isAction0On = false;

    private void switchSpeaker() {
        if (isSpeakerOn) {
            ivDialSpeaker.setImageResource(R.drawable.ic_speaker_normal);
            isSpeakerOn = false;
        } else {
            ivDialSpeaker.setImageResource(R.drawable.ic_speaker_checked);
            isSpeakerOn = true;
        }
        MediaPlayerHelper.getInstance().switchAudioOutput(CallActivity.this, isSpeakerOn);
    }

    private void switchAction0() {
        if (isAction0On) {
            ivAction0.setImageResource(R.drawable.ic_call_action_0_1);
            tvAction0.setTextColor(getResources().getColor(R.color.white_50));
            isAction0On = false;
        } else {
            ivAction0.setImageResource(R.drawable.ic_call_action_0_2);
            tvAction0.setTextColor(getResources().getColor(R.color.white));
            isAction0On = true;
        }
    }

    private void updateAction() {
        ivAction0.setImageResource(R.drawable.ic_call_action_0_1);
        tvAction0.setTextColor(getResources().getColor(R.color.white));

        ivAction1.setImageResource(R.drawable.ic_call_action_1_1);
        tvAction1.setTextColor(getResources().getColor(R.color.white));

        ivAction2.setImageResource(R.drawable.ic_call_action_2_1);
        tvAction2.setTextColor(getResources().getColor(R.color.white));

        ivAction3.setImageResource(R.drawable.ic_call_action_3_1);
        tvAction3.setTextColor(getResources().getColor(R.color.white));

        ivAction4.setImageResource(R.drawable.ic_call_action_4_1);
        tvAction4.setTextColor(getResources().getColor(R.color.white));

        ivAction5.setImageResource(R.drawable.ic_call_action_5_1);
        tvAction5.setTextColor(getResources().getColor(R.color.white));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        hangUp("正在挂断...", true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaPlayerHelper.getInstance().stopAudio();
        handler.removeCallbacksAndMessages(null);
        if (cmCallTime != null) {
            cmCallTime.stop();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_dial_switch:
                hangUp("通话结束", true);
                break;
            case R.id.ll_dial_hang_up:
                hangUp("正在挂断...", false);
                break;
            case R.id.ll_dial_speaker:
                switchSpeaker();
                break;
            case R.id.ll_action_0:
                switchAction0();
                break;
            case R.id.ll_action_3:
                //停止倒计时
                handler.removeMessages(CONNECTED);
                handler.sendEmptyMessageDelayed(PLAY_NO_RESPONSE_SOUND, 26 * 1000);
                break;
            case R.id.ll_action_4:
//                //停止倒计时
//                handler.removeMessages(CONNECTED);
//                handler.sendEmptyMessageDelayed(PLAY_NO_RESPONSE_SOUND, 26 * 1000);
                break;
            default:
                break;
        }
    }
}