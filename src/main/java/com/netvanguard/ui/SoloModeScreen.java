package com.netvanguard.ui;

import com.netvanguard.core.SoloDifficulty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Difficulty picker shown only before starting solo mode. */
public class SoloModeScreen extends VBox {
    public SoloModeScreen(Navigator nav) {
        super(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        setStyle(Theme.BACKGROUND);

        Label title = Theme.title("Choose your challenge");
        Label subtitle = Theme.text("Same fruits. Different speed. Watch out for stones!");

        Button easy = Theme.button("🌷  Easy");
        Button medium = Theme.button("🍓  Medium");
        Button hard = Theme.button("🔥  Hard");
        easy.setOnAction(e -> nav.startSolo(SoloDifficulty.EASY));
        medium.setOnAction(e -> nav.startSolo(SoloDifficulty.MEDIUM));
        hard.setOnAction(e -> nav.startSolo(SoloDifficulty.HARD));

        Label easyInfo = Theme.text("Gentler pace • fewer stones");
        Label mediumInfo = Theme.text("Quicker fruits • more stones");
        Label hardInfo = Theme.text("Fast fruits • frequent stone waves");
        Button back = Theme.secondaryButton("Back to menu");
        back.setOnAction(e -> nav.showMenu());

        getChildren().addAll(title, subtitle, easy, easyInfo, medium, mediumInfo, hard, hardInfo, back);
    }
}
