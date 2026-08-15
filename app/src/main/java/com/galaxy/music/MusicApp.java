package com.galaxyy.music;

import android.app.Application;
import android.content.Context;

/**
 * Application singleton for global context access.
 * Galaxy Y has limited memory, so we keep this extremely light.
 */
public class MusicApp extends Application {
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }

    public static Context getContext() {
        return sContext;
    }
}