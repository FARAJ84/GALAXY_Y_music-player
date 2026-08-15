package com.galaxyy.music.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.galaxyy.music.R;
import com.galaxyy.music.service.MusicService;
import com.galaxyy.music.util.PreferenceManager;

public class EqualizerActivity extends Activity {

    private MusicService mService;
    private boolean mBound = false;
    private LinearLayout mEqContainer;
    private Spinner mPresetSpinner;
    private CheckBox mEqEnableBox;
    private SeekBar mBassBar, mVirtualBar;
    private TextView mBassValue, mVirtualValue;
    private Button mResetBtn;
    private SeekBar[] mBandBars;
    private TextView[] mBandLabels;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.MusicBinder) service).getService();
            mBound = true;
            initEqualizer();
        }
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        mEqContainer = (LinearLayout) findViewById(R.id.eq_container);
        mPresetSpinner = (Spinner) findViewById(R.id.preset_spinner);
        mEqEnableBox = (CheckBox) findViewById(R.id.eq_enable);
        mBassBar = (SeekBar) findViewById(R.id.bass_boost);
        mVirtualBar = (SeekBar) findViewById(R.id.virtualizer);
        mBassValue = (TextView) findViewById(R.id.bass_value);
        mVirtualValue = (TextView) findViewById(R.id.virtual_value);
        mResetBtn = (Button) findViewById(R.id.btn_reset_eq);

        mEqEnableBox.setChecked(PreferenceManager.isEqEnabled());
        mBassBar.setProgress(PreferenceManager.getBassBoost());
        mVirtualBar.setProgress(PreferenceManager.getVirtualizer());
        mBassValue.setText(String.valueOf(PreferenceManager.getBassBoost()));
        mVirtualValue.setText(String.valueOf(PreferenceManager.getVirtualizer()));

        mEqEnableBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.setEqEnabled(isChecked);
                if (mBound && mService.getPlayer() != null) {
                    mService.getPlayer().setEqEnabled(isChecked);
                }
                mEqContainer.setEnabled(isChecked);
            }
        });

        mBassBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBassValue.setText(String.valueOf(progress));
                PreferenceManager.setBassBoost(progress);
                if (mBound) mService.getPlayer().setBassBoost((short) progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mVirtualBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVirtualValue.setText(String.valueOf(progress));
                PreferenceManager.setVirtualizer(progress);
                if (mBound) mService.getPlayer().setVirtualizer((short) progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mResetBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBassBar.setProgress(0);
                mVirtualBar.setProgress(0);
                if (mBandBars != null) {
                    for (SeekBar bar : mBandBars) {
                        bar.setProgress(bar.getMax() / 2);
                    }
                }
                PreferenceManager.setEqPreset(0);
                if (mBound) mService.getPlayer().applyEqPreset(0);
                Toast.makeText(EqualizerActivity.this, R.string.eq_reset, Toast.LENGTH_SHORT).show();
            }
        });

        bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void initEqualizer() {
        Equalizer eq = mService.getPlayer().getEqualizer();
        if (eq == null) {
            Toast.makeText(this, R.string.eq_not_available, Toast.LENGTH_LONG).show();
            return;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add(getString(R.string.preset_custom));
        short numPresets = eq.getNumberOfPresets();
        for (short i = 0; i < numPresets; i++) {
            adapter.add(eq.getPresetName(i));
        }
        mPresetSpinner.setAdapter(adapter);
        mPresetSpinner.setSelection(PreferenceManager.getEqPreset());

        mPresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreferenceManager.setEqPreset(position);
                if (mBound) mService.getPlayer().applyEqPreset(position);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        short numBands = eq.getNumberOfBands();
        mBandBars = new SeekBar[numBands];
        mBandLabels = new TextView[numBands];
        mEqContainer.removeAllViews();

        for (short i = 0; i < numBands; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(4, 4, 4, 4);

            TextView freqLabel = new TextView(this);
            int freq = eq.getCenterFreq(i) / 1000;
            freqLabel.setText(freq + "Hz");
            freqLabel.setWidth(60);
            mBandLabels[i] = freqLabel;

            SeekBar bar = new SeekBar(this);
            bar.setMax(30);
            bar.setProgress(15);
            final short band = i;
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mBound && fromUser) {
                        short level = (short) ((progress - 15) * 100);
                        try {
                            eq.setBandLevel(band, level);
                        } catch (Exception e) {}
                    }
                }
                public void onStartTrackingTouch(SeekBar seekBar) {}
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            mBandBars[i] = bar;

            row.addView(freqLabel);
            row.addView(bar, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            mEqContainer.addView(row);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) unbindService(mConnection);
    }
}