package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class GameTest {

    /** Remembers everything the game reports. */
    private static class Recorder implements GameEvents {
        int garbageSent;
        int lastScore = -1;
        int scoreCalls;
        int gameOverCalls;

        @Override public void onGarbageSent(int count) { garbageSent += count; }
        @Override public void onScoreChanged(int score) { lastScore = score; scoreCalls++; }
        @Override public void onGameOver() { gameOverCalls++; }
    }

    private static Game newGame(Recorder rec) {
        return new Game(new Random(42), rec);
    }

    @Test
    void startsRunningWithAnItemAndAPreview() {
        Game game = newGame(new Recorder());
        assertEquals(Game.State.RUNNING, game.getState());
        assertNotNull(game.getCurrent());
        assertNotNull(game.getNext());
        assertEquals(0, game.getScore());
        assertEquals(0, game.getPendingGarbage());
    }

    @Test
    void worksWithoutAnyListener() {
        Game game = new Game();
        game.hardDrop();
        assertEquals(1, game.getGrid().countItems());
    }

    @Test
    void tickMovesTheItemDownOneRow() {
        Game game = newGame(new Recorder());
        game.tick();
        assertEquals(1, game.getCurrent().getRow());
    }

    @Test
    void movesLeftAndRight() {
        Game game = newGame(new Recorder());
        int col = game.getCurrent().getCol();
        game.moveLeft();
        assertEquals(col - 1, game.getCurrent().getCol());
        game.moveRight();
        assertEquals(col, game.getCurrent().getCol());
    }

    @Test
    void hardDropLocksItemAtTheBottomAndSpawnsTheNext() {
        Game game = newGame(new Recorder());
        Fruit upcoming = game.getNext();
        game.hardDrop();
        assertNotNull(game.getGrid().get(Grid.ROWS - 1, Grid.COLS / 2));
        assertEquals(1, game.getGrid().countItems());
        assertEquals(upcoming, game.getCurrent().getFruit());
        assertEquals(Game.State.RUNNING, game.getState());
    }

    @Test
    void incomingGarbageLandsAfterTheNextDrop() {
        Game game = newGame(new Recorder());
        game.receiveGarbage(3);
        game.hardDrop();
        assertEquals(0, game.getPendingGarbage());
        assertEquals(1 + 3, game.getGrid().countItems());
    }

    @Test
    void garbageIsCappedPerDrop() {
        Game game = newGame(new Recorder());
        game.receiveGarbage(10);
        game.hardDrop();
        assertEquals(10 - Game.MAX_GARBAGE_PER_DROP, game.getPendingGarbage());
        assertEquals(1 + Game.MAX_GARBAGE_PER_DROP, game.getGrid().countItems());
    }

    /** Arranges the grid so the current item causes a chain of 2 (which sends 2 stones). */
    private static Game gameAboutToChain(Recorder rec) {
        Game game = newGame(rec);
        Fruit f = game.getCurrent().getFruit();
        int col = game.getCurrent().getCol();
        game.getGrid().set(9, col, f);
        game.getGrid().set(9, col + 1, f.next());
        return game;
    }

    @Test
    void aChainReportsScoreAndSendsGarbage() {
        Recorder rec = new Recorder();
        Game game = gameAboutToChain(rec);
        game.hardDrop();
        assertTrue(game.getScore() > 0);
        assertEquals(1, rec.scoreCalls);
        assertEquals(game.getScore(), rec.lastScore);
        assertEquals(2, rec.garbageSent);
    }

    @Test
    void sendingNothingDoesNotFireTheGarbageEvent() {
        Recorder rec = new Recorder();
        Game game = newGame(rec);
        game.hardDrop();
        assertEquals(0, rec.garbageSent);
        assertEquals(0, rec.scoreCalls);
    }

    @Test
    void outgoingGarbageCancelsIncomingGarbageFirst() {
        Recorder rec = new Recorder();
        Game game = gameAboutToChain(rec);
        game.receiveGarbage(3);
        game.hardDrop();
        assertEquals(0, rec.garbageSent); // 2 earned, 2 cancelled
        assertEquals(0, game.getPendingGarbage());
        assertEquals(1 + 1, game.getGrid().countItems()); // merged fruit + the 1 stone that got through
    }

    @Test
    void gameEndsOnceWhenSomethingSitsInTheTopRow() {
        Recorder rec = new Recorder();
        Game game = newGame(rec);
        game.getGrid().set(0, 0, Fruit.STONE);
        game.hardDrop();
        assertTrue(game.isGameOver());
        assertNull(game.getCurrent());
        game.hardDrop();
        game.tick();
        assertEquals(1, rec.gameOverCalls);
    }

    @Test
    void nothingChangesAfterGameOver() {
        Game game = newGame(new Recorder());
        game.getGrid().set(0, 0, Fruit.STONE);
        game.hardDrop();
        int items = game.getGrid().countItems();
        game.tick();
        game.hardDrop();
        game.moveLeft();
        assertEquals(items, game.getGrid().countItems());
    }
}