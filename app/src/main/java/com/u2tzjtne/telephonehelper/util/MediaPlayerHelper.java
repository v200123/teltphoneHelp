package com.u2tzjtne.telephonehelper.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import com.u2tzjtne.telephonehelper.R;

public class MediaPlayerHelper {
    private static MediaPlayerHelper instance;
    private final MediaPlayer mediaPlayer;
    private boolean isSpeakerOn;

    private MediaPlayerHelper() {
        mediaPlayer = new MediaPlayer();
        isSpeakerOn = false;
    }

    public static synchronized MediaPlayerHelper getInstance() {
        if (instance == null) {
            instance = new MediaPlayerHelper();
        }
        return instance;
    }

    private Uri buildRawUri(Context context, int resId) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
    }

    private boolean playRawInternal(Context context, Uri uri, boolean looping, Runnable onCompletion) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.setLooping(looping);
            if (!looping && onCompletion != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.setOnCompletionListener(null);
                    onCompletion.run();
                });
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean playRawResource(Context context, int resId, boolean looping, Runnable onCompletion) {
        return playRawInternal(context, buildRawUri(context, resId), looping, onCompletion);
    }

    public boolean playRawByName(Context context, String rawName, boolean looping, Runnable onCompletion) {
        int resId = context.getResources().getIdentifier(rawName, "raw", context.getPackageName());
        if (resId == 0) {
            return false;
        }
        return playRawResource(context, resId, looping, onCompletion);
    }

    public void playNoResponseSound(Context context) {
        playRawResource(context, R.raw.audio_no_response, true, null);
    }

    public void playGuaduanSound(Context context) {
        playRawResource(context, R.raw.calling, false, null);
    }

    public void playCallSound(Context context) {
        playRawResource(context, R.raw.audio_call, true, null);
    }

    public boolean playCallSoundOnce(Context context, Runnable onCompletion) {
        return playRawResource(context, R.raw.audio_call, false, onCompletion);
    }

    public boolean playBusySound(Context context, String rawName, Runnable onCompletion) {
        return playRawByName(context, rawName, false, onCompletion);
    }

    /**
     * 播放通话结束提示音
     */
    public void playCallEndSound(Context context) {
        playRawResource(context, R.raw.audio_call, false, null);
    }

    public void stopAudio() {
        try {
            mediaPlayer.setOnCompletionListener(null);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void switchAudioOutput(Context context, boolean isSpeaker) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (isSpeaker) {
            audioManager.setSpeakerphoneOn(true);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
    }
}
