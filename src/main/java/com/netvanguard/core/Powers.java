package com.netvanguard.core;

/**
 * The two powers and how you earn them. Pure logic, no JavaFX.
 *
 *  BURST: every small fruit (CHERRY and STRAWBERRY) on your grid pops. Gravity then runs, which can
 *         start a chain. Costs one charge and does nothing if there is nothing to pop.
 *  SWAP:  changes the NEXT fruit to the next kind in the spawn list (Cherry, Strawberry, Grape, back
 *         to Cherry). Costs one charge.
 *
 * Charges: you start with START_BURSTS and START_SWAPS. A chain of 2 or more earns a burst, and every
 * 500 points earns a swap. You can never hold more than MAX_CHARGES of either. All numbers are here
 * so they are easy to tune.
 */
public class Powers {

    public static final int START_BURSTS = 1;
    public static final int START_SWAPS = 2;
    public static final int MAX_CHARGES = 3;
    public static final int CHAIN_FOR_BURST = 2;
    public static final int POINTS_PER_SWAP = 500;
    public static final int BURST_MAX_TIER = Fruit.STRAWBERRY.tier();
    public static final int POINTS_PER_BURST_TIER = 10;

    /** How many fruits popped and the points they gave (tier * 10 each). */
    public record BurstResult(int count, int points) {}

    private int bursts = START_BURSTS;
    private int swaps = START_SWAPS;

    public int getBursts() {
        return bursts;
    }

    public int getSwaps() {
        return swaps;
    }

    /** Uses one burst charge. Returns false if there is none. */
    public boolean spendBurst() {
        if (bursts == 0) {
            return false;
        }
        bursts--;
        return true;
    }

    /** Uses one swap charge. Returns false if there is none. */
    public boolean spendSwap() {
        if (swaps == 0) {
            return false;
        }
        swaps--;
        return true;
    }

    public void earnFromChain(int chainLength) {
        if (chainLength >= CHAIN_FOR_BURST) {
            bursts = Math.min(MAX_CHARGES, bursts + 1);
        }
    }

    public void earnFromScore(int oldScore, int newScore) {
        int gained = newScore / POINTS_PER_SWAP - oldScore / POINTS_PER_SWAP;
        if (gained > 0) {
            swaps = Math.min(MAX_CHARGES, swaps + gained);
        }
    }

    /** Removes every small fruit from the grid (it does not apply gravity). Stones are never removed. */
    public static BurstResult burst(Grid grid) {
        int count = 0;
        int points = 0;
        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                Fruit f = grid.get(r, c);
                if (f != null && f.isMergeable() && f.tier() <= BURST_MAX_TIER) {
                    grid.set(r, c, null);
                    count++;
                    points += f.tier() * POINTS_PER_BURST_TIER;
                }
            }
        }
        return new BurstResult(count, points);
    }
}