package com.netvanguard.ui;

import com.netvanguard.core.FallingItem;
import com.netvanguard.core.Fruit;
import com.netvanguard.core.Game;
import com.netvanguard.core.GameEvents;
import com.netvanguard.core.Grid;
import com.netvanguard.net.MatchState;
import com.netvanguard.net.NetListener;
import com.netvanguard.net.Peer;
import com.netvanguard.net.Protocol;
import com.netvanguard.net.RelayListener;
import com.netvanguard.core.ScoreBoard;
import com.netvanguard.core.ScoreStore;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * The real game screen. Same constructor as PlaceholderGameScreen so App can swap one for the other.
 *
 * Solo: peer and relay are null, nothing is sent. Online: my earned garbage and my score are sent
 * to the opponent through the peer.
 *
 * Keys: Left/Right (or A/D) move, Down (or S) soft drop, Space hard drop, R restart (solo only).
 * The game loop runs only while this screen is on screen, and stops when the app switches away.
 */
public class GameScreen extends StackPane {

    private static final double CELL = 48;
    private static final double PREVIEW = 80;
    private static final Color BOARD_BG = Color.web("#1f2937");
    private static final double OPP_CELL = 24;          // size of one cell in the opponent's small board
    private static final int MAX_INCOMING_GARBAGE = 30; // ignore absurd GARBAGE values

    private final boolean online;

    // Two-player state (unused in solo)
    private final Peer peer;
    private final MatchState match = new MatchState();
    private Grid opponentGrid = new Grid();
    private int opponentScore;
    private String lastSentState;
    private long lastStateNanos;
    private int sentCount;
    private long sentFlashUntilNanos;
    private final Canvas opponentBoard = new Canvas(Grid.COLS * OPP_CELL, Grid.ROWS * OPP_CELL);
    private final Label opponentScoreLabel = label("Score: 0", 16);
    private final Label sentLabel = label("", 16);
    private final Label resultTitle = label("GAME OVER", 44);
    private final Label hintLabel = label("", 18);
    private final Label controlsLabel = label("", 12);

    private final GameEvents events;
    private Game game;
    private long lastTickNanos;

    // Scoreboard (solo only; scoreBoard is null online)
    private final ScoreBoard scoreBoard;
    private final Path scoreFile = ScoreStore.defaultPath();
    private boolean scoreRecorded;
    private final ScoreboardView scoreboardView = new ScoreboardView();
    private final Label rankLabel = label("", 18);
    private final Label bestLabel = label("", 16);

    private final Canvas board = new Canvas(Grid.COLS * CELL, Grid.ROWS * CELL);
    private final Canvas preview = new Canvas(PREVIEW, PREVIEW);
    private final Label statusLabel = label("", 14);
    private final Label scoreLabel = label("Score: 0", 18);
    private final Label chainLabel = label("", 18);
    private final Label garbageLabel = label("", 16);
    private final Label finalScoreLabel = label("", 22);
    private final VBox gameOverBox = new VBox(12);

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastTickNanos == 0) {
                lastTickNanos = now;
            }
            if (!isMatchOver() && now - lastTickNanos >= game.tickMillis() * 1_000_000L) {
                game.tick();
                lastTickNanos = now;
            }
            if (online) {
                maybeSendState(now);
            }
            render();
        }
    };

    /** Solo screen with no menu button (handy for quick tests). */
    public GameScreen() {
        this("SOLO", null, null, null);
    }

    /** peer and relay are null in solo mode. nav may be null (then there is no Back button). */
    public GameScreen(String mode, Peer peer, RelayListener relay, Navigator nav) {
        this.peer = peer;
        online = peer != null;
        scoreBoard = online ? null : ScoreStore.load(scoreFile);

        // The game reports what happens; here we turn that into network messages.
        events = new GameEvents() {
            @Override
            public void onGarbageSent(int count) {
                if (peer != null) {
                    Sounds.play(Sounds.Effect.SEND);
                    peer.send(Protocol.garbage(count));
                    sentCount = count;
                    sentFlashUntilNanos = System.nanoTime() + 2_000_000_000L; // show "Sent n stones!" for 2 s
                }
            }

            @Override
            public void onScoreChanged(int score) {
                // the score only changes when something merged
                Sounds.play(game.getLastResult().chainLength() >= 2 ? Sounds.Effect.CHAIN : Sounds.Effect.MERGE);
                if (peer != null) {
                    peer.send(Protocol.score(score));
                }
            }

            @Override
            public void onGameOver() {
                Sounds.play(Sounds.Effect.LOSE);
                match.iLost();
                if (peer != null) {
                    peer.send(Protocol.gameOver()); // tells the opponent they won
                }
            }
        };
        game = new Game(events);

        setStyle(Theme.BACKGROUND);

        Label modeLabel = label("Mode: " + mode, 14);
        modeLabel.setTextFill(Color.web("#9ca3af"));
        statusLabel.setTextFill(Color.web("#9ca3af"));
        chainLabel.setTextFill(Color.web("#fbbf24"));
        garbageLabel.setTextFill(Color.web("#f87171"));
        controlsLabel.setText(controlsText());
        controlsLabel.setTextFill(Color.web("#9ca3af"));

        VBox side = new VBox(14, modeLabel, statusLabel, label("NEXT", 18), preview,
                scoreLabel, chainLabel, garbageLabel, controlsLabel);
        side.setPadding(new Insets(15));
        side.setPrefWidth(150);
        if (!online) {
            bestLabel.setTextFill(Color.web("#fbbf24"));
            updateBest();
            scoreboardView.show(scoreBoard.getEntries(), 0);
            side.getChildren().add(side.getChildren().indexOf(scoreLabel) + 1, bestLabel);
        }

        if (online) {
            sentLabel.setTextFill(Color.web("#34d399"));
            side.getChildren().add(side.getChildren().indexOf(garbageLabel) + 1, sentLabel);
        }

        if (nav != null) {
            Button back = Theme.secondaryButton("Back to menu");
            back.setFocusTraversable(false); // so Space (hard drop) can never press it
            back.setMinWidth(0);
            back.setPrefWidth(120);
            back.setMaxWidth(120);
            back.setOnAction(e -> nav.showMenu());
            side.getChildren().add(back);
        }

        BorderPane layout = new BorderPane();
        layout.setCenter(board);
        layout.setRight(side);
        if (online) {
            // The opponent's board, drawn small on the left from their STATE messages.
            Label opponentTitle = label("OPPONENT", 14);
            opponentTitle.setTextFill(Color.web("#9ca3af"));
            opponentScoreLabel.setTextFill(Color.web("#9ca3af"));
            VBox opponentBox = new VBox(10, opponentTitle, opponentBoard, opponentScoreLabel);
            opponentBox.setPadding(new Insets(15, 0, 15, 15));
            layout.setLeft(opponentBox);
        }
        BorderPane.setMargin(board, new Insets(15));

        resultTitle.setTextFill(Color.web("#f87171"));
        rankLabel.setTextFill(Color.web("#fbbf24"));
        hintLabel.setText(online ? "Press R for a rematch, or use Back to menu" : "Press R to play again");
        gameOverBox.getChildren().addAll(resultTitle, finalScoreLabel);
        if (!online) {
            gameOverBox.getChildren().addAll(rankLabel, scoreboardView);
        }
        gameOverBox.getChildren().add(hintLabel);
        if (nav != null) {
            // The dark overlay covers the side panel, so its Back button cannot be clicked once the game is over.
            // This one sits on the overlay itself.
            Button overlayBack = Theme.secondaryButton("Back to menu");
            overlayBack.setFocusTraversable(false); // so Space or R can never press it by accident
            overlayBack.setOnAction(e -> nav.showMenu());
            gameOverBox.getChildren().add(overlayBack);
        }
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        gameOverBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        gameOverBox.setVisible(false);

        getChildren().addAll(layout, gameOverBox);

        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);
        // Run the loop only while this screen is showing; stop it when the app switches screens.
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                timer.start();
                Platform.runLater(this::requestFocus);
            } else {
                timer.stop();
            }
        });

        if (online) {
            statusLabel.setText("Opponent connected");
            relay.setTarget(new NetListener() {
                @Override
                public void onConnected() {
                    // already connected
                }

                @Override
                public void onMessage(String line) {
                    Platform.runLater(() -> handleNetMessage(line));
                }

                @Override
                public void onDisconnected() {
                    Platform.runLater(() -> {
                        match.opponentLeft();
                        showStatus("Opponent disconnected");
                    });
                }

                @Override
                public void onError(String message) {
                    Platform.runLater(() -> showStatus("Network error: " + message));
                }
            });
        }
    }

    // ---------- Network ----------

    /** Runs on the JavaFX thread for every message from the opponent. */
    private void handleNetMessage(String line) {
        switch (Protocol.command(line)) {
            case Protocol.GARBAGE -> {
                // stones only land while the match is still running
                if (!isMatchOver()) {
                    game.receiveGarbage(Math.min(Protocol.intArgument(line), MAX_INCOMING_GARBAGE));
                    Sounds.play(Sounds.Effect.INCOMING);
                }
            }
            case Protocol.SCORE -> opponentScore = Protocol.intArgument(line);
            case Protocol.STATE -> {
                try {
                    opponentGrid = Grid.deserialize(Protocol.argument(line));
                } catch (IllegalArgumentException e) {
                    // damaged grid message: ignore it, the next one fixes the picture
                }
            }
            case Protocol.GAME_OVER -> {
                match.opponentLost();
                if (match.result() == MatchState.Result.WIN) {
                    Sounds.play(Sounds.Effect.WIN);
                }
            }
            case Protocol.REMATCH -> {
                match.opponentWantsRematch();
                startRematchIfBothReady();
            }
            default -> {
                // unknown message: ignore it
            }
        }
    }

    /** Sends my grid (with the falling item) to the opponent, at most 10 times a second and only if it changed. */
    private void maybeSendState(long now) {
        if (now - lastStateNanos < 100_000_000L) {
            return;
        }
        lastStateNanos = now;
        String state = snapshot();
        if (!state.equals(lastSentState)) {
            lastSentState = state;
            peer.send(Protocol.state(state));
        }
    }

    private String snapshot() {
        Grid copy = Grid.deserialize(game.getGrid().serialize());
        FallingItem item = game.getCurrent();
        if (item != null && copy.inBounds(item.getRow(), item.getCol())
                && copy.isEmpty(item.getRow(), item.getCol())) {
            copy.set(item.getRow(), item.getCol(), item.getFruit());
        }
        return copy.serialize();
    }

    /** True when this player can no longer play: my grid overflowed, or (online) the opponent's did or they left. */
    private boolean isMatchOver() {
        return online ? match.isOver() : game.isGameOver();
    }

    private void requestRematch() {
        if (match.requestRematch()) {
            peer.send(Protocol.rematch());
        }
        startRematchIfBothReady();
    }

    private void startRematchIfBothReady() {
        if (!match.bothWantRematch()) {
            return;
        }
        match.reset();
        game = new Game(events);
        opponentGrid = new Grid();
        opponentScore = 0;
        lastTickNanos = 0;
        lastSentState = null;
        statusLabel.setText("Opponent connected");
        statusLabel.setTextFill(Color.web("#9ca3af"));
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.web("#f87171"));
    }

    // ---------- Input ----------

    private String controlsText() {
        return "Left/Right: move\nDown: soft drop\nSpace: hard drop"
                + (online ? "" : "\nR: restart")
                + "\nM: sound " + (Sounds.isMuted() ? "off" : "on");
    }

    private void onKey(KeyEvent e) {
        if (e.getCode() == KeyCode.M) { // works any time, even after the game is over
            Sounds.toggleMuted();
            controlsLabel.setText(controlsText());
            e.consume();
            return;
        }
        if (online && isMatchOver()) {
            if (e.getCode() == KeyCode.R) {
                requestRematch();
                e.consume();
            }
            return; // no other keys do anything once the match is over
        }
        switch (e.getCode()) {
            case LEFT, A -> game.moveLeft();
            case RIGHT, D -> game.moveRight();
            case DOWN, S -> {
                game.softDrop();
                lastTickNanos = 0; // restart the tick timer so the item does not drop twice in a row
            }
            case SPACE -> game.hardDrop();
            case R -> restart();
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void restart() {
        if (online) {
            return; // restarting mid-match would put the two players out of sync
        }
        game = new Game(events);
        scoreRecorded = false;
        lastTickNanos = 0;
    }

    // ---------- Scoreboard ----------

    /** Called once when a solo game ends: adds the score, saves the file, updates the table. */
    private void recordScore() {
        scoreRecorded = true;
        int rank = scoreBoard.add(game.getScore(), LocalDate.now());
        if (rank > 0) {
            try {
                ScoreStore.save(scoreFile, scoreBoard);
            } catch (IOException e) {
                // could not save the file; the score still shows for this session
            }
        }
        rankLabel.setText(rank == 1 ? "New high score!"
                : rank > 0 ? "You made the top 10: #" + rank : "");
        scoreboardView.show(scoreBoard.getEntries(), rank);
        updateBest();
    }

    private void updateBest() {
        List<ScoreBoard.Entry> entries = scoreBoard.getEntries();
        bestLabel.setText("Best: " + (entries.isEmpty() ? 0 : entries.get(0).score()));
    }

    // ---------- Drawing ----------

    private void render() {
        drawBoard();
        drawPreview();

        scoreLabel.setText("Score: " + game.getScore());
        int chain = game.getLastResult().chainLength();
        chainLabel.setText(chain >= 2 ? "Chain x" + chain + "!" : "");
        int pending = game.getPendingGarbage();
        garbageLabel.setText(pending > 0 ? "Incoming: " + pending : "");

        if (online) {
            drawOpponent();
            opponentScoreLabel.setText("Score: " + opponentScore);
            sentLabel.setText(System.nanoTime() < sentFlashUntilNanos ? "Sent " + sentCount + " stones!" : "");
        }

        boolean over = isMatchOver();
        gameOverBox.setVisible(over);
        if (over) {
            if (online) {
                showMatchResult();
            } else {
                finalScoreLabel.setText("Final score: " + game.getScore());
                if (!scoreRecorded) {
                    recordScore();
                }
            }
        }
    }

    /** Fills the game-over overlay for a two-player match: win, lose or draw, both scores, and the rematch hint. */
    private void showMatchResult() {
        MatchState.Result result = match.result();
        boolean won = result == MatchState.Result.WIN || result == MatchState.Result.OPPONENT_LEFT;
        boolean draw = result == MatchState.Result.DRAW;
        resultTitle.setText(won ? "YOU WIN!" : draw ? "DRAW" : "YOU LOSE");
        resultTitle.setTextFill(Color.web(won ? "#34d399" : draw ? "#fbbf24" : "#f87171"));
        finalScoreLabel.setText("You: " + game.getScore() + "   Opponent: " + opponentScore);
        hintLabel.setText(match.rematchImpossible() ? "Opponent left. Use Back to menu."
                : match.iWantRematch() ? "Waiting for your opponent to accept the rematch..."
                : "Press R for a rematch, or use Back to menu");
    }

    private void drawOpponent() {
        GraphicsContext g = opponentBoard.getGraphicsContext2D();
        double w = opponentBoard.getWidth();
        double h = opponentBoard.getHeight();
        g.setFill(BOARD_BG);
        g.fillRect(0, 0, w, h);
        g.setFill(Color.web("#7f1d1d", 0.45)); // top row = danger zone
        g.fillRect(0, 0, w, OPP_CELL);
        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                Fruit f = opponentGrid.get(r, c);
                if (f != null) {
                    drawFruit(g, f, c * OPP_CELL, r * OPP_CELL, OPP_CELL, 1.0);
                }
            }
        }
    }

    private void drawBoard() {
        GraphicsContext g = board.getGraphicsContext2D();
        double w = board.getWidth();
        double h = board.getHeight();

        g.setFill(BOARD_BG);
        g.fillRect(0, 0, w, h);
        g.setFill(Color.web("#7f1d1d", 0.45)); // top row = danger zone
        g.fillRect(0, 0, w, CELL);

        g.setStroke(Color.web("#374151"));
        g.setLineWidth(1);
        for (int c = 1; c < Grid.COLS; c++) {
            g.strokeLine(c * CELL, 0, c * CELL, h);
        }
        for (int r = 1; r < Grid.ROWS; r++) {
            g.strokeLine(0, r * CELL, w, r * CELL);
        }

        Grid grid = game.getGrid();
        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                Fruit f = grid.get(r, c);
                if (f != null) {
                    drawFruit(g, f, c * CELL, r * CELL, CELL, 1.0);
                }
            }
        }

        FallingItem item = game.getCurrent();
        if (item != null) {
            int ghostRow = item.landingRow(grid);
            if (ghostRow != item.getRow()) {
                drawFruit(g, item.getFruit(), item.getCol() * CELL, ghostRow * CELL, CELL, 0.25);
            }
            drawFruit(g, item.getFruit(), item.getCol() * CELL, item.getRow() * CELL, CELL, 1.0);
        }
    }

    private void drawPreview() {
        GraphicsContext g = preview.getGraphicsContext2D();
        g.setFill(BOARD_BG);
        g.fillRect(0, 0, PREVIEW, PREVIEW);
        drawFruit(g, game.getNext(), 0, 0, PREVIEW, 1.0);
    }

    /** Draws the fruit's picture if there is one (see FruitSprites), otherwise a coloured shape. */
    private static void drawFruit(GraphicsContext g, Fruit fruit, double x, double y, double size, double alpha) {
        Color color = Color.web(fruit.colorHex());
        g.setGlobalAlpha(alpha);
        g.setLineWidth(2);

        Image sprite = FruitSprites.get(fruit);
        if (sprite != null) {
            // Bigger fruits are drawn bigger. Stones fill most of the cell.
            double diameter = fruit == Fruit.STONE ? size * 0.84 : size * (0.52 + 0.07 * fruit.tier());
            g.drawImage(sprite, x + (size - diameter) / 2, y + (size - diameter) / 2, diameter, diameter);
        } else if (fruit == Fruit.STONE) {
            double pad = size * 0.08;
            g.setFill(color);
            g.fillRoundRect(x + pad, y + pad, size - 2 * pad, size - 2 * pad, 12, 12);
            g.setStroke(color.darker());
            g.strokeRoundRect(x + pad, y + pad, size - 2 * pad, size - 2 * pad, 12, 12);
        } else {
            double cx = x + size / 2;
            double cy = y + size / 2;
            double radius = size * (0.26 + 0.035 * fruit.tier());
            g.setFill(color);
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setStroke(color.darker());
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g.setFill(Color.WHITE);
            g.setFont(Font.font("System", FontWeight.BOLD, size * 0.3));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(fruit.label().substring(0, 1), cx, cy);
        }
        g.setGlobalAlpha(1.0);
    }

    private static Label label(String text, double size) {
        Label l = new Label(text);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font("System", FontWeight.BOLD, size));
        l.setWrapText(true);
        return l;
    }
}