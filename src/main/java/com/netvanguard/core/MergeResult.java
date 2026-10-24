package com.netvanguard.core;

import java.util.List;

/** Everything that happened while resolving merges after one item landed. */
public record MergeResult(List<Step> steps) {

    /** One merged group: the fruit it was made of and how many items it contained. */
    public record Merge(Fruit from, int size) {}

    /**
     * One round of a chain: all groups that merged at the same time, plus the
     * stones that were destroyed because they touched a merging group.
     * chainIndex starts at 1.
     */
    public record Step(int chainIndex, List<Merge> merges, int stonesCleared) {}

    public static MergeResult empty() {
        return new MergeResult(List.of());
    }

    /** Number of rounds. 0 = nothing merged, 1 = single merge, 2+ = a combo. */
    public int chainLength() {
        return steps.size();
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }
}