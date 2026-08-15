package com.galaxyy.music.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.galaxyy.music.R;
import com.galaxyy.music.Song;
import com.galaxyy.music.service.MusicService;
import com.galaxyy.music.util.PreferenceManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity implements MusicService.ServiceCallback {

    private MusicService mService;
    private boolean mBound = false;
    private Handler mHandler = new Handler();

    private TextView mTitleText, mArtistText, mAlbumText;
    private TextView mCurrentTime, mTotalTime;
    private SeekBar mProgressBar;
    private ImageButton mPlayPauseBtn, mPrevBtn, mNextBtn;
    private ImageButton mShuffleBtn, mRepeatBtn;
    private ImageButton mPlaylistBtn, mEqualizerBtn, mSettingsBtn, mFilesBtn, mLyricsBtn;
    private ImageView mAlbumArt;
    private View mRepeatIndicator, mShuffleIndicator;

    private boolean mUserSeeking = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mService = binder.getService();
            mService.setServiceCallback(MainActivity.this);
            mBound = true;
            updateUI();
        }
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();
        loadLastSong();
        startAndBindService();
    }

    private void initViews() {
        mTitleText = (TextView) findViewById(R.id.song_title);
        mArtistText = (TextView) findViewById(R.id.song_artist);
        mAlbumText = (TextView) findViewById(R.id.song_album);
        mCurrentTime = (TextView) findViewById(R.id.current_time);
        mTotalTime = (TextView) findViewById(R.id.total_time);
        mProgressBar = (SeekBar) findViewById(R.id.progress_bar);
        mPlayPauseBtn = (ImageButton) findViewById(R.id.btn_play_pause);
        mPrevBtn = (ImageButton) findViewById(R.id.btn_prev);
        mNextBtn = (ImageButton) findViewById(R.id.btn_next);
        mShuffleBtn = (ImageButton) findViewById(R.id.btn_shuffle);
        mRepeatBtn = (ImageButton) findViewById(R.id.btn_repeat);
        mPlaylistBtn = (ImageButton) findViewById(R.id.btn_playlist);
        mEqualizerBtn = (ImageButton) findViewById(R.id.btn_equalizer);
        mSettingsBtn = (ImageButton) findViewById(R.id.btn_settings);
        mFilesBtn = (ImageButton) findViewById(R.id.btn_files);
        mLyricsBtn = (ImageButton) findViewById(R.id.btn_lyrics);
        mAlbumArt = (ImageView) findViewById(R.id.album_art);
        mRepeatIndicator = findViewById(R.id.repeat_indicator);
        mShuffleIndicator = findViewById(R.id.shuffle_indicator);

        updateRepeatShuffleUI();
    }

    private void initListeners() {
        mPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBound) mService.togglePlayPause();
            }
        });
        mPrevBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBound) mService.previous();
            }
        });
        mNextBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBound) mService.next();
            }
        });
        mShuffleBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean newState = !PreferenceManager.isShuffle();
                PreferenceManager.setShuffle(newState);
                updateRepeatShuffleUI();
                Toast.makeText(MainActivity.this,
                        newState ? R.string.shuffle_on : R.string.shuffle_off, Toast.LENGTH_SHORT).show();
            }
        });
        mRepeatBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int mode = PreferenceManager.getRepeatMode();
                mode = (mode + 1) % 3;
                PreferenceManager.setRepeatMode(mode);
                updateRepeatShuffleUI();
                String[] labels = getResources().getStringArray(R.array.repeat_labels);
                Toast.makeText(MainActivity.this, labels[mode], Toast.LENGTH_SHORT).show();
            }
        });
        mPlaylistBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PlaylistActivity.class));
            }
        });
        mEqualizerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EqualizerActivity.class));
            }
        });
        mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        mFilesBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FileBrowserActivity.class));
            }
        });
        mLyricsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LyricsActivity.class));
            }
        });

        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mBound) {
                    mCurrentTime.setText(formatTime(progress));
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                mUserSeeking = true;
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserSeeking = false;
                if (mBound) mService.seekTo(seekBar.getProgress());
            }
        });
    }

    private void startAndBindService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadLastSong() {
        new Thread(new Runnable() {
            public void run() {
                final ArrayList<Song> songs = scanMediaStore();
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (mBound && songs.size() > 0) {
                            String lastPath = PreferenceManager.getLastSong();
                            int startIndex = 0;
                            if (lastPath != null) {
                                for (int i = 0; i < songs.size(); i++) {
                                    if (lastPath.equals(songs.get(i).path)) {
                                        startIndex = i;
                                        break;
                                    }
                                }
                            }
                            mService.setPlaylist(songs, startIndex);
                        }
                    }
                });
            }
        }).start();
    }

    private ArrayList<Song> scanMediaStore() {
        ArrayList<Song> songs = new ArrayList<Song>();
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";
        int minDuration = PreferenceManager.getFilterDuration() * 1000;
        if (minDuration > 0) {
            selection += " AND " + MediaStore.Audio.Media.DURATION + ">" + minDuration;
        }

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null,
                MediaStore.Audio.Media.TITLE + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Song song = new Song(
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                );
                songs.add(song);
            } while (cursor.moveToNext());
            cursor.close();
        }

        final int sortOrder = PreferenceManager.getSortOrder();
        Collections.sort(songs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                switch (sortOrder) {
                    case 1: return a.artist.compareToIgnoreCase(b.artist);
                    case 2: return a.album.compareToIgnoreCase(b.album);
                    case 3: return Long.valueOf(b.id).compareTo(a.id);
                    default: return a.title.compareToIgnoreCase(b.title);
                }
            }
        });

        return songs;
    }

    private void updateUI() {
        if (mService == null) return;
        Song song = mService.getCurrentSong();
        if (song != null) {
            mTitleText.setText(song.title);
            mArtistText.setText(song.artist);
            mAlbumText.setText(song.album);
            mTotalTime.setText(formatTime(song.duration));
            mProgressBar.setMax((int) song.duration);
            try {
                Uri artUri = Uri.parse("content://media/external/audio/albumart/" + song.albumId);
                mAlbumArt.setImageURI(artUri);
            } catch (Exception e) {
                mAlbumArt.setImageResource(R.drawable.ic_album_placeholder);
            }
        }
        updatePlayPauseButton(mService.isPlaying());
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        mPlayPauseBtn.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateRepeatShuffleUI() {
        int repeatMode = PreferenceManager.getRepeatMode();
        mRepeatIndicator.setVisibility(repeatMode > 0 ? View.VISIBLE : View.GONE);
        mShuffleIndicator.setVisibility(PreferenceManager.isShuffle() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSongChanged(Song song) {
        updateUI();
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updatePlayPauseButton(isPlaying);
    }

    @Override
    public void onProgressUpdate(int position, int duration) {
        if (!mUserSeeking) {
            mProgressBar.setProgress(position);
            mCurrentTime.setText(formatTime(position));
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            mService.setServiceCallback(null);
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_exit) {
            if (mBound) {
                mService.stopSelf();
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}