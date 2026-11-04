package com.autoplanner.solver;

import settlement.main.SETT;
import settlement.room.main.construction.ConstructionData;
import settlement.room.main.furnisher.FurnisherItem;
import settlement.room.main.furnisher.FurnisherItemTile;
import settlement.room.main.placement.RoomPlacer;
import snake2d.util.datatypes.AREA;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;

/**
 * High-performance, Zero-GC 2D grid snapshot representing the active room draft.
 * Backed by pre-allocated flat arrays to eliminate heap allocations during solving.
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

    private static final int MAX_BUFFER_DIM = 256;
    private final int[] grid = new int[MAX_BUFFER_DIM * MAX_BUFFER_DIM];

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
        }

        roomTileCount = 0;
        doorCount = 0;

        // 1. Populate room interior
        for (int ly = 0; ly < height; ly++) {
            int ty = originY + ly;
            for (int lx = 0; lx < width; lx++) {
                int tx = originX + lx;
                int idx = ly * width + lx;

                if (PlacerBridge.isTileInRoom(placer, tx, ty)) {
                    grid[idx] = TILE_ROOM_FLOOR;
                    roomTileCount++;
                }
            }
        }

        // 2. Classify surrounding walls & detect doors
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
                        // Check if door opening mask is set on this border tile
                        int m = ConstructionData.dWall.get(ntx, nty) & 0x0F;
                        if (m != 0) {
                            grid[idx] = TILE_DOOR;
                            if (doorCount < MAX_DOORS) {
                                doorX[doorCount] = tx;
                                doorY[doorCount] = ty;
                                doorCount++;
                            }
                        }
                        break;
                    }
                }

                if (adjToRoom && grid[idx] != TILE_DOOR) {
                    if (SETT.ROOMS().map.is(tx, ty) && SETT.PATH().getAvailability(tx, ty).player > 0) {
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

        // Fallback: If no explicit door opening found yet, assume any walkable border tile is an entrance
        if (doorCount == 0) {
            for (int ly = 0; ly < height && doorCount < MAX_DOORS; ly++) {
                int ty = originY + ly;
                for (int lx = 0; lx < width && doorCount < MAX_DOORS; lx++) {
                    int tx = originX + lx;
                    int idx = ly * width + lx;
                    if (grid[idx] == TILE_WALL || grid[idx] == TILE_OUTSIDE) {
                        for (DIR d : DIR.ORTHO) {
                            int ntx = tx + d.x();
                            int nty = ty + d.y();
                            if (isRoom(ntx, nty) && SETT.PATH().getAvailability(tx, ty) != null 
                                    && SETT.PATH().getAvailability(tx, ty).player >= 0) {
                                grid[idx] = TILE_DOOR;
                                doorX[doorCount] = tx;
                                doorY[doorCount] = ty;
                                doorCount++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 3. Mark existing items if any
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
                    if (tile != null && tile.mustBeReachable) {
                        grid[idx] = TILE_WORKSTATION_ACCESS;
                    } else if (tile != null && tile.availability.player <= settlement.path.AVAILABILITY.ROOM.player) {
                        grid[idx] = TILE_FURNITURE_WALKABLE;
                    } else {
                        grid[idx] = TILE_FURNITURE_BLOCK;
                    }
                }
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
            grid[ly * width + lx] = type;
        }
    }

    public boolean isRoom(int tx, int ty) {
        int t = get(tx, ty);
        return t == TILE_ROOM_FLOOR || t == TILE_FURNITURE_BLOCK 
            || t == TILE_FURNITURE_WALKABLE || t == TILE_WORKSTATION_ACCESS;
    }

    public boolean isWalkable(int tx, int ty) {
        int t = get(tx, ty);
        return t == TILE_ROOM_FLOOR || t == TILE_DOOR 
            || t == TILE_FURNITURE_WALKABLE || t == TILE_WORKSTATION_ACCESS;
    }

    public boolean isObstacle(int tx, int ty) {
        int t = get(tx, ty);
        return t == TILE_WALL || t == TILE_STRUT || t == TILE_FURNITURE_BLOCK || t == TILE_OUTSIDE;
    }

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
