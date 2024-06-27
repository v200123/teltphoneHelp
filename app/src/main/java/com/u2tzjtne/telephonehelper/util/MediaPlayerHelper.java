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

    private final Uri audioCallUri = Uri.parse("android.resource://com.u2tzjtne.telephonehelper/" + R.raw.audio_call);
    private final Uri audioNoResponseUri = Uri.parse("android.resource://com.u2tzjtne.telephonehelper/" + R.raw.audio_no_response);

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

    public void playNoResponseSound(Context context) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, audioNoResponseUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playCallSound(Context context) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, audioCallUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void switchAudioOutput(Context context, boolean isSpeaker) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (isSpeaker) {
            audioManager.setSpeakerphoneOn(true);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        } else {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
    }
}
