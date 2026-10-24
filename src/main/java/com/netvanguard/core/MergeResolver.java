package com.netvanguard.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Merge and chain detection.
 *
 * Rules:
 *  - A group is 2 or more identical, mergeable fruits touching up/down/left/right.
 *  - A group becomes ONE fruit of the next tier, placed at the group's lowest
 *    (then leftmost) cell. The top tier (WATERMELON) has no next tier, so a group
 *    of them simply disappears.
 *  - Stones touching a merging group are destroyed.
 *  - After every round gravity is applied and the grid is scanned again. Each extra
 *    round is one step of the chain.
 */
public final class MergeResolver {

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private MergeResolver() {}

    /** Resolves the whole chain on the grid (modifies it) and reports what happened. */
    public static MergeResult resolve(Grid grid) {
        List<MergeResult.Step> steps = new ArrayList<>();
        MergeResult.Step step;
        while ((step = resolveOnce(grid, steps.size() + 1)) != null) {
            grid.applyGravity();
            steps.add(step);
        }
        return new MergeResult(steps);
    }

    /** One round: merges every group currently on the grid. Returns null if there is none. */
    private static MergeResult.Step resolveOnce(Grid grid, int chainIndex) {
        List<List<int[]>> groups = findGroups(grid);
        if (groups.isEmpty()) {
            return null;
        }

        boolean[][] stoneHit = new boolean[Grid.ROWS][Grid.COLS];
        List<MergeResult.Merge> merges = new ArrayList<>();

        for (List<int[]> group : groups) {
            Fruit fruit = grid.get(group.get(0)[0], group.get(0)[1]);

            int[] anchor = group.get(0);
            for (int[] cell : group) {
                if (cell[0] > anchor[0] || (cell[0] == anchor[0] && cell[1] < anchor[1])) {
                    anchor = cell;
                }
                for (int[] d : DIRECTIONS) {
                    int nr = cell[0] + d[0];
                    int nc = cell[1] + d[1];
                    if (grid.inBounds(nr, nc) && grid.get(nr, nc) == Fruit.STONE) {
                        stoneHit[nr][nc] = true;
                    }
                }
            }

            for (int[] cell : group) {
                grid.set(cell[0], cell[1], null);
            }
            if (fruit.hasNext()) {
                grid.set(anchor[0], anchor[1], fruit.next());
            }
            merges.add(new MergeResult.Merge(fruit, group.size()));
        }

        int stonesCleared = 0;
        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                if (stoneHit[r][c]) {
                    grid.set(r, c, null);
                    stonesCleared++;
                }
            }
        }
        return new MergeResult.Step(chainIndex, merges, stonesCleared);
    }

    /** Flood fill: every connected set of 2+ identical mergeable fruits. */
    private static List<List<int[]>> findGroups(Grid grid) {
        boolean[][] seen = new boolean[Grid.ROWS][Grid.COLS];
        List<List<int[]>> groups = new ArrayList<>();

        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                Fruit fruit = grid.get(r, c);
                if (fruit == null || !fruit.isMergeable() || seen[r][c]) {
                    continue;
                }
                List<int[]> group = new ArrayList<>();
                Deque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[] {r, c});
                seen[r][c] = true;

                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    group.add(cell);
                    for (int[] d : DIRECTIONS) {
                        int nr = cell[0] + d[0];
                        int nc = cell[1] + d[1];
                        if (grid.inBounds(nr, nc) && !seen[nr][nc] && grid.get(nr, nc) == fruit) {
                            seen[nr][nc] = true;
                            queue.add(new int[] {nr, nc});
                        }
                    }
                }
                if (group.size() >= 2) {
                    groups.add(group);
                }
            }
        }
        return groups;
    }
}