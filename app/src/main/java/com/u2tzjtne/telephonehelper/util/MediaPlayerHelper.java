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

    public void playNoResponseSound(Context context) {
        final Uri audioNoResponseUri = Uri.parse("android.resource://"+context.getPackageName()+"/" + R.raw.audio_no_response);

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

    public void playGuaduanSound(Context context) {
      final Uri audioGuaDuanUri = Uri.parse("android.resource://"+context.getPackageName()+"/" + + R.raw.calling);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, audioGuaDuanUri);
            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void playCallSound(Context context) {
        final Uri audioCallUri = Uri.parse("android.resource://"+context.getPackageName()+"/" + R.raw.audio_call);

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
