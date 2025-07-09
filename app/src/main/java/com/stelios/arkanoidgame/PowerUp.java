package com.stelios.arkanoidgame;

public class PowerUp {
    public float x, y, size, speed;
    public int type; // 1=big paddle, 2=small paddle, 3=multi-ball, 4=slow, 5=extra life

    public PowerUp(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.size = 50; // μέγεθος power-up
        this.speed = 7; // ταχύτητα πτώσης
        this.type = type;
    }
}
