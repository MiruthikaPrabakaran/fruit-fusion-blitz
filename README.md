# NetVanguard: Fruit Fusion Blitz

A real-time, two-player puzzle battle over a local network (desktop GUI, JavaFX).

Each player has their own grid. Drop fruits, match identical ones to merge them into bigger fruits, and score points. A big combo sends **stone blocks** into your opponent's grid in real time. The first grid to overflow loses.

**Authors:** `Miruthika Prabakaran`, `Prasharadha Kumar`

## Tech stack

- Java 17+ (21 recommended), Maven
- JavaFX (desktop GUI only)
- Plain `java.net` sockets for networking
- JUnit 5 for tests
- Git / GitHub

## Run it

```bash
mvn javafx:run      # opens the game window
mvn test            # runs the unit tests
```

In an IDE, run `com.netvanguard.Launcher` (not `App`).

## Game rules

1. Each grid is **6 columns x 10 rows**. One fruit falls at a time. Move it left/right and drop it.
2. When 2 or more identical fruits touch, they merge into the next tier:
   Cherry -> Strawberry -> Grape -> Orange -> Apple -> Watermelon.
3. Merges can chain: a merged fruit that lands next to an identical one merges again.
4. Chains and high-tier merges send **stone blocks** to the opponent. Stones appear at the top of their grid and never merge.
5. If anything sits in the top row after items settle, that grid has **overflowed** and that player loses.

## Project structure

```
src/main/java/com/netvanguard/
  Launcher.java      entry point
  App.java           JavaFX application
  core/              game logic only (no UI, no network): Grid, Fruit, ...
  net/               sockets and the message protocol
  ui/                JavaFX screens and drawing
src/test/java/...    unit tests (core logic)
```

## Network protocol

One text line per message.

| Message | Meaning |
| --- | --- |
| `GARBAGE 3` | I sent you 3 stone blocks |
| `STATE <60 chars>` | My current grid (`Grid.serialize()`) |
| `SCORE 1200` | My current score |
| `GAMEOVER` | My grid overflowed, you win |

Default port: `5050`.

## Roadmap

- [x] Project skeleton, `Grid`, `Fruit`, protocol constants
- [ ] Falling item, movement, landing
- [ ] Merge and chain detection
- [ ] Scoring and garbage rules
- [ ] Playable single-player board (keyboard controls, next-item preview)
- [x] Host/join over sockets
- [x] Opponent grid display, garbage sending/receiving
- [ ] Game over and rematch
- [ ] Polish: warning bar, sound, animations, how-to-play screen

## Git workflow

Work on a branch, open a pull request into `main`, and pull before you start each session.
