package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FallingItemTest {

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid();
    }

    @Test
    void spawnsAtTopMiddle() {
        FallingItem item = FallingItem.spawn(Fruit.CHERRY);
        assertEquals(0, item.getRow());
        assertEquals(Grid.COLS / 2, item.getCol());
        assertEquals(Fruit.CHERRY, item.getFruit());
        assertFalse(item.isLanded());
    }

    @Test
    void stoneCannotBeAFallingItem() {
        assertThrows(IllegalArgumentException.class, () -> FallingItem.spawn(Fruit.STONE));
    }

    @Test
    void movesLeftAndRight() {
        FallingItem item = new FallingItem(Fruit.CHERRY, 0, 3);
        assertTrue(item.moveLeft(grid));
        assertEquals(2, item.getCol());
        assertTrue(item.moveRight(grid));
        assertEquals(3, item.getCol());
    }

    @Test
    void cannotMoveThroughWalls() {
        FallingItem left = new FallingItem(Fruit.CHERRY, 0, 0);
        assertFalse(left.moveLeft(grid));
        assertEquals(0, left.getCol());

        FallingItem right = new FallingItem(Fruit.CHERRY, 0, Grid.COLS - 1);
        assertFalse(right.moveRight(grid));
        assertEquals(Grid.COLS - 1, right.getCol());
    }

    @Test
    void cannotMoveIntoOccupiedCell() {
        grid.set(0, 2, Fruit.GRAPE);
        FallingItem item = new FallingItem(Fruit.CHERRY, 0, 3);
        assertFalse(item.moveLeft(grid));
        assertEquals(3, item.getCol());
    }

    @Test
    void movesDownOneRow() {
        FallingItem item = new FallingItem(Fruit.CHERRY, 0, 3);
        assertTrue(item.moveDown(grid));
        assertEquals(1, item.getRow());
        assertFalse(item.isLanded());
    }

    @Test
    void landsOnFloor() {
        FallingItem item = new FallingItem(Fruit.CHERRY, Grid.ROWS - 1, 3);
        assertFalse(item.moveDown(grid));
        assertTrue(item.isLanded());
        assertEquals(Grid.ROWS - 1, item.getRow());
    }

    @Test
    void landsOnTopOfOtherItem() {
        grid.set(5, 3, Fruit.GRAPE);
        FallingItem item = new FallingItem(Fruit.CHERRY, 3, 3);
        assertTrue(item.moveDown(grid)); // row 4
        assertFalse(item.moveDown(grid)); // blocked by row 5
        assertTrue(item.isLanded());
        assertEquals(4, item.getRow());
    }

    @Test
    void landedItemCannotMove() {
        FallingItem item = new FallingItem(Fruit.CHERRY, Grid.ROWS - 1, 3);
        item.moveDown(grid);
        assertFalse(item.moveLeft(grid));
        assertFalse(item.moveRight(grid));
        assertEquals(3, item.getCol());
    }

    @Test
    void hardDropFallsToFloor() {
        FallingItem item = new FallingItem(Fruit.CHERRY, 0, 3);
        int fell = item.hardDrop(grid);
        assertEquals(Grid.ROWS - 1, item.getRow());
        assertEquals(Grid.ROWS - 1, fell);
        assertTrue(item.isLanded());
    }

    @Test
    void hardDropStopsOnStack() {
        grid.set(7, 3, Fruit.GRAPE);
        FallingItem item = new FallingItem(Fruit.CHERRY, 0, 3);
        item.hardDrop(grid);
        assertEquals(6, item.getRow());
    }

    @Test
    void landingRowDoesNotMoveItem() {
        grid.set(7, 3, Fruit.GRAPE);
        FallingItem item = new FallingItem(Fruit.CHERRY, 2, 3);
        assertEquals(6, item.landingRow(grid));
        assertEquals(2, item.getRow());
        assertFalse(item.isLanded());
    }

    @Test
    void lockIntoWritesFruitToGrid() {
        FallingItem item = new FallingItem(Fruit.STRAWBERRY, 0, 3);
        item.hardDrop(grid);
        item.lockInto(grid);
        assertEquals(Fruit.STRAWBERRY, grid.get(Grid.ROWS - 1, 3));
        assertEquals(1, grid.countItems());
    }

    @Test
    void lockIntoBeforeLandingThrows() {
        FallingItem item = new FallingItem(Fruit.CHERRY, 0, 3);
        assertThrows(IllegalStateException.class, () -> item.lockInto(grid));
    }

    @Test
    void canPlaceIsFalseWhenSpawnCellIsTaken() {
        grid.set(0, Grid.COLS / 2, Fruit.GRAPE);
        assertFalse(FallingItem.spawn(Fruit.CHERRY).canPlace(grid));
    }
}