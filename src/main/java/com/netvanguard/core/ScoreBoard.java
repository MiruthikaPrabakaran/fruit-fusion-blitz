package com.netvanguard.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Top ten scores, highest first. Pure logic: no files and no JavaFX. */
public class ScoreBoard {
    public static final int MAX_ENTRIES = 10;

    public record Entry(String name, int score, LocalDate date) {
        public Entry {
            name = name == null || name.isBlank() ? "Player" : name.trim();
            if (name.length() > 18) name = name.substring(0, 18);
        }
        /** Backwards-compatible constructor for older callers and saved-score tests. */
        public Entry(int score, LocalDate date) { this("Player", score, date); }
    }

    private final List<Entry> entries = new ArrayList<>();

    public List<Entry> getEntries() { return List.copyOf(entries); }

    public boolean qualifies(int score) {
        if (score <= 0) return false;
        return entries.size() < MAX_ENTRIES || score > entries.get(entries.size() - 1).score();
    }

    public int add(int score, LocalDate date) { return add("Player", score, date); }

    /** Adds a named score if it qualifies; returns rank (1 = best), or 0 if outside top ten. */
    public int add(String name, int score, LocalDate date) {
        if (!qualifies(score)) return 0;
        int index = 0;
        while (index < entries.size() && entries.get(index).score() >= score) index++;
        entries.add(index, new Entry(name, score, date));
        if (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        return index + 1;
    }
}
