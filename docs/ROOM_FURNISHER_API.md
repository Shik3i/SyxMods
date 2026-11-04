# Songs of Syx (v0.71.44) - Room Furnisher & Structural Support API Specification

> **Target Version:** Songs of Syx `v0.71.44` (`VERSION_MAJOR = 71`, `VERSION_MINOR = 44`)  
> **Export Context:** Syx Architect API Export for Sister Project **"Syx Auto-Planner"**  
> **Source Verification:** Decompiled & audited against `SongsOfSyx-sources.jar` & `SongsOfSyx.jar`

---

## 1. Structural Support & Ceiling Isolation ("Yellow Area" Mechanics)

When dragging and planning a room indoors, Songs of Syx evaluates whether the room's ceiling can structurally hold without collapsing or generating exorbitant maintenance/construction costs. Tiles with insufficient ceiling support are marked in **yellow** (`GCOLOR.MAP().SOSO`).

### 1.1 Central Classes & Accessors
- **`settlement.room.main.placement.UtilExtraCost`**: Computes roof support and foundation costs.
- **`settlement.room.main.placement.RoomPlacer`**: Master placement controller (`SETT.ROOMS().placement.placer`).
- **`settlement.room.main.construction.ConstructionData`**: Bitmaps storing tile states:
  - `ConstructionData.dExpensive`: Tile bit indicating yellow/expensive unsupported ceiling.

```java
// Accessing active room placer and extra cost calculator
RoomPlacer placer = SETT.ROOMS().placement.placer;
UtilExtraCost cost = placer.cost();
```

### 1.2 Mathematical Support Calculation
Ceiling support is calculated in `UtilExtraCost.support(AREA a, int tx, int ty)` using a 16-ray tracer:
```java
// TileRayTracer(4) casts 16 discrete rays up to 4 tiles away
for (Ray r : tracer.rays()) {
    for (int i = 0; i < r.size(); i++) {
        int dx = tx + r.get(i).x();
        int dy = ty + r.get(i).y();
        if (!SETT.IN_BOUNDS(dx, dy))
            break;
        if (!a.is(dx, dy)) { // Ray has hit an unfloored tile / boundary / wall
            if (!SETT.ROOMS().map.is(dx, dy) && tracer.check(r.get(i))) {
                s += CLAMP.d((double)(3.5 - i) / 3.5, 0, 1);
            }
            break;
        }
    }
}
```

### 1.3 The Yellow/Expensive Threshold
In `UtilExtraCost.support(ROOMA a)`:
- If calculated support `v >= 1.0`: `ConstructionData.dExpensive.set(a, c, 0)` &rarr; **Normal / Supported (Green / Valid)**.
- If calculated support `v < 1.0`: `ConstructionData.dExpensive.set(a, c, 1)` &rarr; **Unsupported / Expensive (Yellow / SOSO Warning)**.

### 1.4 How to Read Support for a Draft Tile `(tx, ty)`
```java
// 1. Check whether tile is marked expensive/yellow:
boolean isYellow = ConstructionData.dExpensive.is(tx, ty, 1);
// Alternatively via UtilExtraCost:
boolean isUnsupported = placer.cost().is(tx, ty);

// 2. Read normalized support factor (0.0 to 1.0):
double supportNorm = placer.cost().get(tx, ty);
```

### 1.5 Programmatically Placing a Support Pillar / Strut
When a tile is excluded from the room's floor draft, it naturally acts as a wall/strut column (`!a.is(dx, dy)` stops the ray early and boosts `s >= 1.0` for all surrounding tiles).

```java
// To insert a support column inside a room draft:
placer.instance.clear(tx, ty); // Clears the room floor tile -> becomes structural pillar

// To remove a support column (restore room floor):
placer.instance.set(tx, ty);

// Recalculate support:
placer.cost().support();
```

---

## 2. Room Furnisher & Blueprint Placement (`settlement.room.main.furnisher.*`)

### 2.1 Accessing the Active Room Planner
During in-game room construction:
```java
// Placer state and active draft
RoomPlacer placer = SETT.ROOMS().placement.placer;
Instance draftInstance = placer.instance;
RoomBlueprintImp blueprint = placer.blueprint();
Furnisher furnisher = blueprint.constructor();
```

### 2.2 Inspecting Item Groups (`FurnisherItemGroup`) & Detecting Workstations vs. Efficiency vs. Storage
A `Furnisher` exposes its item groups via `furnisher.groups()` or `furnisher.pgroups()`. Each group contains one or more size variations and rotation angles.

```java
LIST<FurnisherItemGroup> groups = furnisher.groups();
LIST<FurnisherStat> stats = furnisher.stats();

for (FurnisherItemGroup group : groups) {
    String groupName = group.name.toString();
    
    // Check stat contributions:
    for (int si = 0; si < stats.size(); si++) {
        FurnisherStat stat = stats.get(si);
        double value = group.stat(si);
        
        if (value > 0) {
            // WORKSTATION (Requires workers / employees)
            if (stat instanceof FurnisherStat.FurnisherStatEmployees ||
                stat instanceof FurnisherStat.FurnisherStatEmployeesR ||
                stat.name().toString().contains("Employees") ||
                stat.name().toString().contains("Workers")) {
                // Group is a Workstation
            }
            
            // EFFICIENCY / AUXILIARIES (Boosts workstation output)
            else if (stat instanceof FurnisherStat.FurnisherStatRelative ||
                     stat.name().toString().contains("Efficiency")) {
                // Group is an Efficiency / Auxiliary upgrade
            }
            
            // CAPACITY / STORAGE
            else if (stat.name().toString().contains("Storage") ||
                     stat.name().toString().contains("Capacity") ||
                     stat.name().toString().contains("Area")) {
                // Group is Storage / Capacity
            }
        }
    }
}
```

### 2.3 Structure of `FurnisherItem` and `FurnisherItemTile`
A `FurnisherItem` represents a specific size and rotation of a group:
```java
int sizeIndex = 0; // standard variation
int rotIndex = 0;  // 0: 0°, 1: 90°, 2: 180°, 3: 270°
FurnisherItem item = group.item(sizeIndex, rotIndex);

int width = item.width();
int height = item.height();
int area = item.area;

// Tile matrix inside the item:
for (int ry = 0; ry < height; ry++) {
    for (int rx = 0; rx < width; rx++) {
        FurnisherItemTile tile = item.get(rx, ry);
        if (tile != null) {
            // Pathfinding availability:
            // AVAILABILITY.WALKABLE (0) vs solid/blocked (< 0)
            boolean isWalkable = tile.availability.player >= 0;
            
            // CRITICAL FOR REACHABILITY:
            // mustBeReachable indicates this tile (e.g. stool/bench/interaction point)
            // MUST be connected via walkable path to room doorways!
            boolean mustBeReachable = tile.mustBeReachable;
        }
    }
}
```

### 2.4 Legality Checks & Programmatic Placement in Placement Grid
Before placing an item, verify legality against the room boundary, other items, and accessibility.

#### Legality Check
```java
// Checks whole item placement legality at (tx, ty):
CharSequence problem = placer.placability.itemProblem(tx, ty, group, item, placer.instance);
if (problem == null) {
    // Placement is 100% legal!
} else {
    // problem describes why it cannot be placed (e.g. blocked, outside area, unreachable)
}

// Single-tile placability:
CharSequence tileProblem = placer.placability.itemPlacable(tx, ty, rx, ry, item, placer.instance);
```

#### Programmatic Placement
```java
// Place item into room draft:
SETT.ROOMS().fData.itemSet(tx, ty, item, placer.instance);
placer.history.placeItem(item, tx, ty, 1);

// Programmatic removal (undo):
SETT.ROOMS().fData.itemClear(tx, ty, placer.instance);
```

### 2.5 Reading Placed Doors (`PlacerDoor`) for BFS Reachability Flood-Fill
When validating reachability for `tile.mustBeReachable`, start pathfinding from the room's doorways.

```java
PlacerDoor doorManager = placer.door;
int totalDoors = doorManager.getOpenings();

List<COORDINATE> doorTiles = new ArrayList<>();

// Iterate around room boundary
for (COORDINATE c : placer.instance.body()) {
    for (DIR d : DIR.ORTHO) {
        int nx = c.x() + d.x();
        int ny = c.y() + d.y();
        if (doorManager.isOpening(nx, ny)) {
            // (nx, ny) is an active doorway!
            // (c.x, c.y) inside the room is the interior doorway step
            doorTiles.add(new Coo(c.x(), c.y()));
        }
    }
}

// Run BFS flood-fill from doorTiles across placer.instance
// using:
// boolean isWalkable = (itemTile == null || itemTile.availability.player >= 0);
```

---

## 3. Complete Minimal Auto-Planner Blueprint Example

```java
public class AutoPlannerPlacementHelper {

    public static void autoPlaceBestStrutAndFurnish(RoomPlacer placer) {
        Instance draft = placer.instance;
        UtilExtraCost cost = placer.cost();

        // 1. Fix all yellow support areas by placing struts
        for (COORDINATE c : draft.body()) {
            if (draft.is(c) && cost.is(c.x(), c.y())) {
                // Tile is yellow/unsupported: place a strut/pillar here
                draft.clear(c.x(), c.y());
                cost.support(); // Recalculate
            }
        }

        // 2. Identify primary workstation group
        Furnisher furnisher = placer.blueprint().constructor();
        FurnisherItemGroup workstationGroup = null;

        for (FurnisherItemGroup group : furnisher.groups()) {
            for (int si = 0; si < furnisher.stats().size(); si++) {
                if (group.stat(si) > 0 && furnisher.stats().get(si).name().toString().contains("Workers")) {
                    workstationGroup = group;
                    break;
                }
            }
            if (workstationGroup != null) break;
        }

        if (workstationGroup == null) return;

        // 3. Grid-scan and place workstations with 0° rotation
        FurnisherItem item = workstationGroup.item(0, 0);
        for (int y = draft.body().y1(); y <= draft.body().y2() - item.height(); y++) {
            for (int x = draft.body().x1(); x <= draft.body().x2() - item.width(); x++) {
                if (placer.placability.itemProblem(x, y, workstationGroup, item, draft) == null) {
                    SETT.ROOMS().fData.itemSet(x, y, item, draft);
                    placer.history.placeItem(item, x, y, 1);
                }
            }
        }
    }
}
```
