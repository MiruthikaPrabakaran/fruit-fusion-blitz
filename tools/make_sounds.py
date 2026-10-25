#!/usr/bin/env python3
"""
Generates the game's sound effects as small 16-bit mono WAV files.

The sounds are synthesized from simple tones, so they are original and have no licensing
restrictions. Run from the project root to regenerate them:

    python3 tools/make_sounds.py

Output: src/main/resources/sounds/*.wav
"""
import math
import os
import random
import struct
import wave

RATE = 44100
OUT_DIR = os.path.join("src", "main", "resources", "sounds")


def tone(freq, seconds, volume=0.35, shape="sine", end_freq=None, release=0.08):
    """One note. end_freq makes the pitch slide from freq to end_freq."""
    count = int(RATE * seconds)
    samples = []
    phase = 0.0
    for i in range(count):
        t = i / count
        f = freq if end_freq is None else freq + (end_freq - freq) * t
        phase += 2 * math.pi * f / RATE
        if shape == "square":
            value = 0.6 * math.sin(phase) + 0.25 * math.sin(3 * phase) + 0.1 * math.sin(5 * phase)
        elif shape == "triangle":
            value = (2 / math.pi) * math.asin(math.sin(phase))
        else:
            value = math.sin(phase)
        attack = min(1.0, i / (RATE * 0.004))                        # 4 ms fade-in, no click
        tail = min(1.0, (count - i) / max(1, RATE * release))        # fade-out
        samples.append(value * volume * attack * tail)
    return samples


def silence(seconds):
    return [0.0] * int(RATE * seconds)


def noise(seconds, volume=0.2):
    count = int(RATE * seconds)
    return [(random.random() * 2 - 1) * volume * (1 - i / count) for i in range(count)]


def mix(a, b):
    length = max(len(a), len(b))
    a = a + [0.0] * (length - len(a))
    b = b + [0.0] * (length - len(b))
    return [x + y for x, y in zip(a, b)]


def save(name, samples):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, name)
    with wave.open(path, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        frames = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples)
        f.writeframes(frames)
    print(f"wrote {path} ({len(samples) / RATE:.2f}s)")


def main():
    random.seed(1)  # same files every run

    # A fruit merged: two quick rising notes
    save("merge.wav", tone(659.25, 0.07) + tone(880.00, 0.14))

    # A chain: rising arpeggio
    save("chain.wav",
         tone(523.25, 0.07) + tone(659.25, 0.07) + tone(783.99, 0.07) + tone(1046.50, 0.22))

    # I sent stones: falling "whoosh"
    save("send.wav", tone(900, 0.26, volume=0.55, shape="square", end_freq=240))

    # Stones are coming at me: low thud
    save("incoming.wav", mix(tone(110, 0.30, volume=0.38, end_freq=50, release=0.2), noise(0.06, 0.12)))

    # I won: bright fanfare
    save("win.wav",
         tone(523.25, 0.11) + tone(659.25, 0.11) + tone(783.99, 0.11) + tone(1046.50, 0.45, release=0.3))

    # I lost: sad falling notes
    save("lose.wav",
         tone(392.00, 0.18, shape="triangle") + tone(329.63, 0.18, shape="triangle")
         + tone(261.63, 0.18, shape="triangle") + tone(196.00, 0.50, shape="triangle", release=0.35))


if __name__ == "__main__":
    main()
