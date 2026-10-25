package com.netvanguard.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The top ten scores, highest first. Pure logic: no files and no JavaFX (see ScoreStore for saving).
 *
 * A score of 0 is never recorded. If a new score ties an older one, the older one stays above it.
 */
public class ScoreBoard {

    public static final int MAX_ENTRIES = 10;

    public record Entry(int score, LocalDate date) {}

    private final List<Entry> entries = new ArrayList<>();

    /** A copy of the current entries, best score first. */
    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    /** True if this score would make it onto the board. */
    public boolean qualifies(int score) {
        if (score <= 0) {
            return false;
        }
        return entries.size() < MAX_ENTRIES || score > entries.get(entries.size() - 1).score();
    }

    /**
     * Adds the score if it qualifies.
     *
     * @return its rank (1 = best), or 0 if it did not make the top ten
     */
    public int add(int score, LocalDate date) {
        if (!qualifies(score)) {
            return 0;
        }
        int index = 0;
        while (index < entries.size() && entries.get(index).score() >= score) {
            index++;
        }
        entries.add(index, new Entry(score, date));
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
        return index + 1;
    }
}