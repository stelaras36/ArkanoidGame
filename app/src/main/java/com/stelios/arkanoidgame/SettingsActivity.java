package com.stelios.arkanoidgame;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.content.Intent;



public class SettingsActivity extends AppCompatActivity {

    private RadioGroup controlModeGroup;
    private Switch switchMusic, switchVibration;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ArkanoidSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Button saveButton = findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();  // ✅ Απλώς κλείνει το Settings
            }
        });




        controlModeGroup = findViewById(R.id.controlModeGroup);
        switchMusic = findViewById(R.id.switchMusic);
        switchVibration = findViewById(R.id.switchVibration);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Φόρτωσε τις αποθηκευμένες ρυθμίσεις
        boolean isSwipe = prefs.getBoolean("control_swipe", true);
        boolean musicOn = prefs.getBoolean("music_on", true);
        boolean vibrationOn = prefs.getBoolean("vibration_on", true);

        ((RadioButton) findViewById(isSwipe ? R.id.radioSwipe : R.id.radioButtons)).setChecked(true);
        switchMusic.setChecked(musicOn);
        switchVibration.setChecked(vibrationOn);

        // Άμεση εφαρμογή μουσικής χωρίς αποθήκευση
        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                MusicManager.start(SettingsActivity.this);
            } else {
                MusicManager.stop();
            }
        });

        // Save - αποθηκεύει όλες τις αλλαγές για επόμενες φορές
        findViewById(R.id.buttonSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean selectedSwipe = (controlModeGroup.getCheckedRadioButtonId() == R.id.radioSwipe);
                boolean selectedMusic = switchMusic.isChecked();
                boolean selectedVibration = switchVibration.isChecked();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("control_swipe", selectedSwipe);
                editor.putBoolean("music_on", selectedMusic);
                editor.putBoolean("vibration_on", selectedVibration);
                editor.apply();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}
