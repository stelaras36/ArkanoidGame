package com.stelios.arkanoidgame;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.SoundPool;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import android.os.Vibrator;
import android.os.VibrationEffect; // για Android 8.0+
import android.os.Build;           // για έλεγχο έκδοσης Android
import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;








public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private SurfaceHolder holder;
    private boolean isPlaying;
    private Paint paint;
    private boolean levelCompleted = false;

    private float paddleX, paddleY, paddleWidth, paddleHeight;
    private Bitmap backgroundBitmap;
    private android.media.MediaPlayer bounceSound;

    private List<Ball> balls = new ArrayList<>();
    private float ballRadius;
    private float ballSpeedX, ballSpeedY;

    private Block[][] blocks;
    private android.media.SoundPool soundPool;
    private int bounceSoundId;
    private int numRows = 5;
    private int numCols = 7;

    private boolean useSwipeControl;
    private Vibrator vibrator;
    private boolean vibrationEnabled = true; // default ενεργοποιημένο

    private boolean moveLeft, moveRight;
    private boolean isLoadingLevel = false;

    private int currentLevel = 1;
    private List<PowerUp> powerUps = new ArrayList<>();
    private int lives = 3;
    private Rect pauseButtonRect;

    private int score = 0;
    private int highScore = 0;
    private boolean isGameOver = false;
    private Rect resumeButtonRect;
    private int settingsButtonWidth = 80;   // πλάτος κουμπιού σε pixels
    private int settingsButtonHeight = 80;  // ύψος κουμπιού σε pixels
    private int settingsButtonLeft;          // θέση κουμπιού από αριστερά
    private int settingsButtonTop;           // θέση κουμπιού από πάνω


    private Random random = new Random();

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        paint = new Paint();

        paddleWidth = 300;
        paddleHeight = 30;
        ballRadius = 20;

        ballSpeedX = 10;
        ballSpeedY = -10;
        float startX = 500;
        float startY = 1000;
        balls.add(new Ball(startX, startY, ballSpeedX, ballSpeedY));

        SharedPreferences prefs = context.getSharedPreferences("ArkanoidSettings", Context.MODE_PRIVATE);
        highScore = prefs.getInt("high_score", 0);
        useSwipeControl = prefs.getBoolean("control_swipe", true);
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new android.media.SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new android.media.SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0);
        }

        bounceSoundId = soundPool.load(getContext(), R.raw.bounce, 1);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);


    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }


    private class Ball {
        float x, y;
        float speedX, speedY;

        Ball(float x, float y, float speedX, float speedY) {
            this.x = x;
            this.y = y;
            this.speedX = speedX;
            this.speedY = speedY;
        }
    }

    @Override
    public void run() {
        while (true) {
            if (isPlaying) {
                update();
            }

            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                draw(canvas);   // περνάς το canvas στη μέθοδο draw
                holder.unlockCanvasAndPost(canvas);
            }

            control();
        }

    }

    private void update() {
        if (blocks == null) return;

        for (int i = 0; i < balls.size(); i++) {
            Ball ball = balls.get(i);

            ball.x += ball.speedX;
            ball.y += ball.speedY;

            if (ball.x - ballRadius <= 0 || ball.x + ballRadius >= getWidth()) {
                ball.speedX = -ball.speedX;
                soundPool.play(bounceSoundId, 1, 1, 0, 0, 1);

            }
            if (ball.y - ballRadius <= 0) {
                ball.speedY = -ball.speedY;
                soundPool.play(bounceSoundId, 1, 1, 0, 0, 1);

            }

            if (ball.y + ballRadius >= paddleY &&
                    ball.x >= paddleX &&
                    ball.x <= paddleX + paddleWidth) {
                ball.speedY = -ball.speedY;
                if (bounceSound != null) bounceSound.start();
                triggerVibration();
            }

            boolean ballRemoved = false;
            for (int row = 0; row < blocks.length && !ballRemoved; row++) {
                for (int col = 0; col < blocks[0].length; col++) {
                    Block block = blocks[row][col];
                    if (block.hitsLeft > 0) {
                        if (ball.x + ballRadius >= block.x &&
                                ball.x - ballRadius <= block.x + block.width &&
                                ball.y + ballRadius >= block.y &&
                                ball.y - ballRadius <= block.y + block.height) {

                            ball.speedY = -ball.speedY;
                            block.hitsLeft--;
                            triggerVibration();

                            if (block.hitsLeft == 0) {
                                if (block.originalHits > 1) {
                                    score += 7; // block με 2+ ζωές
                                } else {
                                    score += 5; // απλό block με 1 ζωή
                                }
                                if (score > highScore) {
                                    highScore = score;
                                }
                                if (random.nextFloat() < 0.5f) {
                                    spawnPowerUp(block.x + block.width / 2, block.y);
                                }
                            }


                            break;
                        }
                    }
                }
            }

            if (ball.y - ballRadius > getHeight()) {
                balls.remove(i);
                i--;
                if (balls.isEmpty()) {
                    lives--;
                    if (lives <= 0) {
                        isPlaying = false;
                        isGameOver = true;
                        SharedPreferences prefs = getContext().getSharedPreferences("ArkanoidSettings", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("high_score", highScore);
                        editor.apply();
                    }
                    else {
                        resetBallAndPaddle();
                        balls.add(new Ball(paddleX + paddleWidth / 2, paddleY - ballRadius * 2, ballSpeedX, ballSpeedY));
                    }
                }
            }
        }

        // Ενημέρωση power-ups
        Iterator<PowerUp> iterator = powerUps.iterator();
        while (iterator.hasNext()) {
            PowerUp p = iterator.next();
            p.y += p.speed;

            if (p.y + p.size >= paddleY && p.x + p.size >= paddleX && p.x <= paddleX + paddleWidth) {
                activatePowerUp(p.type);
                iterator.remove();
            } else if (p.y > getHeight()) {
                iterator.remove();
            }
        }

        // Έλεγχος αν όλα τα blocks έχουν σπάσει
        boolean allBroken = true;
        for (Block[] rowBlocks : blocks) {
            for (Block block : rowBlocks) {
                if (block.hitsLeft > 0) {
                    allBroken = false;
                    break;
                }
            }
            if (allBroken && !isLoadingLevel) {
                isLoadingLevel = true;          // Μπλοκάρει πολλαπλά level-ups
                currentLevel++;
                balls.clear();
                powerUps.clear();
                resetLevel();
                resetBallAndPaddle();
                isPlaying = true;

            }
        }

        // Χειρισμός κουπιών σε button mode
        if (!useSwipeControl) {
            if (moveLeft) paddleX -= 20;
            if (moveRight) paddleX += 20;
            if (paddleX < 0) paddleX = 0;
            if (paddleX + paddleWidth > getWidth()) paddleX = getWidth() - paddleWidth;
        }
    }

    private void spawnPowerUp(float x, float y) {
        int type = random.nextInt(5) + 1;
        powerUps.add(new PowerUp(x, y, type));
    }

    private void activatePowerUp(int type) {
        switch (type) {
            case 1: // Big paddle
                paddleWidth *= 1.5f;
                postDelayed(() -> paddleWidth /= 1.5f, 11000);
                break;
            case 2: // Small paddle
                paddleWidth *= 0.7f;
                postDelayed(() -> paddleWidth /= 0.7f, 11000);
                break;
            case 3: // Multi-ball
                Ball mainBall = balls.get(0);
                for (int i = 0; i < 2; i++) {
                    float newSpeedX = ballSpeedX * (i % 2 == 0 ? 1 : -1);
                    float newSpeedY = ballSpeedY;
                    balls.add(new Ball(mainBall.x, mainBall.y, newSpeedX, newSpeedY));
                }
                break;
            case 4: // Slow motion
                ballSpeedX *= 0.5f;
                ballSpeedY *= 0.5f;
                postDelayed(() -> {
                    ballSpeedX *= 2;
                    ballSpeedY *= 2;
                }, 11000);
                break;
            case 5: // Extra life
                lives++;
                break;
        }
    }

    private void resetBallAndPaddle() {
        balls.clear();
        balls.add(new Ball(getWidth() / 2f, getHeight() - 200, ballSpeedX, ballSpeedY));
        ballSpeedX = 10;
        ballSpeedY = -10;
        paddleX = getWidth() / 2f - paddleWidth / 2f;
        paddleY = getHeight() - 150;
    }

    public void movePaddle(int direction) {
        if (direction < 0) {
            moveLeft = true;
            moveRight = false;
        } else if (direction > 0) {
            moveRight = true;
            moveLeft = false;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawColor(Color.BLACK); // ή το background σου
        canvas.drawBitmap(backgroundBitmap, null, new Rect(0, 0, getWidth(), getHeight()), null);

        if (paddleX == 0 && paddleY == 0) {
            resetBallAndPaddle();
            resetLevel();
        }

        paint.setColor(Color.WHITE);
        canvas.drawRect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight, paint);

        for (Ball ball : balls) {
            canvas.drawCircle(ball.x, ball.y, ballRadius, paint);
        }
        // Σχεδίαση pause εικονιδίου πάνω δεξιά
        int pauseX = getWidth() - 150; // Απόσταση από το δεξί άκρο
        int pauseY = 50;               // Απόσταση από το πάνω μέρος
        Drawable pauseDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.pause);
        if (pauseDrawable != null) {
            int pauseWidth = pauseDrawable.getIntrinsicWidth();
            int pauseHeight = pauseDrawable.getIntrinsicHeight();
            pauseDrawable.setBounds(pauseX, pauseY, pauseX + pauseWidth, pauseY + pauseHeight);
            pauseButtonRect = new Rect(pauseX, pauseY, pauseX + pauseWidth, pauseY + pauseHeight);
            pauseDrawable.draw(canvas);
        }



        if (blocks != null) {
            for (Block[] rowBlocks : blocks) {
                for (Block block : rowBlocks) {
                    if (block.hitsLeft > 0) {
                        if (block.hitsLeft == 3) paint.setColor(Color.RED);
                        else if (block.hitsLeft == 2) paint.setColor(Color.YELLOW);
                        else paint.setColor(Color.GREEN);
                        canvas.drawRect(block.x, block.y, block.x + block.width, block.y + block.height, paint);
                    }
                }
            }
        }

        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Score: " + score, 10, 70, paint);
        canvas.drawText("High Score: " + highScore, 10, 110, paint);
        canvas.drawText("Level: " + currentLevel, 50, 150, paint);
        canvas.drawText("Lives: " + lives, 50, 190, paint);

        for (PowerUp p : powerUps) {
            switch (p.type) {
                case 1: paint.setColor(Color.MAGENTA); break;
                case 2: paint.setColor(Color.BLUE); break;
                case 3: paint.setColor(Color.CYAN); break;
                case 4: paint.setColor(Color.YELLOW); break;
                case 5: paint.setColor(Color.GREEN); break;
            }
            float radius = p.size / 2f;
            canvas.drawCircle(p.x + radius, p.y + radius, radius, paint);
        }

        if (isGameOver) {
            paint.setColor(Color.RED);
            paint.setTextSize(120);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GAME OVER", getWidth() / 2f, getHeight() / 2f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
        if (!isPlaying) { // Όταν το παιχνίδι είναι σε pause
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int resumeWidth = 300;
            int resumeHeight = 120;

            resumeButtonRect = new Rect(centerX - resumeWidth/2, centerY - resumeHeight/2,
                    centerX + resumeWidth/2, centerY + resumeHeight/2);

            paint.setColor(Color.WHITE);
            canvas.drawRect(resumeButtonRect, paint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(50);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Resume", centerX, centerY + 15, paint);

            int settingsSize = 100; // μέγεθος εικόνας
            int settingsX = centerX + resumeButtonRect.width() / 2 + 50; // δεξιά από το κουμπί Resume
            int settingsY = centerY - settingsSize / 2;

            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_settings_gear);
            if (drawable != null) {
                Bitmap settingsBitmap = Bitmap.createBitmap(
                        settingsSize,
                        settingsSize,
                        Bitmap.Config.ARGB_8888);
                Canvas tempCanvas = new Canvas(settingsBitmap);
                drawable.setBounds(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                drawable.draw(tempCanvas);

            }
        }

    }

    private void control() {
        try {
            Thread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        isPlaying = false;
        try {
            if (gameThread != null)
                gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
    public void setVibrationEnabled(boolean enabled) {
        vibrationEnabled = enabled;
    }
    private void triggerVibration() {
        if (vibrationEnabled && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50); // συμβατότητα με παλιότερες εκδόσεις
            }
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
        soundPool = new SoundPool.Builder().setMaxStreams(5).build();
        bounceSoundId = soundPool.load(getContext(), R.raw.bounce, 1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            if (pauseButtonRect != null && pauseButtonRect.contains((int) touchX, (int) touchY)) {
                togglePause();
                return true;
            }
            if (!isPlaying && resumeButtonRect != null &&
                    resumeButtonRect.contains((int) touchX, (int) touchY)) {
                togglePause();
                return true;
            }


        }
        if (useSwipeControl) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                paddleX = event.getX() - paddleWidth / 2f;
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                if (event.getX() < getWidth() / 2f) {
                    moveLeft = true;
                    moveRight = false;
                } else {
                    moveRight = true;
                    moveLeft = false;
                }
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                moveLeft = false;
                moveRight = false;
            }
        }
        return true;
    }
    public void togglePause() {
        if (isPlaying) {
            isPlaying = false;
        } else {
            isPlaying = true;
        }
    }

    private void resetLevel() {
        int rows = Math.min(12, numRows + (currentLevel - 1));
        blocks = new Block[rows][numCols];

        float blockWidth = getWidth() / (float) numCols;
        float blockHeight = 60;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < numCols; col++) {
                blocks[row][col] = new Block(
                        col * blockWidth,
                        row * blockHeight + 100,
                        blockWidth - 5,
                        blockHeight - 5,
                        1);
            }
        }

        if (currentLevel >= 2) {
            int totalBlocks = rows * numCols;
            int strongBlocks = Math.min(2 + (currentLevel - 2) * 2, totalBlocks / 3);

            for (int i = 0; i < strongBlocks; i++) {
                int row = random.nextInt(rows);
                int col = random.nextInt(numCols);
                int hits = random.nextBoolean() ? 2 : 3;
                blocks[row][col].hitsLeft = hits;
            }
        }
        isLoadingLevel = false;
    }
}
