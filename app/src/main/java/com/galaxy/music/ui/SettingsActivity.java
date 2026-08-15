package com.galaxyy.music.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.galaxyy.music.R;
import com.galaxyy.music.util.PreferenceManager;

public class SettingsActivity extends Activity {

    private CheckBox mHeadsetAuto, mGapless, mShowHidden, mShowLyrics, mLyricsAutoScroll;
    private SeekBar mFadeBar, mFilterBar;
    private TextView mFadeValue, mFilterValue;
    private Spinner mSortSpinner;
    private Button mClearHistoryBtn, mAboutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mHeadsetAuto = (CheckBox) findViewById(R.id.setting_headset);
        mGapless = (CheckBox) findViewById(R.id.setting_gapless);
        mShowHidden = (CheckBox) findViewById(R.id.setting_show_hidden);
        mShowLyrics = (CheckBox) findViewById(R.id.setting_show_lyrics);
        mLyricsAutoScroll = (CheckBox) findViewById(R.id.setting_lyrics_autoscroll);
        mFadeBar = (SeekBar) findViewById(R.id.setting_fade);
        mFilterBar = (SeekBar) findViewById(R.id.setting_filter);
        mFadeValue = (TextView) findViewById(R.id.fade_value);
        mFilterValue = (TextView) findViewById(R.id.filter_value);
        mSortSpinner = (Spinner) findViewById(R.id.setting_sort);
        mClearHistoryBtn = (Button) findViewById(R.id.btn_clear_history);
        mAboutBtn = (Button) findViewById(R.id.btn_about);

        mHeadsetAuto.setChecked(PreferenceManager.isHeadsetAutoplay());
        mGapless.setChecked(PreferenceManager.isGapless());
        mShowHidden.setChecked(PreferenceManager.isShowHidden());
        mShowLyrics.setChecked(PreferenceManager.isShowLyrics());
        mLyricsAutoScroll.setChecked(PreferenceManager.isLyricsAutoScroll());
        mFadeBar.setProgress(PreferenceManager.getFadeDuration() / 100);
        mFilterBar.setProgress(PreferenceManager.getFilterDuration() / 5);
        mFadeValue.setText(PreferenceManager.getFadeDuration() + "ms");
        mFilterValue.setText(PreferenceManager.getFilterDuration() + "s");

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSortSpinner.setAdapter(adapter);
        mSortSpinner.setSelection(PreferenceManager.getSortOrder());

        mHeadsetAuto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.setHeadsetAutoplay(isChecked);
            }
        });
        mGapless.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.setGapless(isChecked);
            }
        });
        mShowHidden.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.setShowHidden(isChecked);
            }
        });
        mShowLyrics.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.setShowLyrics(isChecked);
            }
        });
        mLyricsAutoScroll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.setLyricsAutoScroll(isChecked);
            }
        });
        mFadeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int ms = progress * 100;
                mFadeValue.setText(ms + "ms");
                PreferenceManager.setFadeDuration(ms);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        mFilterBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int sec = progress * 5;
                mFilterValue.setText(sec + "s");
                PreferenceManager.setFilterDuration(sec);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        mSortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreferenceManager.setSortOrder(position);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mClearHistoryBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(R.string.clear_history)
                    .setMessage(R.string.confirm_clear_history)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(SettingsActivity.this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
            }
        });

        mAboutBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            }
        });
    }
}