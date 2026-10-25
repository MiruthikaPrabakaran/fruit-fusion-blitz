package com.netvanguard.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScoreStoreTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 25);

    private Path dir;

    @BeforeEach
    void setUp() throws IOException {
        dir = Files.createTempDirectory("ffb-scores");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void savedScoresComeBackIdentical() throws IOException {
        ScoreBoard board = new ScoreBoard();
        board.add(250, DAY);
        board.add(900, DAY.plusDays(1));
        board.add(40, DAY.plusDays(2));

        Path file = dir.resolve("scores.txt");
        ScoreStore.save(file, board);

        assertEquals(board.getEntries(), ScoreStore.load(file).getEntries());
    }

    @Test
    void aMissingFileGivesAnEmptyBoard() {
        assertTrue(ScoreStore.load(dir.resolve("nope.txt")).getEntries().isEmpty());
    }

    @Test
    void badLinesAreSkipped() throws IOException {
        Path file = dir.resolve("scores.txt");
        Files.write(file, List.of("abc", "100,2026-10-25", "50,not-a-date", "", "70,2026-10-24", "1,2,3"));

        List<ScoreBoard.Entry> entries = ScoreStore.load(file).getEntries();
        assertEquals(2, entries.size());
        assertEquals(100, entries.get(0).score());
        assertEquals(70, entries.get(1).score());
    }

    @Test
    void saveCreatesMissingFolders() throws IOException {
        Path file = dir.resolve("sub").resolve("deeper").resolve("scores.txt");
        ScoreBoard board = new ScoreBoard();
        board.add(10, DAY);
        ScoreStore.save(file, board);
        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void aHandEditedUnsortedFileIsSortedOnLoad() throws IOException {
        Path file = dir.resolve("scores.txt");
        Files.write(file, List.of("10,2026-10-25", "300,2026-10-25", "20,2026-10-25"));
        assertEquals(300, ScoreStore.load(file).getEntries().get(0).score());
    }
}