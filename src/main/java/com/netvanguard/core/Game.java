package com.netvanguard.core;

import java.util.Random;

/**
 * One player's game: the grid, the current and next item, the score and the garbage queues.
 *
 * Pure logic, no JavaFX. The UI calls tick() on a timer and the move methods on key presses.
 * Networking (later) uses takeOutgoingGarbage() to send stones and addIncomingGarbage() when
 * stones arrive. Call everything from one thread (the JavaFX thread, via Platform.runLater).
 */
public class Game {

    public enum State { RUNNING, GAME_OVER }

    /** Fruits that can spawn. Bigger fruits only come from merging. */
    public static final Fruit[] SPAWN_POOL = {Fruit.CHERRY, Fruit.STRAWBERRY, Fruit.GRAPE};

    /** At most this many stones land after one drop; the rest wait in the queue. */
    public static final int MAX_GARBAGE_PER_DROP = Grid.COLS;

    private final Grid grid = new Grid();
    private final Random rng;

    private FallingItem current;
    private Fruit next;
    private int score;
    private int pendingGarbage;   // stones waiting to land on MY grid
    private int outgoingGarbage;  // stones to send to the OPPONENT
    private MergeResult lastResult = MergeResult.empty();
    private State state = State.RUNNING;

    public Game() {
        this(new Random());
    }

    public Game(Random rng) {
        this.rng = rng;
        this.next = randomFruit();
        spawnNext();
    }

    // ---------- Controls ----------

    /** Moves the item down one row. If it cannot move, it locks into the grid. */
    public void tick() {
        if (state != State.RUNNING) {
            return;
        }
        if (!current.moveDown(grid)) {
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

    // ---------- Garbage (used by networking later) ----------

    /** The opponent sent stones. They land after my next drop. */
    public void addIncomingGarbage(int stones) {
        if (stones > 0) {
            pendingGarbage += stones;
        }
    }

    /** Stones I earned that still need to be sent. Returns the amount and resets it to 0. */
    public int takeOutgoingGarbage() {
        int stones = outgoingGarbage;
        outgoingGarbage = 0;
        return stones;
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
        score += Scoring.points(lastResult);

        // My combo first cancels stones that were about to land on me.
        int sent = Scoring.garbageFor(lastResult);
        int cancelled = Math.min(sent, pendingGarbage);
        pendingGarbage -= cancelled;
        outgoingGarbage += sent - cancelled;

        int incoming = Math.min(pendingGarbage, MAX_GARBAGE_PER_DROP);
        if (incoming > 0) {
            GarbageReceiver.receive(grid, incoming, rng);
            pendingGarbage -= incoming;
        }

        if (grid.isOverflowing()) {
            state = State.GAME_OVER;
        } else {
            spawnNext();
        }
    }

    private void spawnNext() {
        current = FallingItem.spawn(next);
        next = randomFruit();
        if (!current.canPlace(grid)) {
            current = null;
            state = State.GAME_OVER;
        }
    }

    private Fruit randomFruit() {
        return SPAWN_POOL[rng.nextInt(SPAWN_POOL.length)];
    }
}