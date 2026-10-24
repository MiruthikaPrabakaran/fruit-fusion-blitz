package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GarbageReceiverTest {

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid();
    }

    @Test
    void sixStonesLandOnePerColumnOnTheFloor() {
        assertEquals(6, GarbageReceiver.receive(grid, 6, new Random(1)));
        for (int c = 0; c < Grid.COLS; c++) {
            assertEquals(Fruit.STONE, grid.get(Grid.ROWS - 1, c));
        }
        assertEquals(6, grid.countItems());
    }

    @Test
    void stonesStackOnTopOfExistingItems() {
        grid.set(9, 0, Fruit.CHERRY);
        GarbageReceiver.receive(grid, 6, new Random(1));
        assertEquals(Fruit.STONE, grid.get(8, 0));
        assertEquals(Fruit.CHERRY, grid.get(9, 0));
    }

    @Test
    void fullColumnCannotTakeAStone() {
        for (int r = 0; r < Grid.ROWS; r++) {
            grid.set(r, 2, Fruit.CHERRY);
        }
        assertEquals(5, GarbageReceiver.receive(grid, 6, new Random(1)));
    }

    @Test
    void stoneLandingInTopRowOverflowsTheGrid() {
        for (int r = 1; r < Grid.ROWS; r++) {
            grid.set(r, 0, Fruit.CHERRY);
        }
        assertFalse(grid.isOverflowing());
        GarbageReceiver.receive(grid, 6, new Random(1));
        assertTrue(grid.isOverflowing());
    }
}