package com.netvanguard.ui;

import com.netvanguard.net.Host;
import com.netvanguard.net.NetListener;
import com.netvanguard.net.NetUtil;
import com.netvanguard.net.Protocol;
import com.netvanguard.net.RelayListener;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Shown to the player who hosts: displays the IP address to share and waits for the opponent. */
public class HostLobbyScreen extends VBox {
    private final Navigator nav;
    private final RelayListener relay = new RelayListener();
    private final Label status = Theme.text("Starting...");
    private Host host;

    public HostLobbyScreen(Navigator nav) {
        super(14);
        this.nav = nav;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));
        setStyle(Theme.BACKGROUND);

        List<String> addresses = NetUtil.localIPv4Addresses();
        Label ipLabel = Theme.title(addresses.isEmpty() ? "No network found" : addresses.get(0));
        Label hint = Theme.text(addresses.isEmpty()
                ? "Connect to Wi-Fi or a phone hotspot, then go back and try again."
                : "Tell your opponent to choose Join game and type this IP address.  Port: "
                + Protocol.DEFAULT_PORT);
        Label others = Theme.text(addresses.size() > 1
                ? "Other addresses if the first one does not work: "
                + String.join(", ", addresses.subList(1, addresses.size()))
                : "");

        Button cancel = Theme.secondaryButton("Cancel");
        cancel.setOnAction(e -> {
            if (host != null) {
                host.close();
            }
            nav.showMenu();
        });

        getChildren().addAll(Theme.title("Host a game"), ipLabel, hint, others, status, cancel);
        startHosting();
    }

    private void startHosting() {
        host = new Host(Protocol.DEFAULT_PORT, relay);
        relay.setTarget(new NetListener() {
            @Override
            public void onConnected() {
                relay.setTarget(null); // hold messages until the game screen takes over
                Platform.runLater(() -> nav.onConnected(host, relay, true));
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
                Platform.runLater(() -> status.setText("Error: " + message));
            }
        });

        try {
            host.start();
            status.setText("Waiting for your opponent to join...");
        } catch (IOException e) {
            status.setText("Could not start hosting: " + e.getMessage()
                    + "\nIs another copy of the game already hosting?");
        }
    }
}
