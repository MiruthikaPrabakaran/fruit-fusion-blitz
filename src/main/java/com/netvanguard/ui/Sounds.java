package com.netvanguard.ui;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.media.AudioClip;

/**
 * Short sound effects (files in src/main/resources/sounds, made by tools/make_sounds.py).
 *
 * Never throws: if a file is missing or the computer has no audio, the game simply stays silent.
 * Call from the JavaFX thread.
 */
public final class Sounds {

    public enum Effect {
        MERGE("merge.wav"),
        CHAIN("chain.wav"),
        SEND("send.wav"),
        INCOMING("incoming.wav"),
        WIN("win.wav"),
        LOSE("lose.wav");

        private final String file;

        Effect(String file) {
            this.file = file;
        }
    }

    private static final Map<Effect, AudioClip> CLIPS = new EnumMap<>(Effect.class);
    private static boolean muted;
    private static boolean broken; // audio failed once: stop trying

    private Sounds() {
    }

    public static synchronized void play(Effect effect) {
        if (muted || broken) {
            return;
        }
        try {
            AudioClip clip = CLIPS.get(effect);
            if (clip == null) {
                URL url = Sounds.class.getResource("/sounds/" + effect.file);
                if (url == null) {
                    return; // file missing: stay silent
                }
                clip = new AudioClip(url.toExternalForm());
                CLIPS.put(effect, clip);
            }
            clip.play();
        } catch (RuntimeException | LinkageError e) {
            broken = true; // no audio device or media module problem: give up quietly
        }
    }

    /** Switches sound on/off. Returns true if sound is now muted. */
    public static synchronized boolean toggleMuted() {
        muted = !muted;
        return muted;
    }

    public static synchronized boolean isMuted() {
        return muted;
    }
}
