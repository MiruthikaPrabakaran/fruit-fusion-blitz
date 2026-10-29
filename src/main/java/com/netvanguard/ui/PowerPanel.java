package com.netvanguard.ui;

import com.netvanguard.core.Game;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * The POWERS box in the side panel: how many Burst and Swap charges you have, plus a short message
 * when you use one. GameScreen calls refresh() every frame and useBurst()/useSwap() on the B and N keys.
 */
public class PowerPanel extends VBox {

    private static final Color ON = Color.web("#60445a");
    private static final Color OFF = Color.web("#b49cab");
    private static final Color GOLD = Color.web("#b7791f");

    private final Supplier<Game> game;
    private final Label burstLabel = label("", 14);
    private final Label swapLabel = label("", 14);
    private final Label messageLabel = label("", 13);
    private final PauseTransition fade = new PauseTransition(Duration.seconds(1.5));

    /** game is a supplier because the screen replaces its Game when you restart. */
    public PowerPanel(Supplier<Game> game) {
        super(6);
        this.game = game;

        Label hint = label("B pops cherries and strawberries\nN changes the next fruit", 11);
        hint.setTextFill(OFF);
        messageLabel.setTextFill(GOLD);
        fade.setOnFinished(e -> messageLabel.setText(""));

        getChildren().addAll(label("POWERS", 16), burstLabel, swapLabel, hint, messageLabel);
    }

    /** Updates the counters. Called every frame by GameScreen. */
    public void refresh() {
        Game g = game.get();
        burstLabel.setText("Burst x" + g.getBursts() + "  [B]");
        burstLabel.setTextFill(g.getBursts() > 0 ? ON : OFF);
        swapLabel.setText("Swap x" + g.getSwaps() + "  [N]");
        swapLabel.setTextFill(g.getSwaps() > 0 ? ON : OFF);
    }

    public void useBurst() {
        Game g = game.get();
        if (g.isGameOver()) {
            return;
        }
        if (g.useBurst()) {
            show("BURST!");
        } else {
            show(g.getBursts() == 0 ? "No bursts left" : "No small fruits");
        }
    }

    public void useSwap() {
        Game g = game.get();
        if (g.isGameOver()) {
            return;
        }
        show(g.useSwap() ? "Next fruit changed" : "No swaps left");
    }

    private void show(String text) {
        messageLabel.setText(text);
        fade.playFromStart();
    }

    private static Label label(String text, double size) {
        Label l = new Label(text);
        l.setTextFill(ON);
        l.setFont(Font.font("System", FontWeight.BOLD, size));
        l.setWrapText(true);
        return l;
    }
}