package com.netvanguard.ui;

import com.netvanguard.core.Fruit;
import com.netvanguard.core.Game;
import com.netvanguard.core.Grid;
import com.netvanguard.core.MergeResult;
import com.netvanguard.core.Scoring;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The "juice" brain: watches a Game from frame to frame and creates sparks, rings, floating text and
 * screen shake when something happens. It has no JavaFX in it (EffectsLayer draws what it produces),
 * so it can be unit tested.
 *
 * How it notices things: each update() compares the grid with the previous frame, and looks at
 * game.getLastResult() (a new object after every merge) and game.getBurstsUsed().
 * Positions are pixels on the board canvas; cell is the size of one grid cell in pixels.
 */
public class GameEffects {

    public static final String GOLD = "#fbbf24";
    public static final String WHITE = "#ffffff";
    public static final String RED = "#f87171";
    public static final String ORANGE = "#fb923c";

    private static final double GRAVITY = 520;
    private static final double SHAKE_DECAY = 30;
    private static final double MAX_SHAKE = 14;
    private static final int MAX_PARTICLES = 300;
    private static final int MAX_TEXTS = 12;
    private static final int DANGER_ROWS = 3;

    public static final class Particle {
        public double x, y, vx, vy, life, maxLife, size;
        public String color;

        public double alpha() {
            return Math.max(0, life / maxLife);
        }
    }

    public static final class FloatingText {
        public final String text;
        public final String color;
        public final double fontSize;
        public final double duration;
        public double x, y, age;

        FloatingText(String text, String color, double fontSize, double duration, double x, double y) {
            this.text = text;
            this.color = color;
            this.fontSize = fontSize;
            this.duration = duration;
            this.x = x;
            this.y = y;
        }

        /** Fully visible for the first 60% of its life, then fades out. */
        public double alpha() {
            double fadeStart = duration * 0.6;
            return age <= fadeStart ? 1 : Math.max(0, 1 - (age - fadeStart) / (duration - fadeStart));
        }
    }

    public static final class Ring {
        public final double x, y, duration;
        public final String color;
        public double age;

        Ring(double x, double y, double duration, String color) {
            this.x = x;
            this.y = y;
            this.duration = duration;
            this.color = color;
        }

        /** 0 when it appears, 1 when it is gone. */
        public double progress() {
            return Math.min(1, age / duration);
        }
    }

    private final double cell;
    private final Random rnd;
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();

    private Game lastGame;
    private final Fruit[][] prev = new Fruit[Grid.ROWS][Grid.COLS];
    private int prevStones;
    private MergeResult lastResult = MergeResult.empty();
    private int lastBursts;

    private double time;
    private double shake;
    private double offsetX;
    private double offsetY;
    private double dangerLevel;
    private double incomingFraction;

    public GameEffects(double cell) {
        this(cell, new Random());
    }

    public GameEffects(double cell, Random rnd) {
        this.cell = cell;
        this.rnd = rnd;
    }

    // ---------- What the drawing code reads ----------

    public List<Particle> particles() { return Collections.unmodifiableList(particles); }
    public List<FloatingText> texts() { return Collections.unmodifiableList(texts); }
    public List<Ring> rings() { return Collections.unmodifiableList(rings); }

    public double shakeX() { return offsetX; }
    public double shakeY() { return offsetY; }
    public double shakeStrength() { return shake; }

    /** 0 = safe, up to 1 = fruit sitting right under the top row. */
    public double dangerLevel() { return dangerLevel; }

    /** 0 to 1: how much garbage is waiting to land (full bar = a full drop's worth). */
    public double incomingFraction() { return incomingFraction; }

    /** Goes smoothly between 0 and 1, for pulsing things. */
    public double pulse() { return 0.5 + 0.5 * Math.sin(time * 7); }

    public double boardWidth() { return Grid.COLS * cell; }
    public double boardHeight() { return Grid.ROWS * cell; }

    // ---------- Per-frame update ----------

    /** Call once per frame. dt is the time since the last frame in seconds. */
    public void update(Game game, double dt) {
        time += dt;
        advance(dt);

        if (game != lastGame) {
            // A different game (restart): forget everything and start fresh.
            lastGame = game;
            particles.clear();
            texts.clear();
            rings.clear();
            shake = 0;
        } else {
            detect(game);
        }
        snapshot(game);
        lastResult = game.getLastResult();
        lastBursts = game.getBurstsUsed();

        computeWarnings(game);
        if (shake > 0) {
            offsetX = (rnd.nextDouble() * 2 - 1) * shake;
            offsetY = (rnd.nextDouble() * 2 - 1) * shake;
        } else {
            offsetX = 0;
            offsetY = 0;
        }
    }

    private void advance(double dt) {
        for (Particle p : particles) {
            p.life -= dt;
            p.vy += GRAVITY * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
        particles.removeIf(p -> p.life <= 0);
        for (FloatingText t : texts) {
            t.age += dt;
            t.y -= 36 * dt;
        }
        texts.removeIf(t -> t.age >= t.duration);
        for (Ring r : rings) {
            r.age += dt;
        }
        rings.removeIf(r -> r.age >= r.duration);
        shake = Math.max(0, shake - SHAKE_DECAY * dt);
    }

    private void snapshot(Game game) {
        Grid grid = game.getGrid();
        prevStones = 0;
        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                prev[r][c] = grid.get(r, c);
                if (prev[r][c] == Fruit.STONE) {
                    prevStones++;
                }
            }
        }
    }

    private void detect(Game game) {
        Grid grid = game.getGrid();
        MergeResult result = game.getLastResult();
        boolean merged = result != lastResult && !result.isEmpty();
        boolean burst = game.getBurstsUsed() != lastBursts;

        List<int[]> removed = new ArrayList<>();
        List<int[]> grown = new ArrayList<>();   // a fruit that is new or a different fruit than before
        List<int[]> stones = new ArrayList<>();  // stones that are new
        int stonesNow = 0;
        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                Fruit now = grid.get(r, c);
                Fruit before = prev[r][c];
                if (now == Fruit.STONE) {
                    stonesNow++;
                }
                if (before != null && now == null) {
                    removed.add(new int[] {r, c});
                }
                if (now != null && now != before) {
                    if (now == Fruit.STONE) {
                        stones.add(new int[] {r, c});
                    } else {
                        grown.add(new int[] {r, c});
                    }
                }
            }
        }

        if (merged) {
            int chain = result.chainLength();
            int points = Scoring.points(result);
            double[] at = anchorOf(grid, grown, removed);
            if (chain >= 2) {
                addText("CHAIN x" + chain + "!", at[0], at[1], GOLD, 26, 1.3);
                addText("+" + points, at[0], at[1] + 26, WHITE, 18, 1.3);
                addRing(at[0], at[1], GOLD);
                addShake(Math.min(MAX_SHAKE, 4 + 3.0 * chain));
            } else {
                addText("+" + points, at[0], at[1], WHITE, 18, 0.9);
                addRing(at[0], at[1], WHITE);
                addShake(2.5);
            }
        }
        if (merged || burst) {
            for (int[] pos : removed) {
                double[] p = center(pos);
                sparks(p[0], p[1], colorOf(prev[pos[0]][pos[1]]), 4, 140);
            }
        }
        if (burst) {
            addText("BURST!", boardWidth() / 2, cell * 2, ORANGE, 34, 1.2);
            for (int[] pos : removed) {
                double[] p = center(pos);
                addRing(p[0], p[1], ORANGE);
            }
            addShake(7);
        }
        if (!merged && !burst) {
            for (int[] pos : grown) { // just landed: a little puff of dust
                double[] p = center(pos);
                sparks(p[0], p[1] + cell * 0.3, "#d1d5db", 3, 60);
            }
        }
        int addedStones = stonesNow - prevStones;
        if (addedStones > 0 && !burst) {
            addText("+" + addedStones + (addedStones == 1 ? " STONE" : " STONES"),
                    boardWidth() / 2, cell * 1.0, RED, 24, 1.2);
            for (int[] pos : stones) {
                double[] p = center(pos);
                sparks(p[0], p[1], colorOf(Fruit.STONE), 3, 90);
            }
            addShake(Math.min(MAX_SHAKE, 3 + addedStones));
        }
    }

    private void computeWarnings(Game game) {
        Grid grid = game.getGrid();
        int highest = Grid.ROWS;
        search:
        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                if (grid.get(r, c) != null) {
                    highest = r;
                    break search;
                }
            }
        }
        dangerLevel = highest <= DANGER_ROWS ? (DANGER_ROWS + 1 - highest) / (double) (DANGER_ROWS + 1) : 0;
        incomingFraction = Math.min(1.0, game.getPendingGarbage() / (double) Game.MAX_GARBAGE_PER_DROP);
    }

    // ---------- Helpers ----------

    /** Where to put the text for a merge: the biggest new fruit, else the pop location, else the middle. */
    private double[] anchorOf(Grid grid, List<int[]> grown, List<int[]> removed) {
        int[] best = null;
        int bestTier = -1;
        for (int[] pos : grown) {
            int tier = grid.get(pos[0], pos[1]).tier();
            if (tier > bestTier || (tier == bestTier && pos[0] > best[0])) {
                best = pos;
                bestTier = tier;
            }
        }
        if (best != null) {
            return center(best);
        }
        if (!removed.isEmpty()) {
            double x = 0;
            double y = 0;
            for (int[] pos : removed) {
                double[] p = center(pos);
                x += p[0];
                y += p[1];
            }
            return new double[] {x / removed.size(), y / removed.size()};
        }
        return new double[] {boardWidth() / 2, boardHeight() / 2};
    }

    private double[] center(int[] pos) {
        return new double[] {(pos[1] + 0.5) * cell, (pos[0] + 0.5) * cell};
    }

    private void addText(String text, double x, double y, String color, double size, double duration) {
        double half = text.length() * size * 0.32;
        double cx = Math.max(half, Math.min(boardWidth() - half, x));
        double cy = Math.max(size, y);
        texts.add(new FloatingText(text, color, size, duration, cx, cy));
        while (texts.size() > MAX_TEXTS) {
            texts.remove(0);
        }
    }

    private void addRing(double x, double y, String color) {
        rings.add(new Ring(x, y, 0.45, color));
    }

    private void addShake(double strength) {
        shake = Math.min(MAX_SHAKE, Math.max(shake, strength));
    }

    private void sparks(double x, double y, String color, int count, double speed) {
        for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double v = speed * (0.5 + rnd.nextDouble());
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = Math.cos(angle) * v;
            p.vy = Math.sin(angle) * v - speed * 0.4;
            p.maxLife = 0.45 + rnd.nextDouble() * 0.35;
            p.life = p.maxLife;
            p.size = 3 + rnd.nextDouble() * 3;
            p.color = color;
            particles.add(p);
        }
    }

    /** Colours chosen to match the fruit pictures. */
    static String colorOf(Fruit fruit) {
        if (fruit == null) {
            return WHITE;
        }
        return switch (fruit) {
            case STONE -> "#9ca3af";
            case CHERRY -> "#ef4444";
            case STRAWBERRY -> "#fb7185";
            case GRAPE -> "#8b5cf6";
            case ORANGE -> "#fb923c";
            case APPLE -> "#a3e635";
            case WATERMELON -> "#22c55e";
        };
    }
}