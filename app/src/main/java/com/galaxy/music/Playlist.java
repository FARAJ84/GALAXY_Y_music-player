package com.galaxyy.music;

import java.util.ArrayList;

/**
 * Playlist model with minimal memory footprint.
 */
public class Playlist {
    public long id;
    public String name;
    public ArrayList<Song> songs;
    public long dateAdded;

    public Playlist(long id, String name) {
        this.id = id;
        this.name = name;
        this.songs = new ArrayList<Song>();
        this.dateAdded = System.currentTimeMillis();
    }

    public int getSongCount() {
        return songs != null ? songs.size() : 0;
    }

    public long getTotalDuration() {
        long total = 0;
        if (songs != null) {
            for (Song s : songs) {
                total += s.duration;
            }
        }
        return total;
    }
}