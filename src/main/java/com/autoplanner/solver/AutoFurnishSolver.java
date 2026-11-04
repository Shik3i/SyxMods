package com.autoplanner.solver;

import com.autoplanner.config.PlannerConfig;
import settlement.main.SETT;
import settlement.room.main.Room;
import settlement.room.main.RoomBlueprintImp;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemGroup;
import settlement.room.main.furnisher.FurnisherItemTile;
import settlement.room.main.placement.RoomPlacer;
import snake2d.util.datatypes.RECTANGLE;

/**
 * High-performance, 2-Pass Packing Solver for Songs of Syx rooms.
 * Packs maximal workstations and required efficiency modules with
 * BFS Flood-Fill pathfinding reachability guarantees from all doors.
 */
public final class AutoFurnishSolver {

    private final RoomGridSnapshot snapshot = new RoomGridSnapshot();
    private final ReachabilityChecker reachability = new ReachabilityChecker();

    // Workstation access tracking buffers (Zero-GC)
    private static final int MAX_WORKSTATIONS = 512;
    private final int[] accessX = new int[MAX_WORKSTATIONS * 4];
    private final int[] accessY = new int[MAX_WORKSTATIONS * 4];
    private int accessCount = 0;

    // Placed items buffer for final commit
    private static final int MAX_PLACED_ITEMS = 1024;
    private final int[] itemX = new int[MAX_PLACED_ITEMS];
    private final int[] itemY = new int[MAX_PLACED_ITEMS];
    private final FurnisherItem[] itemsPlaced = new FurnisherItem[MAX_PLACED_ITEMS];
    private int placedCount = 0;

    public static final class Result {
        public final boolean success;
        public final String errorMessage;
        public final int workstations;
        public final int efficiencyModules;
        public final int efficiencyPercent;
        public final long elapsedMs;

        public Result(boolean success, String errorMessage, int ws, int effMods, int effPercent, long elapsedMs) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.workstations = ws;
            this.efficiencyModules = effMods;
            this.efficiencyPercent = effPercent;
            this.elapsedMs = elapsedMs;
        }
    }

    /**
     * Solves and commits optimal furniture layout to the active room draft.
     */
    public synchronized Result solve(RoomPlacer placer, PlannerConfig config) {
        long start = System.currentTimeMillis();

        if (placer == null || placer.blueprint() == null || PlacerBridge.getArea(placer) <= 0) {
            return new Result(false, "No active room draft.", 0, 0, 0, 0);
        }

        RoomBlueprintImp bp = placer.blueprint();
        if (bp.constructor() == null || bp.constructor().pgroups().size() == 0) {
            return new Result(false, "Room blueprint has no furniture groups.", 0, 0, 0, 0);
        }

        // Initialize snapshot
        snapshot.initFrom(placer);

        if (snapshot.doorCount == 0 && bp.constructor().mustBeIndoors()) {
            return new Result(false, "msg.furnish_fail_doors", 0, 0, 0, 0);
        }

        // Clear existing furniture from snapshot and placer draft
        clearExistingFurniture(placer);
        snapshot.initFrom(placer);

        accessCount = 0;
        placedCount = 0;

        // Categorize groups: Workstations vs Efficiency / Aux
        FurnisherItemGroup workstationGroup = null;
        FurnisherItemGroup efficiencyGroup = null;
        FurnisherItemGroup storageGroup = null;

        for (int gi = 0; gi < bp.constructor().pgroups().size(); gi++) {
            FurnisherItemGroup g = bp.constructor().pgroups().get(gi);
            if (g == null) continue;

            FurnisherItem sample = g.item(0, 0);
            if (sample == null) continue;

            boolean hasMustBeReachable = false;
            for (int y = 0; y < sample.height(); y++) {
                for (int x = 0; x < sample.width(); x++) {
                    FurnisherItemTile t = sample.get(x, y);
                    if (t != null && t.mustBeReachable) {
                        hasMustBeReachable = true;
                        break;
                    }
                }
                if (hasMustBeReachable) break;
            }

            if (hasMustBeReachable) {
                if (workstationGroup == null) {
                    workstationGroup = g;
                }
            } else {
                String nameStr = g.name != null ? g.name.toString().toLowerCase() : "";
                if (nameStr.contains("crate") || nameStr.contains("storage") || nameStr.contains("lager")) {
                    storageGroup = g;
                } else if (efficiencyGroup == null) {
                    efficiencyGroup = g;
                }
            }
        }

        // If no workstation group found, use group 0
        if (workstationGroup == null && bp.constructor().pgroups().size() > 0) {
            workstationGroup = bp.constructor().pgroups().get(0);
        }

        if (workstationGroup == null) {
            return new Result(false, "msg.furnish_no_workstations", 0, 0, 0, 0);
        }

        int x1 = snapshot.getOriginX();
        int y1 = snapshot.getOriginY();
        int x2 = x1 + snapshot.getWidth();
        int y2 = y1 + snapshot.getHeight();

        int effModsPlaced = 0;
        int workstationsPlaced = 0;

        // --- PASS 1: Place Efficiency Modules along walls / perimeter ---
        if (efficiencyGroup != null) {
            int targetEffMods = Math.max(1, (int) Math.round(snapshot.getRoomTileCount() * 0.05 * config.getTargetEfficiency()));
            FurnisherItem effItem = efficiencyGroup.item(0, 0);

            if (effItem != null) {
                // Perimeter scan (tiles adjacent to walls)
                for (int ty = y1; ty < y2 && effModsPlaced < targetEffMods; ty++) {
                    for (int tx = x1; tx < x2 && effModsPlaced < targetEffMods; tx++) {
                        if (isAdjacentToWall(tx, ty) && canFitItem(snapshot, tx, ty, effItem)) {
                            if (reachability.canPlaceWithoutBlocking(snapshot, tx, ty, effItem, accessX, accessY, accessCount)) {
                                placeItemInSnapshot(tx, ty, effItem);
                                effModsPlaced++;
                            }
                        }
                    }
                }
            }
        }

        // --- PASS 2: Pack Maximal Workstations ---
        int bestSize = 0; // Use base size
        int rots = 4;

        int stepX = 1;
        int stepY = 1;

        for (int ty = y1; ty < y2; ty += stepY) {
            for (int tx = x1; tx < x2; tx += stepX) {
                if (snapshot.get(tx, ty) != RoomGridSnapshot.TILE_ROOM_FLOOR) {
                    continue;
                }

                // Try each rotation
                for (int r = 0; r < rots; r++) {
                    FurnisherItem wsItem = null;
                    try {
                        wsItem = workstationGroup.item(bestSize, r);
                    } catch (Exception ignored) {
                        break;
                    }
                    if (wsItem == null) continue;

                    if (canFitItem(snapshot, tx, ty, wsItem)) {
                        if (reachability.canPlaceWithoutBlocking(snapshot, tx, ty, wsItem, accessX, accessY, accessCount)) {
                            // Register access point(s)
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

                            placeItemInSnapshot(tx, ty, wsItem);
                            workstationsPlaced++;
                            break; // Successfully placed in this cell, proceed to next
                        }
                    }
                }
            }
        }

        // --- PASS 3: Optional Storage Crates in remaining empty spots ---
        if (storageGroup != null && config.getStorageRatio() > 0.0) {
            int maxStorage = Math.max(1, (int) (snapshot.getRoomTileCount() * config.getStorageRatio()));
            int storagePlaced = 0;
            FurnisherItem crateItem = storageGroup.item(0, 0);

            if (crateItem != null) {
                for (int ty = y1; ty < y2 && storagePlaced < maxStorage; ty++) {
                    for (int tx = x1; tx < x2 && storagePlaced < maxStorage; tx++) {
                        if (canFitItem(snapshot, tx, ty, crateItem)) {
                            if (reachability.canPlaceWithoutBlocking(snapshot, tx, ty, crateItem, accessX, accessY, accessCount)) {
                                placeItemInSnapshot(tx, ty, crateItem);
                                storagePlaced++;
                            }
                        }
                    }
                }
            }
        }

        // --- COMMIT PHASE: Apply to game room draft ---
        Room.RoomInstanceImp ins = PlacerBridge.getInstance(placer);
        for (int i = 0; i < placedCount; i++) {
            int px = itemX[i];
            int py = itemY[i];
            FurnisherItem item = itemsPlaced[i];
            SETT.ROOMS().fData.itemSet(px, py, item, ins);
            PlacerBridge.recordHistoryItem(placer, item, px, py, 1);
        }

        PlacerBridge.updateResources(placer);

        int efficiencyPercent = Math.min(100, (int) Math.round(100.0 * config.getTargetEfficiency()));
        long elapsed = System.currentTimeMillis() - start;

        return new Result(true, null, workstationsPlaced, effModsPlaced, efficiencyPercent, elapsed);
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

    private void placeItemInSnapshot(int tx, int ty, FurnisherItem item) {
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

        if (placedCount < MAX_PLACED_ITEMS) {
            itemX[placedCount] = tx;
            itemY[placedCount] = ty;
            itemsPlaced[placedCount] = item;
            placedCount++;
        }
    }

    private boolean isAdjacentToWall(int tx, int ty) {
        return snapshot.get(tx + 1, ty) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx - 1, ty) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx, ty + 1) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx, ty - 1) == RoomGridSnapshot.TILE_WALL
            || snapshot.get(tx + 1, ty) == RoomGridSnapshot.TILE_STRUT
            || snapshot.get(tx - 1, ty) == RoomGridSnapshot.TILE_STRUT
            || snapshot.get(tx, ty + 1) == RoomGridSnapshot.TILE_STRUT
            || snapshot.get(tx, ty - 1) == RoomGridSnapshot.TILE_STRUT;
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
        PlacerBridge.updateResources(placer);
    }
}
