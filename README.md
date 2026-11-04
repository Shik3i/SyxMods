# SyxMods — Songs of Syx (v0.71.44) Mod Suite

Consolidated multi-project repository containing two native UI & gameplay mods for **Songs of Syx (v0.71.44)**:

1. **`SyxAutoBuild` (`Syx Auto-Planner`)** — Package `com.autoplanner.*`
   - **Auto-Strut Solver**: 16-ray ceiling physics solver (`TileRayTracer(4)`) with multi-pass engine cost verification (`0` remaining yellow tiles in a single click).
   - **Auto-Furnish Solver**: Density- and capacity-optimized room furniture & stockpile packer (`multiplierStats` + spatial density heuristic) with workstation backtracking and Zero-GC BFS reachability verification.
   - **Quick-Build**: Combines Auto-Strut and Auto-Furnish in one click.

2. **`CreativeSyx` (`Syx Architect`)** — Package `com.architect.*`
   - **Creative & Sandbox Master Suite**: Instant construction, free materials, zero maintenance decay, omniscience (+1,000,000 innovation), automated crate logistics buffer, infinite Denari treasury, raid immunity, population spawner, and environmental locks.

---

## Build & Deployment

Compile, run all automated verification checks (`sanityCheck`), and build V71 JARs (Java 16 bytecode target):
```powershell
.\gradlew.bat build
```

Deploy both mods directly to `%APPDATA%\songsofsyx\mods\SyxAutoPlanner` and `%APPDATA%\songsofsyx\mods\SyxArchitect` (with case-exact `_Info.txt` in root and `V71/`):
```powershell
.\gradlew.bat deployAll
```
