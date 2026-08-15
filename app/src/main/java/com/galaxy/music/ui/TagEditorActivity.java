package com.galaxyy.music.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.galaxyy.music.R;
import com.galaxyy.music.util.TagEditor;

public class TagEditorActivity extends Activity {

    public static final String EXTRA_FILE_PATH = "file_path";

    private EditText mTitleInput, mArtistInput, mAlbumInput;
    private Button mSaveBtn;
    private String mFilePath;
    private TagEditor mTagEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_editor);

        mTitleInput = (EditText) findViewById(R.id.tag_title);
        mArtistInput = (EditText) findViewById(R.id.tag_artist);
        mAlbumInput = (EditText) findViewById(R.id.tag_album);
        mSaveBtn = (Button) findViewById(R.id.btn_save_tags);

        mFilePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (mFilePath == null) {
            Toast.makeText(this, R.string.error_no_file, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mTagEditor = new TagEditor(mFilePath);
        mTitleInput.setText(mTagEditor.getTitle() != null ? mTagEditor.getTitle() : "");
        mArtistInput.setText(mTagEditor.getArtist() != null ? mTagEditor.getArtist() : "");
        mAlbumInput.setText(mTagEditor.getAlbum() != null ? mTagEditor.getAlbum() : "");

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveTags();
            }
        });
    }

    private void saveTags() {
        mTagEditor.setTitle(mTitleInput.getText().toString().trim());
        mTagEditor.setArtist(mArtistInput.getText().toString().trim());
        mTagEditor.setAlbum(mAlbumInput.getText().toString().trim());

        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_save_tags)
            .setMessage(R.string.tag_editor_warning)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (mTagEditor.save()) {
                        Toast.makeText(TagEditorActivity.this, R.string.tags_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(TagEditorActivity.this, R.string.tags_save_failed, Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
}