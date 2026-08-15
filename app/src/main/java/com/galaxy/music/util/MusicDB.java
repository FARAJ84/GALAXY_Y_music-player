package com.galaxyy.music.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.galaxyy.music.Playlist;
import com.galaxyy.music.Song;
import java.util.ArrayList;

/**
 * Lightweight SQLite database for playlists, favorites, and playback history.
 * Optimized for devices with limited RAM by using cursor recycling and batch operations.
 */
public class MusicDB extends SQLiteOpenHelper {
    private static final String DB_NAME = "galaxyy_music.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";
    private static final String TABLE_FAVORITES = "favorites";
    private static final String TABLE_HISTORY = "history";

    private static MusicDB sInstance;

    public static synchronized MusicDB getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MusicDB(context.getApplicationContext());
        }
        return sInstance;
    }

    private MusicDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PLAYLISTS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, date_added INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_PLAYLIST_SONGS + " (playlist_id INTEGER, song_path TEXT, song_title TEXT, song_artist TEXT, song_duration INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " (song_path TEXT PRIMARY KEY, song_title TEXT, song_artist TEXT, added_date INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (song_path TEXT, played_at INTEGER, duration INTEGER)");
        db.execSQL("CREATE TABLE recently_added (song_path TEXT PRIMARY KEY, added_date INTEGER)");
        db.execSQL("CREATE TABLE song_folders (song_path TEXT, folder TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long createPlaylist(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("date_added", System.currentTimeMillis());
        return db.insert(TABLE_PLAYLISTS, null, values);
    }

    public void deletePlaylist(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLAYLISTS, "id=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_PLAYLIST_SONGS, "playlist_id=?", new String[]{String.valueOf(id)});
    }

    public void renamePlaylist(long id, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        db.update(TABLE_PLAYLISTS, values, "id=?", new String[]{String.valueOf(id)});
    }

    public ArrayList<Playlist> getAllPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<Playlist>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLISTS, null, null, null, null, null, "date_added DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Playlist p = new Playlist(cursor.getLong(cursor.getColumnIndex("id")),
                        cursor.getString(cursor.getColumnIndex("name")));
                p.dateAdded = cursor.getLong(cursor.getColumnIndex("date_added"));
                playlists.add(p);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return playlists;
    }

    public void addSongToPlaylist(long playlistId, Song song) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("playlist_id", playlistId);
        values.put("song_path", song.path);
        values.put("song_title", song.title);
        values.put("song_artist", song.artist);
        values.put("song_duration", song.duration);
        db.insert(TABLE_PLAYLIST_SONGS, null, values);
    }

    public void removeSongFromPlaylist(long playlistId, String songPath) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLAYLIST_SONGS, "playlist_id=? AND song_path=?",
                new String[]{String.valueOf(playlistId), songPath});
    }

    public ArrayList<Song> getPlaylistSongs(long playlistId) {
        ArrayList<Song> songs = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLIST_SONGS, null, "playlist_id=?",
                new String[]{String.valueOf(playlistId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Song s = new Song();
                s.path = cursor.getString(cursor.getColumnIndex("song_path"));
                s.title = cursor.getString(cursor.getColumnIndex("song_title"));
                s.artist = cursor.getString(cursor.getColumnIndex("song_artist"));
                s.duration = cursor.getLong(cursor.getColumnIndex("song_duration"));
                songs.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songs;
    }

    public void addFavorite(Song song) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("song_path", song.path);
        values.put("song_title", song.title);
        values.put("song_artist", song.artist);
        values.put("added_date", System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeFavorite(String songPath) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_FAVORITES, "song_path=?", new String[]{songPath});
    }

    public boolean isFavorite(String songPath) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, new String[]{"song_path"}, "song_path=?",
                new String[]{songPath}, null, null, null);
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return exists;
    }

    public ArrayList<Song> getFavorites() {
        ArrayList<Song> songs = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, null, null, null, null, null, "added_date DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Song s = new Song();
                s.path = cursor.getString(cursor.getColumnIndex("song_path"));
                s.title = cursor.getString(cursor.getColumnIndex("song_title"));
                s.artist = cursor.getString(cursor.getColumnIndex("song_artist"));
                songs.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songs;
    }

    public void addToHistory(Song song) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("song_path", song.path);
        values.put("played_at", System.currentTimeMillis());
        values.put("duration", song.duration);
        db.insert(TABLE_HISTORY, null, values);
        db.execSQL("DELETE FROM " + TABLE_HISTORY + " WHERE rowid NOT IN (SELECT rowid FROM " +
                TABLE_HISTORY + " ORDER BY played_at DESC LIMIT 100)");
    }

    public ArrayList<Song> getHistory() {
        ArrayList<Song> songs = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null, "played_at DESC LIMIT 50");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Song s = new Song();
                s.path = cursor.getString(cursor.getColumnIndex("song_path"));
                songs.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songs;
    }

    public void markRecentlyAdded(String songPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("song_path", songPath);
        values.put("added_date", System.currentTimeMillis());
        db.insertWithOnConflict("recently_added", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.execSQL("DELETE FROM recently_added WHERE rowid NOT IN (SELECT rowid FROM recently_added ORDER BY added_date DESC LIMIT 200)");
    }

    public ArrayList<Song> getRecentlyAdded(int limit) {
        ArrayList<Song> songs = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("recently_added", null, null, null, null, null, "added_date DESC LIMIT " + limit);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Song s = new Song();
                s.path = cursor.getString(cursor.getColumnIndex("song_path"));
                songs.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songs;
    }

    public ArrayList<ArrayList<Song>> findDuplicates(ArrayList<Song> allSongs) {
        ArrayList<ArrayList<Song>> duplicates = new ArrayList<ArrayList<Song>>();
        java.util.HashMap<String, ArrayList<Song>> groups = new java.util.HashMap<String, ArrayList<Song>>();

        for (Song s : allSongs) {
            String key = (s.artist + "|" + s.title + "|" + s.duration).toLowerCase();
            ArrayList<Song> group = groups.get(key);
            if (group == null) {
                group = new ArrayList<Song>();
                groups.put(key, group);
            }
            group.add(s);
        }

        for (ArrayList<Song> group : groups.values()) {
            if (group.size() > 1) {
                duplicates.add(group);
            }
        }
        return duplicates;
    }

    public ArrayList<String> getAllFolders() {
        ArrayList<String> folders = new ArrayList<String>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT folder FROM song_folders ORDER BY folder", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                folders.add(cursor.getString(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return folders;
    }

    public void addSongFolder(String songPath, String folder) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("song_path", songPath);
        values.put("folder", folder);
        db.insertWithOnConflict("song_folders", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
