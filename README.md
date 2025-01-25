# OSR-Style Dungeon Generator

This project demonstrates an **OSR "Appendix A"–inspired** random dungeon generator
and a **2D grid fitter**. The code is written in Java, with examples of:

1. **Generating** a dungeon as an **abstract graph** of Rooms and Corridors.
2. **Fitting** that dungeon onto a 2D grid:
    - A **BFS/Naive** approach (`DungeonGridFitter`).
    - An **A***-based approach (`AStarDungeonGridFitter`) for more advanced pathfinding and offsetting corridors.

Neither fitter works that well so far.

The project also includes:

- A **GUI Viewer** (`DungeonGridViewer`) that:
    - Displays the fitted dungeon in a Swing window.
    - Allows **re-fitting** the same `Dungeon` with either BFS or A* at the click of a button.
    - Allows saving/loading **JSON** for dungeon layout and room data.
    - Allows saving an **image (PNG)** screenshot of the grid.

> **Note**: Part of this code was generated with the assistance of a Large Language Model (ChatGPT, provided by OpenAI).
> Any modifications, expansions, or improvements are fully permissible under typical open-source usage.

---

## Table of Contents

1. [Repository Structure](#repository-structure)
2. [Installation & Setup](#installation--setup)
3. [Usage](#usage)
    - [Dungeon Generation](#dungeon-generation)
    - [Fitting with BFS or A*](#fitting-with-bfs-or-a)
    - [Saving & Loading JSON](#saving--loading-json)
    - [Saving PNG Screenshots](#saving-png-screenshots)
4. [Key Classes & Interfaces](#key-classes--interfaces)
5. [Future Work](#future-work)
6. [Credits](#credits)

---

## Repository Structure

A typical directory layout might look like:

├── build.gradle
├── src
│ ├── main
│ │ └── java
│ │ ├── Dungeon.java
│ │ ├── Room.java
│ │ ├── Corridor.java
│ │ ├── RoomShape.java
│ │ ├── AdvancedDungeonGenerator.java
│ │ ├── DungeonFitter.java // The interface
│ │ ├── DungeonGridFitter.java // BFS-based fitter
│ │ ├── AStarDungeonGridFitter.java // A*-based fitter
│ │ ├── GridCell.java
│ │ ├── DungeonGridViewer.java // The Swing GUI
│ │ └── ...
└── README.md

- **`DungeonGridViewer`** contains the Swing-based GUI.
- **`build.gradle`** manages dependencies (e.g., Jackson for JSON, JGraphT for A*).

---

## Installation & Setup

1. **Clone** or download the repository.
2. **Java 17** is required. (also tested on 21)
3. **Gradle** is recommended for building.
4. Run`./gradlew build`

---

## Usage

After you build the project, you can run the GUI by executing the `main` method in **`DungeonGridViewer`** (or whichever
class you designate as the entry point).

### Dungeon Generation

- We use **`AdvancedDungeonGenerator`** to produce a random dungeon graph:
    - Creates **Rooms** (with sizes and shapes).
    - Links them via **Corridors** using a partial Appendix A/C–style approach (Table I, II, III, etc.).
- The `Dungeon` object is stored in memory so you can repeatedly **re-fit** it.

### Fitting with BFS or A*

- **`DungeonGridFitter` (BFS)**: A simpler approach, corridor paths might run adjacent to rooms.
- **`AStarDungeonGridFitter`**: A more advanced approach, offsetting corridors away from rooms by using **A***
  pathfinding with a “penalty near rooms” cost.

In the GUI, you'll see two buttons:

- **“Fit with BFS”**: Uses `DungeonGridFitter`.
- **“Fit with AStar”**: Uses `AStarDungeonGridFitter`.

Once you click either button, the viewer will recalculate the layout on the 2D grid and redraw.

### Saving & Loading JSON

The **GUI** provides:

- **“Save JSON”**: Serializes the current **fitted** layout (all cells plus the bounding rectangle) and **room data** to
  a JSON file.
- **“Load JSON”**: Reads a previously saved JSON, reconstructs the `Dungeon` and cell layout (or keeps them in memory
  for display).
    - After loading, you can **re-fit** the loaded `Dungeon` with BFS or A* if you wish to generate a fresh layout.

### Saving PNG Screenshots

Click the **“Save as PNG”** button to open a file chooser. The **current** grid (BFS or A*) is rendered into a PNG image
for you to store locally.

---

## Key Classes & Interfaces

1. **`DungeonFitter`** (Interface)
    - Methods: `fitDungeon(Dungeon)`, `getAllCells()`, `getBounds()`.
    - A minimal contract for any dungeon “fitting” algorithm.

2. **`DungeonGridFitter`** (Implements `DungeonFitter`)
    - Classic BFS or naive corridor approach.
    - Simple bounding rectangle and grid-based pathing.

3. **`AStarDungeonGridFitter`** (Implements `DungeonFitter`)
    - Uses **JGraphT** to build a graph and run **A*** pathfinding.
    - Offsets corridors from rooms via a cost-based approach.

4. **`AdvancedDungeonGenerator`**
    - Randomly produces a `Dungeon` (list of `Room`, `Corridor`), loosely following AD&D 1e tables.

5. **`DungeonGridViewer`** (Swing GUI)
    - The main entry point for interactive usage.
    - Buttons for “Regenerate,” “Fit with BFS/AStar,” “Save/Load JSON,” and “Save PNG.”

6. **`Room`, `Corridor`, `GridCell`**
    - Data structures modeling **abstract** vs. **fitted** dungeon elements.

---

## Future Work

- **Collision Checks & Room Overlaps**: Currently, large rooms may overwrite or partially collide. A more robust
  approach would attempt multiple placements or dynamic shifting.
- **More Appendix Features**: Expand the generator with further tables (pools, traps, tricks, stairs across multiple
  levels, etc.).
- **Performance**: For very large dungeons, generating a single global graph for corridor pathfinding or more efficient
  bounding constraints can speed things up.
- **Heuristics**: Incorporate a “compactness” measure to reduce the bounding rectangle or to randomize corridor shapes
  further.
- **Interactive Editing**: Let the user drag rooms or corridors, place doors, etc.

---

## Credits

- **Core logic** inspired by OSR "Appendix" random dungeon method.
- **Portions of code** (including the BFS, A* approach, JSON saving/loading) were generated with help from a **Large
  Language Model** (ChatGPT, OpenAI).
- **Jackson** for JSON serialization/deserialization.
- **JGraphT** for graph-based algorithms and **A*** pathfinding.

Enjoy exploring and extending your Old-School Roleplaying style random dungeons in Java!

