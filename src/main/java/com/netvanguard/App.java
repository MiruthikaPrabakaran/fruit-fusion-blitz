package com.netvanguard;

import com.netvanguard.core.Fruit;
import com.netvanguard.core.Grid;
import com.netvanguard.ui.BoardCanvas;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Demo grid so you can see the core and ui packages are wired together.
        // Replace this with the real game once Game/GameLoop exists.
        Grid demo = new Grid();
        demo.set(9, 0, Fruit.CHERRY);
        demo.set(9, 1, Fruit.CHERRY);
        demo.set(9, 2, Fruit.STRAWBERRY);
        demo.set(9, 3, Fruit.GRAPE);
        demo.set(8, 3, Fruit.ORANGE);
        demo.set(9, 5, Fruit.STONE);

        BoardCanvas board = new BoardCanvas(70);
        board.draw(demo);

        Label title = new Label("NetVanguard: Fruit Fusion Blitz");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        VBox root = new VBox(16, title, board);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #111827;");

        stage.setTitle("NetVanguard: Fruit Fusion Blitz");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
