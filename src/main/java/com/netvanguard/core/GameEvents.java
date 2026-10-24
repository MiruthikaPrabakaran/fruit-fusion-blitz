package com.netvanguard.core;

public interface GameEvents {
    void onGarbageSent(int count);
    void onScoreChanged(int score);
    void onGameOver();
}