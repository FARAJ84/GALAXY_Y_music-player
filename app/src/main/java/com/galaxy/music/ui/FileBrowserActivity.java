package com.galaxyy.music.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.galaxyy.music.R;
import com.galaxyy.music.Song;
import com.galaxyy.music.util.MusicDB;
import com.galaxyy.music.util.PreferenceManager;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class FileBrowserActivity extends Activity {

    private ListView mListView;
    private FileAdapter mAdapter;
    private TextView mPathText;
    private Button mAddBtn;
    private File mCurrentDir;
    private ArrayList<File> mFiles = new ArrayList<File>();
    private ArrayList<File> mSelectedFiles = new ArrayList<File>();

    private static final String[] AUDIO_EXTENSIONS = {".mp3", ".ogg", ".wma", ".m4a", ".aac", ".wav", ".flac"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        mListView = (ListView) findViewById(R.id.list_view);
        mPathText = (TextView) findViewById(R.id.current_path);
        mAddBtn = (Button) findViewById(R.id.action_button);
        mAddBtn.setText(R.string.add_to_playlist);
        mAddBtn.setVisibility(View.GONE);

        mCurrentDir = Environment.getExternalStorageDirectory();
        loadDirectory(mCurrentDir);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = mFiles.get(position);
                if (file.isDirectory()) {
                    loadDirectory(file);
                } else {
                    toggleSelection(file);
                }
            }
        });

        mAddBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAddToPlaylistDialog();
            }
        });

        registerForContextMenu(mListView);
    }

    private void loadDirectory(File dir) {
        mCurrentDir = dir;
        mPathText.setText(dir.getAbsolutePath());
        mFiles.clear();
        mSelectedFiles.clear();
        mAddBtn.setVisibility(View.GONE);

        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (!PreferenceManager.isShowHidden() && name.startsWith(".")) return false;
                File f = new File(dir, name);
                if (f.isDirectory()) return true;
                String lower = name.toLowerCase();
                for (String ext : AUDIO_EXTENSIONS) {
                    if (lower.endsWith(ext)) return true;
                }
                return false;
            }
        });

        if (files != null) {
            mFiles.addAll(Arrays.asList(files));
            Collections.sort(mFiles, new Comparator<File>() {
                public int compare(File a, File b) {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
        }

        if (dir.getParentFile() != null) {
            mFiles.add(0, dir.getParentFile());
        }

        mAdapter = new FileAdapter();
        mListView.setAdapter(mAdapter);
    }

    private void toggleSelection(File file) {
        if (mSelectedFiles.contains(file)) {
            mSelectedFiles.remove(file);
        } else {
            mSelectedFiles.add(file);
        }
        mAdapter.notifyDataSetChanged();
        mAddBtn.setVisibility(mSelectedFiles.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private void showAddToPlaylistDialog() {
        final ArrayList<com.galaxyy.music.Playlist> playlists = MusicDB.getInstance(this).getAllPlaylists();
        if (playlists.size() == 0) {
            Toast.makeText(this, R.string.no_playlists, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;

        new AlertDialog.Builder(this)
            .setTitle(R.string.select_playlist)
            .setItems(names, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    long playlistId = playlists.get(which).id;
                    for (File f : mSelectedFiles) {
                        Song s = new Song();
                        s.path = f.getAbsolutePath();
                        s.title = f.getName();
                        s.artist = "Unknown";
                        s.duration = 0;
                        MusicDB.getInstance(FileBrowserActivity.this).addSongToPlaylist(playlistId, s);
                    }
                    Toast.makeText(FileBrowserActivity.this,
                            getString(R.string.added_songs, mSelectedFiles.size()), Toast.LENGTH_SHORT).show();
                    mSelectedFiles.clear();
                    mAdapter.notifyDataSetChanged();
                    mAddBtn.setVisibility(View.GONE);
                }
            })
            .show();
    }

    @Override
    public void onBackPressed() {
        if (!mCurrentDir.equals(Environment.getExternalStorageDirectory()) && mCurrentDir.getParentFile() != null) {
            loadDirectory(mCurrentDir.getParentFile());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.file_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        File file = mFiles.get(info.position);
        if (item.getItemId() == R.id.menu_edit_tags) {
            if (!file.isDirectory() && file.getName().toLowerCase().endsWith(".mp3")) {
                Intent intent = new Intent(this, TagEditorActivity.class);
                intent.putExtra(TagEditorActivity.EXTRA_FILE_PATH, file.getAbsolutePath());
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.only_mp3_tags, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getItemId() == R.id.menu_add_favorite) {
            if (!file.isDirectory()) {
                Song s = new Song();
                s.path = file.getAbsolutePath();
                s.title = file.getName();
                s.artist = "Unknown";
                MusicDB.getInstance(this).addFavorite(s);
                Toast.makeText(this, R.string.added_to_favorites, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private class FileAdapter extends BaseAdapter {
        public int getCount() { return mFiles.size(); }
        public Object getItem(int position) { return mFiles.get(position); }
        public long getItemId(int position) { return position; }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(FileBrowserActivity.this).inflate(R.layout.item_file, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.file_name);
                holder.icon = (TextView) convertView.findViewById(R.id.file_icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            File file = mFiles.get(position);
            holder.name.setText(file.getName());
            if (file.isDirectory()) {
                holder.icon.setText("[DIR]");
                convertView.setBackgroundColor(0x00000000);
            } else {
                holder.icon.setText("[MUS]");
                if (mSelectedFiles.contains(file)) {
                    convertView.setBackgroundColor(0xFF336699);
                } else {
                    convertView.setBackgroundColor(0x00000000);
                }
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView name;
        TextView icon;
    }
}