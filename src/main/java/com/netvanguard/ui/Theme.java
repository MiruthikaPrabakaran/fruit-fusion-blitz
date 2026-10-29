package com.netvanguard.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Shared pastel storybook styling for menus, lobbies, and game controls. */
public final class Theme {
    public static final String BACKGROUND =
            "-fx-background-color: linear-gradient(to bottom right, #fff8f3, #fce7f3, #f3e8ff);";

    private Theme() {}

    public static Label title(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #713f5a; -fx-font-size: 30px; -fx-font-weight: bold;");
        label.setWrapText(true);
        return label;
    }

    public static Label text(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #765b70; -fx-font-size: 15px;");
        label.setWrapText(true);
        return label;
    }

    public static Button button(String text) {
        Button button = new Button(text);
        button.setPrefWidth(260);
        button.setStyle("-fx-background-color: #e9a6c5; -fx-text-fill: #5b2944; -fx-font-size: 16px;"
                + " -fx-font-weight: bold; -fx-background-radius: 18; -fx-padding: 11 20 11 20;"
                + " -fx-cursor: hand; -fx-effect: dropshadow(gaussian, #d8a7c6, 10, 0.12, 0, 3);");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #f4bdd5; -fx-text-fill: #5b2944;"
                + " -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 18;"
                + " -fx-padding: 11 20 11 20; -fx-cursor: hand;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #e9a6c5; -fx-text-fill: #5b2944;"
                + " -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 18;"
                + " -fx-padding: 11 20 11 20; -fx-cursor: hand;"
                + " -fx-effect: dropshadow(gaussian, #d8a7c6, 10, 0.12, 0, 3);"));
        return button;
    }

    public static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(260);
        button.setStyle("-fx-background-color: #fffafc; -fx-text-fill: #765b70; -fx-font-size: 15px;"
                + " -fx-background-radius: 16; -fx-border-color: #e8c8da; -fx-border-radius: 16;"
                + " -fx-padding: 9 20 9 20; -fx-cursor: hand;");
        return button;
    }
}
