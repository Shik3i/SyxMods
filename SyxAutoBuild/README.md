# Syx Auto-Planner (`SyxAutoBuild`)

Automated room planning mod for **Songs of Syx (v0.71.44)** providing 1-click ceiling support placement (`Auto-Strut`) and furniture/stockpile layout packing (`Auto-Furnish`).

## Architecture

- **`AutoPlannerScript` / `AutoPlannerInstance`**: Lifecycle entry point (`script.SCRIPT`) and settlement view interrupter hook. Injects the `AUTO` button into `UIPanelTop` and renders the draggable room editor toolbar whenever `RoomPlacer` is active on an area-based blueprint.
- **` solver/AutoStrutSolver`**: Evaluates ceiling support using `TileRayTracer(4)` across all 16 rays ($\sum \text{clamp}((3.5 - d)/3.5, 0, 1) \ge 1.0$). Scores candidate pillar tiles by marginal support gain and verifies against `RoomPlacer.cost()` across multiple passes until 0 unsupported tiles remain.
- **`solver/AutoFurnishSolver`**: Packs primary furniture groups sorted by capacity and spatial density (`multiplierStats` weighted by `stats / area`), places auxiliary efficiency modules along walls and struts, and backtracks workstations if needed to meet the target room efficiency.
- **`solver/ReachabilityChecker`**: Zero-GC array-based BFS flood-fill and 8-neighbor Yokoi simple-point connectivity check ensuring all workstation interaction tiles and walkable floor tiles remain connected to room doors.
- **`solver/RoomGridSnapshot` & `PlacerBridge`**: Flat-array 2D room draft representation and reflection bridge to package-private `RoomPlacer` internals.

## Build & Deploy

From the repository root:

```powershell
.\gradlew.bat :SyxAutoBuild:build
.\gradlew.bat :SyxAutoBuild:deployMod
```
