package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScoringTest {

    private static MergeResult.Step step(int chainIndex, Fruit from, int size, int stones) {
        return new MergeResult.Step(chainIndex, List.of(new MergeResult.Merge(from, size)), stones);
    }

    @Test
    void emptyResultScoresNothingAndSendsNothing() {
        assertEquals(0, Scoring.points(MergeResult.empty()));
        assertEquals(0, Scoring.garbageFor(MergeResult.empty()));
    }

    @Test
    void singleMergeScoresTierTimesTenTimesSize() {
        MergeResult r = new MergeResult(List.of(step(1, Fruit.CHERRY, 2, 0)));
        assertEquals(20, Scoring.points(r));
        assertEquals(0, Scoring.garbageFor(r));
    }

    @Test
    void laterChainStepsAreMultiplied() {
        MergeResult r = new MergeResult(List.of(
                step(1, Fruit.CHERRY, 2, 0),      // 1*10*2 = 20
                step(2, Fruit.STRAWBERRY, 2, 0))); // (2*10*2) * 2 = 80
        assertEquals(100, Scoring.points(r));
    }

    @Test
    void destroyedStonesGivePoints() {
        MergeResult r = new MergeResult(List.of(step(1, Fruit.CHERRY, 2, 2)));
        assertEquals(20 + 40, Scoring.points(r));
    }

    @Test
    void chainOfTwoSendsTwoStones() {
        MergeResult r = new MergeResult(List.of(
                step(1, Fruit.CHERRY, 2, 0),
                step(2, Fruit.STRAWBERRY, 2, 0)));
        assertEquals(2, Scoring.garbageFor(r));
    }

    @Test
    void chainOfThreeSendsFiveStones() {
        MergeResult r = new MergeResult(List.of(
                step(1, Fruit.CHERRY, 2, 0),
                step(2, Fruit.STRAWBERRY, 2, 0),
                step(3, Fruit.GRAPE, 2, 0)));
        assertEquals(5, Scoring.garbageFor(r));
    }

    @Test
    void bigMergeSendsExtraStonesEvenWithoutAChain() {
        MergeResult r = new MergeResult(List.of(step(1, Fruit.APPLE, 2, 0)));
        assertEquals(2, Scoring.garbageFor(r));
    }
}