package com.netvanguard.ui;

import com.netvanguard.net.NetListener;
import com.netvanguard.net.Peer;
import com.netvanguard.net.Protocol;
import com.netvanguard.net.RelayListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Stand-in for the real game screen until the board is ready. It proves the menu and the
 * network work end to end: both players can press the test buttons and see each other's messages.
 *
 * To replace it: in App, create the real GameScreen instead of this class.
 */
public class PlaceholderGameScreen extends VBox {
    private final TextArea log = new TextArea();

    /** peer and relay are null in solo mode. */
    public PlaceholderGameScreen(String mode, Peer peer, RelayListener relay, Navigator nav) {
        super(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));
        setStyle(Theme.BACKGROUND);

        log.setEditable(false);
        log.setWrapText(true);
        log.setPrefRowCount(12);

        Button sendGarbage = Theme.button("Send GARBAGE 3");
        Button sendScore = Theme.button("Send SCORE 100");
        Button back = Theme.secondaryButton("Back to menu");

        boolean online = peer != null;
        sendGarbage.setDisable(!online);
        sendScore.setDisable(!online);

        if (online) {
            sendGarbage.setOnAction(e -> {
                peer.send(Protocol.garbage(3));
                append("sent: GARBAGE 3");
            });
            sendScore.setOnAction(e -> {
                peer.send(Protocol.score(100));
                append("sent: SCORE 100");
            });
        }
        back.setOnAction(e -> nav.showMenu());

        getChildren().addAll(
                Theme.title("Game screen (placeholder)"),
                Theme.text("Mode: " + mode + ". The real board will replace this screen."),
                log, sendGarbage, sendScore, back);

        if (online) {
            append("Connected! You are the " + mode + ".");
            relay.setTarget(new NetListener() {
                @Override
                public void onConnected() {
                    // already connected
                }

                @Override
                public void onMessage(String line) {
                    Platform.runLater(() -> append("received: " + line));
                }

                @Override
                public void onDisconnected() {
                    Platform.runLater(() -> append("Opponent disconnected."));
                }

                @Override
                public void onError(String message) {
                    Platform.runLater(() -> append("error: " + message));
                }
            });
        } else {
            append("Solo mode: no network needed.");
        }
    }

    private void append(String line) {
        log.appendText(line + "\n");
    }
}
