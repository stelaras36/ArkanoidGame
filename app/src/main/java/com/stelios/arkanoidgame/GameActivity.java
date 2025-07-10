package com.stelios.arkanoidgame;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.util.Log;
import android.widget.ImageButton;
import android.os.Handler;
import android.os.Looper;






public class GameActivity extends AppCompatActivity {
    private GameView gameView;
    private boolean useSwipeControl;
    private ActivityResultLauncher<Intent> settingsLauncher;
    private Handler handler = new Handler();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        ImageButton settingsButton = findViewById(R.id.button_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
                // Διάβασε το control mode
        useSwipeControl = getSharedPreferences("ArkanoidSettings", MODE_PRIVATE)
                .getBoolean("control_swipe", true);
        SharedPreferences prefs = getSharedPreferences("ArkanoidSettings", MODE_PRIVATE);
        boolean vibrationOn = prefs.getBoolean("vibration_on", true);


        // Δημιουργία GameView και προσθήκη στο container
        FrameLayout container = findViewById(R.id.game_container);
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        gameView.togglePause(); // Ξαναεμφανίζει το pause menu
                    }
                }
        );

        gameView = new GameView(this);
        gameView.startGame();
        gameView.setVibrationEnabled(vibrationOn);
        container.addView(gameView, 0);

        // Αν είναι Buttons mode, εμφάνισε τα κουμπιά
        if (!useSwipeControl) {
            LinearLayout buttonsLayout = findViewById(R.id.buttons_layout);
            buttonsLayout.setVisibility(View.VISIBLE);

            Button buttonLeft = findViewById(R.id.button_left);
            Button buttonRight = findViewById(R.id.button_right);

            buttonLeft.setOnTouchListener((v, event) -> {
                gameView.movePaddle(-1);
                return true;
            });

            buttonRight.setOnTouchListener((v, event) -> {
                gameView.movePaddle(1);
                return true;
            });
        }
        gameView.startGame();
    }
    public void togglePause() {
        gameView.togglePause();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();       // αν θέλεις να κάνει pause το game
            gameView.stopThread();  // σταματάει το thread για να μην συνεχίζει στο background
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.setIsPlaying(true);
            gameView.resumeThread(); // ξεκινάει το thread πάλι
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            gameView.togglePause(); // Ξαναεμφανίζει το pause menu
        }
    }
    @Override
    public void onBackPressed() {
        // Do nothing to ignore the back button
    }

}
