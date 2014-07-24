package com.grepsound.services;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class AudioService extends Service implements OnErrorListener, OnCompletionListener, AudioManager.OnAudioFocusChangeListener {
    public static final String INTENT_BASE_NAME = "com.musical.service.AudioService";

    public static final String INFO_TRACK = INTENT_BASE_NAME + ".INFO_TRACK";
    public static final String NOT_PLAYING = INTENT_BASE_NAME + ".INFO_TRACK";

    public static final String PLAY_NEXT = INTENT_BASE_NAME + ".PLAY_NEXT";
    public static final String PLAY_PREVIOUS = INTENT_BASE_NAME + ".PLAY_PREVIOUS";
    public static final String PLAY = INTENT_BASE_NAME + ".PLAY";
    public static final String PAUSE = INTENT_BASE_NAME + ".PAUSE";
    public static final String RESUME = INTENT_BASE_NAME + ".RESUME";
    public static final String UPDATE = INTENT_BASE_NAME + ".UPDATE";
    public static final String SHUFFLE = INTENT_BASE_NAME + ".SHUFFLE";
    public static final String REPEAT = INTENT_BASE_NAME + ".REPEAT";
    public static final String SEEK_MOVED = INTENT_BASE_NAME + ".SEEK_MOVED";
    public static final String CHATHEAD = INTENT_BASE_NAME + ".SEEK_MOVED";
    private AudioManager mAudioManager;
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    private final String TAG = AudioService.class.getSimpleName();
    private MediaPlayer mMediaPlayer;

    private boolean headsetConnected = false;
    private AudioPlayerBroadcastReceiver broadcastReceiver = new AudioPlayerBroadcastReceiver();

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                onGainedAudioFocus();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                onLostAudioFocus(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                onLostAudioFocus(true);
                break;
            default:
        }
    }

    public class AudioPlayerBinder extends Binder {
        public AudioService getService() {
            Log.v(TAG, "AudioPlayerBinder: getService() called");
            return AudioService.this;
        }
    }

    private final IBinder audioPlayerBinder = new AudioPlayerBinder();

    private WakeLock wl;

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "AudioPlayer: onBind() called");
        return audioPlayerBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Toast.makeText(getApplicationContext(), "onStartCommand",
        // Toast.LENGTH_LONG).show();
        PowerManager wm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = wm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.grepsound.lock");
        if (!wl.isHeld())
            wl.acquire();

        mMediaPlayer.setOnPreparedListener(mediaPreparedListener);
        mMediaPlayer.prepareAsync();
        mMediaPlayer.setOnCompletionListener(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY_NEXT);
        intentFilter.addAction(PLAY_PREVIOUS);
        intentFilter.addAction(PLAY);
        intentFilter.addAction(PAUSE);
        intentFilter.addAction(RESUME);
        intentFilter.addAction(PAUSE);
        intentFilter.addAction(UPDATE);
        intentFilter.addAction(SHUFFLE);
        intentFilter.addAction(REPEAT);
        intentFilter.addAction(SEEK_MOVED);
        intentFilter.addAction(CHATHEAD);
        registerReceiver(broadcastReceiver, intentFilter);


        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        receiverFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        checkHeadsetReceiver = new HeadsetStateReceiver();
        registerReceiver(checkHeadsetReceiver, receiverFilter);

    }

    HeadsetStateReceiver checkHeadsetReceiver;

    private class HeadsetStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.contentEquals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                headsetConnected = false;
                sendBroadcast(new Intent(AudioService.PAUSE));
            }

            if (action.contentEquals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                if (state == 0 && headsetConnected) {
                    headsetConnected = false;
                    sendBroadcast(new Intent(AudioService.PAUSE));
                } else if (state == 0 && !headsetConnected) {
                    headsetConnected = true;
                } else if (headsetConnected && state == 1) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2, 0);
                    sendBroadcast(new Intent(AudioService.RESUME));
                } else if (!headsetConnected && state == 1) {
                    headsetConnected = true;
                }
            }
        }
    }

    @Override
    public void onLowMemory() {
        unregisterReceiver(checkHeadsetReceiver);
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "AudioPlayer: onDestroy() called");
        if (wl != null)
            wl.release();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(checkHeadsetReceiver);
    }

    @Override
    public void onCompletion(MediaPlayer _mediaPlayer) {

    }

    OnPreparedListener mediaPreparedListener = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            mMediaPlayer.start();
        }
    };


    private class AudioPlayerBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();

        }
    }


    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck, // we don't have audio focus, and can't duck
        NoFocusCanDuck, // we don't have focus, but can play at a low volume
        // ("ducking")
        Focused // we have full audio focus
    }

    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mMediaPlayer != null && mMediaPlayer.isPlaying())
            mMediaPlayer.setVolume(1.0f, 1.0f);

    }

    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            if (canDuck) {
                mMediaPlayer.setVolume(0.1f, 0.1f);
            } else {
                Intent intent = new Intent(AudioService.PAUSE);
                sendBroadcast(intent);
            }
        }

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }
}