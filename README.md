# NetVanguard: Fruit Fusion Blitz

A real-time puzzle game for **one or two players** on a local network, built as a desktop app with JavaFX.

Drop fruits into your grid and match identical ones to merge them into bigger fruits. Big combos
send **stone blocks** into your opponent's grid in real time, and the first grid to overflow loses.
Play solo for a high score, or host a game and let a friend join from another computer.

<!-- Add your screenshots to docs/ and uncomment these lines:
![Main menu](docs/menu.png)
![Two-player match](docs/versus.png)
-->

**Authors:** Miruthika Prabakaran, Prasharadha Kumar

## Features

- **Solo mode** with a saved top-10 scoreboard and a game that speeds up as your score grows
- **Two-player mode** over Wi-Fi or a hotspot: one player hosts, the other joins by typing the host's IP address
- Live **opponent board** and score, **incoming-stones warning**, and a "Sent n stones!" notice
- Win / lose / draw screen and a **rematch** button (press R)
- **Sound effects** for merges, chains, sending and receiving stones, winning and losing (press M to mute)
- Next-fruit preview, ghost piece showing where the fruit will land, hard drop
- Game logic has no UI or network code, and is covered by unit tests

## Run it

Requirements: **JDK 17 or newer** (21 recommended) and **Maven**. IntelliJ IDEA includes Maven and can download a JDK for you.

```bash
mvn javafx:run      # opens the game window
mvn test            # runs the unit tests
```

In an IDE, open the folder containing `pom.xml` and run `com.netvanguard.Launcher` (not `App`).

## How to play

| Key | Action |
| --- | --- |
| Left / Right (or A / D) | Move the falling fruit |
| Down (or S) | Soft drop |
| Space | Hard drop |
| R | Restart (solo) / ask for a rematch (two-player, after the match) |
| M | Sound on / off |

### Rules

1. Each grid is **6 columns x 10 rows**. One fruit falls at a time. Only Cherry, Strawberry and Grape spawn; bigger fruits come from merging.
2. **2 or more identical fruits touching** (up, down, left, right) merge into one fruit of the next tier:
   Cherry -> Strawberry -> Grape -> Orange -> Apple -> Watermelon. A group of Watermelons disappears.
3. After a merge, everything falls and the grid is checked again. Each extra round is one step of a **chain**.
4. Stones never merge, but a merge touching a stone **destroys** it.
5. You lose when a fruit or stone ends up in the top row, or a new fruit has no room to appear.

### Scoring and stones

- **Points** for each chain step: (fruit tier x 10 x group size, added up over all merged groups, plus 20 per stone destroyed) x the chain step number. Later steps of a chain are worth more.
- **Stones sent to your opponent:** every chain step from step 2 onward sends as many stones as its step number (a 3-step chain sends 2 + 3 = 5), and every merged group of Apples or bigger sends 2 extra. A single plain merge sends nothing.
- **Incoming stones** wait in a queue (shown as "Incoming: n"). Your own combos cancel them first. The rest land after your next drop, at most 6 at a time, spread over different columns.
- **Speed:** the fruit falls every 700 ms at the start and gets faster as your score grows, down to a minimum of 150 ms.

## Two-player game

1. Both computers must be on the **same network**. A phone hotspot works well; some school or college Wi-Fi blocks computers from talking to each other.
2. Player 1 chooses **Host game**. The screen shows an IP address such as `192.168.1.5`.
3. Player 2 chooses **Join game**, types that IP address and clicks **Connect**.
4. The match starts for both players. When it ends, both press **R** for a rematch.

If the connection fails, allow Java through the firewall on **Private networks** when Windows asks (or add `java.exe` in Windows Defender Firewall settings), and check that both players are on the same network. The game uses TCP port **5050**.

## Project structure

```
src/main/java/com/netvanguard/
  Launcher.java        entry point (run this one)
  App.java             the window; switches between screens
  core/                game logic only, no JavaFX and no networking
    Game, FallingItem, Grid, Fruit
    MergeResolver, MergeResult, Scoring, GarbageReceiver
    GameEvents         how the game reports score, stones sent and game over
    ScoreBoard, ScoreStore   solo top-10 scores
  net/                 networking, no JavaFX
    Host, Client, Connection, Peer, NetListener, RelayListener
    Protocol           the text messages
    MatchState         who won, rematch handling
    NetUtil            finds this computer's IP address
  ui/                  JavaFX screens
    MenuScreen, HostLobbyScreen, JoinLobbyScreen, GameScreen
    Navigator, Theme, BoardCanvas, FruitSprites, ScoreboardView, Sounds
src/main/resources/    fruit images (fruits/) and sound effects (sounds/)
src/test/java/...      unit tests for core and net
tools/make_sounds.py   generates the sound effects
```

### How it fits together

`Game` knows nothing about the network. It reports what happens through `GameEvents`
(`onScoreChanged`, `onGarbageSent`, `onGameOver`), and `GameScreen` turns those events into network
messages. That is why solo mode and two-player mode use exactly the same game code: solo simply has no
peer to send to. Each player runs their own game and sends events, so there is no central server.
Network callbacks arrive on a background thread and are moved to the JavaFX thread before they touch
the game or the screen.

## Network protocol

One UTF-8 text line per message over a TCP connection, default port `5050`.

| Message | Meaning |
| --- | --- |
| `GARBAGE 3` | I sent you 3 stones |
| `STATE <60 chars>` | My grid, 6 x 10 cells row by row, `.` is empty (see `Grid.serialize`) |
| `SCORE 1200` | My current score |
| `GAMEOVER` | My grid overflowed, so you win |
| `REMATCH` | I want to play again; the new match starts when both players have sent it |

## Tests

`mvn test` runs the unit tests for merging, chains, scoring, stone placement, the falling item, the
scoreboard, and the network layer (a host and a client talking over the loopback address, plus the
match and rematch logic).

## Sound effects

The sounds are generated by `tools/make_sounds.py` from simple tones, so they are original and free of
licensing restrictions. To regenerate them: `python3 tools/make_sounds.py`.

## Credits

Built by Miruthika Prabakaran and Prasharadha Kumar for a Java course project.
Fruit images: see `src/main/resources/fruits/`.

## Git workflow

Each of us worked on our own branch, opened a pull request into `main`, and pulled before starting each session.
