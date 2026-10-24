package com.netvanguard.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Shared look for the menu and lobby screens, so every screen matches. */
public final class Theme {
    public static final String BACKGROUND = "-fx-background-color: #111827;";

    private Theme() {
    }

    public static Label title(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        label.setWrapText(true);
        return label;
    }

    public static Label text(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 15px;");
        label.setWrapText(true);
        return label;
    }

    public static Button button(String text) {
        Button button = new Button(text);
        button.setPrefWidth(260);
        button.setStyle("-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 16px;"
                + " -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
        return button;
    }

    public static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(260);
        button.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-size: 15px;"
                + " -fx-background-radius: 10; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
        return button;
    }
}
