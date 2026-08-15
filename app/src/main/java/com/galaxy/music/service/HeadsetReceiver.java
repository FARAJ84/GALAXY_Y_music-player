package com.galaxyy.music.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.galaxyy.music.util.PreferenceManager;

public class HeadsetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
            int state = intent.getIntExtra("state", -1);
            Intent serviceIntent = new Intent(context, MusicService.class);

            if (state == 0) {
                serviceIntent.setAction(MusicService.ACTION_PAUSE);
                context.startService(serviceIntent);
            } else if (state == 1) {
                if (PreferenceManager.isHeadsetAutoplay()) {
                    serviceIntent.setAction(MusicService.ACTION_PLAY);
                    context.startService(serviceIntent);
                }
            }
        }
    }
}