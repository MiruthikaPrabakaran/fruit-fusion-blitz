package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class GameTest {

    private static Game newGame() {
        return new Game(new Random(42));
    }

    @Test
    void startsRunningWithAnItemAndAPreview() {
        Game game = newGame();
        assertEquals(Game.State.RUNNING, game.getState());
        assertNotNull(game.getCurrent());
        assertNotNull(game.getNext());
        assertEquals(0, game.getScore());
        assertEquals(0, game.getPendingGarbage());
        assertEquals(0, game.takeOutgoingGarbage());
    }

    @Test
    void tickMovesTheItemDownOneRow() {
        Game game = newGame();
        game.tick();
        assertEquals(1, game.getCurrent().getRow());
    }

    @Test
    void movesLeftAndRight() {
        Game game = newGame();
        int col = game.getCurrent().getCol();
        game.moveLeft();
        assertEquals(col - 1, game.getCurrent().getCol());
        game.moveRight();
        assertEquals(col, game.getCurrent().getCol());
    }

    @Test
    void hardDropLocksItemAtTheBottomAndSpawnsTheNext() {
        Game game = newGame();
        Fruit upcoming = game.getNext();
        game.hardDrop();
        assertNotNull(game.getGrid().get(Grid.ROWS - 1, Grid.COLS / 2));
        assertEquals(1, game.getGrid().countItems());
        assertEquals(upcoming, game.getCurrent().getFruit());
        assertEquals(Game.State.RUNNING, game.getState());
    }

    @Test
    void incomingGarbageLandsAfterTheNextDrop() {
        Game game = newGame();
        game.addIncomingGarbage(3);
        game.hardDrop();
        assertEquals(0, game.getPendingGarbage());
        assertEquals(1 + 3, game.getGrid().countItems());
    }

    @Test
    void garbageIsCappedPerDrop() {
        Game game = newGame();
        game.addIncomingGarbage(10);
        game.hardDrop();
        assertEquals(10 - Game.MAX_GARBAGE_PER_DROP, game.getPendingGarbage());
        assertEquals(1 + Game.MAX_GARBAGE_PER_DROP, game.getGrid().countItems());
    }

    /** Arranges the grid so the current item causes a chain of 2 (which sends 2 stones). */
    private static Game gameAboutToChain() {
        Game game = newGame();
        Fruit f = game.getCurrent().getFruit();
        int col = game.getCurrent().getCol();
        game.getGrid().set(9, col, f);
        game.getGrid().set(9, col + 1, f.next());
        return game;
    }

    @Test
    void aChainSendsGarbageToTheOpponent() {
        Game game = gameAboutToChain();
        game.hardDrop();
        assertTrue(game.getScore() > 0);
        assertEquals(2, game.takeOutgoingGarbage());
        assertEquals(0, game.takeOutgoingGarbage());
    }

    @Test
    void outgoingGarbageCancelsIncomingGarbageFirst() {
        Game game = gameAboutToChain();
        game.addIncomingGarbage(3);
        game.hardDrop();
        assertEquals(0, game.takeOutgoingGarbage()); // 2 sent, 2 cancelled
        assertEquals(0, game.getPendingGarbage());
        assertEquals(1 + 1, game.getGrid().countItems()); // merged fruit + the 1 stone that got through
    }

    @Test
    void gameEndsWhenSomethingSitsInTheTopRow() {
        Game game = newGame();
        game.getGrid().set(0, 0, Fruit.STONE);
        game.hardDrop();
        assertTrue(game.isGameOver());
        assertNull(game.getCurrent());
    }

    @Test
    void nothingChangesAfterGameOver() {
        Game game = newGame();
        game.getGrid().set(0, 0, Fruit.STONE);
        game.hardDrop();
        int items = game.getGrid().countItems();
        game.tick();
        game.hardDrop();
        game.moveLeft();
        assertEquals(items, game.getGrid().countItems());
    }
}