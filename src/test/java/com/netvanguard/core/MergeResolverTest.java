package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MergeResolverTest {

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid();
    }

    @Test
    void singleItemsAndDiagonalsDoNotMerge() {
        grid.set(9, 0, Fruit.CHERRY);
        grid.set(8, 1, Fruit.CHERRY); // diagonal only
        MergeResult result = MergeResolver.resolve(grid);
        assertTrue(result.isEmpty());
        assertEquals(2, grid.countItems());
    }

    @Test
    void horizontalPairMergesIntoNextTierAtLeftmostCell() {
        grid.set(9, 0, Fruit.CHERRY);
        grid.set(9, 1, Fruit.CHERRY);
        MergeResult result = MergeResolver.resolve(grid);
        assertEquals(1, result.chainLength());
        assertEquals(Fruit.STRAWBERRY, grid.get(9, 0));
        assertNull(grid.get(9, 1));
        assertEquals(1, grid.countItems());
        MergeResult.Merge merge = result.steps().get(0).merges().get(0);
        assertEquals(Fruit.CHERRY, merge.from());
        assertEquals(2, merge.size());
    }

    @Test
    void verticalPairMergesAtTheLowerCell() {
        grid.set(9, 2, Fruit.CHERRY);
        grid.set(8, 2, Fruit.CHERRY);
        MergeResolver.resolve(grid);
        assertEquals(Fruit.STRAWBERRY, grid.get(9, 2));
        assertNull(grid.get(8, 2));
    }

    @Test
    void groupOfThreeBecomesOneNextTierFruit() {
        grid.set(9, 0, Fruit.CHERRY);
        grid.set(9, 1, Fruit.CHERRY);
        grid.set(9, 2, Fruit.CHERRY);
        MergeResult result = MergeResolver.resolve(grid);
        assertEquals(1, grid.countItems());
        assertEquals(Fruit.STRAWBERRY, grid.get(9, 0));
        assertEquals(3, result.steps().get(0).merges().get(0).size());
    }

    @Test
    void mergedFruitCanMergeAgainAsAChain() {
        grid.set(9, 0, Fruit.STRAWBERRY);
        grid.set(9, 1, Fruit.CHERRY);
        grid.set(9, 2, Fruit.CHERRY);
        MergeResult result = MergeResolver.resolve(grid);
        assertEquals(2, result.chainLength());
        assertEquals(Fruit.GRAPE, grid.get(9, 0));
        assertEquals(1, grid.countItems());
    }

    @Test
    void gravityIsAppliedBetweenChainSteps() {
        grid.set(9, 1, Fruit.CHERRY);
        grid.set(9, 2, Fruit.CHERRY);
        grid.set(8, 2, Fruit.STRAWBERRY); // falls into the gap, then merges with the new strawberry
        MergeResult result = MergeResolver.resolve(grid);
        assertEquals(2, result.chainLength());
        assertEquals(Fruit.GRAPE, grid.get(9, 1));
        assertEquals(1, grid.countItems());
    }

    @Test
    void stonesNextToAMergeAreDestroyedOthersStay() {
        grid.set(9, 0, Fruit.CHERRY);
        grid.set(9, 1, Fruit.CHERRY);
        grid.set(9, 2, Fruit.STONE); // touches the group
        grid.set(9, 5, Fruit.STONE); // far away
        MergeResult result = MergeResolver.resolve(grid);
        assertEquals(1, result.steps().get(0).stonesCleared());
        assertNull(grid.get(9, 2));
        assertEquals(Fruit.STONE, grid.get(9, 5));
        assertEquals(2, grid.countItems());
    }

    @Test
    void stonesNeverMergeWithEachOther() {
        grid.set(9, 0, Fruit.STONE);
        grid.set(9, 1, Fruit.STONE);
        assertTrue(MergeResolver.resolve(grid).isEmpty());
        assertEquals(2, grid.countItems());
    }

    @Test
    void topTierPairDisappears() {
        grid.set(9, 0, Fruit.WATERMELON);
        grid.set(9, 1, Fruit.WATERMELON);
        MergeResult result = MergeResolver.resolve(grid);
        assertEquals(0, grid.countItems());
        assertEquals(Fruit.WATERMELON, result.steps().get(0).merges().get(0).from());
    }
}