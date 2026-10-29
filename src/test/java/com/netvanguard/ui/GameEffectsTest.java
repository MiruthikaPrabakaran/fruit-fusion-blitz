package com.netvanguard.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.netvanguard.core.Fruit;
import com.netvanguard.core.Game;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GameEffectsTest {

    private static final double DT = 0.016;

    private static Game newGame() {
        return new Game(new Random(42));
    }

    private static GameEffects newEffects() {
        return new GameEffects(48, new Random(1));
    }

    private static boolean hasText(GameEffects fx, String part) {
        return fx.texts().stream().anyMatch(t -> t.text.contains(part));
    }

    /** Sets the grid so the falling fruit causes a chain of 2, then drops it. */
    private static void dropIntoAChain(Game game, GameEffects fx) {
        Fruit f = game.getCurrent().getFruit();
        int col = game.getCurrent().getCol();
        game.getGrid().set(9, col, f);
        game.getGrid().set(9, col + 1, f.next());
        fx.update(game, DT); // sees the grid before the drop
        game.hardDrop();
        fx.update(game, DT);
    }

    @Test
    void aQuietGameMakesNoEffects() {
        Game game = newGame();
        GameEffects fx = newEffects();
        for (int i = 0; i < 5; i++) {
            fx.update(game, DT);
            game.tick();
        }
        assertTrue(fx.texts().isEmpty());
        assertTrue(fx.rings().isEmpty());
        assertEquals(0.0, fx.shakeStrength());
    }

    @Test
    void landingWithoutAMergeMakesDustButNoText() {
        Game game = newGame();
        GameEffects fx = newEffects();
        fx.update(game, DT);
        game.hardDrop();
        fx.update(game, DT);
        assertFalse(fx.particles().isEmpty());
        assertTrue(fx.texts().isEmpty());
        assertTrue(fx.rings().isEmpty());
    }

    @Test
    void aSingleMergeShowsItsPointsWithoutChainText() {
        Game game = newGame();
        GameEffects fx = newEffects();
        Fruit f = game.getCurrent().getFruit();
        game.getGrid().set(9, game.getCurrent().getCol(), f);
        fx.update(game, DT);
        game.hardDrop();
        fx.update(game, DT);
        assertTrue(hasText(fx, "+"));
        assertFalse(hasText(fx, "CHAIN"));
        assertFalse(fx.rings().isEmpty());
    }

    @Test
    void aChainShowsChainTextRingsAndShakes() {
        Game game = newGame();
        GameEffects fx = newEffects();
        dropIntoAChain(game, fx);
        assertTrue(hasText(fx, "CHAIN x2!"));
        assertFalse(fx.rings().isEmpty());
        assertFalse(fx.particles().isEmpty());
        assertTrue(fx.shakeStrength() > 0);
    }

    @Test
    void aBurstShowsBurstText() {
        Game game = newGame();
        GameEffects fx = newEffects();
        game.getGrid().set(9, 0, Fruit.CHERRY);
        fx.update(game, DT);
        assertTrue(game.useBurst());
        fx.update(game, DT);
        assertTrue(hasText(fx, "BURST!"));
        assertTrue(fx.shakeStrength() > 0);
    }

    @Test
    void landingStonesShowsHowManyAndShakes() {
        Game game = newGame();
        GameEffects fx = newEffects();
        fx.update(game, DT);
        game.receiveGarbage(3);
        game.hardDrop();
        fx.update(game, DT);
        assertTrue(hasText(fx, "+3 STONES"));
        assertTrue(fx.shakeStrength() > 0);
    }

    @Test
    void dangerRisesWithTheStackHeight() {
        Game game = newGame();
        GameEffects fx = newEffects();
        fx.update(game, DT);
        assertEquals(0.0, fx.dangerLevel());
        game.getGrid().set(1, 0, Fruit.GRAPE);
        fx.update(game, DT);
        assertEquals(0.75, fx.dangerLevel());
    }

    @Test
    void incomingBarFollowsPendingGarbage() {
        Game game = newGame();
        GameEffects fx = newEffects();
        game.receiveGarbage(3);
        fx.update(game, DT);
        assertEquals(0.5, fx.incomingFraction());
        game.receiveGarbage(10);
        fx.update(game, DT);
        assertEquals(1.0, fx.incomingFraction());
    }

    @Test
    void effectsFadeAwayByThemselves() {
        Game game = newGame();
        GameEffects fx = newEffects();
        dropIntoAChain(game, fx);
        for (int i = 0; i < 5; i++) {
            fx.update(game, 1.0);
        }
        assertTrue(fx.texts().isEmpty());
        assertTrue(fx.rings().isEmpty());
        assertTrue(fx.particles().isEmpty());
        assertEquals(0.0, fx.shakeStrength());
        assertEquals(0.0, fx.shakeX());
    }

    @Test
    void aNewGameClearsOldEffects() {
        Game game = newGame();
        GameEffects fx = newEffects();
        dropIntoAChain(game, fx);
        assertFalse(fx.texts().isEmpty());
        fx.update(new Game(new Random(7)), DT);
        assertTrue(fx.texts().isEmpty());
        assertTrue(fx.rings().isEmpty());
        assertTrue(fx.particles().isEmpty());
        assertEquals(0.0, fx.shakeStrength());
    }
}