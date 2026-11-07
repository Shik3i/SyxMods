package com.treezones.zone;

import init.resources.RESOURCE;
import settlement.main.SETT;
import settlement.tilemap.terrain.TForest;
import settlement.tilemap.terrain.TGrowable;
import settlement.tilemap.terrain.Terrain;

/**
 * High-performance clearing engine for trees, flowers, bushes, mushrooms, and wild crops.
 * Supports physical item drops vs. clean void erasure, with optional automatic No-Growth locking.
 */
public final class GardenerActions {

    private GardenerActions() {}

    /**
     * Clears flora on a single tile with full mountain/water safety guards.
     */
    public static boolean clearTileFlora(int tx, int ty, boolean dropResources) {
        if (!SETT.IN_BOUNDS(tx, ty) || SETT.TERRAIN() == null) {
            return false;
        }

        Terrain.TerrainTile tile = SETT.TERRAIN().get(tx, ty);
        if (tile == null || tile == SETT.TERRAIN().NADA) {
            return false;
        }

        // Safety check: Never demolish natural mountains, caves, or water bodies
        if (tile == SETT.TERRAIN().MOUNTAIN || tile == SETT.TERRAIN().CAVE) {
            return false;
        }
        if (SETT.TERRAIN().WATER != null && SETT.TERRAIN().WATER.is != null && SETT.TERRAIN().WATER.is.is(tx, ty)) {
            return false;
        }

        if (dropResources) {
            if (tile.clearing() != null && tile.clearing().can()) {
                while (SETT.TERRAIN().get(tx, ty).clearing().can()) {
                    RESOURCE res = SETT.TERRAIN().get(tx, ty).clearing().clear1(tx, ty);
                    if (res != null && SETT.THINGS() != null && SETT.THINGS().resources != null) {
                        SETT.THINGS().resources.create(tx, ty, res, 1);
                    }
                }
            } else {
                SETT.TERRAIN().NADA.placeFixed(tx, ty);
            }
        } else {
            if (tile.clearing() != null) {
                tile.clearing().destroy(tx, ty);
            } else {
                SETT.TERRAIN().NADA.placeFixed(tx, ty);
            }
        }

        // Clear floor growth layer
        if (SETT.TILE_MAP() != null && SETT.TILE_MAP().growth != null) {
            SETT.TILE_MAP().growth.tear(tx, ty);
        }

        return true;
    }

    public static int clearAllTrees(boolean dropResources, boolean autoNoGrowth, TreeZoneManager zm) {
        if (SETT.TERRAIN() == null || SETT.TERRAIN().TREES == null) return 0;
        int count = 0;
        int w = SETT.TWIDTH;
        int h = SETT.THEIGHT;
        TForest trees = SETT.TERRAIN().TREES;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (trees.isTree(x, y)) {
                    if (clearTileFlora(x, y, dropResources)) {
                        if (autoNoGrowth && zm != null) {
                            zm.applyNoTree(x, y, false, false);
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int clearAllFlowers(boolean dropResources, boolean autoNoGrowth, TreeZoneManager zm) {
        if (SETT.TERRAIN() == null || SETT.TERRAIN().FLOWER == null) return 0;
        int count = 0;
        int w = SETT.TWIDTH;
        int h = SETT.THEIGHT;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (SETT.TERRAIN().FLOWER.is(x, y)) {
                    if (clearTileFlora(x, y, dropResources)) {
                        if (autoNoGrowth && zm != null) {
                            zm.applyNoTree(x, y, false, false);
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int clearAllBushes(boolean dropResources, boolean autoNoGrowth, TreeZoneManager zm) {
        if (SETT.TERRAIN() == null || SETT.TERRAIN().BUSH == null) return 0;
        int count = 0;
        int w = SETT.TWIDTH;
        int h = SETT.THEIGHT;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (SETT.TERRAIN().BUSH.is(x, y)) {
                    if (clearTileFlora(x, y, dropResources)) {
                        if (autoNoGrowth && zm != null) {
                            zm.applyNoTree(x, y, false, false);
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int clearAllMushrooms(boolean dropResources, boolean autoNoGrowth, TreeZoneManager zm) {
        if (SETT.TERRAIN() == null || SETT.TERRAIN().MUSHROOM == null) return 0;
        int count = 0;
        int w = SETT.TWIDTH;
        int h = SETT.THEIGHT;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (SETT.TERRAIN().MUSHROOM.is(x, y)) {
                    if (clearTileFlora(x, y, dropResources)) {
                        if (autoNoGrowth && zm != null) {
                            zm.applyNoTree(x, y, false, false);
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int clearAllCrops(boolean dropResources, boolean autoNoGrowth, TreeZoneManager zm) {
        if (SETT.TERRAIN() == null || SETT.TERRAIN().GROWABLES == null) return 0;
        int count = 0;
        int w = SETT.TWIDTH;
        int h = SETT.THEIGHT;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean isCrop = false;
                for (TGrowable g : SETT.TERRAIN().GROWABLES) {
                    if (g.is(x, y)) {
                        isCrop = true;
                        break;
                    }
                }
                if (isCrop) {
                    if (clearTileFlora(x, y, dropResources)) {
                        if (autoNoGrowth && zm != null) {
                            zm.applyNoTree(x, y, false, false);
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int clearAllFlora(boolean dropResources, boolean autoNoGrowth, TreeZoneManager zm) {
        int total = 0;
        total += clearAllTrees(dropResources, autoNoGrowth, zm);
        total += clearAllFlowers(dropResources, autoNoGrowth, zm);
        total += clearAllBushes(dropResources, autoNoGrowth, zm);
        total += clearAllMushrooms(dropResources, autoNoGrowth, zm);
        total += clearAllCrops(dropResources, autoNoGrowth, zm);
        return total;
    }
}
