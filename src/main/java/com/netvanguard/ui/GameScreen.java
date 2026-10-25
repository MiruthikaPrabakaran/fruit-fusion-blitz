package com.netvanguard.ui;

import com.netvanguard.core.FallingItem;
import com.netvanguard.core.Fruit;
import com.netvanguard.core.Game;
import com.netvanguard.core.GameEvents;
import com.netvanguard.core.Grid;
import com.netvanguard.net.NetListener;
import com.netvanguard.net.Peer;
import com.netvanguard.net.Protocol;
import com.netvanguard.net.RelayListener;
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

    private final boolean online;
    private final GameEvents events;
    private Game game;
    private long lastTickNanos;

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
            if (!game.isGameOver() && now - lastTickNanos >= game.tickMillis() * 1_000_000L) {
                game.tick();
                lastTickNanos = now;
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
        online = peer != null;

        // The game reports what happens; here we turn that into network messages.
        events = new GameEvents() {
            @Override
            public void onGarbageSent(int count) {
                if (peer != null) {
                    peer.send(Protocol.garbage(count));
                }
            }

            @Override
            public void onScoreChanged(int score) {
                if (peer != null) {
                    peer.send(Protocol.score(score));
                }
            }

            @Override
            public void onGameOver() {
                // nothing to send yet
            }
        };
        game = new Game(events);

        setStyle(Theme.BACKGROUND);

        Label modeLabel = label("Mode: " + mode, 14);
        modeLabel.setTextFill(Color.web("#9ca3af"));
        statusLabel.setTextFill(Color.web("#9ca3af"));
        chainLabel.setTextFill(Color.web("#fbbf24"));
        garbageLabel.setTextFill(Color.web("#f87171"));
        Label controls = label("Left/Right: move\nDown: soft drop\nSpace: hard drop"
                + (online ? "" : "\nR: restart"), 12);
        controls.setTextFill(Color.web("#9ca3af"));

        VBox side = new VBox(14, modeLabel, statusLabel, label("NEXT", 18), preview,
                scoreLabel, chainLabel, garbageLabel, controls);
        side.setPadding(new Insets(15));
        side.setPrefWidth(150);

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
        BorderPane.setMargin(board, new Insets(15));

        Label gameOverTitle = label("GAME OVER", 44);
        gameOverTitle.setTextFill(Color.web("#f87171"));
        gameOverBox.getChildren().addAll(gameOverTitle, finalScoreLabel,
                label(online ? "Use Back to menu to leave" : "Press R to play again", 18));
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
                    Platform.runLater(() -> showStatus("Opponent disconnected"));
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
        // TODO (next step): parse the line with Protocol and call
        //   game.receiveGarbage(count)  for a GARBAGE message,
        //   and show the opponent's score / grid for the others.
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.web("#f87171"));
    }

    // ---------- Input ----------

    private void onKey(KeyEvent e) {
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
        lastTickNanos = 0;
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

        boolean over = game.isGameOver();
        gameOverBox.setVisible(over);
        if (over) {
            finalScoreLabel.setText("Final score: " + game.getScore());
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