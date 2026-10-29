package com.netvanguard.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Main menu: Solo, Host game, Join game, Quit. */
public class MenuScreen extends VBox {

    public MenuScreen(Navigator nav) {
        super(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));
        setStyle(Theme.BACKGROUND);

        Label title = Theme.title("Fruit Fusion Blitz");
        Label subtitle = Theme.text("A little fruit chaos, a lot of satisfying merges.");

        Button solo = Theme.button("Solo");
        solo.setOnAction(e -> nav.startSolo());

        Button leaderboard = Theme.button("Leaderboard");
        leaderboard.setOnAction(e -> nav.showLeaderboard());

        Button host = Theme.button("Host game");
        host.setOnAction(e -> nav.showHostLobby());

        Button join = Theme.button("Join game");
        join.setOnAction(e -> nav.showJoinLobby());

        Button quit = Theme.secondaryButton("Quit");
        quit.setOnAction(e -> Platform.exit());

        getChildren().addAll(title, subtitle, solo, leaderboard, host, join, quit);
    }
}
