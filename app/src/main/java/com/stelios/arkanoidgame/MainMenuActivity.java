package com.stelios.arkanoidgame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        boolean musicOn = getSharedPreferences("ArkanoidSettings", MODE_PRIVATE)
                .getBoolean("music_on", true);
        if (musicOn) {
            MusicManager.start(this);
        } else {
            MusicManager.stop();
        }

    }

    public void startGame(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void exitGame(View view) {
        MusicManager.stop();
        finishAffinity();
    }
}
