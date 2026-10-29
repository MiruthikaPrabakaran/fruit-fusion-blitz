package com.netvanguard.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class ScoreStore {
    private ScoreStore() {}

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".fruit-fusion-blitz", "scores.txt");
    }

    /** Separate rankings keep scores fair across difficulty levels. */
    public static Path defaultPath(SoloDifficulty difficulty) {
        // Keep the existing scoreboard as Medium so current players retain their saved scores.
        String fileName = difficulty == SoloDifficulty.MEDIUM
                ? "scores.txt" : "scores-" + difficulty.name().toLowerCase() + ".txt";
        return Path.of(System.getProperty("user.home"), ".fruit-fusion-blitz", fileName);
    }

    /** Loads both the old score,date format and the current name,score,date format. */
    public static ScoreBoard load(Path file) {
        ScoreBoard board = new ScoreBoard();
        if (!Files.isRegularFile(file)) return board;
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return board;
        }
        for (String line : lines) {
            String[] parts = line.trim().split(",", 3);
            try {
                if (parts.length == 2) {
                    board.add(Integer.parseInt(parts[0].trim()), LocalDate.parse(parts[1].trim()));
                } else if (parts.length == 3) {
                    board.add(parts[0].trim(), Integer.parseInt(parts[1].trim()),
                            LocalDate.parse(parts[2].trim()));
                }
            } catch (NumberFormatException | DateTimeParseException e) {
                // Ignore malformed rows instead of preventing the game from starting.
            }
        }
        return board;
    }

    public static void save(Path file, ScoreBoard board) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        List<String> lines = new ArrayList<>();
        for (ScoreBoard.Entry entry : board.getEntries()) {
            String safeName = entry.name().replace(",", " ").replace("\n", " ");
            lines.add(safeName + "," + entry.score() + "," + entry.date());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
