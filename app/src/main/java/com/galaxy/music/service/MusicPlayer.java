package com.galaxyy.music.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import com.galaxyy.music.Song;
import com.galaxyy.music.util.PreferenceManager;
import java.io.IOException;

public class MusicPlayer implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {

    private static final String TAG = "GalaxyYMusic";
    private static final int FADE_STEPS = 20;

    public interface Callback {
        void onPrepared();
        void onCompletion();
        void onError(int what, int extra);
        void onProgressUpdate(int position, int duration);
    }

    private MediaPlayer mPlayer;
    private Equalizer mEqualizer;
    private BassBoost mBassBoost;
    private Virtualizer mVirtualizer;
    private Callback mCallback;
    private Handler mProgressHandler;
    private Runnable mProgressRunnable;
    private Song mCurrentSong;
    private Context mContext;
    private boolean mIsPrepared = false;
    private boolean mIsFading = false;

    public MusicPlayer(Context context) {
        mContext = context.getApplicationContext();
        mProgressHandler = new Handler();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void load(Song song) {
        release();
        mCurrentSong = song;
        mIsPrepared = false;

        try {
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(mContext, Uri.parse("file://" + song.path));
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnPreparedListener(this);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Failed to load song: " + e.getMessage());
            if (mCallback != null) mCallback.onError(0, 0);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mIsPrepared = true;
        initAudioEffects();
        startProgressUpdates();
        if (mCallback != null) mCallback.onPrepared();
    }

    public void play() {
        if (mPlayer != null && mIsPrepared && !mPlayer.isPlaying()) {
            int fadeMs = PreferenceManager.getFadeDuration();
            if (fadeMs > 0 && !mIsFading) {
                fadeIn(fadeMs);
            } else {
                mPlayer.start();
            }
            startProgressUpdates();
        }
    }

    public void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            int fadeMs = PreferenceManager.getFadeDuration();
            if (fadeMs > 0) {
                fadeOutAndPause(fadeMs);
            } else {
                mPlayer.pause();
            }
            stopProgressUpdates();
        }
    }

    public void stop() {
        stopProgressUpdates();
        if (mPlayer != null && mIsPrepared) {
            mPlayer.stop();
            mIsPrepared = false;
        }
    }

    public void seekTo(int position) {
        if (mPlayer != null && mIsPrepared) {
            mPlayer.seekTo(position);
        }
    }

    public int getPosition() {
        if (mPlayer != null && mIsPrepared) {
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mPlayer != null && mIsPrepared) {
            return mPlayer.getDuration();
        }
        return 0;
    }

    public boolean isPlaying() {
        return mPlayer != null && mIsPrepared && mPlayer.isPlaying();
    }

    public Song getCurrentSong() {
        return mCurrentSong;
    }

    private void fadeIn(final int durationMs) {
        mIsFading = true;
        mPlayer.setVolume(0f, 0f);
        mPlayer.start();
        final float step = 1.0f / FADE_STEPS;
        final int delay = durationMs / FADE_STEPS;
        final Handler handler = new Handler();
        Runnable fade = new Runnable() {
            float volume = 0f;
            public void run() {
                volume += step;
                if (volume >= 1.0f) {
                    mPlayer.setVolume(1f, 1f);
                    mIsFading = false;
                } else {
                    mPlayer.setVolume(volume, volume);
                    handler.postDelayed(this, delay);
                }
            }
        };
        handler.postDelayed(fade, delay);
    }

    private void fadeOutAndPause(final int durationMs) {
        mIsFading = true;
        final float step = 1.0f / FADE_STEPS;
        final int delay = durationMs / FADE_STEPS;
        final Handler handler = new Handler();
        Runnable fade = new Runnable() {
            float volume = 1.0f;
            public void run() {
                volume -= step;
                if (volume <= 0f) {
                    mPlayer.setVolume(0f, 0f);
                    mPlayer.pause();
                    mPlayer.setVolume(1f, 1f);
                    mIsFading = false;
                } else {
                    mPlayer.setVolume(volume, volume);
                    handler.postDelayed(this, delay);
                }
            }
        };
        handler.postDelayed(fade, delay);
    }

    private void initAudioEffects() {
        if (mPlayer == null) return;
        int audioSession = mPlayer.getAudioSessionId();

        try {
            if (mEqualizer != null) mEqualizer.release();
            mEqualizer = new Equalizer(0, audioSession);
            mEqualizer.setEnabled(PreferenceManager.isEqEnabled());
            applyEqPreset(PreferenceManager.getEqPreset());
        } catch (Exception e) {
            Log.w(TAG, "EQ not available: " + e.getMessage());
        }

        try {
            if (mBassBoost != null) mBassBoost.release();
            mBassBoost = new BassBoost(0, audioSession);
            mBassBoost.setEnabled(PreferenceManager.getBassBoost() > 0);
            mBassBoost.setStrength((short) PreferenceManager.getBassBoost());
        } catch (Exception e) {
            Log.w(TAG, "BassBoost not available: " + e.getMessage());
        }

        try {
            if (mVirtualizer != null) mVirtualizer.release();
            mVirtualizer = new Virtualizer(0, audioSession);
            mVirtualizer.setEnabled(PreferenceManager.getVirtualizer() > 0);
            mVirtualizer.setStrength((short) PreferenceManager.getVirtualizer());
        } catch (Exception e) {
            Log.w(TAG, "Virtualizer not available: " + e.getMessage());
        }
    }

    public void applyEqPreset(int presetIndex) {
        if (mEqualizer == null || !mEqualizer.getEnabled()) return;
        try {
            if (presetIndex == 0) {
                short bands = mEqualizer.getNumberOfBands();
                for (short b = 0; b < bands; b++) {
                    mEqualizer.setBandLevel(b, (short) 0);
                }
            } else if (presetIndex < mEqualizer.getNumberOfPresets() + 1) {
                mEqualizer.usePreset((short) (presetIndex - 1));
            }
        } catch (Exception e) {
            Log.w(TAG, "EQ preset error: " + e.getMessage());
        }
    }

    public void setEqEnabled(boolean enabled) {
        if (mEqualizer != null) {
            mEqualizer.setEnabled(enabled);
        }
    }

    public void setBassBoost(short strength) {
        if (mBassBoost != null) {
            mBassBoost.setEnabled(strength > 0);
            mBassBoost.setStrength(strength);
        }
    }

    public void setVirtualizer(short strength) {
        if (mVirtualizer != null) {
            mVirtualizer.setEnabled(strength > 0);
            mVirtualizer.setStrength(strength);
        }
    }

    public Equalizer getEqualizer() {
        return mEqualizer;
    }

    private void startProgressUpdates() {
        stopProgressUpdates();
        mProgressRunnable = new Runnable() {
            public void run() {
                if (mPlayer != null && mIsPrepared && mCallback != null) {
                    mCallback.onProgressUpdate(getPosition(), getDuration());
                }
                mProgressHandler.postDelayed(this, 500);
            }
        };
        mProgressHandler.postDelayed(mProgressRunnable, 500);
    }

    private void stopProgressUpdates() {
        if (mProgressRunnable != null) {
            mProgressHandler.removeCallbacks(mProgressRunnable);
            mProgressRunnable = null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopProgressUpdates();
        if (mCallback != null) mCallback.onCompletion();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Player error: " + what + "," + extra);
        mIsPrepared = false;
        if (mCallback != null) mCallback.onError(what, extra);
        return true;
    }

    public void release() {
        stopProgressUpdates();
        mIsPrepared = false;
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (mEqualizer != null) {
            mEqualizer.release();
            mEqualizer = null;
        }
        if (mBassBoost != null) {
            mBassBoost.release();
            mBassBoost = null;
        }
        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
    }
}