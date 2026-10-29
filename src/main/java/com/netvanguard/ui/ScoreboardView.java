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

/** Reusable top-ten table showing rank, player name, score, and date. */
public class ScoreboardView extends VBox {
    private static final Color NORMAL = Color.web("#60445a");
    private static final Color EMPTY = Color.web("#b49cab");
    private static final Color HIGHLIGHT = Color.web("#b45309");

    private final Label[] ranks = new Label[ScoreBoard.MAX_ENTRIES];
    private final Label[] names = new Label[ScoreBoard.MAX_ENTRIES];
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
        grid.setHgap(10);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);
        for (int i = 0; i < ScoreBoard.MAX_ENTRIES; i++) {
            ranks[i] = cell(28, Pos.CENTER_RIGHT);
            names[i] = cell(92, Pos.CENTER_LEFT);
            scores[i] = cell(58, Pos.CENTER_RIGHT);
            dates[i] = cell(84, Pos.CENTER_LEFT);
            grid.addRow(i, ranks[i], names[i], scores[i], dates[i]);
        }
        getChildren().addAll(title, grid);
        show(List.of(), 0);
    }

    public void show(List<ScoreBoard.Entry> entries, int highlightRank) {
        for (int i = 0; i < ScoreBoard.MAX_ENTRIES; i++) {
            boolean filled = i < entries.size();
            ranks[i].setText((i + 1) + ".");
            names[i].setText(filled ? entries.get(i).name() : "—");
            scores[i].setText(filled ? String.valueOf(entries.get(i).score()) : "-");
            dates[i].setText(filled ? entries.get(i).date().toString() : "");
            Color color = !filled ? EMPTY : (i + 1 == highlightRank ? HIGHLIGHT : NORMAL);
            ranks[i].setTextFill(color);
            names[i].setTextFill(color);
            scores[i].setTextFill(color);
            dates[i].setTextFill(color);
        }
    }

    private static Label cell(double minWidth, Pos alignment) {
        Label label = new Label();
        label.setMinWidth(minWidth);
        label.setAlignment(alignment);
        label.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        return label;
    }
}
