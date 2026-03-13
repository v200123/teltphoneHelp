package com.u2tzjtne.telephonehelper.ui.activity;

import static com.u2tzjtne.telephonehelper.ui.activity.newCallActivity.GUADUAN;

import android.Manifest;
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
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.u2tzjtne.telephonehelper.R;
import com.u2tzjtne.telephonehelper.db.AppDatabase;
import com.u2tzjtne.telephonehelper.db.CallRecord;
import com.u2tzjtne.telephonehelper.db.Recording;
import com.u2tzjtne.telephonehelper.http.bean.PhoneLocalBean;
import com.u2tzjtne.telephonehelper.http.download.getLocalCallback;
import com.u2tzjtne.telephonehelper.util.AudioRecorderHelper;
import com.u2tzjtne.telephonehelper.util.MediaPlayerHelper;
import com.u2tzjtne.telephonehelper.util.PhoneNumberUtils;
import com.u2tzjtne.telephonehelper.util.ToastUtils;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.MaybeObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
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

    private CallRecord oldCallRecord = null;

    // 录音工具类
    private final AudioRecorderHelper audioRecorderHelper = AudioRecorderHelper.Companion.getInstance();
    // 录音状态标志
    private boolean isRecording = false;
    // 临时存储本次通话的所有录音信息（等待通话记录保存后关联）
    private final List<AudioRecorderHelper.RecordingInfo> pendingRecordings = new ArrayList<>();

    private final int CONNECTED = 1;

    private final int PLAY_RING = 2;

    private final int FINISH = 3;
    private final int WAIT_FINISH = 4;
    private final int PLAY_NO_RESPONSE_SOUND = 5;
    private final int CALL_END = 6;

    public static void start(Context context, String phoneNumber) {
        Intent intent;
        if (context.getPackageName().contains("old"))
            intent = new Intent(context, CallActivity.class);
        else {
            intent = new Intent(context, newCallActivity.class);
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


    private void updateHangUpColor() {
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
        findViewById(R.id.ll_action_0).setOnClickListener(this); // 录音按钮
        findViewById(R.id.ll_action_3).setOnClickListener(this);
        findViewById(R.id.ll_action_4).setOnClickListener(this);
        findViewById(R.id.ll_action_1).setOnClickListener(this);

        //未接通
        updateCallTip(false);
        callRecord.startTime = System.currentTimeMillis();
        callRecord.phoneNumber = number;

        tvCallNumber.setText(callRecord.phoneNumber);
    }

    private void getData() {
        number = getIntent().getStringExtra("phoneNumber");
        AppDatabase.getInstance().callRecordModel()
                .getByNumber(number)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new MaybeObserver<CallRecord>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(CallRecord oldCallRecord) {
                        Log.d("result", "onNext: 走了onNext流程");
                        CallActivity.this.oldCallRecord = oldCallRecord;
                        if(oldCallRecord==null||oldCallRecord.attribution == null ||oldCallRecord.attribution.equals("null")||oldCallRecord.attribution.isBlank()||oldCallRecord.attribution.equals("未知")) {
                            PhoneNumberUtils.getProvince(number, new getLocalCallback() {
                                @Override
                                public void result(PhoneLocalBean bean) {
                                    if (!bean.getProvince().equals(bean.getCity()))
                                        callRecord.attribution = bean.getProvince() + bean.getCity();
                                    else callRecord.attribution = bean.getProvince();
                                    callRecord.operator = bean.getCarrier();
                                    tvAttribution.setText(callRecord.attribution + " " + callRecord.operator);

                                }
                            });
                        }else{
                            tvAttribution.setText(oldCallRecord.attribution + " " + oldCallRecord.operator);
                            callRecord.attribution = oldCallRecord.attribution;
                            callRecord.operator = oldCallRecord.operator;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("result", "onError: 走了onError流程");

                    }

                    @Override
                    public void onComplete() {
                        if(oldCallRecord==null){
                            PhoneNumberUtils.getProvince(number, new getLocalCallback() {
                                @Override
                                public void result(PhoneLocalBean bean) {
                                    if (!bean.getProvince().equals(bean.getCity()))
                                        callRecord.attribution = bean.getProvince() + bean.getCity();
                                    else callRecord.attribution = bean.getProvince();
                                    callRecord.operator = bean.getCarrier();
                                    tvAttribution.setText(callRecord.attribution + " " + callRecord.operator);
                                }
                            });
                        }
                        Log.d("result", "onComplete: 走了onComplete流程");

                    }
                });
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
                case GUADUAN:
                    handler.removeMessages(CONNECTED);
                    handler.removeMessages(PLAY_RING);
                    handler.removeMessages(PLAY_NO_RESPONSE_SOUND);
                    // 停止录音并保存
                    stopAndSaveRecording();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            MediaPlayerHelper.getInstance().playCallSound(CallActivity.this);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            MediaPlayerHelper.getInstance().playGuaduanSound(CallActivity.this);
                            handler.sendEmptyMessageDelayed(CALL_END, 47_000);

                        }
                    }).start();
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
        
        // 如果有录音，设置第一个录音路径到 callRecord（向后兼容）
        if (!pendingRecordings.isEmpty()) {
            AudioRecorderHelper.RecordingInfo firstRecording = pendingRecordings.get(0);
            callRecord.recordingPath = firstRecording.getFilePath();
            callRecord.recordingStartTime = firstRecording.getStartTime();
            callRecord.recordingEndTime = firstRecording.getEndTime();
        }
        
        AppDatabase.getInstance().callRecordModel()
                .insert(callRecord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new io.reactivex.CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onComplete() {
                        Log.d("CallActivity", "通话记录保存成功");
                        // 获取刚插入的通话记录ID，然后保存录音记录
                        saveRecordingRecords();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("CallActivity", "通话记录保存失败: " + e.getMessage());
                    }
                });
    }

    /**
     * 保存所有录音记录到 Recording 表
     */
    private void saveRecordingRecords() {
        if (pendingRecordings.isEmpty()) return;
        
        // 查询刚插入的通话记录获取ID
        AppDatabase.getInstance().callRecordModel()
                .getByNumber(number)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new MaybeObserver<CallRecord>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(CallRecord savedCallRecord) {
                        // 为每条录音创建 Recording 记录
                        for (AudioRecorderHelper.RecordingInfo info : pendingRecordings) {
                            Recording recording = new Recording();
                            recording.callRecordId = savedCallRecord.id;
                            recording.filePath = info.getFilePath();
                            recording.startTime = info.getStartTime();
                            recording.endTime = info.getEndTime();
                            recording.duration = info.getDuration();
                            recording.format = "m4a";
                            recording.createdAt = System.currentTimeMillis();
                            // 获取文件大小
                            try {
                                File file = new File(info.getFilePath());
                                recording.fileSize = file.length();
                            } catch (Exception e) {
                                recording.fileSize = 0L;
                            }
                            
                            AppDatabase.getInstance().recordingModel()
                                    .insert(recording)
                                    .subscribeOn(Schedulers.io())
                                    .subscribe();
                        }
                        Log.d("CallActivity", "已保存 " + pendingRecordings.size() + " 条录音记录");
                        pendingRecordings.clear();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("CallActivity", "查询通话记录失败: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.d("CallActivity", "未找到通话记录");
                    }
                });
    }

    /**
     * 开始录音
     */
    private void startRecording() {
        // 检查并请求录音权限
        AndPermission.with(this)
                .runtime()
                .permission(Permission.RECORD_AUDIO)
                .onDenied(data -> {
                    ToastUtils.s("需要录音权限才能使用录音功能");
                    // 重置录音按钮状态
                    isRecording = false;
                    ivAction0.setImageResource(R.drawable.ic_call_action_0_0);
                    tvAction0.setTextColor(getResources().getColor(R.color.white_50));
                    tvAction0.setText("录音");
                })
                .onGranted(data -> {
                    // 开始录音
                    String recordingPath = audioRecorderHelper.startRecording(number);
                    if (recordingPath != null) {
                        Log.d("CallActivity", "录音开始: " + recordingPath);
                        ToastUtils.s("开始录音");
                    } else {
                        ToastUtils.s("录音启动失败");
                        // 重置状态
                        isRecording = false;
                        ivAction0.setImageResource(R.drawable.ic_call_action_0_0);
                        tvAction0.setTextColor(getResources().getColor(R.color.white_50));
                        tvAction0.setText("录音");
                    }
                }).start();
    }

    /**
     * 停止录音并将录音信息添加到待保存列表
     */
    private void stopAndSaveRecording() {
        if (audioRecorderHelper.isCurrentlyRecording()) {
            AudioRecorderHelper.RecordingInfo recordingInfo = audioRecorderHelper.stopRecording();
            if (recordingInfo != null) {
                Log.d("CallActivity", "录音停止: " + recordingInfo.getFilePath() + 
                      ", 时长: " + recordingInfo.getFormattedDuration());
                // 将录音信息添加到待保存列表
                pendingRecordings.add(recordingInfo);
            }
        }
    }

    /**
     * 停止录音（用户手动停止）
     */
    private void stopRecordingByUser() {
        if (audioRecorderHelper.isCurrentlyRecording()) {
            AudioRecorderHelper.RecordingInfo recordingInfo = audioRecorderHelper.stopRecording();
            if (recordingInfo != null) {
                Log.d("CallActivity", "录音停止: " + recordingInfo.getFilePath() + 
                      ", 时长: " + recordingInfo.getFormattedDuration());
                // 将录音信息添加到待保存列表
                pendingRecordings.add(recordingInfo);
                ToastUtils.s("录音已停止，共 " + pendingRecordings.size() + " 条录音");
            }
        }
    }

    /**
     * 切换录音状态
     */
    private void toggleRecording() {
        if (isRecording) {
            // 停止录音
            stopRecordingByUser();
            ivAction0.setImageResource(R.drawable.ic_call_action_0_0);
            tvAction0.setTextColor(getResources().getColor(R.color.white_50));
            tvAction0.setText("录音");
            isRecording = false;
        } else {
            // 开始录音
            startRecording();
            ivAction0.setImageResource(R.drawable.ic_call_action_0_1);
            tvAction0.setTextColor(getResources().getColor(R.color.white));
            tvAction0.setText("录音中");
            isRecording = true;
        }
    }


    /**
     * 挂断
     */
    private void hangUp(String text, Boolean needSave) {
        // 停止录音
        stopAndSaveRecording();
        
        if (needSave) {
            saveCallRecord();
        }
        updateCallTip(false);
        tvCallStatus.setText(text);
        // 播放通话结束提示音
        MediaPlayerHelper.getInstance().stopAudio();
        MediaPlayerHelper.getInstance().playCallEndSound(CallActivity.this);
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
        // 切换录音状态
        toggleRecording();
    }

    private void updateAction() {
        ivAction0.setImageResource(R.drawable.ic_call_action_0_0);
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
        // 确保停止录音
        stopAndSaveRecording();
        
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
                switchAction0(); // 录音按钮
                break;
            case R.id.ll_action_1:
                handler.sendEmptyMessageDelayed(GUADUAN, 0);

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
