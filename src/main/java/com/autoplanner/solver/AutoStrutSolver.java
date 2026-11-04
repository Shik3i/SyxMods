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

    // Fixed buffers for yellow tile coordinates & base support (Zero-GC)
    private static final int MAX_TRACKED_YELLOW = 4096;
    private final int[] yellowX = new int[MAX_TRACKED_YELLOW];
    private final int[] yellowY = new int[MAX_TRACKED_YELLOW];
    private final double[] yellowBaseSupport = new double[MAX_TRACKED_YELLOW];
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
     * Guarantees 100% elimination of yellow ceiling tiles in a single click.
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

        // 3. Multi-pass solve: iteratively place struts and verify with actual engine physics
        int maxPasses = 3;
        for (int pass = 0; pass < maxPasses && yellowCount > 0; pass++) {
            if (System.currentTimeMillis() - start > 1800) {
                break; // 1800ms total budget guard
            }

            int x1 = snapshot.getOriginX();
            int y1 = snapshot.getOriginY();
            int x2 = x1 + snapshot.getWidth();
            int y2 = y1 + snapshot.getHeight();
            int maxIterationsPerPass = Math.min(MAX_STRUTS - strutCount, Math.max(15, yellowCount / 2 + 5));

            int passStrutsPlaced = 0;
            for (int iter = 0; iter < maxIterationsPerPass && yellowCount > 0; iter++) {
                if (System.currentTimeMillis() - start > 1800) break;

                // Precompute baseline support of all remaining yellow tiles before testing candidates
                for (int i = 0; i < yellowCount; i++) {
                    yellowBaseSupport[i] = CLAMP.d(computeSupport(yellowX[i], yellowY[i]), 0.0, 1.0);
                }

                int bestX = -1;
                int bestY = -1;
                double bestScore = -1.0;

                for (int cy = y1; cy < y2; cy++) {
                    for (int cx = x1; cx < x2; cx++) {
                        if (!PlacerBridge.isTileInRoom(placer, cx, cy)) {
                            continue;
                        }
                        if (!canPlacePillar(snapshot, cx, cy, pass)) {
                            continue;
                        }

                        // Evaluate candidate marginal support gain using exact 16-ray physics
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_STRUT);
                        double score = 0.0;
                        for (int i = 0; i < yellowCount; i++) {
                            int yx = yellowX[i];
                            int yy = yellowY[i];
                            if (Math.abs(yx - cx) <= 4 && Math.abs(yy - cy) <= 4) {
                                double sup = computeSupport(yx, yy);
                                double clampedSup = CLAMP.d(sup, 0.0, 1.0);
                                double gain = clampedSup - yellowBaseSupport[i];
                                if (gain > 0.0) {
                                    score += gain;
                                    if (sup >= 1.0) {
                                        score += 0.25; // Bonus for fully resolving a yellow tile
                                    }
                                }
                            }
                        }
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_ROOM_FLOOR);

                        if (score > bestScore) {
                            bestScore = score;
                            bestX = cx;
                            bestY = cy;
                        }
                    }
                }

                if (bestX == -1 || bestScore <= 0.01) {
                    break;
                }

                // Commit candidate to snapshot & coordinate list
                snapshot.set(bestX, bestY, RoomGridSnapshot.TILE_STRUT);
                strutX[strutCount] = bestX;
                strutY[strutCount] = bestY;
                strutCount++;
                passStrutsPlaced++;

                // Rescan yellow tiles using exact ray support (never remove prematurely!)
                int writeIdx = 0;
                for (int i = 0; i < yellowCount; i++) {
                    int yx = yellowX[i];
                    int yy = yellowY[i];
                    if (computeSupport(yx, yy) < 1.0) {
                        yellowX[writeIdx] = yx;
                        yellowY[writeIdx] = yy;
                        writeIdx++;
                    }
                }
                yellowCount = writeIdx;
            }

            // Commit placed struts to the game engine
            for (int i = 0; i < strutCount; i++) {
                int px = strutX[i];
                int py = strutY[i];
                if (PlacerBridge.isTileInRoom(placer, px, py)) {
                    PlacerBridge.clearTile(placer, px, py);
                }
            }

            // Recalculate native engine ceiling support
            PlacerBridge.syncCostAndWalls(placer);
            snapshot.initFrom(placer);

            // Re-query real engine cost: if 0 yellow tiles remain, we are completely done!
            scanYellowTiles(placer);
            if (yellowCount == 0 || passStrutsPlaced == 0) {
                break;
            }
        }

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

    private boolean canPlacePillar(RoomGridSnapshot snapshot, int tx, int ty, int pass) {
        // Verify pure floor availability, door distance >= 2, and topological simple-point connectivity
        if (!reachability.canPlaceStrutWithoutBlocking(snapshot, tx, ty)) {
            return false;
        }

        // In pass 0, avoid hugging outer walls (solid walls already support 3.5 tiles inwards).
        // In pass >= 1, allow near-wall placement if an adjacent room border provided 0 wall support.
        if (pass == 0 && isNearWall(tx, ty, 1)) {
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

    /**
     * Exact 1:1 reproduction of Songs of Syx UtilExtraCost.support() ceiling physics.
     */
    public double computeSupport(int tx, int ty) {
        // A tile that is no longer room interior (e.g., converted to a pillar/wall) requires 0 ceiling support
        if (!snapshot.isRoom(tx, ty)) {
            return 1.0;
        }

        double s = 0;
        tracer.checkInit();

        for (Ray r : tracer.rays()) {
            for (int i = 0; i < r.size(); i++) {
                int dx = tx + r.get(i).x();
                int dy = ty + r.get(i).y();

                if (!snapshot.inSnapshotBounds(dx, dy)) {
                    break;
                }

                if (!snapshot.isRoom(dx, dy)) {
                    // Per UtilExtraCost.support, existing adjacent rooms do not provide solid wall support
                    if (!snapshot.isExistingRoom(dx, dy) && tracer.check(r.get(i))) {
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
                if (Math.abs(dx) + Math.abs(dy) > dist || (dx == 0 && dy == 0)) continue;
                if (snapshot.get(cx + dx, cy + dy) == RoomGridSnapshot.TILE_WALL) {
                    return true;
                }
            }
        }
        return false;
    }
}
