package com.netvanguard;

import com.netvanguard.net.Peer;
import com.netvanguard.net.RelayListener;
import com.netvanguard.ui.HostLobbyScreen;
import com.netvanguard.ui.JoinLobbyScreen;
import com.netvanguard.ui.MenuScreen;
import com.netvanguard.ui.Navigator;
import com.netvanguard.ui.PlaceholderGameScreen;
import com.netvanguard.ui.GameScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** The application window. Owns the single Scene and switches which screen is shown. */
public class App extends Application implements Navigator {
    private Scene scene;
    private Peer currentPeer; // the open network connection, if any

    @Override
    public void start(Stage stage) {
        scene = new Scene(new MenuScreen(this), 520, 720);
        stage.setTitle("NetVanguard: Fruit Fusion Blitz");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        closeCurrentPeer(); // closing the window also closes the connection
    }

    private void closeCurrentPeer() {
        if (currentPeer != null) {
            currentPeer.close();
            currentPeer = null;
        }
    }

    @Override
    public void showMenu() {
        closeCurrentPeer();
        scene.setRoot(new MenuScreen(this));
    }

    @Override
    public void startSolo() {
        scene.setRoot(new GameScreen("SOLO", null, null, this));
    }

    @Override
    public void showHostLobby() {
        scene.setRoot(new HostLobbyScreen(this));
    }

    @Override
    public void showJoinLobby() {
        scene.setRoot(new JoinLobbyScreen(this));
    }

    @Override
    public void onConnected(Peer peer, RelayListener relay, boolean isHost) {
        currentPeer = peer;
        // TODO: replace with the real GameScreen once the board is ready
        scene.setRoot(new PlaceholderGameScreen(isHost ? "HOST" : "CLIENT", peer, relay, this));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
