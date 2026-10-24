package com.netvanguard.core;

import java.util.Objects;

/**
 * The single fruit currently falling in a player's grid.
 *
 * It tracks its own position and whether it has landed. It only reads the Grid
 * to check for walls and occupied cells; it never writes to it until
 * {@link #lockInto(Grid)} is called by the Game once the item has landed.
 *
 * Coordinates match Grid: row 0 is the top, rows increase downward.
 */
public class FallingItem {

    private int row;
    private int col;
    private final Fruit fruit;
    private boolean landed;

    public FallingItem(Fruit fruit, int row, int col) {
        Objects.requireNonNull(fruit, "fruit");
        if (!fruit.isMergeable()) {
            throw new IllegalArgumentException("STONE is garbage and cannot be a falling item");
        }
        this.fruit = fruit;
        this.row = row;
        this.col = col;
    }

    /** Creates an item in the top row, middle column. */
    public static FallingItem spawn(Fruit fruit) {
        return new FallingItem(fruit, 0, Grid.COLS / 2);
    }

    // ---------- Movement ----------

    /** Moves one column left. Returns true if it moved. */
    public boolean moveLeft(Grid grid) {
        return tryMove(grid, 0, -1);
    }

    /** Moves one column right. Returns true if it moved. */
    public boolean moveRight(Grid grid) {
        return tryMove(grid, 0, 1);
    }

    /**
     * Moves one row down (game tick or soft-drop key).
     * If the cell below is blocked, the item is marked as landed.
     *
     * @return true if it moved, false if it landed
     */
    public boolean moveDown(Grid grid) {
        if (landed) {
            return false;
        }
        if (isFree(grid, row + 1, col)) {
            row++;
            return true;
        }
        landed = true;
        return false;
    }

    /**
     * Drops the item as far as it can go and marks it as landed.
     *
     * @return how many rows it fell
     */
    public int hardDrop(Grid grid) {
        if (landed) {
            return 0;
        }
        int fell = landingRow(grid) - row;
        row += fell;
        landed = true;
        return fell;
    }

    /** The row this item would land on if dropped now (for a ghost preview). Does not move it. */
    public int landingRow(Grid grid) {
        int r = row;
        while (isFree(grid, r + 1, col)) {
            r++;
        }
        return r;
    }

    // ---------- Grid interaction ----------

    /** True if the item's current cell is free. Game uses this right after spawning. */
    public boolean canPlace(Grid grid) {
        return isFree(grid, row, col);
    }

    /** Writes the landed item into the grid. Call once {@link #isLanded()} is true. */
    public void lockInto(Grid grid) {
        if (!landed) {
            throw new IllegalStateException("Item has not landed yet");
        }
        grid.set(row, col, fruit);
    }

    // ---------- State ----------

    public boolean isLanded() {
        return landed;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Fruit getFruit() {
        return fruit;
    }

    // ---------- Internal ----------

    private boolean tryMove(Grid grid, int dRow, int dCol) {
        if (landed) {
            return false;
        }
        int newRow = row + dRow;
        int newCol = col + dCol;
        if (isFree(grid, newRow, newCol)) {
            row = newRow;
            col = newCol;
            return true;
        }
        return false;
    }

    private static boolean isFree(Grid grid, int r, int c) {
        return grid.inBounds(r, c) && grid.isEmpty(r, c);
    }

    @Override
    public String toString() {
        return "FallingItem{" + fruit + ", row=" + row + ", col=" + col + ", landed=" + landed + "}";
    }
}