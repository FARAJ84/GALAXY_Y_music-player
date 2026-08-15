package com.galaxyy.music.util;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Lightweight LRC (LyRiCs) parser optimized for low-memory devices.
 * Supports standard [mm:ss.xx] and [mm:ss:xx] timestamp formats.
 * Does NOT use regex — uses manual char scanning for speed on 832MHz CPU.
 */
public class LrcParser {
    private static final String TAG = "GalaxyYLRC";

    public static class LrcLine implements Comparable<LrcLine> {
        public long timeMs;
        public String text;

        public LrcLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }

        public int compareTo(LrcLine other) {
            return Long.valueOf(this.timeMs).compareTo(other.timeMs);
        }
    }

    private ArrayList<LrcLine> mLines = new ArrayList<LrcLine>();
    private String mTitle;
    private String mArtist;
    private String mAlbum;

    public boolean loadForSong(String songPath) {
        mLines.clear();
        mTitle = null;
        mArtist = null;
        mAlbum = null;

        if (songPath == null) return false;

        File songFile = new File(songPath);
        File lrcFile = new File(songFile.getParent(), getBaseName(songFile.getName()) + ".lrc");

        if (!lrcFile.exists()) {
            File parent = songFile.getParentFile();
            if (parent != null) {
                String baseLower = getBaseName(songFile.getName()).toLowerCase();
                File[] files = parent.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().toLowerCase().equals(baseLower + ".lrc")) {
                            lrcFile = f;
                            break;
                        }
                    }
                }
            }
        }

        if (!lrcFile.exists()) return false;
        return parseFile(lrcFile);
    }

    private String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public boolean parseFile(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line.trim());
            }
            Collections.sort(mLines);
            return mLines.size() > 0;
        } catch (IOException e) {
            Log.w(TAG, "Failed to read LRC: " + e.getMessage());
            return false;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException e) {}
            }
        }
    }

    private void parseLine(String line) {
        if (line.length() == 0) return;

        if (line.startsWith("[ti:")) {
            mTitle = extractTag(line);
            return;
        }
        if (line.startsWith("[ar:")) {
            mArtist = extractTag(line);
            return;
        }
        if (line.startsWith("[al:")) {
            mAlbum = extractTag(line);
            return;
        }

        int lastBracket = line.lastIndexOf(']');
        if (lastBracket <= 0 || lastBracket >= line.length() - 1) return;

        String text = line.substring(lastBracket + 1).trim();
        String timestamps = line.substring(0, lastBracket + 1);

        int idx = 0;
        while (idx < timestamps.length()) {
            int open = timestamps.indexOf('[', idx);
            int close = timestamps.indexOf(']', open);
            if (open == -1 || close == -1 || close <= open + 1) break;

            long time = parseTimestamp(timestamps.substring(open + 1, close));
            if (time >= 0) {
                mLines.add(new LrcLine(time, text));
            }
            idx = close + 1;
        }
    }

    private String extractTag(String line) {
        int start = line.indexOf(':') + 1;
        int end = line.lastIndexOf(']');
        if (start > 0 && end > start) {
            return line.substring(start, end).trim();
        }
        return null;
    }

    private long parseTimestamp(String ts) {
        try {
            int colon1 = ts.indexOf(':');
            if (colon1 <= 0) return -1;

            int minutes = Integer.parseInt(ts.substring(0, colon1));

            int colon2 = ts.indexOf(':', colon1 + 1);
            int dot = ts.indexOf('.', colon1 + 1);

            int seconds;
            int centis = 0;

            if (colon2 > 0) {
                seconds = Integer.parseInt(ts.substring(colon1 + 1, colon2));
                centis = Integer.parseInt(ts.substring(colon2 + 1));
            } else if (dot > 0) {
                seconds = Integer.parseInt(ts.substring(colon1 + 1, dot));
                String frac = ts.substring(dot + 1);
                if (frac.length() == 2) {
                    centis = Integer.parseInt(frac);
                } else if (frac.length() == 1) {
                    centis = Integer.parseInt(frac) * 10;
                }
            } else {
                seconds = Integer.parseInt(ts.substring(colon1 + 1));
            }

            return minutes * 60000L + seconds * 1000L + centis * 10L;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public ArrayList<LrcLine> getLines() {
        return mLines;
    }

    public String getTitle() { return mTitle; }
    public String getArtist() { return mArtist; }
    public String getAlbum() { return mAlbum; }

    public int findCurrentLine(long positionMs) {
        if (mLines.size() == 0) return -1;

        int low = 0;
        int high = mLines.size() - 1;
        int result = 0;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midTime = mLines.get(mid).timeMs;

            if (midTime <= positionMs) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    public boolean hasLyrics() {
        return mLines.size() > 0;
    }
}