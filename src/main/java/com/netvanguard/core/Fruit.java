package com.netvanguard.core;

/**
 * Every item that can sit in a grid.
 *
 * Order matters: the ordinal is the tier. Two equal fruits merge into the next one in this list.
 * STONE is the garbage item sent to the opponent. It never merges.
 */
public enum Fruit {
    STONE("Stone", "#6b7280"),
    CHERRY("Cherry", "#dc2626"),
    STRAWBERRY("Strawberry", "#fb7185"),
    GRAPE("Grape", "#7c3aed"),
    ORANGE("Orange", "#f97316"),
    APPLE("Apple", "#84cc16"),
    WATERMELON("Watermelon", "#0ea5e9");

    private final String label;
    private final String colorHex;

    Fruit(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String label() {
        return label;
    }

    public String colorHex() {
        return colorHex;
    }

    /** 0 for STONE, 1 for CHERRY, and so on up to WATERMELON. */
    public int tier() {
        return ordinal();
    }

    public boolean isMergeable() {
        return this != STONE;
    }

    /** True if merging two of these produces a bigger fruit. */
    public boolean hasNext() {
        return isMergeable() && ordinal() < values().length - 1;
    }

    public Fruit next() {
        if (!hasNext()) {
            throw new IllegalStateException(label + " has no next tier");
        }
        return values()[ordinal() + 1];
    }

    /** Single character used when a grid is sent over the network. */
    public char code() {
        return (char) ('0' + ordinal());
    }

    public static Fruit fromCode(char c) {
        int index = c - '0';
        if (index < 0 || index >= values().length) {
            throw new IllegalArgumentException("Unknown fruit code: " + c);
        }
        return values()[index];
    }
}
