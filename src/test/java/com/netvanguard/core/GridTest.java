package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GridTest {

    @Test
    void newGridIsEmpty() {
        Grid grid = new Grid();
        assertEquals(0, grid.countItems());
        assertFalse(grid.isOverflowing());
    }

    @Test
    void setAndGet() {
        Grid grid = new Grid();
        grid.set(5, 2, Fruit.GRAPE);
        assertEquals(Fruit.GRAPE, grid.get(5, 2));
        assertNull(grid.get(5, 3));
    }

    @Test
    void gravityDropsItemsToTheBottom() {
        Grid grid = new Grid();
        grid.set(2, 1, Fruit.CHERRY);
        grid.set(4, 1, Fruit.APPLE);
        grid.applyGravity();
        assertEquals(Fruit.APPLE, grid.get(Grid.ROWS - 1, 1));
        assertEquals(Fruit.CHERRY, grid.get(Grid.ROWS - 2, 1));
        assertNull(grid.get(2, 1));
    }

    @Test
    void overflowWhenTopRowIsOccupied() {
        Grid grid = new Grid();
        grid.set(0, 3, Fruit.STONE);
        assertTrue(grid.isOverflowing());
    }

    @Test
    void serializeRoundTrip() {
        Grid grid = new Grid();
        grid.set(9, 0, Fruit.CHERRY);
        grid.set(9, 5, Fruit.STONE);
        grid.set(3, 2, Fruit.WATERMELON);

        String data = grid.serialize();
        assertEquals(Grid.ROWS * Grid.COLS, data.length());

        Grid copy = Grid.deserialize(data);
        assertEquals(data, copy.serialize());
        assertEquals(Fruit.WATERMELON, copy.get(3, 2));
    }

    @Test
    void fruitTiersMergeUpwardButStoneAndTopTierDoNot() {
        assertEquals(Fruit.STRAWBERRY, Fruit.CHERRY.next());
        assertFalse(Fruit.STONE.hasNext());
        assertFalse(Fruit.WATERMELON.hasNext());
        assertThrows(IllegalStateException.class, Fruit.STONE::next);
    }
}
