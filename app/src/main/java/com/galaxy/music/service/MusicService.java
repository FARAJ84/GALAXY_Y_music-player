package com.galaxyy.music.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.galaxyy.music.R;
import com.galaxyy.music.Song;
import com.galaxyy.music.ui.MainActivity;
import com.galaxyy.music.util.MusicDB;
import com.galaxyy.music.util.PreferenceManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MusicService extends Service implements MusicPlayer.Callback {

    private static final String TAG = "GalaxyYMusicSvc";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY = "com.galaxyy.music.PLAY";
    public static final String ACTION_PAUSE = "com.galaxyy.music.PAUSE";
    public static final String ACTION_NEXT = "com.galaxyy.music.NEXT";
    public static final String ACTION_PREV = "com.galaxyy.music.PREV";
    public static final String ACTION_STOP = "com.galaxyy.music.STOP";
    public static final String ACTION_TOGGLE = "com.galaxyy.music.TOGGLE";

    private final IBinder mBinder = new MusicBinder();
    private MusicPlayer mPlayer;
    private ArrayList<Song> mPlaylist = new ArrayList<Song>();
    private int mCurrentIndex = -1;
    private boolean mWasPlayingBeforeCall = false;
    private AudioManager mAudioManager;
    private ComponentName mMediaButtonReceiver;
    private NotificationManager mNotificationManager;
    private Random mRandom = new Random();

    public interface ServiceCallback {
        void onSongChanged(Song song);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressUpdate(int position, int duration);
    }
    private ServiceCallback mServiceCallback;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayer = new MusicPlayer(this);
        mPlayer.setCallback(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mMediaButtonReceiver = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());

        try {
            mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Media button registration failed");
        }

        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    if (mPlayer.isPlaying()) {
                        mWasPlayingBeforeCall = true;
                        pause();
                    }
                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    if (mWasPlayingBeforeCall) {
                        mWasPlayingBeforeCall = false;
                        play();
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                play();
            } else if (ACTION_PAUSE.equals(action)) {
                pause();
            } else if (ACTION_NEXT.equals(action)) {
                next();
            } else if (ACTION_PREV.equals(action)) {
                previous();
            } else if (ACTION_TOGGLE.equals(action)) {
                togglePlayPause();
            } else if (ACTION_STOP.equals(action)) {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.release();
        }
        try {
            mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiver);
        } catch (Exception e) {}
        stopForeground(true);
    }

    public void setPlaylist(ArrayList<Song> songs, int startIndex) {
        mPlaylist = songs != null ? songs : new ArrayList<Song>();
        mCurrentIndex = startIndex;
        if (mCurrentIndex >= 0 && mCurrentIndex < mPlaylist.size()) {
            loadAndPlay(mPlaylist.get(mCurrentIndex));
        }
    }

    public ArrayList<Song> getPlaylist() {
        return mPlaylist;
    }

    public void addToPlaylist(Song song) {
        mPlaylist.add(song);
    }

    public void removeFromPlaylist(int index) {
        if (index >= 0 && index < mPlaylist.size()) {
            mPlaylist.remove(index);
            if (index == mCurrentIndex) {
                next();
            } else if (index < mCurrentIndex) {
                mCurrentIndex--;
            }
        }
    }

    public Song getCurrentSong() {
        return mPlayer.getCurrentSong();
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public void play() {
        if (mPlayer.getCurrentSong() == null && mPlaylist.size() > 0) {
            if (mCurrentIndex < 0) mCurrentIndex = 0;
            loadAndPlay(mPlaylist.get(mCurrentIndex));
        } else {
            mPlayer.play();
            updateNotification(true);
            if (mServiceCallback != null) mServiceCallback.onPlaybackStateChanged(true);
        }
    }

    public void pause() {
        mPlayer.pause();
        updateNotification(false);
        if (mServiceCallback != null) mServiceCallback.onPlaybackStateChanged(false);
    }

    public void togglePlayPause() {
        if (mPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void next() {
        if (mPlaylist.size() == 0) return;
        if (PreferenceManager.isShuffle()) {
            mCurrentIndex = mRandom.nextInt(mPlaylist.size());
        } else {
            mCurrentIndex++;
            if (mCurrentIndex >= mPlaylist.size()) {
                mCurrentIndex = 0;
            }
        }
        loadAndPlay(mPlaylist.get(mCurrentIndex));
    }

    public void previous() {
        if (mPlaylist.size() == 0) return;
        if (PreferenceManager.isShuffle()) {
            mCurrentIndex = mRandom.nextInt(mPlaylist.size());
        } else {
            mCurrentIndex--;
            if (mCurrentIndex < 0) {
                mCurrentIndex = mPlaylist.size() - 1;
            }
        }
        loadAndPlay(mPlaylist.get(mCurrentIndex));
    }

    public void seekTo(int position) {
        mPlayer.seekTo(position);
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public int getPosition() {
        return mPlayer.getPosition();
    }

    public int getDuration() {
        return mPlayer.getDuration();
    }

    private void loadAndPlay(Song song) {
        if (song == null) return;
        mPlayer.load(song);
        MusicDB.getInstance(this).addToHistory(song);
        PreferenceManager.setLastSong(song.path);
    }

    @Override
    public void onPrepared() {
        mPlayer.play();
        updateNotification(true);
        if (mServiceCallback != null) {
            mServiceCallback.onSongChanged(mPlayer.getCurrentSong());
            mServiceCallback.onPlaybackStateChanged(true);
        }
    }

    @Override
    public void onCompletion() {
        int repeatMode = PreferenceManager.getRepeatMode();
        if (repeatMode == 2) {
            mPlayer.seekTo(0);
            mPlayer.play();
        } else {
            if (mCurrentIndex >= mPlaylist.size() - 1 && repeatMode == 0) {
                pause();
                if (mServiceCallback != null) mServiceCallback.onPlaybackStateChanged(false);
            } else {
                next();
            }
        }
    }

    @Override
    public void onError(int what, int extra) {
        next();
    }

    @Override
    public void onProgressUpdate(int position, int duration) {
        if (mServiceCallback != null) {
            mServiceCallback.onProgressUpdate(position, duration);
        }
    }

    public void setServiceCallback(ServiceCallback callback) {
        mServiceCallback = callback;
    }

    private void updateNotification(boolean isPlaying) {
        Song song = mPlayer.getCurrentSong();
        if (song == null) return;

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_player);
        views.setTextViewText(R.id.notif_title, song.title);
        views.setTextViewText(R.id.notif_artist, song.artist);
        views.setImageViewResource(R.id.notif_play_pause,
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

        PendingIntent playPauseIntent = PendingIntent.getService(this, 0,
                new Intent(ACTION_TOGGLE).setClass(this, MusicService.class), 0);
        PendingIntent nextIntent = PendingIntent.getService(this, 1,
                new Intent(ACTION_NEXT).setClass(this, MusicService.class), 0);
        PendingIntent prevIntent = PendingIntent.getService(this, 2,
                new Intent(ACTION_PREV).setClass(this, MusicService.class), 0);
        PendingIntent stopIntent = PendingIntent.getService(this, 3,
                new Intent(ACTION_STOP).setClass(this, MusicService.class), 0);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        views.setOnClickPendingIntent(R.id.notif_play_pause, playPauseIntent);
        views.setOnClickPendingIntent(R.id.notif_next, nextIntent);
        views.setOnClickPendingIntent(R.id.notif_prev, prevIntent);
        views.setOnClickPendingIntent(R.id.notif_close, stopIntent);

        Notification notification = new Notification();
        notification.icon = R.drawable.ic_launcher;
        notification.contentView = views;
        notification.contentIntent = contentIntent;
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        notification.tickerText = song.title + " - " + song.artist;

        startForeground(NOTIFICATION_ID, notification);
    }

    public MusicPlayer getPlayer() {
        return mPlayer;
    }
}