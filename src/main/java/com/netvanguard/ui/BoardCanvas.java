package com.netvanguard.ui;

import com.netvanguard.core.Fruit;
import com.netvanguard.core.Grid;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Draws one Grid. Call draw(grid) whenever the grid changes. */
public class BoardCanvas extends Canvas {
    private final double cellSize;

    public BoardCanvas(double cellSize) {
        super(Grid.COLS * cellSize, Grid.ROWS * cellSize);
        this.cellSize = cellSize;
    }

    public void draw(Grid grid) {
        GraphicsContext g = getGraphicsContext2D();

        g.setFill(Color.web("#1f2937"));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setStroke(Color.web("#374151"));
        for (int r = 0; r <= Grid.ROWS; r++) {
            g.strokeLine(0, r * cellSize, getWidth(), r * cellSize);
        }
        for (int c = 0; c <= Grid.COLS; c++) {
            g.strokeLine(c * cellSize, 0, c * cellSize, getHeight());
        }

        g.setFont(Font.font("System", FontWeight.BOLD, cellSize * 0.22));
        g.setTextAlign(TextAlignment.CENTER);

        for (int r = 0; r < Grid.ROWS; r++) {
            for (int c = 0; c < Grid.COLS; c++) {
                Fruit fruit = grid.get(r, c);
                if (fruit == null) {
                    continue;
                }
                double x = c * cellSize;
                double y = r * cellSize;
                double pad = cellSize * 0.08;

                g.setFill(Color.web(fruit.colorHex()));
                g.fillRoundRect(x + pad, y + pad, cellSize - 2 * pad, cellSize - 2 * pad, 14, 14);

                g.setFill(Color.WHITE);
                g.fillText(fruit.label(), x + cellSize / 2, y + cellSize / 2 + cellSize * 0.08);
            }
        }
    }
}
