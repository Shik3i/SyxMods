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
     */
    public boolean canReachAllAccessPoints(RoomGridSnapshot snapshot, int[] accessX, int[] accessY, int accessCount) {
        if (snapshot == null || snapshot.doorCount == 0) {
            return false;
        }
        if (accessCount <= 0) {
            return true;
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

        // Verify all required access points have been visited
        for (int i = 0; i < accessCount; i++) {
            int ax = accessX[i];
            int ay = accessY[i];
            int alx = ax - originX;
            int aly = ay - originY;

            if (alx < 0 || alx >= width || aly < 0 || aly >= height) {
                return false;
            }

            int idx = aly * width + alx;
            if (visitedStamp[idx] != stamp) {
                return false; // Access point not reachable from doors!
            }
        }

        return true;
    }

    /**
     * Validates whether placing an item at (tx, ty) keeps all existing access points and
     * the new item's access point(s) fully reachable from the doors.
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

        // 1. Temporarily apply item to snapshot
        int newAccessCount = 0;
        int[] newAccessX = new int[4];
        int[] newAccessY = new int[4];

        for (int iy = 0; iy < item.height(); iy++) {
            for (int ix = 0; ix < item.width(); ix++) {
                FurnisherItemTile t = item.get(ix, iy);
                if (t != null) {
                    int cx = tx + ix;
                    int cy = ty + iy;
                    if (t.mustBeReachable) {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_WORKSTATION_ACCESS);
                        if (newAccessCount < 4) {
                            newAccessX[newAccessCount] = cx;
                            newAccessY[newAccessCount] = cy;
                            newAccessCount++;
                        }
                    } else if (t.availability.player <= settlement.path.AVAILABILITY.ROOM.player) {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_FURNITURE_WALKABLE);
                    } else {
                        snapshot.set(cx, cy, RoomGridSnapshot.TILE_FURNITURE_BLOCK);
                    }
                }
            }
        }

        // 2. Build combined check list of access points
        int totalAccess = existingCount + newAccessCount;
        int[] combinedX = new int[totalAccess];
        int[] combinedY = new int[totalAccess];

        System.arraycopy(existingAccessX, 0, combinedX, 0, existingCount);
        System.arraycopy(existingAccessY, 0, combinedY, 0, existingCount);
        for (int i = 0; i < newAccessCount; i++) {
            combinedX[existingCount + i] = newAccessX[i];
            combinedY[existingCount + i] = newAccessY[i];
        }

        boolean reachable = canReachAllAccessPoints(snapshot, combinedX, combinedY, totalAccess);

        // 3. Rollback item in snapshot
        for (int iy = 0; iy < item.height(); iy++) {
            for (int ix = 0; ix < item.width(); ix++) {
                if (item.get(ix, iy) != null) {
                    snapshot.set(tx + ix, ty + iy, RoomGridSnapshot.TILE_ROOM_FLOOR);
                }
            }
        }

        return reachable;
    }

    /**
     * Checks if placing a 1x1 strut (pillar) at (tx, ty) maintains room connectivity.
     */
    public boolean canPlaceStrutWithoutBlocking(RoomGridSnapshot snapshot, int tx, int ty) {
        if (snapshot == null || snapshot.doorCount == 0) {
            return false;
        }

        // Must be pure available room floor (never reserved door landing or existing items)
        if (snapshot.get(tx, ty) != RoomGridSnapshot.TILE_ROOM_FLOOR) {
            return false;
        }

        // Must not be adjacent to a door entrance (door threshold must remain completely clear)
        for (int i = 0; i < snapshot.doorCount; i++) {
            int dx = snapshot.doorX[i];
            int dy = snapshot.doorY[i];
            int dist = Math.abs(tx - dx) + Math.abs(ty - dy);
            if (dist <= 1) {
                return false;
            }
        }

        // Temporarily punch strut
        snapshot.set(tx, ty, RoomGridSnapshot.TILE_STRUT);

        // Run flood fill and count reachable room floor tiles
        int stamp = nextStamp();
        int head = 0;
        int tail = 0;

        int originX = snapshot.getOriginX();
        int originY = snapshot.getOriginY();
        int width = snapshot.getWidth();
        int height = snapshot.getHeight();

        int reachableFloors = 0;

        for (int i = 0; i < snapshot.doorCount; i++) {
            int dx = snapshot.doorX[i];
            int dy = snapshot.doorY[i];
            int lx = dx - originX;
            int ly = dy - originY;
            if (lx >= 0 && lx < width && ly >= 0 && ly < height) {
                visitedStamp[ly * width + lx] = stamp;
            }
            qx[tail] = dx;
            qy[tail] = dy;
            tail = (tail + 1) % MAX_QUEUE_SIZE;
        }

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
                        if (snapshot.get(nx, ny) == RoomGridSnapshot.TILE_ROOM_FLOOR) {
                            reachableFloors++;
                        }
                        qx[tail] = nx;
                        qy[tail] = ny;
                        tail = (tail + 1) % MAX_QUEUE_SIZE;
                    }
                }
            }
        }

        // Rollback
        snapshot.set(tx, ty, RoomGridSnapshot.TILE_ROOM_FLOOR);

        // All remaining room floor tiles (total - 1 for the strut itself) must still be reached
        int expectedReachable = snapshot.getRoomTileCount() - 1;
        return reachableFloors >= expectedReachable;
    }
}
