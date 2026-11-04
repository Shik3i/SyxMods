# SyxMods

Multi-project repository containing two mods for **Songs of Syx (v0.71.44 / V71)**:

- **[Syx Auto-Planner](SyxAutoBuild/)** (`com.autoplanner`) — Automated ceiling support pillar placement and room furniture/stockpile layout solver integrated into the room construction tool.
- **[Syx Architect](CreativeSyx/)** (`com.architect`) — Sandbox and creative mode toolkit providing configurable construction, maintenance, research, logistics, treasury, and population controls.

Both mods target Java 16 bytecode (`release = 16`), use the native `snake2d` / `util.gui` UI framework, and operate without per-frame heap allocations in the 60 Hz update loop.

---

## Repository Structure

```text
SyxMods/
├── SyxAutoBuild/          # Syx Auto-Planner mod source, tests, and V71 metadata
├── CreativeSyx/           # Syx Architect mod source, tests, docs, and V71 metadata
├── build.gradle           # Shared Java 16 toolchain configuration and master deployAll task
└── settings.gradle        # Multi-project definition (:SyxAutoBuild, :CreativeSyx)
```

---

## Mods

### 1. Syx Auto-Planner (`SyxAutoBuild`)

Integrates a draggable toolbar directly beneath the native room editor when drafting area-based rooms, along with an `AUTO` button in the top HUD bar.

- **Auto-Strut (`AutoStrutSolver`)**: Replicates the game's 16-ray ceiling support calculation (`TileRayTracer(4)`, $\sum \text{clamp}((3.5 - d)/3.5, 0, 1) \ge 1.0$) and scores candidate pillar positions by marginal support gain. Uses a multi-pass verification loop against `RoomPlacer.cost()` to eliminate unsupported ceiling tiles in a single click while keeping door thresholds clear and preserving floor connectivity via digital topology simple-point checks.
- **Auto-Furnish (`AutoFurnishSolver`)**: Packs workstations, beds, or storage crates sorted by effective output and spatial density (`multiplierStats` and `stats / area`), then places auxiliary efficiency items along walls and pillars to hit the configured target room efficiency. Includes automatic workstation backtracking if an initial pass leaves insufficient space for required efficiency modules.
- **Reachability Verification (`ReachabilityChecker`)**: Stamp-based array BFS flood-fill from real (or deterministic virtual) room doors ensuring every required interaction tile has a reachable interior walkable neighbor and no empty floor pockets are sealed off (`WillBlockRoom`).
- **Quick-Build**: Runs Auto-Strut followed by Auto-Furnish in one pass.

### 2. Syx Architect (`CreativeSyx`)

Provides a native configuration modal accessible via `F10` or the `CREATIVE` button in the top HUD bar, with state persisted per savegame.

- **Construction Tiers**: Vanilla, Free Materials (workers still perform construction tasks without consuming resources), Fast & Free (10x build speed), or Instant Build.
- **Maintenance Tiers**: Vanilla, Free Janitor Repairs (no material consumption), or Zero Decay (locks building condition at 100%).
- **Knowledge Tiers**: Vanilla, High-Watermark Lock (prevents tech point loss when laboratories are understaffed), or Omniscience (+1,000,000 innovation points via an additive `BoosterImp`).
- **Logistics & Economy**: Automated crate replenishment (raw materials or all assigned warehouse crates at ~80% capacity), infinite Denari treasury buffer, and food spoilage toggle.
- **World & Population**: On-demand immigrant spawning per species, raid immunity, battlefield morale auto-win, anti-aging, daylight/weather lock, and map-wide soil moisture control.

---

## Building from Source

### Prerequisites

- **Songs of Syx** v0.71.44 installed via Steam
- **JDK 16+** (tested with JDK 21, cross-compiled to Java 16 via `options.release = 16`)

By default, the build looks for `SongsOfSyx.jar` at:
```text
C:/Program Files (x86)/Steam/steamapps/common/Songs of Syx/SongsOfSyx.jar
```
If your game is installed in a different location, pass `-PsongsJar=/path/to/SongsOfSyx.jar` to Gradle.

### Build & Run Verification Suites

Compile both mods, package the V71 JARs, and run the automated pre-flight test suites (`:SyxAutoBuild:sanityCheck` and `:CreativeSyx:sanityCheck`):

```powershell
.\gradlew.bat build
```

### Deploy to Game Directory

Build and copy both mods into `%APPDATA%\songsofsyx\mods\SyxAutoPlanner` and `%APPDATA%\songsofsyx\mods\SyxArchitect` with case-exact `_Info.txt` metadata in both the mod root and `V71/` directories:

```powershell
.\gradlew.bat deployAll
```

You can also build or deploy a single project:

```powershell
.\gradlew.bat :SyxAutoBuild:deployMod
.\gradlew.bat :CreativeSyx:deployMod
```

---

## In-Game Activation

1. Start **Songs of Syx** and enable **Syx Auto-Planner** and/or **Syx Architect** in the launcher's Mods menu.
2. Load a savegame or start a new settlement.
3. Use the `AUTO` and `CREATIVE` buttons in the top-left HUD bar (or press `F10` for Syx Architect). Both mods support switching between English and German at runtime.

---

## License

Released under the [MIT License](LICENSE).
