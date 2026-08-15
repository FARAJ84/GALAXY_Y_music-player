package com.galaxyy.music.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import com.galaxyy.music.R;
import com.galaxyy.music.Song;
import com.galaxyy.music.service.MusicService;
import com.galaxyy.music.util.LrcParser;
import com.galaxyy.music.util.PreferenceManager;
import java.util.ArrayList;

public class LyricsActivity extends Activity implements MusicService.ServiceCallback {

    private MusicService mService;
    private boolean mBound = false;
    private ListView mListView;
    private TextView mNoLyricsText;
    private CheckBox mAutoScrollBox;
    private LrcParser mParser;
    private LyricAdapter mAdapter;
    private Handler mSyncHandler = new Handler();
    private Runnable mSyncRunnable;
    private int mCurrentLine = -1;
    private boolean mAutoScroll = true;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.MusicBinder) service).getService();
            mService.setServiceCallback(LyricsActivity.this);
            mBound = true;
            loadLyrics();
        }
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);

        mListView = (ListView) findViewById(R.id.lyrics_list);
        mNoLyricsText = (TextView) findViewById(R.id.no_lyrics);
        mAutoScrollBox = (CheckBox) findViewById(R.id.auto_scroll);

        mAutoScroll = PreferenceManager.isLyricsAutoScroll();
        mAutoScrollBox.setChecked(mAutoScroll);
        mAutoScrollBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAutoScroll = isChecked;
                PreferenceManager.setLyricsAutoScroll(isChecked);
            }
        });

        bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadLyrics() {
        Song song = mService != null ? mService.getCurrentSong() : null;
        if (song == null) {
            showNoLyrics();
            return;
        }

        mParser = new LrcParser();
        if (mParser.loadForSong(song.path)) {
            mAdapter = new LyricAdapter(mParser.getLines());
            mListView.setAdapter(mAdapter);
            mNoLyricsText.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            startSyncTimer();
        } else {
            showNoLyrics();
        }
    }

    private void showNoLyrics() {
        mNoLyricsText.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
    }

    private void startSyncTimer() {
        stopSyncTimer();
        mSyncRunnable = new Runnable() {
            public void run() {
                if (mBound && mService != null && mParser != null && mParser.hasLyrics()) {
                    int newLine = mParser.findCurrentLine(mService.getPosition());
                    if (newLine != mCurrentLine && newLine >= 0) {
                        mCurrentLine = newLine;
                        mAdapter.setHighlightLine(newLine);
                        if (mAutoScroll) {
                            mListView.setSelection(newLine);
                        }
                    }
                }
                mSyncHandler.postDelayed(this, 400);
            }
        };
        mSyncHandler.postDelayed(mSyncRunnable, 400);
    }

    private void stopSyncTimer() {
        if (mSyncRunnable != null) {
            mSyncHandler.removeCallbacks(mSyncRunnable);
            mSyncRunnable = null;
        }
    }

    @Override
    public void onSongChanged(Song song) {
        mCurrentLine = -1;
        loadLyrics();
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (isPlaying) {
            startSyncTimer();
        } else {
            stopSyncTimer();
        }
    }

    @Override
    public void onProgressUpdate(int position, int duration) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSyncTimer();
        if (mBound) {
            mService.setServiceCallback(null);
            unbindService(mConnection);
        }
    }

    private class LyricAdapter extends BaseAdapter {
        private ArrayList<LrcParser.LrcLine> mLines;
        private int mHighlight = -1;

        LyricAdapter(ArrayList<LrcParser.LrcLine> lines) {
            mLines = lines;
        }

        void setHighlightLine(int line) {
            mHighlight = line;
            notifyDataSetChanged();
        }

        public int getCount() { return mLines.size(); }
        public Object getItem(int position) { return mLines.get(position); }
        public long getItemId(int position) { return position; }

        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = new TextView(LyricsActivity.this);
                tv.setPadding(16, 8, 16, 8);
                tv.setTextSize(14);
            } else {
                tv = (TextView) convertView;
            }
            tv.setText(mLines.get(position).text);
            if (position == mHighlight) {
                tv.setTextColor(0xFF00CCFF);
                tv.setTextSize(16);
            } else {
                tv.setTextColor(0xFFAAAAAA);
                tv.setTextSize(14);
            }
            return tv;
        }
    }
}