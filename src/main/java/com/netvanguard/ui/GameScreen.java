package com.netvanguard.ui;

import com.netvanguard.core.FallingItem;
import com.netvanguard.core.Fruit;
import com.netvanguard.core.Game;
import com.netvanguard.core.Grid;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * The playable board: grid, ghost preview, next-item preview, score, chain/garbage info,
 * keyboard controls and the game-over overlay.
 *
 * Keys: Left/Right (or A/D) move, Down (or S) soft drop, Space hard drop, R restart.
 */
public class GameScreen extends StackPane {

    private static final double CELL = 56;
    private static final double PREVIEW = 90;
    private static final Color BOARD_BG = Color.web("#1f2937");

    private Game game = new Game();
    private long lastTickNanos;

    private final Canvas board = new Canvas(Grid.COLS * CELL, Grid.ROWS * CELL);
    private final Canvas preview = new Canvas(PREVIEW, PREVIEW);
    private final Label scoreLabel = label("Score: 0", 22);
    private final Label chainLabel = label("", 18);
    private final Label garbageLabel = label("", 18);
    private final Label finalScoreLabel = label("", 22);
    private final VBox gameOverBox = new VBox(12);

    public GameScreen() {
        setStyle("-fx-background-color: #111827;");

        chainLabel.setTextFill(Color.web("#fbbf24"));
        garbageLabel.setTextFill(Color.web("#f87171"));
        Label controls = label("Left/Right: move\nDown: soft drop\nSpace: hard drop\nR: restart", 14);
        controls.setTextFill(Color.web("#9ca3af"));

        VBox side = new VBox(14, label("NEXT", 18), preview, scoreLabel, chainLabel, garbageLabel, controls);
        side.setPadding(new Insets(20));
        side.setPrefWidth(210);

        BorderPane layout = new BorderPane();
        layout.setCenter(board);
        layout.setRight(side);
        BorderPane.setMargin(board, new Insets(20));

        Label gameOverTitle = label("GAME OVER", 44);
        gameOverTitle.setTextFill(Color.web("#f87171"));
        gameOverBox.getChildren().addAll(gameOverTitle, finalScoreLabel, label("Press R to play again", 18));
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        gameOverBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        gameOverBox.setVisible(false);

        getChildren().addAll(layout, gameOverBox);

        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                Platform.runLater(this::requestFocus);
            }
        });

        new AnimationTimer() {
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
        }.start();
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
        game = new Game();
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

    /** Bigger fruits are drawn bigger, so a merge visibly "grows". Stones are grey squares. */
    private static void drawFruit(GraphicsContext g, Fruit fruit, double x, double y, double size, double alpha) {
        Color color = Color.web(fruit.colorHex());
        g.setGlobalAlpha(alpha);
        g.setLineWidth(2);

        if (fruit == Fruit.STONE) {
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
        return l;
    }
}