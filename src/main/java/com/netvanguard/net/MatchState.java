package com.netvanguard.net;

/**
 * Who is winning a two-player match, and whether both players want a rematch.
 * Pure logic with no JavaFX, so it can be unit tested. GameScreen feeds it events.
 */
public class MatchState {

    public enum Result {
        PLAYING,
        WIN,            // opponent's grid overflowed
        LOSE,           // my grid overflowed
        DRAW,           // both overflowed
        OPPONENT_LEFT   // opponent quit or disconnected while I was still alive: I win
    }

    private boolean iLost;
    private boolean opponentLost;
    private boolean opponentLeft;
    private boolean iWantRematch;
    private boolean opponentWantsRematch;

    public void iLost() {
        iLost = true;
    }

    public void opponentLost() {
        opponentLost = true;
    }

    public void opponentLeft() {
        opponentLeft = true;
    }

    public Result result() {
        if (iLost && opponentLost) {
            return Result.DRAW;
        }
        if (iLost) {
            return Result.LOSE;
        }
        if (opponentLost) {
            return Result.WIN;
        }
        if (opponentLeft) {
            return Result.OPPONENT_LEFT;
        }
        return Result.PLAYING;
    }

    public boolean isOver() {
        return result() != Result.PLAYING;
    }

    /** True if the match is over and the opponent is gone, so a rematch can never start. */
    public boolean rematchImpossible() {
        return opponentLeft;
    }

    /** I pressed the rematch key. Returns true if a REMATCH message should now be sent (only the first time). */
    public boolean requestRematch() {
        if (!isOver() || opponentLeft || iWantRematch) {
            return false;
        }
        iWantRematch = true;
        return true;
    }

    public void opponentWantsRematch() {
        opponentWantsRematch = true;
    }

    public boolean iWantRematch() {
        return iWantRematch;
    }

    /** True when both players asked for a rematch: time to start a fresh game. */
    public boolean bothWantRematch() {
        return iWantRematch && opponentWantsRematch && !opponentLeft;
    }

    /** Clears everything for a new match. */
    public void reset() {
        iLost = false;
        opponentLost = false;
        opponentLeft = false;
        iWantRematch = false;
        opponentWantsRematch = false;
    }
}
