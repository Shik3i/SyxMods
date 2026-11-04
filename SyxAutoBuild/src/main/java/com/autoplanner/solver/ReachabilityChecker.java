package com.autoplanner.solver;

import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemTile;
import snake2d.util.datatypes.DIR;

/**
 * Ultra-fast array-based BFS Flood-Fill pathfinding solver.
 * Uses a stamp-based visited array to achieve true Zero-GC with zero per-run memset overhead.
 */
public final class ReachabilityChecker {

    private static final int MAX_QUEUE_SIZE = 16384;
    private final int[] qx = new int[MAX_QUEUE_SIZE];
    private final int[] qy = new int[MAX_QUEUE_SIZE];

    private static final int BUFFER_DIM = 256;
    private final int[] visitedStamp = new int[BUFFER_DIM * BUFFER_DIM];
    private int currentStamp = 1;

    // Pre-allocated access buffers to guarantee Zero-GC
    private static final int MAX_ACCESS_POINTS = 2048;
    private final int[] tempAccessX = new int[MAX_ACCESS_POINTS];
    private final int[] tempAccessY = new int[MAX_ACCESS_POINTS];
    private final int[] newAccessX = new int[256];
    private final int[] newAccessY = new int[256];

    // Temporary rollback buffer for item placement checks
    private final int[] rollbackPrevTypes = new int[256];

    private int nextStamp() {
        currentStamp++;
        if (currentStamp <= 0) { // integer overflow protection
            java.util.Arrays.fill(visitedStamp, 0);
            currentStamp = 1;
        }
        return currentStamp;
    }

    /**
     * Runs BFS from all room doors and determines if all required workstation access points are reached.
     * Accurately supports both walkable access tiles and blocking access tiles (where worker stands
     * on an adjacent walkable tile, matching Songs of Syx UtilPlacability mechanics).
     */
    public boolean canReachAllAccessPoints(RoomGridSnapshot snapshot, int[] accessX, int[] accessY, int accessCount) {
        if (snapshot == null || snapshot.doorCount == 0) {
            return false;
        }

        int stamp = nextStamp();
        int head = 0;
        int tail = 0;

        int originX = snapshot.getOriginX();
        int originY = snapshot.getOriginY();
        int width = snapshot.getWidth();
        int height = snapshot.getHeight();

        // Seed BFS with all door tiles and their immediate interior neighbors
        for (int i = 0; i < snapshot.doorCount; i++) {
            int dx = snapshot.doorX[i];
            int dy = snapshot.doorY[i];

            int lx = dx - originX;
            int ly = dy - originY;
            if (lx >= 0 && lx < width && ly >= 0 && ly < height) {
                visitedStamp[ly * width + lx] = stamp;
            }

            // Push door tile
            qx[tail] = dx;
            qy[tail] = dy;
            tail = (tail + 1) % MAX_QUEUE_SIZE;

            // Also check interior adjacent tiles
            for (DIR d : DIR.ORTHO) {
                int nx = dx + d.x();
                int ny = dy + d.y();
                int nlx = nx - originX;
                int nly = ny - originY;

                if (nlx >= 0 && nlx < width && nly >= 0 && nly < height) {
                    int idx = nly * width + nlx;
                    if (visitedStamp[idx] != stamp && snapshot.isWalkable(nx, ny)) {
                        visitedStamp[idx] = stamp;
                        qx[tail] = nx;
                        qy[tail] = ny;
                        tail = (tail + 1) % MAX_QUEUE_SIZE;
                    }
                }
            }
        }

        // BFS loop
        while (head != tail) {
            int cx = qx[head];
            int cy = qy[head];
            head = (head + 1) % MAX_QUEUE_SIZE;

            for (DIR d : DIR.ORTHO) {
                int nx = cx + d.x();
                int ny = cy + d.y();
                int nlx = nx - originX;
                int nly = ny - originY;

                if (nlx >= 0 && nlx < width && nly >= 0 && nly < height) {
                    int idx = nly * width + nlx;
                    if (visitedStamp[idx] != stamp && snapshot.isWalkable(nx, ny)) {
                        visitedStamp[idx] = stamp;
                        qx[tail] = nx;
                        qy[tail] = ny;
                        tail = (tail + 1) % MAX_QUEUE_SIZE;
                    }
                }
            }
        }

        // Verify all required access points have been reached
        for (int i = 0; i < accessCount; i++) {
            int ax = accessX[i];
            int ay = accessY[i];
            int alx = ax - originX;
            int aly = ay - originY;

            if (alx < 0 || alx >= width || aly < 0 || aly >= height) {
                return false;
            }

            int idx = aly * width + alx;
            // Case A: The access tile itself is walkable and reached by BFS
            if (visitedStamp[idx] == stamp && snapshot.isWalkable(ax, ay)) {
                continue;
            }

            // Case B: The access tile itself is a blocker tile (e.g. solid machine/workbench/crate).
            // Per Songs of Syx UtilPlacability, the worker interacts from an orthogonal adjacent walkable tile inside the room.
            boolean neighborReached = false;
            for (DIR d : DIR.ORTHO) {
                int nx = ax + d.x();
                int ny = ay + d.y();
                int nlx = nx - originX;
                int nly = ny - originY;

                if (nlx >= 0 && nlx < width && nly >= 0 && nly < height) {
                    int nidx = nly * width + nlx;
                    if (visitedStamp[nidx] == stamp && snapshot.isRoom(nx, ny) && snapshot.isWalkable(nx, ny)) {
                        neighborReached = true;
                        break;
                    }
                }
            }

            if (!neighborReached) {
                return false; // Neither the access point nor any adjacent interior walkable spot is reachable!
            }
        }

        // Verify no walkable room floor tile was sealed off into an isolated pocket (UtilPlacability WillBlockRoom)
        for (int ly = 0; ly < height; ly++) {
            int ty = originY + ly;
            int rowOffset = ly * width;
            for (int lx = 0; lx < width; lx++) {
                int tx = originX + lx;
                if (snapshot.isRoom(tx, ty) && snapshot.isWalkable(tx, ty)) {
                    if (visitedStamp[rowOffset + lx] != stamp) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Validates whether placing an item at (tx, ty) keeps all existing access points and
     * the new item's access point(s) fully reachable from the doors.
     * Zero-GC implementation backed by pre-allocated instance buffers.
     */
    public boolean canPlaceWithoutBlocking(
            RoomGridSnapshot snapshot, 
            int tx, 
            int ty, 
            FurnisherItem item,
            int[] existingAccessX, 
            int[] existingAccessY, 
            int existingCount) {

        if (snapshot == null || item == null) {
            return false;
        }

        // 1. Temporarily apply item to snapshot while saving previous tile state
        int newAccessCount = 0;
        int tileIndex = 0;

        for (int iy = 0; iy < item.height(); iy++) {
            for (int ix = 0; ix < item.width(); ix++) {
                FurnisherItemTile t = item.get(ix, iy);
                if (t != null) {
                    int cx = tx + ix;
                    int cy = ty + iy;

                    if (tileIndex < rollbackPrevTypes.length) {
                        rollbackPrevTypes[tileIndex] = snapshot.get(cx, cy);
                        tileIndex++;
                    }

                    boolean isBlocker = (t.availability.player < 0 || t.availability.from > 0);

                    if (t.mustBeReachable) {
                        if (newAccessCount < newAccessX.length) {
                            newAccessX[newAccessCount] = cx;
                            newAccessY[newAccessCount] = cy;
                            newAccessCount++;
                        }
                    }

                    if (isBlocker) {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_FURNITURE_BLOCK);
                    } else if (t.mustBeReachable) {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_WORKSTATION_ACCESS);
                    } else {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_FURNITURE_WALKABLE);
                    }
                }
            }
        }

        // 2. Build combined check list of access points in pre-allocated buffers (Zero GC!)
        int totalAccess = existingCount + newAccessCount;
        if (totalAccess > MAX_ACCESS_POINTS) {
            totalAccess = MAX_ACCESS_POINTS;
        }

        System.arraycopy(existingAccessX, 0, tempAccessX, 0, Math.min(existingCount, MAX_ACCESS_POINTS));
        System.arraycopy(existingAccessY, 0, tempAccessY, 0, Math.min(existingCount, MAX_ACCESS_POINTS));
        int availableSpace = MAX_ACCESS_POINTS - existingCount;
        if (availableSpace > 0 && newAccessCount > 0) {
            int toCopy = Math.min(newAccessCount, availableSpace);
            System.arraycopy(newAccessX, 0, tempAccessX, existingCount, toCopy);
            System.arraycopy(newAccessY, 0, tempAccessY, existingCount, toCopy);
        }

        boolean reachable = canReachAllAccessPoints(snapshot, tempAccessX, tempAccessY, totalAccess);

        // 3. Rollback item in snapshot using recorded previous types
        tileIndex = 0;
        for (int iy = 0; iy < item.height(); iy++) {
            for (int ix = 0; ix < item.width(); ix++) {
                if (item.get(ix, iy) != null) {
                    int prev = tileIndex < rollbackPrevTypes.length 
                            ? rollbackPrevTypes[tileIndex] 
                            : RoomGridSnapshot.TILE_ROOM_FLOOR;
                    snapshot.set(tx + ix, ty + iy, prev);
                    tileIndex++;
                }
            }
        }

        return reachable;
    }

    private static final int[] DX8 = { 0, 1, 1, 1, 0, -1, -1, -1 };
    private static final int[] DY8 = { -1, -1, 0, 1, 1, 1, 0, -1 };

    /**
     * Checks if placing a 1x1 strut (pillar) at (tx, ty) maintains room connectivity.
     * Uses digital topology simple-point analysis (Euler/Yokoi connectivity number).
     * O(1) complexity, Zero-GC, and 100% immune to room size or doorway counts.
     */
    public boolean canPlaceStrutWithoutBlocking(RoomGridSnapshot snapshot, int tx, int ty) {
        if (snapshot == null) {
            return false;
        }

        // Must be pure available room floor (never reserved door landing or existing items)
        if (snapshot.get(tx, ty) != RoomGridSnapshot.TILE_ROOM_FLOOR) {
            return false;
        }

        // If doors exist, must not block door threshold
        if (snapshot.doorCount > 0) {
            for (int i = 0; i < snapshot.doorCount; i++) {
                int dx = snapshot.doorX[i];
                int dy = snapshot.doorY[i];
                int dist = Math.abs(tx - dx) + Math.abs(ty - dy);
                if (dist <= 1) {
                    return false;
                }
            }
        }
        if (snapshot.hasVirtualDoor) {
            int dist = Math.abs(tx - snapshot.virtualDoorX) + Math.abs(ty - snapshot.virtualDoorY);
            if (dist <= 1) {
                return false;
            }
            int idist = Math.abs(tx - snapshot.virtualInteriorX) + Math.abs(ty - snapshot.virtualInteriorY);
            if (idist == 0) {
                return false;
            }
        }

        // Digital topology simple point check:
        // Examine the 8-neighborhood around (tx, ty) in circular order.
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            if (snapshot.isWalkable(tx + DX8[i], ty + DY8[i])) {
                mask |= (1 << i);
            }
        }

        // If no walkable neighbors around, it's isolated
        if (mask == 0) {
            return false;
        }

        // Count circular 0 -> 1 transitions
        int transitions = 0;
        for (int i = 0; i < 8; i++) {
            int curr = (mask >> i) & 1;
            int next = (mask >> ((i + 1) % 8)) & 1;
            if (curr == 0 && next == 1) {
                transitions++;
            }
        }

        // transitions <= 1 guarantees that surrounding walkable floor remains in a single connected component!
        return transitions <= 1;
    }
}
