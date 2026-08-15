package com.galaxyy.music;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Lightweight song model optimized for low memory devices.
 * Implements Parcelable for fast inter-activity transfer without serialization overhead.
 */
public class Song implements Parcelable {
    public long id;
    public String title;
    public String artist;
    public String album;
    public String path;
    public long duration;
    public long albumId;
    public int trackNumber;

    public Song() {}

    public Song(long id, String title, String artist, String album, String path, long duration, long albumId) {
        this.id = id;
        this.title = title != null ? title : "Unknown Title";
        this.artist = artist != null && artist.length() > 0 ? artist : "Unknown Artist";
        this.album = album != null ? album : "Unknown Album";
        this.path = path;
        this.duration = duration;
        this.albumId = albumId;
    }

    protected Song(Parcel in) {
        id = in.readLong();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        path = in.readString();
        duration = in.readLong();
        albumId = in.readLong();
        trackNumber = in.readInt();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        public Song createFromParcel(Parcel in) { return new Song(in); }
        public Song[] newArray(int size) { return new Song[size]; }
    };

    public int describeContents() { return 0; }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(path);
        dest.writeLong(duration);
        dest.writeLong(albumId);
        dest.writeInt(trackNumber);
    }

    @Override
    public String toString() {
        return title + " - " + artist;
    }
}