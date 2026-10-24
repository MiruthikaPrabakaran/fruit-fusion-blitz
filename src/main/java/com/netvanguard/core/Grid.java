package com.netvanguard.core;

/**
 * One player's board. Row 0 is the TOP, row ROWS-1 is the bottom.
 * An empty cell is null.
 *
 * Pure game logic: no JavaFX and no networking in this package.
 */
public class Grid {
    public static final int COLS = 6;
    public static final int ROWS = 10;

    private final Fruit[][] cells = new Fruit[ROWS][COLS];

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public Fruit get(int row, int col) {
        return cells[row][col];
    }

    public void set(int row, int col, Fruit fruit) {
        cells[row][col] = fruit;
    }

    public boolean isEmpty(int row, int col) {
        return cells[row][col] == null;
    }

    public void clear() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                cells[r][c] = null;
            }
        }
    }

    public int countItems() {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (cells[r][c] != null) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Makes every item fall straight down until it rests on the floor or another item. */
    public void applyGravity() {
        for (int c = 0; c < COLS; c++) {
            int writeRow = ROWS - 1;
            for (int r = ROWS - 1; r >= 0; r--) {
                Fruit f = cells[r][c];
                if (f != null) {
                    cells[r][c] = null;
                    cells[writeRow][c] = f;
                    writeRow--;
                }
            }
        }
    }

    /**
     * Game-over rule: the grid has overflowed when anything sits in the top row.
     * This is a design choice, so change it if you want a different rule.
     */
    public boolean isOverflowing() {
        for (int c = 0; c < COLS; c++) {
            if (cells[0][c] != null) {
                return true;
            }
        }
        return false;
    }

    /** ROWS * COLS characters, row by row from the top. '.' means empty. Used for network messages. */
    public String serialize() {
        StringBuilder sb = new StringBuilder(ROWS * COLS);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Fruit f = cells[r][c];
                sb.append(f == null ? '.' : f.code());
            }
        }
        return sb.toString();
    }

    public static Grid deserialize(String data) {
        if (data == null || data.length() != ROWS * COLS) {
            throw new IllegalArgumentException("Expected " + (ROWS * COLS) + " characters");
        }
        Grid grid = new Grid();
        for (int i = 0; i < data.length(); i++) {
            char ch = data.charAt(i);
            if (ch != '.') {
                grid.cells[i / COLS][i % COLS] = Fruit.fromCode(ch);
            }
        }
        return grid;
    }
}
