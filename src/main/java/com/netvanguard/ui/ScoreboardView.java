package com.netvanguard.ui;

import com.netvanguard.core.ScoreBoard;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A "TOP 10" table. Call show(...) with the entries to fill it in. Reusable: any screen can
 * add one, for example a Scoreboard screen opened from the menu.
 */
public class ScoreboardView extends VBox {

    private static final Color NORMAL = Color.web("#e5e7eb");
    private static final Color EMPTY = Color.web("#6b7280");
    private static final Color HIGHLIGHT = Color.web("#fbbf24");

    private final Label[] ranks = new Label[ScoreBoard.MAX_ENTRIES];
    private final Label[] scores = new Label[ScoreBoard.MAX_ENTRIES];
    private final Label[] dates = new Label[ScoreBoard.MAX_ENTRIES];

    public ScoreboardView() {
        super(6);
        setAlignment(Pos.CENTER);
        setMaxWidth(Region.USE_PREF_SIZE);

        Label title = new Label("TOP 10");
        title.setTextFill(NORMAL);
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);
        for (int i = 0; i < ScoreBoard.MAX_ENTRIES; i++) {
            ranks[i] = cell(30, Pos.CENTER_RIGHT);
            scores[i] = cell(70, Pos.CENTER_RIGHT);
            dates[i] = cell(100, Pos.CENTER_LEFT);
            grid.addRow(i, ranks[i], scores[i], dates[i]);
        }

        getChildren().addAll(title, grid);
        show(List.of(), 0);
    }

    /**
     * Fills the table.
     *
     * @param highlightRank the row to colour gold (1 = first row), or 0 for none
     */
    public void show(List<ScoreBoard.Entry> entries, int highlightRank) {
        for (int i = 0; i < ScoreBoard.MAX_ENTRIES; i++) {
            boolean filled = i < entries.size();
            ranks[i].setText((i + 1) + ".");
            scores[i].setText(filled ? String.valueOf(entries.get(i).score()) : "-");
            dates[i].setText(filled ? entries.get(i).date().toString() : "");
            Color color = !filled ? EMPTY : (i + 1 == highlightRank ? HIGHLIGHT : NORMAL);
            ranks[i].setTextFill(color);
            scores[i].setTextFill(color);
            dates[i].setTextFill(color);
        }
    }

    private static Label cell(double minWidth, Pos alignment) {
        Label l = new Label();
        l.setMinWidth(minWidth);
        l.setAlignment(alignment);
        l.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
        return l;
    }
}