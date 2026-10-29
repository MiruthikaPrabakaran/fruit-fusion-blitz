package com.netvanguard;

import com.netvanguard.core.SoloDifficulty;
import com.netvanguard.net.Peer;
import com.netvanguard.net.RelayListener;
import com.netvanguard.ui.HostLobbyScreen;
import com.netvanguard.ui.JoinLobbyScreen;
import com.netvanguard.ui.MenuScreen;
import com.netvanguard.ui.LeaderboardScreen;
import com.netvanguard.ui.SoloModeScreen;
import com.netvanguard.ui.Navigator;
import com.netvanguard.ui.GameScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** The application window. Owns the single Scene and switches which screen is shown. */
public class App extends Application implements Navigator {
    private Stage stage;
    private Scene scene;
    private Peer currentPeer; // the open network connection, if any
    private double baseWidth;  // window size for the menu, lobbies and solo
    private double baseHeight;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        scene = new Scene(new MenuScreen(this), 520, 720);
        stage.setTitle("NetVanguard: Fruit Fusion Blitz");
        stage.setScene(scene);
        stage.show();
        baseWidth = stage.getWidth();
        baseHeight = stage.getHeight();
    }

    /** The two-player screen is wider (it shows the opponent's board), so the window is resized for it. */
    private void restoreBaseSize() {
        if (baseWidth > 0) {
            stage.setWidth(baseWidth);
            stage.setHeight(baseHeight);
        }
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
        restoreBaseSize();
        scene.setRoot(new MenuScreen(this));
    }

    @Override
    public void startSolo() {
        restoreBaseSize();
        scene.setRoot(new SoloModeScreen(this));
    }

    @Override
    public void startSolo(SoloDifficulty difficulty) {
        restoreBaseSize();
        scene.setRoot(new GameScreen("SOLO · " + difficulty.label().toUpperCase(),
                null, null, this, difficulty));
    }

    @Override
    public void showHostLobby() {
        restoreBaseSize();
        scene.setRoot(new HostLobbyScreen(this));
    }

    @Override
    public void showJoinLobby() {
        restoreBaseSize();
        scene.setRoot(new JoinLobbyScreen(this));
    }

    @Override
    public void showLeaderboard() {
        restoreBaseSize();
        scene.setRoot(new LeaderboardScreen(this));
    }

    @Override
    public void onConnected(Peer peer, RelayListener relay, boolean isHost) {
        currentPeer = peer;
        scene.setRoot(new GameScreen(isHost ? "HOST" : "CLIENT", peer, relay, this));
        stage.sizeToScene(); // wider window: this screen also shows the opponent's board
    }

    public static void main(String[] args) {
        launch(args);
    }
}
