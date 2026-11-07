package com.treezones.zone;

import java.io.IOException;
import settlement.main.SETT;
import settlement.tilemap.terrain.Terrain;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.sets.Bitmap2D;

/**
 * Core manager for custom No-Tree and Forest zones.
 * Tracks custom zones with high-performance 2D bitmasks and handles
 * engine-level enforcement with zero-GC overhead.
 */
public final class TreeZoneManager {

    private static final int MAGIC = 0x54524545; // "TREE"
    private static final int VERSION = 1;

    private Bitmap2D noTreeZones;
    private Bitmap2D forestZones;

    private int activeNoTreeCount = 0;
    private int activeForestCount = 0;

    private int dirtyMinX = Integer.MAX_VALUE;
    private int dirtyMaxX = -1;
    private int dirtyMinY = Integer.MAX_VALUE;
    private int dirtyMaxY = -1;

    private long lastEnforceTime = 0L;
    private static final long ENFORCE_INTERVAL_MS = 5000L; // Every 5 seconds

    public TreeZoneManager() {
        // Lazily initialized when SETT bounds are ready
    }

    public void ensureInitialized() {
        int w = 768;
        int h = 768;
        try {
            if (SETT.TWIDTH > 0 && SETT.THEIGHT > 0) {
                w = SETT.TWIDTH;
                h = SETT.THEIGHT;
            }
        } catch (Throwable ignored) {}
        ensureInitialized(w, h);
    }

    public void ensureInitialized(int w, int h) {
        if (noTreeZones != null) {
            return;
        }
        noTreeZones = new Bitmap2D(w, h, false);
        forestZones = new Bitmap2D(w, h, false);
    }

    public boolean isNoTree(int tx, int ty) {
        if (noTreeZones == null) {
            return false;
        }
        try {
            if (!SETT.IN_BOUNDS(tx, ty)) return false;
        } catch (Throwable t) {
            if (tx < 0 || tx >= noTreeZones.body().width() || ty < 0 || ty >= noTreeZones.body().height()) return false;
        }
        return noTreeZones.is(tx, ty);
    }

    public boolean isForest(int tx, int ty) {
        if (forestZones == null) {
            return false;
        }
        try {
            if (!SETT.IN_BOUNDS(tx, ty)) return false;
        } catch (Throwable t) {
            if (tx < 0 || tx >= forestZones.body().width() || ty < 0 || ty >= forestZones.body().height()) return false;
        }
        return forestZones.is(tx, ty);
    }

    private void expandDirty(int x, int y) {
        if (x < dirtyMinX) dirtyMinX = x;
        if (x > dirtyMaxX) dirtyMaxX = x;
        if (y < dirtyMinY) dirtyMinY = y;
        if (y > dirtyMaxY) dirtyMaxY = y;
    }

    public void applyNoTree(int tx, int ty, boolean instantClear, boolean dropWood) {
        ensureInitialized();
        if (!SETT.IN_BOUNDS(tx, ty)) {
            return;
        }

        expandDirty(tx, ty);

        if (!noTreeZones.is(tx, ty)) {
            noTreeZones.set(tx, ty, true);
            activeNoTreeCount++;
        }
        if (forestZones.is(tx, ty)) {
            forestZones.set(tx, ty, false);
            activeForestCount = Math.max(0, activeForestCount - 1);
        }

        // 1. Permanently mark tile growth type as NOTHING
        if (SETT.TILE_MAP() != null && SETT.TILE_MAP().growth != null) {
            SETT.TILE_MAP().growth.nothing.set(tx, ty, 0.0);
        }

        // 2. Instant clear existing tree or bush if requested
        if (instantClear && SETT.TERRAIN() != null) {
            if (SETT.TERRAIN().TREES != null && SETT.TERRAIN().TREES.isTree(tx, ty)) {
                if (dropWood) {
                    while (SETT.TERRAIN().get(tx, ty).clearing().can()) {
                        SETT.TERRAIN().get(tx, ty).clearing().clear1(tx, ty);
                    }
                } else {
                    SETT.TERRAIN().NADA.placeFixed(tx, ty);
                }
            } else if (SETT.TERRAIN().BUSH != null && SETT.TERRAIN().BUSH.is(tx, ty)) {
                SETT.TERRAIN().NADA.placeFixed(tx, ty);
            }
        }
    }

    public void applyForest(int tx, int ty, boolean instantPlant) {
        ensureInitialized();
        if (!SETT.IN_BOUNDS(tx, ty)) {
            return;
        }

        expandDirty(tx, ty);

        if (!forestZones.is(tx, ty)) {
            forestZones.set(tx, ty, true);
            activeForestCount++;
        }
        if (noTreeZones.is(tx, ty)) {
            noTreeZones.set(tx, ty, false);
            activeNoTreeCount = Math.max(0, activeNoTreeCount - 1);
        }

        // 1. Permanently mark tile growth type as TREE
        if (SETT.TILE_MAP() != null && SETT.TILE_MAP().growth != null) {
            SETT.TILE_MAP().growth.tree.set(tx, ty, 1.0);
        }

        // 2. Instant plant small tree if requested and tile is clear
        if (instantPlant && SETT.TERRAIN() != null && SETT.TERRAIN().TREES != null) {
            if (!SETT.TERRAIN().TREES.isTree(tx, ty)) {
                Terrain.TerrainTile tile = SETT.TERRAIN().get(tx, ty);
                if (tile == SETT.TERRAIN().NADA || tile.clearing().isEasilyCleared()) {
                    SETT.TERRAIN().TREES.SMALL.placeRaw(tx, ty);
                    SETT.TERRAIN().TREES.amount.set(tx, ty, 1);
                }
            }
        }
    }

    public void applyReset(int tx, int ty) {
        ensureInitialized();
        if (!SETT.IN_BOUNDS(tx, ty)) {
            return;
        }

        if (noTreeZones.is(tx, ty)) {
            noTreeZones.set(tx, ty, false);
            activeNoTreeCount = Math.max(0, activeNoTreeCount - 1);
        }
        if (forestZones.is(tx, ty)) {
            forestZones.set(tx, ty, false);
            activeForestCount = Math.max(0, activeForestCount - 1);
        }

        // Restore natural growth based on present terrain
        if (SETT.TILE_MAP() != null && SETT.TILE_MAP().growth != null) {
            if (SETT.TERRAIN() != null && SETT.TERRAIN().TREES != null && SETT.TERRAIN().TREES.isTree(tx, ty)) {
                SETT.TILE_MAP().growth.tree.set(tx, ty, 1.0);
            } else {
                SETT.TILE_MAP().growth.nothing.set(tx, ty, 0.0);
            }
        }
    }

    /**
     * Periodic enforcement: maintains integrity of No-Tree and Forest zones.
     * Anchored to wall-clock time with zero-GC overhead and tight dirty bounding box.
     */
    public void update() {
        if (activeNoTreeCount == 0 && activeForestCount == 0) {
            dirtyMinX = Integer.MAX_VALUE;
            dirtyMaxX = -1;
            dirtyMinY = Integer.MAX_VALUE;
            dirtyMaxY = -1;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastEnforceTime < ENFORCE_INTERVAL_MS) {
            return;
        }
        lastEnforceTime = now;

        if (SETT.TILE_MAP() == null || SETT.TILE_MAP().growth == null || SETT.TERRAIN() == null) {
            return;
        }

        int x1 = Math.max(0, dirtyMinX);
        int x2 = Math.min(SETT.TWIDTH - 1, dirtyMaxX >= 0 ? dirtyMaxX : SETT.TWIDTH - 1);
        int y1 = Math.max(0, dirtyMinY);
        int y2 = Math.min(SETT.THEIGHT - 1, dirtyMaxY >= 0 ? dirtyMaxY : SETT.THEIGHT - 1);

        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (noTreeZones.is(x, y)) {
                    // Suppress any rogue tree/bush that tried to sprout
                    if (SETT.TERRAIN().TREES != null && SETT.TERRAIN().TREES.isTree(x, y)) {
                        SETT.TERRAIN().NADA.placeFixed(x, y);
                    } else if (SETT.TERRAIN().BUSH != null && SETT.TERRAIN().BUSH.is(x, y)) {
                        SETT.TERRAIN().NADA.placeFixed(x, y);
                    }
                    SETT.TILE_MAP().growth.nothing.set(x, y, 0.0);
                } else if (forestZones.is(x, y)) {
                    // Ensure growth stays active
                    SETT.TILE_MAP().growth.tree.set(x, y, 1.0);
                }
            }
        }
    }

    public int getActiveNoTreeCount() {
        return activeNoTreeCount;
    }

    public int getActiveForestCount() {
        return activeForestCount;
    }

    public void save(FilePutter file) {
        ensureInitialized();
        file.i(MAGIC);
        file.i(VERSION);
        int w = noTreeZones != null ? noTreeZones.body().width() : 768;
        int h = noTreeZones != null ? noTreeZones.body().height() : 768;
        file.i(w);
        file.i(h);
        file.i(activeNoTreeCount);
        file.i(activeForestCount);
        noTreeZones.save(file);
        forestZones.save(file);
    }

    public void load(FileGetter file) throws IOException {
        int magic = file.i();
        if (magic != MAGIC) {
            throw new IOException("Invalid SyxTreeZones save data: expected magic " + Integer.toHexString(MAGIC));
        }
        int v = file.i();
        if (v >= 1) {
            int w = file.i();
            int h = file.i();
            activeNoTreeCount = file.i();
            activeForestCount = file.i();
            ensureInitialized();
            noTreeZones.load(file);
            forestZones.load(file);
            if (activeNoTreeCount > 0 || activeForestCount > 0) {
                dirtyMinX = 0;
                dirtyMaxX = w - 1;
                dirtyMinY = 0;
                dirtyMaxY = h - 1;
            } else {
                dirtyMinX = Integer.MAX_VALUE;
                dirtyMaxX = -1;
                dirtyMinY = Integer.MAX_VALUE;
                dirtyMaxY = -1;
            }
        }
    }
}
