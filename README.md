# 🍓 Fruit Fusion Blitz

**Fruit Fusion Blitz** is a JavaFX-based fruit-merging puzzle game that combines strategy, quick thinking, and competitive multiplayer gameplay. Players drop falling fruits, merge matching fruits to create higher-level fruits, earn points, and aim for the highest score.

The game features solo gameplay with three difficulty levels, limited-use power-ups, a ranked leaderboard, and real-time multiplayer over a local network. Its refreshed pastel-themed interface provides a light, playful gaming experience while preserving the original fruit artwork.

## ✨ Features

### 🎮 Solo Mode
- Three difficulty levels: **Easy, Medium, and Hard**.
- Different fruit-falling speeds for each difficulty.
- Stone obstacles with increasing frequency as difficulty increases.
- Score-based gameplay with increasing speed as the score grows.
- Restart functionality for replaying a game.

### ⚡ Power-Ups
- Special powers to support gameplay.
- Limited to **3 uses per power per game**.
- A dedicated power panel highlights the available powers and controls.

### 🏆 Leaderboard
- Records player names and scores.
- Ranks scores from highest to lowest.
- Displays the top scores to encourage replayability.

### 👥 Multiplayer Mode
- Real-time two-player gameplay over Wi-Fi or a mobile hotspot.
- Host and join functionality using the host's IP address.
- Opponent board and score display.
- Stone attacks, incoming-stone warnings, and attack notifications.
- Win, lose, draw, and rematch functionality.

### 🎨 User Interface and Audio
- Light pastel-inspired interface with rounded UI elements.
- Original fruit images preserved.
- Next-fruit preview and ghost-piece landing indicator.
- Sound effects for merges, chains, attacks, wins, and losses.
- Keyboard controls for movement and dropping fruits.

## 🛠️ Technologies and Tools

- **Java 17+** — core programming language.
- **JavaFX** — desktop graphical user interface.
- **JavaFX Canvas** — game-board rendering.
- **Apache Maven** — build and dependency management.
- **JUnit 5** — unit and automated testing.
- **Java Socket Programming** — local-network multiplayer communication.
- **Git and GitHub** — version control and source-code hosting.
- **IntelliJ IDEA** — development environment.

## 🚀 Getting Started

### Prerequisites
- JDK 17 or newer (JDK 21 recommended).
- IntelliJ IDEA or another Java IDE.
- Maven, or a configured Maven Wrapper if included in the project.

### Run the Game

1. Clone the repository:

   ```bash
   git clone https://github.com/MiruthikaPrabakaran/fruit-fusion-blitz.git
   ```

2. Open the project in IntelliJ IDEA by selecting the folder containing `pom.xml`.

3. Allow Maven to import the project dependencies.

4. Run the `com.netvanguard.Launcher` class, or use Maven:

   ```bash
   mvn javafx:run
   ```

### Run Tests

Execute the test suite using:

```bash
mvn clean test
```

If Maven is unavailable in your terminal, use the Maven tool window in IntelliJ IDEA.

## 🕹️ Controls

| Key | Action |
|---|---|
| Left / Right or A / D | Move the falling fruit |
| Down or S | Soft drop |
| Space | Hard drop |
| R | Restart solo / request a multiplayer rematch |
| M | Toggle sound |

Power-up controls are displayed in the game's power panel.

## 🍉 Game Rules

1. The game board contains 6 columns and 10 rows.
2. Matching fruits merge into the next fruit tier.
3. Chain reactions provide additional scoring opportunities.
4. Stones act as obstacles and do not merge with fruits.
5. Merging next to a stone destroys it.
6. The game ends when the board overflows or a new fruit cannot spawn.

Fruit progression:

**Cherry → Strawberry → Grape → Orange → Apple → Watermelon**

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/com/netvanguard/
│   │   ├── App.java
│   │   ├── Launcher.java
│   │   ├── core/       # Game logic, merging, scoring, power-ups
│   │   ├── net/        # Multiplayer networking
│   │   └── ui/         # Screens, rendering, themes and controls
│   └── resources/
│       ├── fruits/     # Fruit and stone images
│       └── sounds/     # Gameplay sound effects
└── test/
    └── java/           # Unit and networking tests
```

## 🌐 Multiplayer Setup

1. Connect both computers to the same Wi-Fi network or mobile hotspot.
2. One player selects **Host Game**.
3. The other player selects **Join Game** and enters the host's IP address.
4. Start the match and compete to keep your board from overflowing.

The multiplayer connection uses TCP port `5050`. Firewall settings may need to allow Java connections on private networks.

## 👩‍💻 Authors

**Miruthika Prabakaran** and **Prasharadha Kumar**

Developed as a Java course project to explore object-oriented programming, desktop UI development, game logic, networking, and automated testing.

---

*Merge fruits, build combos, use your powers wisely, and climb the leaderboard!* 🍓
