package com.autoplanner.solver;

import settlement.main.SETT;
import settlement.room.main.construction.ConstructionData;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemTile;
import settlement.room.main.placement.RoomPlacer;
import settlement.room.main.placement.UtilWallPlacability;
import snake2d.util.datatypes.AREA;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;

/**
 * High-performance, Zero-GC 2D grid snapshot representing the active room draft.
 * Backed by pre-allocated flat arrays to eliminate heap allocations during solving.
 * Includes deterministic virtual door fallback when player has not yet placed any doors.
 */
public final class RoomGridSnapshot implements AREA {

    public static final int TILE_OUTSIDE = 0;
    public static final int TILE_ROOM_FLOOR = 1;
    public static final int TILE_WALL = 2;
    public static final int TILE_STRUT = 3;
    public static final int TILE_DOOR = 4;
    public static final int TILE_FURNITURE_BLOCK = 5;
    public static final int TILE_FURNITURE_WALKABLE = 6;
    public static final int TILE_WORKSTATION_ACCESS = 7;
    public static final int TILE_RESERVED_DOOR_AISLE = 8; // Reserved corridor to future door!

    private static final int MAX_BUFFER_DIM = 256;
    private final int[] grid = new int[MAX_BUFFER_DIM * MAX_BUFFER_DIM];
    private final boolean[] existingRoom = new boolean[MAX_BUFFER_DIM * MAX_BUFFER_DIM];

    private int originX;
    private int originY;
    private int width;
    private int height;
    private int roomTileCount;

    // Door cache
    public static final int MAX_DOORS = 64;
    public final int[] doorX = new int[MAX_DOORS];
    public final int[] doorY = new int[MAX_DOORS];
    public int doorCount = 0;

    // Virtual door fallback state
    public boolean hasVirtualDoor = false;
    public int virtualDoorX = -1;
    public int virtualDoorY = -1;
    public int virtualInteriorX = -1;
    public int virtualInteriorY = -1;

    public void initFrom(RoomPlacer placer) {
        RECTANGLE bounds = PlacerBridge.getBounds(placer);
        int area = PlacerBridge.getArea(placer);

        if (placer == null || placer.blueprint() == null || bounds == null || area <= 0) {
            clear();
            return;
        }

        // Bounding box with 1-tile margin for surrounding walls and doors
        originX = Math.max(0, bounds.x1() - 1);
        originY = Math.max(0, bounds.y1() - 1);
        int x2 = Math.min(SETT.TWIDTH - 1, bounds.x2() + 1);
        int y2 = Math.min(SETT.THEIGHT - 1, bounds.y2() + 1);

        width = Math.min(MAX_BUFFER_DIM, Math.max(1, x2 - originX + 1));
        height = Math.min(MAX_BUFFER_DIM, Math.max(1, y2 - originY + 1));

        int totalCells = width * height;
        for (int i = 0; i < totalCells; i++) {
            grid[i] = TILE_OUTSIDE;
            existingRoom[i] = false;
        }

        roomTileCount = 0;
        doorCount = 0;
        hasVirtualDoor = false;
        virtualDoorX = -1;
        virtualDoorY = -1;
        virtualInteriorX = -1;
        virtualInteriorY = -1;

        // 1. Populate room interior & cache existing built room map
        for (int ly = 0; ly < height; ly++) {
            int ty = originY + ly;
            for (int lx = 0; lx < width; lx++) {
                int tx = originX + lx;
                int idx = ly * width + lx;

                if (PlacerBridge.isTileInRoom(placer, tx, ty)) {
                    grid[idx] = TILE_ROOM_FLOOR;
                    roomTileCount++;
                } else if (SETT.ROOMS() != null && SETT.ROOMS().map != null) {
                    existingRoom[idx] = SETT.ROOMS().map.is(tx, ty);
                }
            }
        }

        // 2. Classify surrounding walls & detect real doors
        for (int ly = 0; ly < height; ly++) {
            int ty = originY + ly;
            for (int lx = 0; lx < width; lx++) {
                int tx = originX + lx;
                int idx = ly * width + lx;

                if (grid[idx] == TILE_ROOM_FLOOR) {
                    continue;
                }

                // Check if adjacent to room floor
                boolean adjToRoom = false;
                for (DIR d : DIR.ORTHO) {
                    int ntx = tx + d.x();
                    int nty = ty + d.y();
                    if (isRoom(ntx, nty)) {
                        adjToRoom = true;
                        break;
                    }
                }

                if (adjToRoom) {
                    boolean isDoor = ConstructionData.dWall != null && (ConstructionData.dWall.get(tx, ty) & 0x0F) != 0;
                    if (isDoor) {
                        grid[idx] = TILE_DOOR;
                        if (doorCount < MAX_DOORS) {
                            doorX[doorCount] = tx;
                            doorY[doorCount] = ty;
                            doorCount++;
                        }
                    } else if (SETT.ROOMS() != null && SETT.ROOMS().map != null && SETT.ROOMS().map.is(tx, ty)
                            && SETT.PATH() != null && SETT.PATH().getAvailability(tx, ty).player > 0) {
                        grid[idx] = TILE_DOOR;
                        if (doorCount < MAX_DOORS) {
                            doorX[doorCount] = tx;
                            doorY[doorCount] = ty;
                            doorCount++;
                        }
                    } else {
                        grid[idx] = TILE_WALL;
                    }
                }
            }
        }

        // 3. Fallback: If 0 doors placed, determine deterministic virtual door anchor
        if (doorCount == 0 && roomTileCount > 0) {
            setupVirtualDoorAnchor(bounds);
        }

        // 4. Mark existing items if any
        for (int ly = 0; ly < height; ly++) {
            int ty = originY + ly;
            for (int lx = 0; lx < width; lx++) {
                int tx = originX + lx;
                if (!isRoom(tx, ty)) {
                    continue;
                }

                FurnisherItem item = SETT.ROOMS().fData.item.get(tx, ty);
                if (item != null) {
                    FurnisherItemTile tile = SETT.ROOMS().fData.tile.get(tx, ty);
                    int idx = ly * width + lx;
                    boolean isBlocker = (tile == null) || tile.availability.player < 0 || tile.availability.from > 0;
                    if (tile != null && tile.mustBeReachable && !isBlocker) {
                        grid[idx] = TILE_WORKSTATION_ACCESS;
                    } else if (!isBlocker) {
                        grid[idx] = TILE_FURNITURE_WALKABLE;
                    } else {
                        grid[idx] = TILE_FURNITURE_BLOCK;
                    }
                }
            }
        }
    }

    /**
     * Finds the best outer border tile to act as a virtual doorway into the room.
     * Keeps the doorway tile and the adjacent interior landing tile permanently reserved.
     */
    private void setupVirtualDoorAnchor(RECTANGLE bounds) {
        int bestDoorX = -1;
        int bestDoorY = -1;
        int bestInX = -1;
        int bestInY = -1;
        double bestScore = -Double.MAX_VALUE;

        int midX = bounds.cX();
        int midY = bounds.cY();

        // Scan all room interior tiles that border the outside
        for (int ly = 0; ly < height; ly++) {
            int ty = originY + ly;
            for (int lx = 0; lx < width; lx++) {
                int tx = originX + lx;
                if (grid[ly * width + lx] != TILE_ROOM_FLOOR) {
                    continue;
                }

                for (DIR d : DIR.ORTHO) {
                    int ox = tx + d.x();
                    int oy = ty + d.y();

                    if (!SETT.IN_BOUNDS(ox, oy)) continue;

                    int olx = ox - originX;
                    int oly = oy - originY;
                    if (olx < 0 || olx >= width || oly < 0 || oly >= height) continue;

                    int otype = grid[oly * width + olx];
                    if (otype == TILE_WALL || otype == TILE_OUTSIDE) {
                        // Check if passable on terrain (not a mountain/impassable rock)
                        if (SETT.TERRAIN().MOUNTAIN.is(ox, oy)) {
                            continue;
                        }

                        // Score candidate: favor midpoint of walls, favor South > East > West > North
                        double distFromCenter = Math.abs(tx - midX) + Math.abs(ty - midY);
                        double score = 100.0 - distFromCenter;
                        if (d == DIR.S) score += 30.0;
                        else if (d == DIR.E) score += 20.0;
                        else if (d == DIR.W) score += 15.0;

                        if (score > bestScore) {
                            bestScore = score;
                            bestDoorX = ox;
                            bestDoorY = oy;
                            bestInX = tx;
                            bestInY = ty;
                        }
                    }
                }
            }
        }

        if (bestDoorX != -1 && bestInX != -1) {
            hasVirtualDoor = true;
            virtualDoorX = bestDoorX;
            virtualDoorY = bestDoorY;
            virtualInteriorX = bestInX;
            virtualInteriorY = bestInY;

            // Register virtual door
            doorX[0] = bestDoorX;
            doorY[0] = bestDoorY;
            doorCount = 1;

            int dlx = bestDoorX - originX;
            int dly = bestDoorY - originY;
            if (dlx >= 0 && dlx < width && dly >= 0 && dly < height) {
                grid[dly * width + dlx] = TILE_DOOR;
            }

            // Reserve the interior landing tile as an open aisle!
            int ilx = bestInX - originX;
            int ily = bestInY - originY;
            if (ilx >= 0 && ilx < width && ily >= 0 && ily < height) {
                grid[ily * width + ilx] = TILE_RESERVED_DOOR_AISLE;
            }
        }
    }

    public void clear() {
        originX = 0;
        originY = 0;
        width = 0;
        height = 0;
        roomTileCount = 0;
        doorCount = 0;
        hasVirtualDoor = false;
        virtualDoorX = -1;
        virtualDoorY = -1;
        virtualInteriorX = -1;
        virtualInteriorY = -1;
    }

    public int get(int tx, int ty) {
        int lx = tx - originX;
        int ly = ty - originY;
        if (lx < 0 || lx >= width || ly < 0 || ly >= height) {
            return TILE_OUTSIDE;
        }
        return grid[ly * width + lx];
    }

    public void set(int tx, int ty, int type) {
        int lx = tx - originX;
        int ly = ty - originY;
        if (lx >= 0 && lx < width && ly >= 0 && ly < height) {
            int idx = ly * width + lx;
            grid[idx] = type;
            if (type == TILE_STRUT) {
                existingRoom[idx] = false;
            }
        }
    }

    public boolean inSnapshotBounds(int tx, int ty) {
        int lx = tx - originX;
        int ly = ty - originY;
        return lx >= 0 && lx < width && ly >= 0 && ly < height;
    }

    public boolean isExistingRoom(int tx, int ty) {
        int lx = tx - originX;
        int ly = ty - originY;
        if (lx < 0 || lx >= width || ly < 0 || ly >= height) {
            return false;
        }
        return existingRoom[ly * width + lx];
    }

    public boolean isRoom(int tx, int ty) {
        int t = get(tx, ty);
        return t == TILE_ROOM_FLOOR || t == TILE_FURNITURE_BLOCK 
            || t == TILE_FURNITURE_WALKABLE || t == TILE_WORKSTATION_ACCESS
            || t == TILE_RESERVED_DOOR_AISLE;
    }

    public boolean isWalkable(int tx, int ty) {
        int t = get(tx, ty);
        return t == TILE_ROOM_FLOOR || t == TILE_DOOR 
            || t == TILE_FURNITURE_WALKABLE || t == TILE_WORKSTATION_ACCESS
            || t == TILE_RESERVED_DOOR_AISLE;
    }

    public boolean isObstacle(int tx, int ty) {
        int t = get(tx, ty);
        return t == TILE_WALL || t == TILE_STRUT || t == TILE_FURNITURE_BLOCK || t == TILE_OUTSIDE;
    }

    /**
     * Returns true ONLY if tile is pure floor ready to receive furniture.
     * Specifically excludes reserved door aisles and struts!
     */
    public boolean isAvailableFloor(int tx, int ty) {
        return get(tx, ty) == TILE_ROOM_FLOOR;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRoomTileCount() {
        return roomTileCount;
    }

    // --- AREA / MAP_BOOLEAN interface implementations ---

    @Override
    public boolean is(int tile) {
        int tx = tile % SETT.TWIDTH;
        int ty = tile / SETT.TWIDTH;
        return isRoom(tx, ty);
    }

    @Override
    public boolean is(int tx, int ty) {
        return isRoom(tx, ty);
    }

    @Override
    public boolean is(COORDINATE coordinate) {
        return is(coordinate.x(), coordinate.y());
    }

    @Override
    public boolean is(int tx, int ty, DIR dir) {
        return is(tx + dir.x(), ty + dir.y());
    }

    @Override
    public boolean is(COORDINATE coordinate, DIR dir) {
        return is(coordinate.x() + dir.x(), coordinate.y() + dir.y());
    }

    @Override
    public RECTANGLE body() {
        return new Rec(originX, originX + width, originY, originY + height);
    }

    @Override
    public int area() {
        return roomTileCount;
    }
}
