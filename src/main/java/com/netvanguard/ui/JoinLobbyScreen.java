package com.netvanguard.ui;

import com.netvanguard.net.Client;
import com.netvanguard.net.NetListener;
import com.netvanguard.net.Protocol;
import com.netvanguard.net.RelayListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Shown to the player who joins: type the host's IP address and connect. */
public class JoinLobbyScreen extends VBox {
    private final Navigator nav;
    private final RelayListener relay = new RelayListener();
    private final TextField ipField = new TextField();
    private final Label status = Theme.text("Type the IP address shown on the host's screen.");
    private final Button connectButton = Theme.button("Connect");
    private Client client;

    public JoinLobbyScreen(Navigator nav) {
        super(14);
        this.nav = nav;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));
        setStyle(Theme.BACKGROUND);

        ipField.setPromptText("e.g. 192.168.1.5");
        ipField.setMaxWidth(260);
        ipField.setOnAction(e -> connect()); // pressing Enter also connects

        connectButton.setOnAction(e -> connect());

        Button back = Theme.secondaryButton("Back");
        back.setOnAction(e -> {
            if (client != null) {
                client.close();
            }
            nav.showMenu();
        });

        getChildren().addAll(Theme.title("Join a game"), ipField, connectButton, status, back);
    }

    private void connect() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            status.setText("Please type the host's IP address first.");
            return;
        }
        setInputsDisabled(true);
        status.setText("Connecting to " + ip + " ...");

        client = new Client(ip, Protocol.DEFAULT_PORT, relay);
        relay.setTarget(new NetListener() {
            @Override
            public void onConnected() {
                relay.setTarget(null); // hold messages until the game screen takes over
                Platform.runLater(() -> nav.onConnected(client, relay, false));
            }

            @Override
            public void onMessage(String line) {
                // nothing to do in the lobby
            }

            @Override
            public void onDisconnected() {
                // nothing to do in the lobby
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    status.setText(message);
                    setInputsDisabled(false);
                });
            }
        });
        client.connect();
    }

    private void setInputsDisabled(boolean disabled) {
        ipField.setDisable(disabled);
        connectButton.setDisable(disabled);
    }
}
