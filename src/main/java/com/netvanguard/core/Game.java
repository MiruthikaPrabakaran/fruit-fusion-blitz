package com.netvanguard.core;

import java.util.Random;

/**
 * One player's game: the grid, the current and next item, the score, the garbage queue and the powers.
 *
 * Pure logic: no JavaFX and nothing from the net package. The outside world is told what
 * happened through {@link GameEvents}:
 *   onScoreChanged(score)  - the score went up
 *   onGarbageSent(count)   - I earned stones for the opponent (after cancelling incoming ones)
 *   onGameOver()           - my grid overflowed (called once)
 * The opponent's stones come in through receiveGarbage(count).
 *
 * Events fire on whichever thread calls tick()/hardDrop()/useBurst(). The net code must hop threads
 * itself (for example Platform.runLater) if it needs to.
 */
public class Game {

    public enum State { RUNNING, GAME_OVER }

    /** Fruits that can spawn. Bigger fruits only come from merging. */
    public static final Fruit[] SPAWN_POOL = {Fruit.CHERRY, Fruit.STRAWBERRY, Fruit.GRAPE};

    /** At most this many stones land after one drop; the rest wait in the queue. */
    public static final int MAX_GARBAGE_PER_DROP = Grid.COLS;
    /** Each power can be activated at most this many times in one game. */
    public static final int MAX_POWER_USES_PER_GAME = 3;

    private static final GameEvents NO_EVENTS = new GameEvents() {
        @Override public void onGarbageSent(int count) { }
        @Override public void onScoreChanged(int score) { }
        @Override public void onGameOver() { }
    };

    private final Grid grid = new Grid();
    private final Random rng;
    private final GameEvents events;
    private final Powers powers = new Powers();
    private int burstsUsed;
    private int swapsUsed;

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

    // ---------- Powers ----------

    /**
     * BURST: pops every cherry and strawberry on the grid, drops what is left and resolves any chain.
     * Returns false (and keeps the charge) if there is no charge or nothing small to pop.
     */
    public boolean useBurst() {
        if (state != State.RUNNING || getBursts() == 0) {
            return false;
        }
        Powers.BurstResult burst = Powers.burst(grid);
        if (burst.count() == 0) {
            return false;
        }
        powers.spendBurst();
        burstsUsed++;
        grid.applyGravity();
        addScore(burst.points());
        settleMerges();
        return true;
    }

    /** SWAP: changes the NEXT fruit to the next kind in SPAWN_POOL. Returns false if no charge is left. */
    public boolean useSwap() {
        if (state != State.RUNNING || getSwaps() == 0 || !powers.spendSwap()) {
            return false;
        }
        swapsUsed++;
        int index = 0;
        for (int i = 0; i < SPAWN_POOL.length; i++) {
            if (SPAWN_POOL[i] == next) {
                index = i;
            }
        }
        next = SPAWN_POOL[(index + 1) % SPAWN_POOL.length];
        return true;
    }

    /** Available burst charges, hidden from the UI after the per-game use limit is reached. */
    public int getBursts() {
        return burstsUsed >= MAX_POWER_USES_PER_GAME ? 0 : powers.getBursts();
    }

    /** Available swap charges, hidden from the UI after the per-game use limit is reached. */
    public int getSwaps() {
        return swapsUsed >= MAX_POWER_USES_PER_GAME ? 0 : powers.getSwaps();
    }

    /** How many bursts have been used this game (lets the UI notice a burst happened). */
    public int getBurstsUsed() { return burstsUsed; }

    /** How many swaps have been used this game. */
    public int getSwapsUsed() { return swapsUsed; }

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

        settleMerges();

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

    /** Resolves merges and chains, then scores them and handles the garbage they earn. */
    private void settleMerges() {
        lastResult = MergeResolver.resolve(grid);
        addScore(Scoring.points(lastResult));
        powers.earnFromChain(lastResult.chainLength());

        // My combo first cancels stones that were about to land on me.
        int earned = Scoring.garbageFor(lastResult);
        int cancelled = Math.min(earned, pendingGarbage);
        pendingGarbage -= cancelled;
        int toSend = earned - cancelled;
        if (toSend > 0) {
            events.onGarbageSent(toSend);
        }
    }

    private void addScore(int points) {
        if (points <= 0) {
            return;
        }
        int before = score;
        score += points;
        powers.earnFromScore(before, score);
        events.onScoreChanged(score);
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