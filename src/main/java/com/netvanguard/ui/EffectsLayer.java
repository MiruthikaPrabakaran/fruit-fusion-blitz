package com.netvanguard.ui;

import com.netvanguard.core.Game;
import java.util.function.Supplier;
import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * A see-through canvas that sits on top of the board and draws the juice: sparks, rings, floating text
 * and the warning bars. It also shakes the board canvas. It runs its own animation loop, which starts
 * when the screen appears and stops when the app switches away, so GameScreen needs no extra code.
 */
public class EffectsLayer extends Canvas {

    private final Supplier<Game> game;
    private final Canvas board;
    private final GameEffects model;
    private final double cell;
    private final AnimationTimer timer;
    private long lastNanos;

    public EffectsLayer(Supplier<Game> game, Canvas board, double cell) {
        super(board.getWidth(), board.getHeight());
        this.game = game;
        this.board = board;
        this.cell = cell;
        this.model = new GameEffects(cell);
        setMouseTransparent(true);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos == 0) {
                    lastNanos = now;
                }
                double dt = Math.min(0.05, (now - lastNanos) / 1e9);
                lastNanos = now;
                model.update(EffectsLayer.this.game.get(), dt);
                shakeBoard(model.shakeX(), model.shakeY());
                draw();
            }
        };

        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                lastNanos = 0;
                timer.start();
            } else {
                timer.stop();
                shakeBoard(0, 0);
            }
        });
    }

    private void shakeBoard(double dx, double dy) {
        board.setTranslateX(dx);
        board.setTranslateY(dy);
        setTranslateX(dx);
        setTranslateY(dy);
    }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth();
        g.clearRect(0, 0, w, getHeight());

        for (GameEffects.Ring r : model.rings()) {
            double p = r.progress();
            double radius = cell * (0.3 + 0.8 * p);
            g.setGlobalAlpha(1 - p);
            g.setStroke(Color.web(r.color));
            g.setLineWidth(4 * (1 - p) + 1);
            g.strokeOval(r.x - radius, r.y - radius, radius * 2, radius * 2);
        }

        for (GameEffects.Particle p : model.particles()) {
            g.setGlobalAlpha(p.alpha());
            g.setFill(Color.web(p.color));
            g.fillOval(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size);
        }
        g.setGlobalAlpha(1.0);

        // Warning bars along the top: red pulse when the stack is high, orange bar for incoming stones.
        double barHeight = cell * 0.3;
        if (model.dangerLevel() > 0) {
            double alpha = Math.min(0.9, model.dangerLevel() * (0.35 + 0.45 * model.pulse()));
            g.setFill(Color.rgb(239, 68, 68, alpha));
            g.fillRect(0, 0, w, barHeight);
        }
        if (model.incomingFraction() > 0) {
            g.setFill(Color.rgb(0, 0, 0, 0.45));
            g.fillRect(0, barHeight, w, 8);
            g.setFill(Color.rgb(251, 146, 60, 0.95));
            g.fillRect(0, barHeight, w * model.incomingFraction(), 8);
        }

        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        for (GameEffects.FloatingText t : model.texts()) {
            g.setGlobalAlpha(t.alpha());
            g.setFont(Font.font("System", FontWeight.BOLD, t.fontSize));
            g.setStroke(Color.rgb(0, 0, 0, 0.7));
            g.setLineWidth(3);
            g.strokeText(t.text, t.x, t.y);
            g.setFill(Color.web(t.color));
            g.fillText(t.text, t.x, t.y);
        }
        g.setGlobalAlpha(1.0);
    }
}