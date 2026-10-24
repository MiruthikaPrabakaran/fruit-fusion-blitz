package com.netvanguard;

import com.netvanguard.core.Fruit;
import com.netvanguard.core.Grid;
import com.netvanguard.ui.BoardCanvas;
import com.netvanguard.ui.GameScreen;
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
        stage.setScene(new Scene(new GameScreen()));
        stage.setTitle("NetVanguard: Fruit Fusion Blitz");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
