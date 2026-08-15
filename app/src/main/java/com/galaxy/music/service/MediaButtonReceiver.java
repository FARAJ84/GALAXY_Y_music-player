package com.galaxyy.music.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaButtonReceiver extends BroadcastReceiver {
    private static long mLastClickTime = 0;
    private static int mClickCount = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) return;

            int keyCode = event.getKeyCode();
            Intent serviceIntent = new Intent(context, MusicService.class);

            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - mLastClickTime < 500) {
                        mClickCount++;
                    } else {
                        mClickCount = 1;
                    }
                    mLastClickTime = currentTime;

                    if (mClickCount == 1) {
                        serviceIntent.setAction(MusicService.ACTION_TOGGLE);
                    } else if (mClickCount == 2) {
                        serviceIntent.setAction(MusicService.ACTION_NEXT);
                        mClickCount = 0;
                    } else if (mClickCount >= 3) {
                        serviceIntent.setAction(MusicService.ACTION_PREV);
                        mClickCount = 0;
                    }
                    context.startService(serviceIntent);
                    break;

                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    serviceIntent.setAction(MusicService.ACTION_NEXT);
                    context.startService(serviceIntent);
                    break;

                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    serviceIntent.setAction(MusicService.ACTION_PREV);
                    context.startService(serviceIntent);
                    break;

                case KeyEvent.KEYCODE_MEDIA_STOP:
                    serviceIntent.setAction(MusicService.ACTION_STOP);
                    context.startService(serviceIntent);
                    break;
            }
            abortBroadcast();
        }
    }
}