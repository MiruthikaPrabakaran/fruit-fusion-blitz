package com.netvanguard.net;

/**
 * The text messages the two players send each other. One message = one line.
 *
 *   GARBAGE 3        I sent you 3 stone blocks
 *   STATE <60 chars> my current grid (see Grid.serialize)
 *   SCORE 1200       my current score
 *   GAMEOVER         my grid overflowed, you win
 */
public final class Protocol {
    public static final int DEFAULT_PORT = 5050;

    public static final String GARBAGE = "GARBAGE";
    public static final String STATE = "STATE";
    public static final String SCORE = "SCORE";
    public static final String GAME_OVER = "GAMEOVER";

    private Protocol() {
    }

    public static String garbage(int count) {
        return GARBAGE + " " + count;
    }

    public static String state(String serializedGrid) {
        return STATE + " " + serializedGrid;
    }

    public static String score(int score) {
        return SCORE + " " + score;
    }

    public static String gameOver() {
        return GAME_OVER;
    }

    /** The first word of a received line, e.g. "GARBAGE" for "GARBAGE 3". */
    public static String command(String line) {
        int space = line.indexOf(' ');
        return space < 0 ? line : line.substring(0, space);
    }

    /** Everything after the first space, e.g. "3" for "GARBAGE 3". Empty string if there is none. */
    public static String argument(String line) {
        int space = line.indexOf(' ');
        return space < 0 ? "" : line.substring(space + 1);
    }
}
