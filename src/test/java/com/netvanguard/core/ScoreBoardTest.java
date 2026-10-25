package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ScoreBoardTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 25);

    @Test
    void startsEmpty() {
        assertTrue(new ScoreBoard().getEntries().isEmpty());
    }

    @Test
    void zeroAndNegativeScoresAreNotRecorded() {
        ScoreBoard board = new ScoreBoard();
        assertEquals(0, board.add(0, DAY));
        assertEquals(0, board.add(-5, DAY));
        assertTrue(board.getEntries().isEmpty());
    }

    @Test
    void keepsScoresSortedHighestFirstAndReturnsTheRank() {
        ScoreBoard board = new ScoreBoard();
        assertEquals(1, board.add(100, DAY));
        assertEquals(1, board.add(300, DAY));
        assertEquals(2, board.add(200, DAY));
        assertEquals(300, board.getEntries().get(0).score());
        assertEquals(200, board.getEntries().get(1).score());
        assertEquals(100, board.getEntries().get(2).score());
    }

    @Test
    void aTieGoesBelowTheOlderScore() {
        ScoreBoard board = new ScoreBoard();
        board.add(100, DAY);
        assertEquals(2, board.add(100, DAY.plusDays(1)));
        assertEquals(DAY, board.getEntries().get(0).date());
    }

    @Test
    void keepsOnlyTheTopTen() {
        ScoreBoard board = new ScoreBoard();
        for (int s = 10; s <= 120; s += 10) {
            board.add(s, DAY); // 12 scores
        }
        assertEquals(ScoreBoard.MAX_ENTRIES, board.getEntries().size());
        assertEquals(120, board.getEntries().get(0).score());
        assertEquals(30, board.getEntries().get(9).score());
    }

    @Test
    void aFullBoardRejectsScoresThatAreNotBetterThanTheLast() {
        ScoreBoard board = new ScoreBoard();
        for (int s = 10; s <= 100; s += 10) {
            board.add(s, DAY);
        }
        assertFalse(board.qualifies(5));
        assertFalse(board.qualifies(10)); // ties the last place
        assertEquals(0, board.add(10, DAY));
        assertTrue(board.qualifies(11));
        assertEquals(10, board.add(11, DAY));
    }

    @Test
    void anEmptyBoardAcceptsAnyPositiveScore() {
        assertTrue(new ScoreBoard().qualifies(1));
    }
}