package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
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

    // ---------- Powers ----------

    @Test
    void startsWithPowerCharges() {
        Game game = newGame(new Recorder());
        assertEquals(Powers.START_BURSTS, game.getBursts());
        assertEquals(Powers.START_SWAPS, game.getSwaps());
    }

    @Test
    void burstPopsOnlySmallFruitsAndSpendsACharge() {
        Recorder rec = new Recorder();
        Game game = newGame(rec);
        Grid g = game.getGrid();
        g.set(9, 0, Fruit.CHERRY);
        g.set(9, 2, Fruit.STRAWBERRY);
        g.set(9, 4, Fruit.GRAPE);
        g.set(9, 5, Fruit.STONE);
        assertTrue(game.useBurst());
        assertEquals(2, g.countItems());
        assertEquals(Powers.START_BURSTS - 1, game.getBursts());
        assertEquals(30, game.getScore());
        assertEquals(30, rec.lastScore);
    }

    @Test
    void burstsUsedCountsSuccessfulBursts() {
        Game game = newGame(new Recorder());
        assertEquals(0, game.getBurstsUsed());
        game.getGrid().set(9, 0, Fruit.CHERRY);
        game.useBurst();
        assertEquals(1, game.getBurstsUsed());
    }

    @Test
    void burstWithNothingSmallToPopKeepsTheCharge() {
        Game game = newGame(new Recorder());
        game.getGrid().set(9, 0, Fruit.GRAPE);
        assertFalse(game.useBurst());
        assertEquals(Powers.START_BURSTS, game.getBursts());
        assertEquals(1, game.getGrid().countItems());
    }

    @Test
    void burstNeedsACharge() {
        Game game = newGame(new Recorder());
        game.getGrid().set(9, 0, Fruit.CHERRY);
        assertTrue(game.useBurst());
        game.getGrid().set(9, 0, Fruit.CHERRY);
        assertFalse(game.useBurst());
        assertEquals(Fruit.CHERRY, game.getGrid().get(9, 0));
    }

    @Test
    void fruitsFallingAfterABurstCanStartAChain() {
        Game game = newGame(new Recorder());
        Grid g = game.getGrid();
        g.set(9, 0, Fruit.CHERRY);
        g.set(8, 0, Fruit.GRAPE); // falls into the gap and meets the grape next to it
        g.set(9, 1, Fruit.GRAPE);
        assertTrue(game.useBurst());
        assertEquals(Fruit.ORANGE, g.get(9, 0));
        assertEquals(1, g.countItems());
        assertEquals(1, game.getLastResult().chainLength());
        assertEquals(10 + 3 * 10 * 2, game.getScore());
    }

    @Test
    void aChainEarnsABurstCharge() {
        Game game = gameAboutToChain(new Recorder());
        game.hardDrop();
        assertEquals(Powers.START_BURSTS + 1, game.getBursts());
    }

    @Test
    void swapCyclesTheNextFruitAndLeavesTheFallingOneAlone() {
        Game game = newGame(new Recorder());
        Fruit falling = game.getCurrent().getFruit();
        int index = Arrays.asList(Game.SPAWN_POOL).indexOf(game.getNext());
        assertTrue(game.useSwap());
        assertEquals(Game.SPAWN_POOL[(index + 1) % Game.SPAWN_POOL.length], game.getNext());
        assertEquals(falling, game.getCurrent().getFruit());
        assertEquals(Powers.START_SWAPS - 1, game.getSwaps());
    }

    @Test
    void swapStopsWhenOutOfCharges() {
        Game game = newGame(new Recorder());
        for (int i = 0; i < Powers.START_SWAPS; i++) {
            assertTrue(game.useSwap());
        }
        Fruit before = game.getNext();
        assertFalse(game.useSwap());
        assertEquals(before, game.getNext());
    }

    @Test
    void swappedFruitIsTheOneThatSpawnsNext() {
        Game game = newGame(new Recorder());
        game.useSwap();
        Fruit chosen = game.getNext();
        game.hardDrop();
        assertEquals(chosen, game.getCurrent().getFruit());
    }

    @Test
    void powersDoNothingAfterGameOver() {
        Game game = newGame(new Recorder());
        game.getGrid().set(0, 0, Fruit.STONE);
        game.hardDrop();
        game.getGrid().set(9, 0, Fruit.CHERRY);
        assertFalse(game.useBurst());
        assertFalse(game.useSwap());
    }
}