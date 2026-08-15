package com.galaxyy.music.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.galaxyy.music.R;
import com.galaxyy.music.Playlist;
import com.galaxyy.music.util.MusicDB;
import java.util.ArrayList;

public class PlaylistActivity extends Activity {

    private ListView mListView;
    private PlaylistAdapter mAdapter;
    private ArrayList<Playlist> mPlaylists;
    private Button mCreateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mListView = (ListView) findViewById(R.id.list_view);
        mCreateBtn = (Button) findViewById(R.id.action_button);
        mCreateBtn.setText(R.string.create_playlist);
        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCreatePlaylistDialog();
            }
        });

        loadPlaylists();
        registerForContextMenu(mListView);
    }

    private void loadPlaylists() {
        mPlaylists = MusicDB.getInstance(this).getAllPlaylists();
        mAdapter = new PlaylistAdapter();
        mListView.setAdapter(mAdapter);
    }

    private void showCreatePlaylistDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.playlist_name_hint);
        new AlertDialog.Builder(this)
            .setTitle(R.string.create_playlist)
            .setView(input)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String name = input.getText().toString().trim();
                    if (name.length() > 0) {
                        MusicDB.getInstance(PlaylistActivity.this).createPlaylist(name);
                        loadPlaylists();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.playlist_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Playlist playlist = mPlaylists.get(info.position);
        int id = item.getItemId();

        if (id == R.id.menu_rename) {
            final EditText input = new EditText(this);
            input.setText(playlist.name);
            new AlertDialog.Builder(this)
                .setTitle(R.string.rename_playlist)
                .setView(input)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (name.length() > 0) {
                            MusicDB.getInstance(PlaylistActivity.this).renamePlaylist(playlist.id, name);
                            loadPlaylists();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
            return true;
        } else if (id == R.id.menu_delete) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.delete_playlist)
                .setMessage(R.string.confirm_delete_playlist)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MusicDB.getInstance(PlaylistActivity.this).deletePlaylist(playlist.id);
                        loadPlaylists();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private class PlaylistAdapter extends BaseAdapter {
        public int getCount() { return mPlaylists.size(); }
        public Object getItem(int position) { return mPlaylists.get(position); }
        public long getItemId(int position) { return mPlaylists.get(position).id; }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(PlaylistActivity.this).inflate(R.layout.item_playlist, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.playlist_name);
                holder.count = (TextView) convertView.findViewById(R.id.song_count);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Playlist p = mPlaylists.get(position);
            holder.name.setText(p.name);
            holder.count.setText(p.getSongCount() + " " + getString(R.string.songs));
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView name;
        TextView count;
    }
}