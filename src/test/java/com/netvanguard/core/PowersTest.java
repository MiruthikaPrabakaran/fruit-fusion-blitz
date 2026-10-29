package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PowersTest {

    @Test
    void startsWithTheStartingCharges() {
        Powers p = new Powers();
        assertEquals(Powers.START_BURSTS, p.getBursts());
        assertEquals(Powers.START_SWAPS, p.getSwaps());
    }

    @Test
    void spendingStopsAtZero() {
        Powers p = new Powers();
        assertTrue(p.spendBurst());
        assertFalse(p.spendBurst());
        assertEquals(0, p.getBursts());
        assertTrue(p.spendSwap());
        assertTrue(p.spendSwap());
        assertFalse(p.spendSwap());
    }

    @Test
    void aChainOfTwoEarnsABurstButASingleMergeDoesNot() {
        Powers p = new Powers();
        p.earnFromChain(1);
        assertEquals(Powers.START_BURSTS, p.getBursts());
        p.earnFromChain(2);
        assertEquals(Powers.START_BURSTS + 1, p.getBursts());
    }

    @Test
    void crossingAScoreMilestoneEarnsASwap() {
        Powers p = new Powers();
        p.earnFromScore(400, 600);
        assertEquals(Powers.START_SWAPS + 1, p.getSwaps());
        p.earnFromScore(600, 700); // no new milestone
        assertEquals(Powers.START_SWAPS + 1, p.getSwaps());
    }

    @Test
    void aBigJumpCanEarnSeveralSwapsButChargesAreCapped() {
        Powers p = new Powers();
        p.earnFromScore(0, 5000);
        assertEquals(Powers.MAX_CHARGES, p.getSwaps());
        for (int i = 0; i < 10; i++) {
            p.earnFromChain(5);
        }
        assertEquals(Powers.MAX_CHARGES, p.getBursts());
    }

    @Test
    void burstOnlyPopsCherriesAndStrawberries() {
        Grid grid = new Grid();
        grid.set(9, 0, Fruit.CHERRY);
        grid.set(9, 1, Fruit.STRAWBERRY);
        grid.set(9, 2, Fruit.GRAPE);
        grid.set(9, 3, Fruit.STONE);
        Powers.BurstResult r = Powers.burst(grid);
        assertEquals(2, r.count());
        assertEquals(1 * 10 + 2 * 10, r.points());
        assertEquals(2, grid.countItems());
        assertEquals(Fruit.GRAPE, grid.get(9, 2));
        assertEquals(Fruit.STONE, grid.get(9, 3));
    }

    @Test
    void burstOnAnEmptyGridDoesNothing() {
        assertEquals(0, Powers.burst(new Grid()).count());
    }
}