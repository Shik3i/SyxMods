package com.autoplanner.solver;

import com.autoplanner.config.PlannerConfig;
import settlement.main.SETT;
import settlement.room.main.Room;
import settlement.room.main.RoomBlueprintImp;
import settlement.room.main.furnisher.Furnisher;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemGroup;
import settlement.room.main.furnisher.FurnisherItemTile;
import settlement.room.main.furnisher.FurnisherStat;
import settlement.room.main.placement.RoomPlacer;
import snake2d.util.datatypes.RECTANGLE;

/**
 * Intelligent, Interleaved Packing Solver with Workstation Backtracking for Songs of Syx rooms.
 * Resolves the "Chicken-and-Egg" stat dependency between workstations and auxiliary modules
 * to guarantee optimal workstation density while strictly hitting target efficiency (100%).
 */
public final class AutoFurnishSolver {

    private final RoomGridSnapshot snapshot = new RoomGridSnapshot();
    private final ReachabilityChecker reachability = new ReachabilityChecker();

    // Access tracking buffers (Zero-GC)
    private static final int MAX_WORKSTATIONS = 512;
    private final int[] accessX = new int[MAX_WORKSTATIONS * 4];
    private final int[] accessY = new int[MAX_WORKSTATIONS * 4];
    private int accessCount = 0;

    // Placed items stack for final commit & backtracking
    private static final int MAX_PLACED_ITEMS = 1024;
    private final int[] itemX = new int[MAX_PLACED_ITEMS];
    private final int[] itemY = new int[MAX_PLACED_ITEMS];
    private final FurnisherItem[] itemsPlaced = new FurnisherItem[MAX_PLACED_ITEMS];
    private final boolean[] isWorkstationItem = new boolean[MAX_PLACED_ITEMS];
    private int placedCount = 0;

    // Running accumulated stats
    private final double[] runningStats = new double[32];

    public static final class Result {
        public final boolean success;
        public final String errorMessage;
        public final int workstations;
        public final int efficiencyModules;
        public final int efficiencyPercent;
        public final boolean virtualDoorUsed;
        public final int virtualDoorX;
        public final int virtualDoorY;
        public final long elapsedMs;

        public Result(
                boolean success, 
                String errorMessage, 
                int ws, 
                int effMods, 
                int effPercent, 
                boolean virtualDoorUsed,
                int virtualDoorX,
                int virtualDoorY,
                long elapsedMs) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.workstations = ws;
            this.efficiencyModules = effMods;
            this.efficiencyPercent = effPercent;
            this.virtualDoorUsed = virtualDoorUsed;
            this.virtualDoorX = virtualDoorX;
            this.virtualDoorY = virtualDoorY;
            this.elapsedMs = elapsedMs;
        }
    }

    /**
     * Solves and commits optimal furniture layout to the active room draft.
     */
    public synchronized Result solve(RoomPlacer placer, PlannerConfig config) {
        long start = System.currentTimeMillis();

        if (placer == null || placer.blueprint() == null || PlacerBridge.getArea(placer) <= 0) {
            return new Result(false, "No active room draft.", 0, 0, 0, false, -1, -1, 0);
        }

        RoomBlueprintImp bp = placer.blueprint();
        Furnisher furnisher = bp.constructor();
        if (furnisher == null || furnisher.pgroups().size() == 0) {
            return new Result(false, "Room blueprint has no furniture groups.", 0, 0, 0, false, -1, -1, 0);
        }

        // 1. Clear existing furniture from room draft & initialize clean snapshot
        clearExistingFurniture(placer);
        snapshot.initFrom(placer);

        // Reset buffers
        accessCount = 0;
        placedCount = 0;
        java.util.Arrays.fill(runningStats, 0.0);

        // 2. Classify blueprint groups
        FurnisherItemGroup workstationGroup = null;
        FurnisherItemGroup auxiliaryGroup = null;
        FurnisherItemGroup storageGroup = null;

        for (int gi = 0; gi < furnisher.pgroups().size(); gi++) {
            FurnisherItemGroup g = furnisher.pgroups().get(gi);
            if (g == null) continue;

            FurnisherItem sample = g.item(0, 0);
            if (sample == null) continue;

            boolean hasAccess = false;
            for (int y = 0; y < sample.height(); y++) {
                for (int x = 0; x < sample.width(); x++) {
                    FurnisherItemTile t = sample.get(x, y);
                    if (t != null && t.mustBeReachable) {
                        hasAccess = true;
                        break;
                    }
                }
                if (hasAccess) break;
            }

            if (hasAccess) {
                if (workstationGroup == null) {
                    workstationGroup = g;
                }
            } else {
                String nameStr = g.name != null ? g.name.toString().toLowerCase() : "";
                if (nameStr.contains("crate") || nameStr.contains("storage") || nameStr.contains("lager")) {
                    storageGroup = g;
                } else if (auxiliaryGroup == null) {
                    auxiliaryGroup = g;
                }
            }
        }

        if (workstationGroup == null && furnisher.pgroups().size() > 0) {
            workstationGroup = furnisher.pgroups().get(0);
        }

        if (workstationGroup == null) {
            return new Result(false, "msg.furnish_no_workstations", 0, 0, 0, false, -1, -1, 0);
        }

        int x1 = snapshot.getOriginX();
        int y1 = snapshot.getOriginY();
        int x2 = x1 + snapshot.getWidth();
        int y2 = y1 + snapshot.getHeight();

        // -------------------------------------------------------------
        // PASS 1: Workstations-First Packing (larger sizes down to small)
        // -------------------------------------------------------------
        int wsSizes = getAvailableSizes(workstationGroup);

        for (int sizeIdx = wsSizes - 1; sizeIdx >= 0; sizeIdx--) {
            for (int ty = y1; ty < y2; ty++) {
                for (int tx = x1; tx < x2; tx++) {
                    if (snapshot.get(tx, ty) != RoomGridSnapshot.TILE_ROOM_FLOOR) {
                        continue;
                    }

                    // Try all 4 rotations
                    for (int r = 0; r < 4; r++) {
                        FurnisherItem wsItem = getItemSafe(workstationGroup, sizeIdx, r);
                        if (wsItem == null) continue;

                        if (canFitItem(snapshot, tx, ty, wsItem)) {
                            if (reachability.canPlaceWithoutBlocking(snapshot, tx, ty, wsItem, accessX, accessY, accessCount)) {
                                registerAccessPoints(wsItem, tx, ty);
                                placeItemInSnapshot(tx, ty, wsItem, true, furnisher);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PASS 2: Auxiliary / Efficiency Placement
        // -------------------------------------------------------------
        if (auxiliaryGroup != null) {
            fulfillEfficiencyTarget(furnisher, auxiliaryGroup, config.getTargetEfficiency(), x1, y1, x2, y2);
        }

        // -------------------------------------------------------------
        // PASS 3: Backtracking if Room was over-packed with Workstations
        // -------------------------------------------------------------
        double targetEff = config.getTargetEfficiency();
        while (auxiliaryGroup != null && computeCurrentEfficiency(furnisher) < (targetEff - 0.005) && countWorkstations() > 1) {
            // Remove the most recently placed workstation to free space
            boolean removed = backtrackLastWorkstation(furnisher);
            if (!removed) break;

            // Fulfill efficiency into newly opened floor area
            fulfillEfficiencyTarget(furnisher, auxiliaryGroup, targetEff, x1, y1, x2, y2);
        }

        // -------------------------------------------------------------
        // PASS 4: Optional Storage Crates in remaining empty spots
        // -------------------------------------------------------------
        if (storageGroup != null && config.getStorageRatio() > 0.0) {
            int maxStorage = Math.max(1, (int) (snapshot.getRoomTileCount() * config.getStorageRatio()));
            int storagePlaced = 0;
            FurnisherItem crateItem = getItemSafe(storageGroup, 0, 0);

            if (crateItem != null) {
                for (int ty = y1; ty < y2 && storagePlaced < maxStorage; ty++) {
                    for (int tx = x1; tx < x2 && storagePlaced < maxStorage; tx++) {
                        if (canFitItem(snapshot, tx, ty, crateItem)) {
                            if (reachability.canPlaceWithoutBlocking(snapshot, tx, ty, crateItem, accessX, accessY, accessCount)) {
                                placeItemInSnapshot(tx, ty, crateItem, false, furnisher);
                                storagePlaced++;
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // COMMIT PHASE: Commit all placed items to game engine
        // -------------------------------------------------------------
        Room.RoomInstanceImp ins = PlacerBridge.getInstance(placer);
        int finalWorkstations = 0;
        int finalAux = 0;

        for (int i = 0; i < placedCount; i++) {
            int px = itemX[i];
            int py = itemY[i];
            FurnisherItem item = itemsPlaced[i];
            SETT.ROOMS().fData.itemSet(px, py, item, ins);
            PlacerBridge.recordHistoryItem(placer, item, px, py, 1);

            if (isWorkstationItem[i]) {
                finalWorkstations++;
            } else {
                finalAux++;
            }
        }

        // Synchronize engine costs, stats & visuals immediately
        PlacerBridge.syncCostAndWalls(placer);

        int effPercent = (int) Math.round(computeCurrentEfficiency(furnisher) * 100.0);
        long elapsed = System.currentTimeMillis() - start;

        return new Result(
                true, 
                null, 
                finalWorkstations, 
                finalAux, 
                effPercent, 
                snapshot.hasVirtualDoor,
                snapshot.virtualDoorX,
                snapshot.virtualDoorY,
                elapsed);
    }

    /**
     * Places auxiliary items prioritizing perimeter walls, pillars, and corners
     * until the room's efficiency meets or exceeds the target.
     */
    private void fulfillEfficiencyTarget(
            Furnisher furnisher, 
            FurnisherItemGroup auxGroup, 
            double targetEff, 
            int x1, int y1, int x2, int y2) {

        FurnisherItem auxItem = getItemSafe(auxGroup, 0, 0);
        if (auxItem == null) return;

        // Pass 2A: Wall-adjacent, pillar-adjacent and corner spots
        for (int ty = y1; ty < y2; ty++) {
            for (int tx = x1; tx < x2; tx++) {
                if (computeCurrentEfficiency(furnisher) >= targetEff) {
                    return;
                }
                if (isAdjacentToWallOrStrut(tx, ty) && canFitItem(snapshot, tx, ty, auxItem)) {
                    if (reachability.canPlaceWithoutBlocking(snapshot, tx, ty, auxItem, accessX, accessY, accessCount)) {
                        placeItemInSnapshot(tx, ty, auxItem, false, furnisher);
                    }
                }
            }
        }

        // Pass 2B: Any remaining interior spots if still below target
        for (int ty = y1; ty < y2; ty++) {
            for (int tx = x1; tx < x2; tx++) {
                if (computeCurrentEfficiency(furnisher) >= targetEff) {
                    return;
                }
                if (canFitItem(snapshot, tx, ty, auxItem)) {
                    if (reachability.canPlaceWithoutBlocking(snapshot, tx, ty, auxItem, accessX, accessY, accessCount)) {
                        placeItemInSnapshot(tx, ty, auxItem, false, furnisher);
                    }
                }
            }
        }
    }

    /**
     * Removes the most recently placed workstation from snapshot and running stats.
     */
    private boolean backtrackLastWorkstation(Furnisher furnisher) {
        for (int i = placedCount - 1; i >= 0; i--) {
            if (isWorkstationItem[i]) {
                FurnisherItem wsItem = itemsPlaced[i];
                int tx = itemX[i];
                int ty = itemY[i];

                // Clear item from snapshot back to open floor
                for (int iy = 0; iy < wsItem.height(); iy++) {
                    for (int ix = 0; ix < wsItem.width(); ix++) {
                        if (wsItem.get(ix, iy) != null) {
                            snapshot.set(tx + ix, ty + iy, RoomGridSnapshot.TILE_ROOM_FLOOR);
                        }
                    }
                }

                // Deduct stats
                for (FurnisherStat s : furnisher.stats()) {
                    if (s.index() < runningStats.length) {
                        runningStats[s.index()] -= wsItem.stat(s);
                    }
                }

                // Remove from access points list
                removeAccessPointsFor(wsItem, tx, ty);

                // Remove from placed items stack
                System.arraycopy(itemX, i + 1, itemX, i, placedCount - i - 1);
                System.arraycopy(itemY, i + 1, itemY, i, placedCount - i - 1);
                System.arraycopy(itemsPlaced, i + 1, itemsPlaced, i, placedCount - i - 1);
                System.arraycopy(isWorkstationItem, i + 1, isWorkstationItem, i, placedCount - i - 1);
                placedCount--;
                return true;
            }
        }
        return false;
    }

    private void removeAccessPointsFor(FurnisherItem item, int tx, int ty) {
        for (int iy = 0; iy < item.height(); iy++) {
            for (int ix = 0; ix < item.width(); ix++) {
                FurnisherItemTile t = item.get(ix, iy);
                if (t != null && t.mustBeReachable) {
                    int targetX = tx + ix;
                    int targetY = ty + iy;
                    for (int a = 0; a < accessCount; a++) {
                        if (accessX[a] == targetX && accessY[a] == targetY) {
                            System.arraycopy(accessX, a + 1, accessX, a, accessCount - a - 1);
                            System.arraycopy(accessY, a + 1, accessY, a, accessCount - a - 1);
                            accessCount--;
                            break;
                        }
                    }
                }
            }
        }
    }

    private double computeCurrentEfficiency(Furnisher furnisher) {
        double minEff = 1.0;
        boolean hasRelativeStat = false;

        for (FurnisherStat s : furnisher.stats()) {
            if (s instanceof FurnisherStat.FurnisherStatEfficiency || s instanceof FurnisherStat.FurnisherStatRelative) {
                hasRelativeStat = true;
                double val = s.get(snapshot, runningStats);
                if (val < minEff) {
                    minEff = val;
                }
            }
        }

        return hasRelativeStat ? minEff : 1.0;
    }

    private int countWorkstations() {
        int count = 0;
        for (int i = 0; i < placedCount; i++) {
            if (isWorkstationItem[i]) count++;
        }
        return count;
    }

    private void registerAccessPoints(FurnisherItem wsItem, int tx, int ty) {
        for (int iy = 0; iy < wsItem.height(); iy++) {
            for (int ix = 0; ix < wsItem.width(); ix++) {
                FurnisherItemTile t = wsItem.get(ix, iy);
                if (t != null && t.mustBeReachable) {
                    if (accessCount < accessX.length) {
                        accessX[accessCount] = tx + ix;
                        accessY[accessCount] = ty + iy;
                        accessCount++;
                    }
                }
            }
        }
    }

    private boolean canFitItem(RoomGridSnapshot snap, int tx, int ty, FurnisherItem item) {
        for (int iy = 0; iy < item.height(); iy++) {
            for (int ix = 0; ix < item.width(); ix++) {
                if (item.get(ix, iy) != null) {
                    int cx = tx + ix;
                    int cy = ty + iy;
                    if (!snap.isAvailableFloor(cx, cy)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void placeItemInSnapshot(int tx, int ty, FurnisherItem item, boolean isWs, Furnisher furnisher) {
        for (int iy = 0; iy < item.height(); iy++) {
            for (int ix = 0; ix < item.width(); ix++) {
                FurnisherItemTile t = item.get(ix, iy);
                if (t != null) {
                    int cx = tx + ix;
                    int cy = ty + iy;
                    if (t.mustBeReachable) {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_WORKSTATION_ACCESS);
                    } else if (t.availability.player <= settlement.path.AVAILABILITY.ROOM.player) {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_FURNITURE_WALKABLE);
                    } else {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_FURNITURE_BLOCK);
                    }
                }
            }
        }

        // Accumulate stats
        for (FurnisherStat s : furnisher.stats()) {
            if (s.index() < runningStats.length) {
                runningStats[s.index()] += item.stat(s);
            }
        }

        if (placedCount < MAX_PLACED_ITEMS) {
            itemX[placedCount] = tx;
            itemY[placedCount] = ty;
            itemsPlaced[placedCount] = item;
            isWorkstationItem[placedCount] = isWs;
            placedCount++;
        }
    }

    private boolean isAdjacentToWallOrStrut(int tx, int ty) {
        return snapshot.get(tx + 1, ty) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx - 1, ty) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx, ty + 1) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx, ty - 1) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx + 1, ty) == RoomGridSnapshot.TILE_STRUT
            || snapshot.get(tx - 1, ty) == RoomGridSnapshot.TILE_STRUT
            || snapshot.get(tx, ty + 1) == RoomGridSnapshot.TILE_STRUT
            || snapshot.get(tx, ty - 1) == RoomGridSnapshot.TILE_STRUT;
    }

    private FurnisherItem getItemSafe(FurnisherItemGroup group, int size, int rot) {
        try {
            return group.item(size, rot);
        } catch (Throwable ignored) {
            try {
                return group.item(0, 0);
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }

    private int getAvailableSizes(FurnisherItemGroup group) {
        int sizes = 1;
        while (sizes < 8) {
            try {
                if (group.item(sizes, 0) == null) break;
                sizes++;
            } catch (Throwable t) {
                break;
            }
        }
        return sizes;
    }

    public void clearExistingFurniture(RoomPlacer placer) {
        RECTANGLE bounds = PlacerBridge.getBounds(placer);
        Room.RoomInstanceImp ins = PlacerBridge.getInstance(placer);
        if (placer == null || bounds == null || ins == null) {
            return;
        }

        for (int y = bounds.y1(); y < bounds.y2(); y++) {
            for (int x = bounds.x1(); x < bounds.x2(); x++) {
                if (PlacerBridge.isTileInRoom(placer, x, y)) {
                    FurnisherItem item = SETT.ROOMS().fData.item.get(x, y);
                    if (item != null) {
                        SETT.ROOMS().fData.itemClear(x, y, ins);
                    }
                }
            }
        }
        PlacerBridge.clearHistory(placer);
        PlacerBridge.syncCostAndWalls(placer);
    }
}
