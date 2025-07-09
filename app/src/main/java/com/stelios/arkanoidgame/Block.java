package com.stelios.arkanoidgame;

public class Block {
    public float x, y, width, height;
    public int hitsLeft;
    public int originalHits;


    public Block(float x, float y, float width, float height, int hitsLeft) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hitsLeft = hitsLeft;
    }
}
