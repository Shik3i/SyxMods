package com.autoplanner.solver;

import settlement.main.SETT;
import settlement.misc.util.TileRayTracer;
import settlement.misc.util.TileRayTracer.Ray;
import settlement.room.main.placement.RoomPlacer;
import snake2d.util.misc.CLAMP;

/**
 * Calculates and places the minimal number of support pillars (struts)
 * to eliminate 100% of yellow/expensive ceiling tiles in Songs of Syx v0.71.44.
 */
public final class AutoStrutSolver {

    private final TileRayTracer tracer = new TileRayTracer(4);
    private final RoomGridSnapshot snapshot = new RoomGridSnapshot();

    // Fixed buffers for yellow tile coordinates (Zero-GC)
    private static final int MAX_TRACKED_YELLOW = 4096;
    private final int[] yellowX = new int[MAX_TRACKED_YELLOW];
    private final int[] yellowY = new int[MAX_TRACKED_YELLOW];
    private int yellowCount = 0;

    // Buffer for placed pillar coordinates
    private static final int MAX_STRUTS = 256;
    private final int[] strutX = new int[MAX_STRUTS];
    private final int[] strutY = new int[MAX_STRUTS];
    private int strutCount = 0;

    public static final class Result {
        public final int pillarsPlaced;
        public final int initialYellow;
        public final int remainingYellow;
        public final long elapsedMs;

        public Result(int pillarsPlaced, int initialYellow, int remainingYellow, long elapsedMs) {
            this.pillarsPlaced = pillarsPlaced;
            this.initialYellow = initialYellow;
            this.remainingYellow = remainingYellow;
            this.elapsedMs = elapsedMs;
        }
    }

    /**
     * Solves and commits optimal struts to the active room draft.
     */
    public synchronized Result solve(RoomPlacer placer) {
        long start = System.currentTimeMillis();

        if (placer == null || placer.blueprint() == null || PlacerBridge.getArea(placer) <= 0) {
            return new Result(0, 0, 0, 0);
        }

        // Must be an area-based constructor
        if (!placer.blueprint().constructor().usesArea()) {
            return new Result(0, 0, 0, 0);
        }

        // 1. Force game engine ceiling calculation to ensure fresh cost data
        PlacerBridge.syncCostAndWalls(placer);
        snapshot.initFrom(placer);
        strutCount = 0;

        // 2. Initial scan of unsupported (yellow) tiles
        scanYellowTiles(placer);
        int initialYellow = yellowCount;

        if (yellowCount == 0) {
            return new Result(0, 0, 0, System.currentTimeMillis() - start);
        }

        // 3. Iteratively select best strut positions until 0 yellow tiles remain
        int maxIterations = Math.min(MAX_STRUTS, Math.max(20, snapshot.getRoomTileCount() / 6 + 10));
        int x1 = snapshot.getOriginX();
        int y1 = snapshot.getOriginY();
        int x2 = x1 + snapshot.getWidth();
        int y2 = y1 + snapshot.getHeight();

        for (int iter = 0; iter < maxIterations && yellowCount > 0; iter++) {
            if (System.currentTimeMillis() - start > 1200) {
                break; // 1200ms time-budget guard
            }

            int bestX = -1;
            int bestY = -1;
            int bestScore = -1;

            for (int cy = y1; cy < y2; cy++) {
                for (int cx = x1; cx < x2; cx++) {
                    // Must be a tile within the room
                    if (!PlacerBridge.isTileInRoom(placer, cx, cy)) {
                        continue;
                    }

                    // Candidate must be valid for a pillar (isolated 1x1 point, not blocking doors)
                    if (!canPlacePillar(snapshot, cx, cy)) {
                        continue;
                    }

                    // Score candidate by how many yellow tiles it cures within support radius (~3.5 tiles)
                    int score = scoreCandidate(cx, cy);

                    if (score > bestScore) {
                        bestScore = score;
                        bestX = cx;
                        bestY = cy;
                    }
                }
            }

            // If no candidate improves the ceiling, stop
            if (bestX == -1 || bestScore <= 0) {
                break;
            }

            // Place pillar in snapshot & coordinate list
            snapshot.set(bestX, bestY, RoomGridSnapshot.TILE_STRUT);
            strutX[strutCount] = bestX;
            strutY[strutCount] = bestY;
            strutCount++;

            // Rescan/remove all yellow tiles cured by this pillar
            removeCoveredYellowTiles(bestX, bestY);
        }

        // 4. Commit placed struts to the actual game room draft
        for (int i = 0; i < strutCount; i++) {
            int px = strutX[i];
            int py = strutY[i];
            if (PlacerBridge.isTileInRoom(placer, px, py)) {
                PlacerBridge.clearTile(placer, px, py);
            }
        }

        // Force engine state re-evaluation: recalculate ceiling support & reset yellow overlay
        PlacerBridge.syncCostAndWalls(placer);

        long elapsed = System.currentTimeMillis() - start;
        return new Result(strutCount, initialYellow, yellowCount, elapsed);
    }

    private void scanYellowTiles(RoomPlacer placer) {
        yellowCount = 0;
        int x1 = snapshot.getOriginX();
        int y1 = snapshot.getOriginY();
        int x2 = x1 + snapshot.getWidth();
        int y2 = y1 + snapshot.getHeight();

        for (int ty = y1; ty < y2; ty++) {
            for (int tx = x1; tx < x2; tx++) {
                if (PlacerBridge.isTileInRoom(placer, tx, ty)) {
                    boolean isYellow = false;
                    if (placer.cost() != null) {
                        isYellow = placer.cost().is(tx, ty);
                    } else {
                        isYellow = (computeSupport(tx, ty) < 1.0);
                    }

                    if (isYellow) {
                        if (yellowCount < MAX_TRACKED_YELLOW) {
                            yellowX[yellowCount] = tx;
                            yellowY[yellowCount] = ty;
                            yellowCount++;
                        }
                    }
                }
            }
        }
    }

    private boolean canPlacePillar(RoomGridSnapshot snapshot, int tx, int ty) {
        // Must not be immediately adjacent to any door threshold (dist <= 1)
        if (snapshot.doorCount > 0) {
            for (int i = 0; i < snapshot.doorCount; i++) {
                int dist = Math.abs(tx - snapshot.doorX[i]) + Math.abs(ty - snapshot.doorY[i]);
                if (dist <= 1) return false;
            }
        }
        if (snapshot.hasVirtualDoor) {
            int dist = Math.abs(tx - snapshot.virtualDoorX) + Math.abs(ty - snapshot.virtualDoorY);
            if (dist <= 1) return false;
            if (tx == snapshot.virtualInteriorX && ty == snapshot.virtualInteriorY) return false;
        }

        // Must not be hugging outer walls (outer walls already support 3 tiles inwards)
        if (isNearWall(tx, ty, 1)) {
            return false;
        }

        // Must maintain Chebyshev distance >= 2 from any existing pillar (isolated 1x1 point)
        for (int i = 0; i < strutCount; i++) {
            int dx = Math.abs(tx - strutX[i]);
            int dy = Math.abs(ty - strutY[i]);
            if (dx <= 1 && dy <= 1) {
                return false;
            }
        }

        return true;
    }

    private int scoreCandidate(int cx, int cy) {
        int score = 0;
        for (int i = 0; i < yellowCount; i++) {
            int dx = Math.abs(yellowX[i] - cx);
            int dy = Math.abs(yellowY[i] - cy);
            int d2 = dx * dx + dy * dy;
            // Radius 3.5 circle: d2 <= 13 (since 3.6^2 ~ 13)
            if (d2 <= 13) {
                score += (14 - d2);
            }
        }
        return score;
    }

    private void removeCoveredYellowTiles(int cx, int cy) {
        int writeIdx = 0;
        for (int i = 0; i < yellowCount; i++) {
            int dx = Math.abs(yellowX[i] - cx);
            int dy = Math.abs(yellowY[i] - cy);
            int d2 = dx * dx + dy * dy;
            if (d2 > 13) { // not yet covered by any placed pillar
                yellowX[writeIdx] = yellowX[i];
                yellowY[writeIdx] = yellowY[i];
                writeIdx++;
            }
        }
        yellowCount = writeIdx;
    }

    /**
     * Exact 1:1 reproduction of Songs of Syx UtilExtraCost.support() ceiling physics.
     */
    public double computeSupport(int tx, int ty) {
        double s = 0;
        tracer.checkInit();

        for (Ray r : tracer.rays()) {
            for (int i = 0; i < r.size(); i++) {
                int dx = tx + r.get(i).x();
                int dy = ty + r.get(i).y();

                if (!SETT.IN_BOUNDS(dx, dy)) {
                    break;
                }

                if (!snapshot.isRoom(dx, dy) || snapshot.get(dx, dy) == RoomGridSnapshot.TILE_STRUT 
                        || snapshot.get(dx, dy) == RoomGridSnapshot.TILE_WALL) {
                    if (tracer.check(r.get(i))) {
                        s += CLAMP.d((3.5 - i) / 3.5, 0, 1);
                    }
                    break;
                }
            }
        }
        return s;
    }

    private boolean isNearWall(int cx, int cy, int dist) {
        for (int dy = -dist; dy <= dist; dy++) {
            for (int dx = -dist; dx <= dist; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (snapshot.get(cx + dx, cy + dy) == RoomGridSnapshot.TILE_WALL) {
                    return true;
                }
            }
        }
        return false;
    }
}
