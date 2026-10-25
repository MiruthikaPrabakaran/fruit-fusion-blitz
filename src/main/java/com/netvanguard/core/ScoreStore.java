package com.netvanguard.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads a ScoreBoard as a plain text file, one "score,date" line per entry, for example
 * 1240,2026-10-25. The file lives in the user's home folder, so it stays out of the Git repo.
 */
public final class ScoreStore {

    private ScoreStore() {}

    /** C:\Users\you\.fruit-fusion-blitz\scores.txt on Windows. */
    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".fruit-fusion-blitz", "scores.txt");
    }

    /** Loads the board. A missing or unreadable file gives an empty board; bad lines are skipped. */
    public static ScoreBoard load(Path file) {
        ScoreBoard board = new ScoreBoard();
        if (!Files.isRegularFile(file)) {
            return board;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return board;
        }
        for (String line : lines) {
            String[] parts = line.trim().split(",");
            if (parts.length != 2) {
                continue;
            }
            try {
                board.add(Integer.parseInt(parts[0].trim()), LocalDate.parse(parts[1].trim()));
            } catch (NumberFormatException | DateTimeParseException e) {
                // skip the bad line
            }
        }
        return board;
    }

    /** Writes the board, creating the folder if needed. */
    public static void save(Path file, ScoreBoard board) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> lines = new ArrayList<>();
        for (ScoreBoard.Entry entry : board.getEntries()) {
            lines.add(entry.score() + "," + entry.date());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}