package com.netvanguard.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MatchStateTest {

    @Test
    void startsInPlaying() {
        MatchState m = new MatchState();
        assertEquals(MatchState.Result.PLAYING, m.result());
        assertFalse(m.isOver());
    }

    @Test
    void iLoseWhenMyGridOverflows() {
        MatchState m = new MatchState();
        m.iLost();
        assertEquals(MatchState.Result.LOSE, m.result());
        assertTrue(m.isOver());
    }

    @Test
    void iWinWhenOpponentLoses() {
        MatchState m = new MatchState();
        m.opponentLost();
        assertEquals(MatchState.Result.WIN, m.result());
    }

    @Test
    void bothLosingIsADraw() {
        MatchState m = new MatchState();
        m.iLost();
        m.opponentLost();
        assertEquals(MatchState.Result.DRAW, m.result());
    }

    @Test
    void opponentLeavingWhileIAmAliveIsAWin() {
        MatchState m = new MatchState();
        m.opponentLeft();
        assertEquals(MatchState.Result.OPPONENT_LEFT, m.result());
        assertTrue(m.rematchImpossible());
    }

    @Test
    void opponentLeavingAfterIAlreadyLostStaysALoss() {
        MatchState m = new MatchState();
        m.iLost();
        m.opponentLeft();
        assertEquals(MatchState.Result.LOSE, m.result());
    }

    @Test
    void opponentLeavingAfterTheyLostStaysAWin() {
        MatchState m = new MatchState();
        m.opponentLost();
        m.opponentLeft();
        assertEquals(MatchState.Result.WIN, m.result());
    }

    @Test
    void rematchNeedsBothPlayersAndOnlySendsOnce() {
        MatchState m = new MatchState();
        assertFalse(m.requestRematch(), "cannot ask for a rematch while still playing");

        m.iLost();
        assertTrue(m.requestRematch(), "first request must be sent");
        assertFalse(m.requestRematch(), "second request must not be sent again");
        assertFalse(m.bothWantRematch());

        m.opponentWantsRematch();
        assertTrue(m.bothWantRematch());

        m.reset();
        assertEquals(MatchState.Result.PLAYING, m.result());
        assertFalse(m.bothWantRematch());
        assertFalse(m.iWantRematch());
    }

    @Test
    void opponentCanAskForRematchBeforeIAmFinished() {
        MatchState m = new MatchState();
        m.opponentWantsRematch();
        assertFalse(m.bothWantRematch());
        m.opponentLost();
        assertTrue(m.requestRematch());
        assertTrue(m.bothWantRematch());
    }

    @Test
    void noRematchAfterOpponentLeft() {
        MatchState m = new MatchState();
        m.iLost();
        m.opponentLeft();
        assertFalse(m.requestRematch());
        m.opponentWantsRematch();
        assertFalse(m.bothWantRematch());
    }

    @Test
    void intArgumentParsesSafely() {
        assertEquals(3, Protocol.intArgument("GARBAGE 3"));
        assertEquals(0, Protocol.intArgument("GARBAGE"));
        assertEquals(0, Protocol.intArgument("GARBAGE abc"));
        assertEquals(0, Protocol.intArgument("GARBAGE -5"));
        assertEquals(1200, Protocol.intArgument("SCORE 1200"));
    }
}
