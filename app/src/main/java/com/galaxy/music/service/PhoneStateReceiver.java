package com.galaxyy.music.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Intent serviceIntent = new Intent(context, MusicService.class);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) ||
            TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            serviceIntent.setAction(MusicService.ACTION_PAUSE);
            context.startService(serviceIntent);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            serviceIntent.setAction(MusicService.ACTION_PLAY);
            context.startService(serviceIntent);
        }
    }
}