package com.netvanguard.ui;

import com.netvanguard.core.Fruit;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.image.Image;

/**
 * Loads the fruit pictures from src/main/resources/fruits/ (cherry.png, strawberry.png, grape.png,
 * orange.png, apple.png, watermelon.png and stone.png). To use your own art, replace those files
 * and keep the names. If a file is missing, get() returns null and the screen draws a plain shape.
 */
public final class FruitSprites {

    private static final Map<Fruit, Image> SPRITES = new EnumMap<>(Fruit.class);

    static {
        for (Fruit fruit : Fruit.values()) {
            String path = "/fruits/" + fruit.name().toLowerCase() + ".png";
            try (InputStream in = FruitSprites.class.getResourceAsStream(path)) {
                if (in != null) {
                    SPRITES.put(fruit, new Image(in, 160, 160, true, true));
                }
            } catch (Exception e) {
                // ignore: the screen falls back to drawn shapes
            }
        }
    }

    private FruitSprites() {}

    /** The picture for this fruit, or null if there is none. */
    public static Image get(Fruit fruit) {
        return SPRITES.get(fruit);
    }
}