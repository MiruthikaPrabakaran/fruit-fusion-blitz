package com.netvanguard.core;

/**
 * Scoring and garbage rules. All the numbers live here so they are easy to tune.
 *
 * Points, per chain step:
 *   (tier * 10 * group size, summed over groups  +  20 per stone destroyed) * chainIndex
 *
 * Garbage (stones sent to the opponent):
 *   - every step with chainIndex >= 2 sends chainIndex stones (chain of 3 = 2 + 3 = 5)
 *   - every merged group made of APPLE or higher sends 2 extra stones
 *   - a plain single merge sends nothing
 */
public final class Scoring {

    public static final int POINTS_PER_TIER = 10;
    public static final int POINTS_PER_STONE = 20;
    public static final int BIG_MERGE_TIER = Fruit.APPLE.tier();
    public static final int BIG_MERGE_GARBAGE = 2;

    private Scoring() {}

    public static int points(MergeResult result) {
        int total = 0;
        for (MergeResult.Step step : result.steps()) {
            int base = step.stonesCleared() * POINTS_PER_STONE;
            for (MergeResult.Merge merge : step.merges()) {
                base += merge.from().tier() * POINTS_PER_TIER * merge.size();
            }
            total += base * step.chainIndex();
        }
        return total;
    }

    public static int garbageFor(MergeResult result) {
        int stones = 0;
        for (MergeResult.Step step : result.steps()) {
            if (step.chainIndex() >= 2) {
                stones += step.chainIndex();
            }
            for (MergeResult.Merge merge : step.merges()) {
                if (merge.from().tier() >= BIG_MERGE_TIER) {
                    stones += BIG_MERGE_GARBAGE;
                }
            }
        }
        return stones;
    }
}