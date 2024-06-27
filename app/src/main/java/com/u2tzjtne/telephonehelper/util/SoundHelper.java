package com.u2tzjtne.telephonehelper.util;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import com.u2tzjtne.telephonehelper.R;

/**
 * author: luke
 * email: u2tzjtne@gmail.com
 * date: 2021/12/9 14:24
 * desc: 音效帮助类
 *
 * @author luke
 */
public class SoundHelper {

    private final int POOL_MAX = 10;
    private Context mContext;
    private static SoundHelper soundHelper;
    private SoundPool soundPool = null;
    private int call_id = -1;
    private int no_response_id = -1;

    public static SoundHelper get(Context context) {
        if (soundHelper == null) {
            soundHelper = new SoundHelper(context);
        }
        return soundHelper;
    }

    private SoundHelper(Context mContext) {
        this.mContext = mContext;
    }

    private void buildSoundPool() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool(POOL_MAX, AudioManager.STREAM_MUSIC, 0);
        } else {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(POOL_MAX)
                    .build();
        }
        call_id = soundPool.load(mContext, R.raw.audio_call, 1);
        no_response_id = soundPool.load(mContext, R.raw.audio_no_response, 1);
    }

    public void initSoundPool() {
        if (soundPool == null) {
            buildSoundPool();
        }
    }

    private float getVolume() {
        // get volume
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Activity.AUDIO_SERVICE);
        float actualVolume = (float) audioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return actualVolume / maxVolume;
    }


    public void playCallSound() {
        if (call_id != -1) {
            float volume = getVolume();
            stopCallSound();
            stopNoResponseSound();
            soundPool.play(call_id, volume, volume, 1, -1, 1f);
            LogUtils.d("playReceiveSound vol:" + volume);
        }
    }

    public void playNoResponseSound() {
        if (no_response_id != -1) {
            float volume = getVolume();
            stopCallSound();
            stopNoResponseSound();
            soundPool.play(no_response_id, volume, volume, 1, -1, 1f);
            LogUtils.d("playReceiveSound vol:" + volume);
        }
    }

    public void stopCallSound() {
        if (call_id != -1) {
            soundPool.stop(call_id);
        }
    }

    public void stopNoResponseSound() {
        if (no_response_id != -1) {
            soundPool.stop(no_response_id);
        }
    }

    public void release() {
        call_id = -1;
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
