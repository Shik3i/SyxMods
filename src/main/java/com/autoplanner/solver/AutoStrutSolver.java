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
    private final ReachabilityChecker reachability = new ReachabilityChecker();

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

        // Only indoor rooms with area require structural ceiling support
        if (!placer.blueprint().constructor().mustBeIndoors() || !placer.blueprint().constructor().usesArea()) {
            return new Result(0, 0, 0, 0);
        }

        snapshot.initFrom(placer);
        strutCount = 0;

        // 1. Initial scan of unsupported (yellow) tiles
        scanYellowTiles();
        int initialYellow = yellowCount;

        if (yellowCount == 0) {
            return new Result(0, 0, 0, System.currentTimeMillis() - start);
        }

        // 2. Iteratively select best strut positions until 0 yellow tiles remain
        int maxIterations = Math.min(MAX_STRUTS, Math.max(1, snapshot.getRoomTileCount() / 10 + 5));
        for (int iter = 0; iter < maxIterations && yellowCount > 0; iter++) {
            int bestX = -1;
            int bestY = -1;
            int bestScore = -1;

            // Search bounding box of room floor
            int x1 = snapshot.getOriginX();
            int y1 = snapshot.getOriginY();
            int x2 = x1 + snapshot.getWidth();
            int y2 = y1 + snapshot.getHeight();

            for (int cy = y1; cy < y2; cy++) {
                for (int cx = x1; cx < x2; cx++) {
                    if (snapshot.get(cx, cy) != RoomGridSnapshot.TILE_ROOM_FLOOR) {
                        continue;
                    }

                    // Candidate must not block doors or divide the room
                    if (!reachability.canPlaceStrutWithoutBlocking(snapshot, cx, cy)) {
                        continue;
                    }

                    // Score candidate by how many yellow tiles it cures
                    int score = evaluateCandidate(cx, cy);
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

            // Place pillar in snapshot
            snapshot.set(bestX, bestY, RoomGridSnapshot.TILE_STRUT);
            strutX[strutCount] = bestX;
            strutY[strutCount] = bestY;
            strutCount++;

            // Rescan remaining yellow tiles
            scanYellowTiles();
        }

        // 3. Commit placed struts to the actual game room draft
        for (int i = 0; i < strutCount; i++) {
            int px = strutX[i];
            int py = strutY[i];
            if (PlacerBridge.isTileInRoom(placer, px, py)) {
                PlacerBridge.clearTile(placer, px, py);
            }
        }

        if (strutCount > 0) {
            PlacerBridge.updateResources(placer);
        }

        long elapsed = System.currentTimeMillis() - start;
        return new Result(strutCount, initialYellow, yellowCount, elapsed);
    }

    private void scanYellowTiles() {
        yellowCount = 0;
        int x1 = snapshot.getOriginX();
        int y1 = snapshot.getOriginY();
        int x2 = x1 + snapshot.getWidth();
        int y2 = y1 + snapshot.getHeight();

        for (int ty = y1; ty < y2; ty++) {
            for (int tx = x1; tx < x2; tx++) {
                if (snapshot.get(tx, ty) == RoomGridSnapshot.TILE_ROOM_FLOOR) {
                    double sup = computeSupport(tx, ty);
                    if (sup < 1.0) {
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

                // If not in the active room interior, or is a wall / strut obstacle
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

    private int evaluateCandidate(int cx, int cy) {
        // Temporarily apply strut
        snapshot.set(cx, cy, RoomGridSnapshot.TILE_STRUT);

        int curedCount = 0;
        for (int i = 0; i < yellowCount; i++) {
            int yx = yellowX[i];
            int yy = yellowY[i];
            if (yx == cx && yy == cy) {
                curedCount++; // Strut itself is no longer an unsupported floor
                continue;
            }
            // Only recheck tiles within radius 4 of the candidate
            int dist = Math.max(Math.abs(yx - cx), Math.abs(yy - cy));
            if (dist <= 4) {
                double sup = computeSupport(yx, yy);
                if (sup >= 1.0) {
                    curedCount++;
                }
            }
        }

        // Rollback
        snapshot.set(cx, cy, RoomGridSnapshot.TILE_ROOM_FLOOR);
        return curedCount;
    }
}
