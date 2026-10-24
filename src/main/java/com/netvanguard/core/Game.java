package com.netvanguard.core;

import java.util.Random;

/**
 * One player's game: the grid, the current and next item, the score and the garbage queue.
 *
 * Pure logic: no JavaFX and nothing from the net package. The outside world is told what
 * happened through {@link GameEvents}:
 *   onScoreChanged(score)  - the score went up
 *   onGarbageSent(count)   - I earned stones for the opponent (after cancelling incoming ones)
 *   onGameOver()           - my grid overflowed (called once)
 * The opponent's stones come in through receiveGarbage(count).
 *
 * Events fire on whichever thread calls tick()/hardDrop(). The net code must hop threads
 * itself (for example Platform.runLater) if it needs to.
 */
public class Game {

    public enum State { RUNNING, GAME_OVER }

    /** Fruits that can spawn. Bigger fruits only come from merging. */
    public static final Fruit[] SPAWN_POOL = {Fruit.CHERRY, Fruit.STRAWBERRY, Fruit.GRAPE};

    /** At most this many stones land after one drop; the rest wait in the queue. */
    public static final int MAX_GARBAGE_PER_DROP = Grid.COLS;

    private static final GameEvents NO_EVENTS = new GameEvents() {
        @Override public void onGarbageSent(int count) { }
        @Override public void onScoreChanged(int score) { }
        @Override public void onGameOver() { }
    };

    private final Grid grid = new Grid();
    private final Random rng;
    private final GameEvents events;

    private FallingItem current;
    private Fruit next;
    private int score;
    private int pendingGarbage;   // stones waiting to land on MY grid
    private MergeResult lastResult = MergeResult.empty();
    private State state = State.RUNNING;

    /** No listener (single-player / practice). */
    public Game() {
        this(new Random(), NO_EVENTS);
    }

    public Game(GameEvents events) {
        this(new Random(), events);
    }

    /** Fixed random source, handy for tests. */
    public Game(Random rng) {
        this(rng, NO_EVENTS);
    }

    public Game(Random rng, GameEvents events) {
        this.rng = rng;
        this.events = events == null ? NO_EVENTS : events;
        this.next = randomFruit();
        spawnNext();
    }

    // ---------- Controls ----------

    /** Moves the item down one row. If it cannot move, it locks into the grid. */
    public void tick() {
        if (state != State.RUNNING) {
            return;
        }
        if (!current.stepDown(grid)) {
            lockCurrent();
        }
    }

    public void moveLeft() {
        if (state == State.RUNNING) {
            current.moveLeft(grid);
        }
    }

    public void moveRight() {
        if (state == State.RUNNING) {
            current.moveRight(grid);
        }
    }

    public void softDrop() {
        tick();
    }

    public void hardDrop() {
        if (state != State.RUNNING) {
            return;
        }
        current.hardDrop(grid);
        lockCurrent();
    }

    // ---------- Garbage ----------

    /** The opponent sent stones. They land after my next drop. */
    public void receiveGarbage(int count) {
        if (count > 0) {
            pendingGarbage += count;
        }
    }

    // ---------- State for the UI ----------

    public Grid getGrid() { return grid; }
    public FallingItem getCurrent() { return current; }
    public Fruit getNext() { return next; }
    public int getScore() { return score; }
    public int getPendingGarbage() { return pendingGarbage; }
    public MergeResult getLastResult() { return lastResult; }
    public State getState() { return state; }
    public boolean isGameOver() { return state == State.GAME_OVER; }

    /** Milliseconds between ticks. Starts at 700 and speeds up as the score grows. */
    public long tickMillis() {
        return Math.max(150, 700 - (score / 200) * 25);
    }

    // ---------- Internal ----------

    private void lockCurrent() {
        current.lockInto(grid);
        current = null;

        lastResult = MergeResolver.resolve(grid);
        int points = Scoring.points(lastResult);
        if (points > 0) {
            score += points;
            events.onScoreChanged(score);
        }

        // My combo first cancels stones that were about to land on me.
        int earned = Scoring.garbageFor(lastResult);
        int cancelled = Math.min(earned, pendingGarbage);
        pendingGarbage -= cancelled;
        int toSend = earned - cancelled;
        if (toSend > 0) {
            events.onGarbageSent(toSend);
        }

        int incoming = Math.min(pendingGarbage, MAX_GARBAGE_PER_DROP);
        if (incoming > 0) {
            GarbageReceiver.receive(grid, incoming, rng);
            pendingGarbage -= incoming;
        }

        if (grid.isOverflowing()) {
            endGame();
        } else {
            spawnNext();
        }
    }

    private void spawnNext() {
        current = FallingItem.spawn(next);
        next = randomFruit();
        if (!current.canPlace(grid)) {
            current = null;
            endGame();
        }
    }

    private void endGame() {
        if (state == State.GAME_OVER) {
            return;
        }
        state = State.GAME_OVER;
        events.onGameOver();
    }

    private Fruit randomFruit() {
        return SPAWN_POOL[rng.nextInt(SPAWN_POOL.length)];
    }
}