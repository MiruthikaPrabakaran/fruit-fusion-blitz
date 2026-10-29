package com.netvanguard.ui;

import com.netvanguard.core.ScoreBoard;
import com.netvanguard.core.ScoreStore;
import com.netvanguard.core.SoloDifficulty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Browsable local leaderboard for each solo difficulty. */
public class LeaderboardScreen extends VBox {
    private final ScoreboardView table = new ScoreboardView();

    public LeaderboardScreen(Navigator nav) {
        super(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(28));
        setStyle(Theme.BACKGROUND);

        Label title = Theme.title("Fruit Hall of Fame");
        Label subtitle = Theme.text("The top 10 runs for each solo difficulty.");

        ComboBox<SoloDifficulty> picker = new ComboBox<>();
        picker.getItems().setAll(SoloDifficulty.values());
        picker.setValue(SoloDifficulty.MEDIUM);
        picker.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(SoloDifficulty item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        picker.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(SoloDifficulty item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        picker.setStyle("-fx-background-color: #fffafc; -fx-border-color: #e8c8da;"
                + " -fx-border-radius: 12; -fx-background-radius: 12;");

        Runnable refresh = () -> {
            ScoreBoard board = ScoreStore.load(ScoreStore.defaultPath(picker.getValue()));
            table.show(board.getEntries(), 0);
        };
        picker.setOnAction(e -> refresh.run());
        refresh.run();

        getChildren().addAll(title, subtitle, picker, table, Theme.secondaryButton("Back to menu"));
        javafx.scene.control.Button back = (javafx.scene.control.Button) getChildren().get(getChildren().size() - 1);
        back.setOnAction(e -> nav.showMenu());
    }
}
