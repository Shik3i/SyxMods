# Songs of Syx (v0.71.44) - API Discovery & Inspection Report

> **Target Version:** Songs of Syx `v0.71.44` (`VERSION_MAJOR = 71`, `VERSION_MINOR = 44`)  
> **Mod Version Folder:** `V71`  
> **Runtime Environment:** OpenJDK Temurin 21.0.11+10 (LTS)  
> **Bytecode Major Version:** `60` (Java 16 target bytecode)  
> **Inspection Date:** November 2026  
> **Methodology:** Automated inspection using `javap -cp SongsOfSyx.jar -public` combined with source auditing of `SongsOfSyx-sources.jar`.

---

## 1. System Paths & Installation Topography

### Detected Installation Paths
- **Game Installation (Steam):** `C:\Program Files (x86)\Steam\steamapps\common\Songs of Syx\`
  - Core JAR: `SongsOfSyx.jar` (MD5/Size: `35,622,238` bytes)
  - Bundled JRE: `jre\bin\java.exe` (OpenJDK 21.0.11 Temurin)
  - Source Bundle: `info\SongsOfSyx-sources.jar`
  - Modding Guide: `info\MAKE_A_MOD.txt`
- **User Mod Directory (AppData):** `%APPDATA%\songsofsyx\mods\`
  - Mod Anchor: `%APPDATA%\songsofsyx\mods\<ModName>\`
  - Active Version Structure: `<ModName>\V71\script\<ModJar>.jar`

---

## 2. Mod Loading Architecture & Entry Points

Songs of Syx discovers scripts dynamically at startup via `init.paths.PATHS.SCRIPT().jar` and `script.ScriptLoad`.

### Discovery Rules
1. The game scans each mod directory in `%APPDATA%\songsofsyx\mods\`.
2. It reads `_Info.txt` at the mod root:
   ```properties
   VERSION: "0.1.0",
   NAME: "Syx Architect",
   DESC: "Creative & Sandbox Suite for Songs of Syx",
   AUTHOR: "CreativeSyx Team",
   INFO: "https://github.com/CreativeSyx/SyxArchitect",
   ```
3. It determines the best compatible engine version folder: for v0.71.x, it seeks `V71/`.
4. Any JAR files inside `V71/script/` are added to the game classloader.
5. `script.ScriptLoad` iterates through all classes in the JAR; any class implementing `script.SCRIPT` with a public no-arg constructor is instantiated.

### Entry-Point Interfaces

#### `script.SCRIPT`
```java
package script;

public interface SCRIPT {
    // Human-readable identifier shown in launcher and script selectors
    CharSequence name();
    
    // Description of the script
    CharSequence desc();
    
    // Called before the game world/resources are created (early initialization / hooks)
    default void initBeforeGameCreated() {}
    
    // Called after game resources are created, before game loop starts (UI/Debug registrations)
    default void initBeforeGameInited() {}
    
    // Factory method returning the active instance for a game session
    SCRIPT_INSTANCE createInstance();
    
    // If true, appears in selectable scripts list on new game setup
    default boolean isSelectable() { return true; }
    
    // If true, automatically injected into all new games and savegame loads
    default boolean forceInit() { return false; }
}
```

#### `script.SCRIPT.SCRIPT_INSTANCE`
```java
public interface SCRIPT_INSTANCE {
    // 60 Hz simulation tick. CRITICAL: Zero heap allocations in this loop!
    void update(double ds);
    
    // Savegame persistence hook
    void save(snake2d.util.file.FilePutter file);
    
    // Savegame deserialization hook
    void load(snake2d.util.file.FileGetter file) throws java.io.IOException;
    
    // Post-render hook for direct screen rendering
    default void render(snake2d.Renderer r, float ds) {}
    
    // Keyboard input listener
    default void keyPush(view.keyboard.KEYS key) {}
    
    // Mouse button click listener
    default void mouseClick(snake2d.MButt button) {}
    
    // Tooltip customization hook
    default void hoverTimer(double mouseTimer, util.gui.misc.GBox text) {}
    
    // Screen cursor hover listener
    default void hover(snake2d.util.datatypes.COORDINATE mCoo, boolean mouseHasMoved) {}
    
    // Recovery handler if save state was corrupted
    default boolean handleBrokenSavedState() { return false; }
}
```

---

## 3. Serialization Engine (`snake2d.util.file.*`)

Songs of Syx uses a binary stream protocol via `FilePutter` and `FileGetter`. It requires strict sequential symmetry: fields must be read in the exact order and type they were written.

### `snake2d.util.file.FilePutter` (Writing)
```java
public final class FilePutter {
    public FilePutter bool(boolean val);
    public FilePutter b(byte val);
    public FilePutter s(short val);
    public FilePutter i(int val);
    public FilePutter l(long val);
    public FilePutter f(float val);
    public FilePutter d(double val);
    public void chars(CharSequence str);
    
    // Array writers
    public void bs(byte[] arr);
    public void ss(short[] arr);
    public void is(int[] arr);
    public void ls(long[] arr);
    public void fs(float[] arr);
    public void ds(double[] arr);
    public void charss(CharSequence[] arr);
    
    // Structural markers for debugging and stream integrity
    public void mark(String tag);
    public void mark(Class<?> clazz);
}
```

### `snake2d.util.file.FileGetter` (Reading)
```java
public final class FileGetter {
    public boolean bool() throws IOException;
    public byte b() throws IOException;
    public short s() throws IOException;
    public int i() throws IOException;
    public long l();
    public float f();
    public double d() throws IOException;
    public String chars() throws IOException;
    
    // Array readers
    public void bs(byte[] arr) throws IOException;
    public void ss(short[] arr) throws IOException;
    public void is(int[] arr) throws IOException;
    public void ls(long[] arr);
    public void fs(float[] arr);
    public void ds(double[] arr) throws IOException;
    public String[] charss() throws IOException;
    
    // Structural validation
    public void check(String tag) throws IOException;
}
```

> **Architecture Recommendation:** Always write a `MAGIC_NUMBER` (int) and `SAVE_VERSION` (int) header at the beginning of `save()` and assert them in `load()`. This guarantees safe migration across mod versions without corrupting player saves.

---

## 4. Boosting Architecture (`game.boosting.*`)

The boosting system computes dynamic multipliers and additive bonuses across settlements, individuals, rooms, and armies.

### Hierarchy & Accessors (`game.boosting.BOOSTABLES`)
```java
public class BOOSTABLES {
    public static BOOSTABLES.Physics PHYSICS();
    public static BOOSTABLES.Civic CIVICS();
    public static BOOSTABLES.Behaviour BEHAVIOUR();
    public static BOOSTABLES.Activity ACTIVITY();
    public static BOOSTABLES.Battle BATTLE();
    public static BOOSTABLES.Noble NOBLE();
    public static BoostableCat ROOMS();
    public static BoostableCat CONSUMPTION();
}
```

### Target Boostables Verified in v0.71.44
| Category | Boostable Field | Game Effect | Syx Architect Target Feature |
| :--- | :--- | :--- | :--- |
| **Civic** | `BOOSTABLES.CIVICS().INNOVATION` | Increases tech/innovation generation | Instant Knowledge / Fast Tech |
| **Civic** | `BOOSTABLES.CIVICS().MAINTENANCE` | Increases building robustness against wear | Zero Degradation / No Janitor Work |
| **Civic** | `BOOSTABLES.CIVICS().SPOILAGE` | Spoilage rate reduction for stored goods | Infinite Shelf Life / No Food Spoilage |
| **Civic** | `BOOSTABLES.CIVICS().RAID_SECURITY` | Increases defense rating against bandit raids | Complete Raid Protection |
| **Behaviour** | `BOOSTABLES.BEHAVIOUR().LOYALTY` | Population loyalty | Max Loyalty / Prevent Rebellions |
| **Behaviour** | `BOOSTABLES.BEHAVIOUR().HAPPI` | General citizen happiness | Max Happiness Cheat |
| **Behaviour** | `BOOSTABLES.BEHAVIOUR().HAPPI_SLAVES` | Slave happiness | Slave Contentment |
| **Physics** | `BOOSTABLES.PHYSICS().SPEED` | Movement speed of humanoids | Super Speed / Fast Logistics |
| **Physics** | `BOOSTABLES.PHYSICS().STAMINA` | Stamina pool of workers and soldiers | Infinite Stamina |

### Booster Injection Architecture
A booster is created by subclassing or instantiating `BoosterImp`:
```java
public class BoosterImp extends Booster implements BValue {
    // isMul = true for multiplier (1.0 = 100%), false for additive bonus
    public BoosterImp(BSourceInfo info, double from, double to, boolean isMul);
    public BoosterImp(BSourceInfo info, double value, boolean isMul);
    
    public double getValue(double v);
    public double vGet(Player player);
    public double vGet(Region region);
    public double vGet(Induvidual induvidual);
}
```
Adding to a boostable:
```java
BSourceInfo sourceInfo = new BSourceInfo("Syx Architect", UI.icons().s.cog);
BoosterImp booster = new BoosterImp(sourceInfo, 10.0, true);
BOOSTABLES.CIVICS().INNOVATION.add(booster);
```

---

## 5. Settlement Rooms & Logistics (`settlement.room.*`)

### Warehouses & Storage (`settlement.room.infra.stockpile.*`)
- **`ROOM_STOCKPILE`**: Main blueprint manager.
- **`StockpileInstance`**: Live instance of a stockpile room in the settlement.
  - `totalCrates()`: Number of crates configured.
  - `crate(int x, int y)`: Retrieves `StorageCrate`.
  - `storedD(RESOURCE res)`: Amount of specified resource currently stored.
  - `crateSize(RESOURCE res)`: Base capacity per crate for the resource.
  - `prioritizeToggle()`: Prioritized hauling toggle.
  - `moveCapacityAm(RESOURCE res)`: Current logistics hauling capacity.
- **Architect Hook:** By adjusting crate capacity multipliers or directly manipulating crate storage in `StockpileInstance`, we achieve infinite warehouse capacity and instant hauling.

### Maintenance & Degradation (`settlement.room.infra.janitor.*` & `settlement.maintenance.*`)
- **`ROOM_JANITOR`**: Janitor room blueprint.
- **`JanitorInstance`**: Janitor room runtime instance.
- **`settlement.maintenance.MAINTENANCE`**: Central degradation manager. Accessible via `SETT.MAINTENANCE()`.
  - `public final MAP_DOUBLE degrade`: Map of current degradation values per tile across the entire settlement.
  - `public void maintain(int x, int y)`: Restores tile condition to 100% (0.0 degradation).
  - `public void clear()`: Clears all pending maintenance tasks.
- **Architect Hook:** In `update()`, iterate degraded tiles or call `SETT.MAINTENANCE().maintain(x, y)` to keep the entire city at pristine condition without requiring janitors or maintenance supplies.

### Construction Sites (`settlement.room.main.construction.*`)
- **`CONSTRUCTION`**: Manages all ongoing construction and demolition. Accessible via `SETT.ROOMS().CONSTRUCTION`.
  - `public void construct(int x, int y)`: **Instantly completes construction of the tile at (x, y)!**
  - `public boolean isRepair(int x, int y)`: Checks whether a tile is undergoing repair.
  - `public void breakIt(TmpArea area, ConstructionInit init, int x, int y)`: Instantly demolishes.
  - `public int instances()`: Number of active construction zones.
- **Architect Hook:** Instant Build cheat triggers `construct(x, y)` across all pending construction tiles, bypassing material deliveries and builder work cycles.

---

## 6. Military, Raids & Battle Morale

### Settlement Defense & Invasions (`settlement.battle.invasion.*`)
- **`SETT.INVADOR()`**: Central invasion controller (`settlement.battle.invasion.Invador`).
  - `invading()`: Returns `true` if an enemy raid/invasion is currently in progress.
  - `invadingPending()`: Returns `true` if an invasion has been declared and is approaching.
  - `cancel(int id)`: Cancels an invasion.
- **`Invasion`**: Active invasion instance.
  - `public boolean victory`: Flag marking player victory.
- **Architect Hook:** Peaceful Sandbox Mode automatically detects approaching raids via `SETT.INVADOR().invading()` and cancels them or sets `victory = true` instantly.

### World Armies (`world.army.*` & `world.entity.army.WArmy`)
- **`world.army.AD`**: Central world military manager.
  - `AD.army(Faction faction)`: Returns `ADArmies` for a faction.
  - `AD.morale(WArmy army)`: Computes army morale on the world map.
  - `AD.moraleFactors()`: List of active morale boosters.
- **`WArmy`**: Represents a deployed army entity on the world map.
  - `disband()`: Disbands the army immediately.
  - `teleport(int x, int y)`: Teleports army to specified coordinates.
  - `stop()`: Halts movement.

### Tactical Battles (`game.battle.*`)
- **`Armies`**: Accessible via `GAME.battle().armies()`.
  - `player()`: Returns player `Army`.
  - `enemy()`: Returns enemy `Army`.
  - `divisions()`: List of all active tactical divisions (`Div`).
- **`Div`**: Individual tactical combat division.
  - `status()`: Returns `DivStatus` (closest enemies, flanking status, collisions).
  - `morale()`: Division morale rating.
- **Architect Hook:** Auto-Win Tactical Battles instantly sets enemy divisions to retreat or reduces their morale to zero while locking player morale at 100%.

---

## 7. UI System & HUD Integration

### Core UI Framework
- **`view.main.VIEW`**: Top-level UI and state coordinator.
  - `VIEW.inters()`: `Interrupters` manager.
  - `VIEW.inters().section`: `InterGuisection` used to open modal and floating UI windows.
  - `VIEW.inters().debugpanel`: `IDebugPanel` (engine debug panel).
- **`view.interrupter.IDebugPanel`**:
  - `IDebugPanel.add(String name, ACTION action)`: Appends custom buttons to the in-game debug menu.
  - `IDebugPanel.add(String name, BOOLEAN_MUTABLE toggle)`: Adds toggleable checkboxes.
- **`util.gui.panel.GPanel`**: Window frame with title bar, close button, and inner body layout (`inner()`).
- **`snake2d.util.gui.GuiSection`**: Canvas for composable UI layouts with directional helpers:
  - `addRight(int pad, RENDEROBJ obj)`
  - `addDown(int pad, RENDEROBJ obj)`
  - `addGrid(...)`
- **`util.gui.misc.GButt.ButtPanel`**: Standard button with text label, hover effects, and click callbacks (`clickActionSet(ACTION)`).
- **`init.sprite.UI.UI.FONT()`**: Game fonts:
  - `UI.FONT().M`: Medium body text font.
  - `UI.FONT().H1`, `UI.FONT().H2`: Header fonts.
  - `UI.FONT().S`: Small subscript font.

### Modal Opening Pattern
```java
GuiSection root = new GuiSection();
GPanel panel = new GPanel(500, 400);
panel.setTitle("Syx Architect");
panel.setCloseAction(() -> VIEW.inters().section.close());
root.add(panel);

// Open modal
VIEW.inters().section.activate(root);
```

---

## 8. Summary of Sprint 1 Action Items

1. **Gradle Build System:** Set up `compileOnly` on `SongsOfSyx.jar` with Java 21 toolchain and Java 16/21 compatibility.
2. **Deploy Task:** Copy `SyxArchitect.jar` to `%APPDATA%\songsofsyx\mods\SyxArchitect\V71\script\` along with `_info.txt` and `assets/`.
3. **Core Classes:**
   - `ArchitectScript`: Entry point implementing `script.SCRIPT`.
   - `ArchitectInstance`: Runtime instance implementing `SCRIPT_INSTANCE` with strict zero-allocation in `update(double ds)`.
   - `ArchitectState`: Binary persistence via `FilePutter` / `FileGetter` with version header.
   - `CheatTiers`: Multi-tier enums for granular cheating.
   - `I18n`: Live English/German localization system.
