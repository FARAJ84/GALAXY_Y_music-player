package com.galaxyy.music.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.galaxyy.music.MusicApp;

/**
 * Centralized settings manager.
 * All preferences are stored with minimal overhead.
 */
public class PreferenceManager {
    private static final String PREFS_NAME = "GalaxyYMusicPrefs";
    private static SharedPreferences sPrefs;

    public static final String KEY_REPEAT_MODE = "repeat_mode";
    public static final String KEY_SHUFFLE = "shuffle";
    public static final String KEY_EQUALIZER_ENABLED = "eq_enabled";
    public static final String KEY_EQUALIZER_PRESET = "eq_preset";
    public static final String KEY_BASS_BOOST = "bass_boost";
    public static final String KEY_VIRTUALIZER = "virtualizer";
    public static final String KEY_LAST_SONG = "last_song_path";
    public static final String KEY_LAST_POSITION = "last_position";
    public static final String KEY_THEME = "theme";
    public static final String KEY_SHOW_HIDDEN = "show_hidden";
    public static final String KEY_SCAN_DEPTH = "scan_depth";
    public static final String KEY_HEADSET_AUTOPLAY = "headset_autoplay";
    public static final String KEY_FADE_DURATION = "fade_duration";
    public static final String KEY_GAPLESS = "gapless";
    public static final String KEY_SORT_ORDER = "sort_order";
    public static final String KEY_FILTER_DURATION = "filter_duration";
    public static final String KEY_SHOW_LYRICS = "show_lyrics";
    public static final String KEY_LYRICS_AUTO_SCROLL = "lyrics_auto_scroll";

    private static SharedPreferences getPrefs() {
        if (sPrefs == null) {
            sPrefs = MusicApp.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        return sPrefs;
    }

    public static int getRepeatMode() { return getPrefs().getInt(KEY_REPEAT_MODE, 0); }
    public static void setRepeatMode(int mode) { getPrefs().edit().putInt(KEY_REPEAT_MODE, mode).commit(); }

    public static boolean isShuffle() { return getPrefs().getBoolean(KEY_SHUFFLE, false); }
    public static void setShuffle(boolean shuffle) { getPrefs().edit().putBoolean(KEY_SHUFFLE, shuffle).commit(); }

    public static boolean isEqEnabled() { return getPrefs().getBoolean(KEY_EQUALIZER_ENABLED, false); }
    public static void setEqEnabled(boolean enabled) { getPrefs().edit().putBoolean(KEY_EQUALIZER_ENABLED, enabled).commit(); }

    public static int getEqPreset() { return getPrefs().getInt(KEY_EQUALIZER_PRESET, 0); }
    public static void setEqPreset(int preset) { getPrefs().edit().putInt(KEY_EQUALIZER_PRESET, preset).commit(); }

    public static int getBassBoost() { return getPrefs().getInt(KEY_BASS_BOOST, 0); }
    public static void setBassBoost(int level) { getPrefs().edit().putInt(KEY_BASS_BOOST, level).commit(); }

    public static int getVirtualizer() { return getPrefs().getInt(KEY_VIRTUALIZER, 0); }
    public static void setVirtualizer(int level) { getPrefs().edit().putInt(KEY_VIRTUALIZER, level).commit(); }

    public static String getLastSong() { return getPrefs().getString(KEY_LAST_SONG, null); }
    public static void setLastSong(String path) { getPrefs().edit().putString(KEY_LAST_SONG, path).commit(); }

    public static int getLastPosition() { return getPrefs().getInt(KEY_LAST_POSITION, 0); }
    public static void setLastPosition(int pos) { getPrefs().edit().putInt(KEY_LAST_POSITION, pos).commit(); }

    public static int getTheme() { return getPrefs().getInt(KEY_THEME, 0); }
    public static void setTheme(int theme) { getPrefs().edit().putInt(KEY_THEME, theme).commit(); }

    public static boolean isShowHidden() { return getPrefs().getBoolean(KEY_SHOW_HIDDEN, false); }
    public static void setShowHidden(boolean show) { getPrefs().edit().putBoolean(KEY_SHOW_HIDDEN, show).commit(); }

    public static boolean isHeadsetAutoplay() { return getPrefs().getBoolean(KEY_HEADSET_AUTOPLAY, true); }
    public static void setHeadsetAutoplay(boolean auto) { getPrefs().edit().putBoolean(KEY_HEADSET_AUTOPLAY, auto).commit(); }

    public static int getFadeDuration() { return getPrefs().getInt(KEY_FADE_DURATION, 0); }
    public static void setFadeDuration(int ms) { getPrefs().edit().putInt(KEY_FADE_DURATION, ms).commit(); }

    public static boolean isGapless() { return getPrefs().getBoolean(KEY_GAPLESS, false); }
    public static void setGapless(boolean gapless) { getPrefs().edit().putBoolean(KEY_GAPLESS, gapless).commit(); }

    public static int getSortOrder() { return getPrefs().getInt(KEY_SORT_ORDER, 0); }
    public static void setSortOrder(int order) { getPrefs().edit().putInt(KEY_SORT_ORDER, order).commit(); }

    public static int getFilterDuration() { return getPrefs().getInt(KEY_FILTER_DURATION, 0); }
    public static void setFilterDuration(int seconds) { getPrefs().edit().putInt(KEY_FILTER_DURATION, seconds).commit(); }

    public static boolean isShowLyrics() { return getPrefs().getBoolean(KEY_SHOW_LYRICS, true); }
    public static void setShowLyrics(boolean show) { getPrefs().edit().putBoolean(KEY_SHOW_LYRICS, show).commit(); }

    public static boolean isLyricsAutoScroll() { return getPrefs().getBoolean(KEY_LYRICS_AUTO_SCROLL, true); }
    public static void setLyricsAutoScroll(boolean auto) { getPrefs().edit().putBoolean(KEY_LYRICS_AUTO_SCROLL, auto).commit(); }
}