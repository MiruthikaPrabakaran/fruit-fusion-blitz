package com.netvanguard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Receiving garbage: stones fall from the top of the grid.
 *
 * Columns are shuffled and used round-robin, so stones spread across the board
 * instead of piling into one column. A column whose top row is already taken
 * cannot take a stone. A stone that lands in row 0 makes Grid.isOverflowing() true.
 * Assumes gravity has already been applied (items rest on the floor or each other).
 */
public final class GarbageReceiver {

    private GarbageReceiver() {}

    /** Drops up to {@code count} stones. Returns how many were actually placed. */
    public static int receive(Grid grid, int count, Random rng) {
        List<Integer> columns = new ArrayList<>();
        for (int c = 0; c < Grid.COLS; c++) {
            columns.add(c);
        }
        Collections.shuffle(columns, rng);

        int placed = 0;
        for (int i = 0; i < count; i++) {
            if (dropStone(grid, columns.get(i % Grid.COLS))) {
                placed++;
            }
        }
        return placed;
    }

    private static boolean dropStone(Grid grid, int col) {
        if (!grid.isEmpty(0, col)) {
            return false;
        }
        int row = 0;
        while (row + 1 < Grid.ROWS && grid.isEmpty(row + 1, col)) {
            row++;
        }
        grid.set(row, col, Fruit.STONE);
        return true;
    }
}