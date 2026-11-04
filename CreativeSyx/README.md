# Syx Architect (`CreativeSyx`)

Creative and sandbox toolkit mod for **Songs of Syx (v0.71.44)**. Provides granular control over construction material costs, building maintenance, research innovation, warehouse crate buffers, treasury funds, population spawning, and environmental conditions.

## Architecture

- **`ArchitectScript` / `ArchitectInstance`**: Implements `script.SCRIPT`, manages per-savegame state serialization (`ArchitectState`, binary format V4 with backward compatibility for V1–V3), hooks `F10` hotkey input, and injects the `CREATIVE` button into `UIPanelTop`.
- **`modules/ConstructionModule`**: Controls room, wall, road, and structure construction behavior (`Vanilla`, `Free Materials`, `Fast & Free`, `Instant Build`) and building maintenance / janitor repairs (`Vanilla`, `Free Janitor Repairs`, `Zero Decay`).
- **`modules/BoostModule`**: Manages dynamic `BoosterImp` instances attached to `BOOSTABLES` for omniscience research (+1,000,000 innovation), high-watermark knowledge retention, citizen loyalty, and spoilage prevention.
- **`modules/LogisticsModule`**: Rolling-chunk warehouse crate buffer and treasury replenishment.
- **`modules/SecurityModule`**: Handles raid auto-cancellation, peace enforcement, and instant battlefield victory.
- **`modules/WorldModule`**: Handles species immigration waves, longevity multipliers, weather locking, and continuous soil moisture synchronization.
- **`ui/ArchitectUI` & `TutorialModal`**: Native `GuiSection` / `GPanel` interface with bilingual support (EN/DE).

## Documentation

- [API Discovery & Internal Architecture](../docs/API_DISCOVERY.md)
- [Steam Workshop Description](../docs/STEAM_WORKSHOP_ARCHITECT.txt)

## Build & Deploy

From the repository root:

```powershell
.\gradlew.bat :CreativeSyx:build
.\gradlew.bat :CreativeSyx:deployMod
```
