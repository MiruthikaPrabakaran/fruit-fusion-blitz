package com.netvanguard.core;

/** Difficulty settings used only by solo mode. */
public enum SoloDifficulty {
    EASY("Easy", 1.00, 1, 28),
    MEDIUM("Medium", 0.85, 1, 16),
    HARD("Hard", 0.70, 2, 12);

    private final String label;
    private final double speedMultiplier;
    private final int stonesPerWave;
    private final int stoneWaveTicks;

    SoloDifficulty(String label, double speedMultiplier, int stonesPerWave, int stoneWaveTicks) {
        this.label = label;
        this.speedMultiplier = speedMultiplier;
        this.stonesPerWave = stonesPerWave;
        this.stoneWaveTicks = stoneWaveTicks;
    }

    public String label() { return label; }
    public double speedMultiplier() { return speedMultiplier; }
    public int stonesPerWave() { return stonesPerWave; }
    public int stoneWaveTicks() { return stoneWaveTicks; }
}
