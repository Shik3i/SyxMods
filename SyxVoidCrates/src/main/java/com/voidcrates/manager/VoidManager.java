package com.voidcrates.manager;

import com.voidcrates.config.VoidConfig;
import init.resources.RESOURCE;
import settlement.main.SETT;
import settlement.room.infra.stockpile.ROOM_STOCKPILE;
import settlement.room.infra.stockpile.StockpileInstance;
import settlement.room.main.job.StorageCrate;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.sets.Bitmap2D;

import java.io.IOException;

public final class VoidManager {

    private Bitmap2D designatedVoidCrates;
    private int designatedCount = 0;

    private long lastCheckMs = 0L;
    private static final long CHECK_INTERVAL_MS = 1000L; // At most once per real-world second

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
        if (designatedVoidCrates == null) {
            designatedVoidCrates = new Bitmap2D(w, h, false);
        }
    }

    public boolean isDesignated(int tx, int ty) {
        ensureInitialized();
        if (designatedVoidCrates == null || !SETT.IN_BOUNDS(tx, ty)) {
            return false;
        }
        return designatedVoidCrates.is(tx, ty);
    }

    public void toggleDesignated(int tx, int ty) {
        ensureInitialized();
        if (designatedVoidCrates == null || !SETT.IN_BOUNDS(tx, ty)) {
            return;
        }
        boolean cur = designatedVoidCrates.is(tx, ty);
        designatedVoidCrates.set(tx, ty, !cur);
        if (!cur) {
            designatedCount++;
        } else {
            designatedCount = Math.max(0, designatedCount - 1);
        }
    }

    public void clearAllDesignations() {
        ensureInitialized();
        if (designatedVoidCrates != null) {
            designatedVoidCrates.clear();
            designatedCount = 0;
        }
    }

    public int markAllCratesOfResource(RESOURCE targetRes) {
        if (!SETT.exists() || SETT.ROOMS() == null || SETT.ROOMS().STOCKPILE == null || targetRes == null) {
            return 0;
        }
        ensureInitialized();
        ROOM_STOCKPILE stockpile = SETT.ROOMS().STOCKPILE;
        int count = stockpile.instancesSize();
        int marked = 0;

        for (int i = 0; i < count; i++) {
            StockpileInstance instance = stockpile.getInstance(i);
            if (instance == null || instance.body() == null) continue;

            RECTANGLE body = instance.body();
            int x1 = body.x1();
            int x2 = body.x2();
            int y1 = body.y1();
            int y2 = body.y2();

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    if (instance.is(x, y)) {
                        StorageCrate crate = instance.crate(x, y);
                        if (crate != null && crate.resource() == targetRes) {
                            if (!designatedVoidCrates.is(x, y)) {
                                designatedVoidCrates.set(x, y, true);
                                designatedCount++;
                                marked++;
                            }
                        }
                    }
                }
            }
        }
        return marked;
    }

    public void update(VoidConfig config) {
        if (!config.isActive() || !SETT.exists() || SETT.ROOMS() == null || SETT.ROOMS().STOCKPILE == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCheckMs < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckMs = now;

        processPurge(config);
    }

    public int[] purgeNow(VoidConfig config) {
        if (!SETT.exists() || SETT.ROOMS() == null || SETT.ROOMS().STOCKPILE == null) {
            return new int[]{0, 0};
        }
        return processPurge(config);
    }

    private int[] processPurge(VoidConfig config) {
        ensureInitialized();
        ROOM_STOCKPILE stockpile = SETT.ROOMS().STOCKPILE;
        int count = stockpile.instancesSize();
        if (count == 0) return new int[]{0, 0};

        int cratesPurged = 0;
        int itemsVoided = 0;

        double triggerRatio = config.getTriggerRatio();
        double retainRatio = config.getRetainRatio();
        boolean designatedOnly = config.isDesignatedOnly();

        for (int i = 0; i < count; i++) {
            StockpileInstance instance = stockpile.getInstance(i);
            if (instance == null || instance.body() == null) continue;

            RECTANGLE body = instance.body();
            int x1 = body.x1();
            int x2 = body.x2();
            int y1 = body.y1();
            int y2 = body.y2();

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    if (instance.is(x, y)) {
                        StorageCrate crate = instance.crate(x, y);
                        if (crate != null && crate.resource() != null && crate.amount() > 0) {
                            RESOURCE res = crate.resource();
                            boolean isTarget = false;

                            if (designatedVoidCrates != null && designatedVoidCrates.is(x, y)) {
                                isTarget = true;
                            } else if (!designatedOnly && isResourceQualifies(res, config)) {
                                isTarget = true;
                            }

                            if (isTarget) {
                                int capacity = instance.crateSize(res);
                                int curAmount = crate.amount();
                                int trigger = (int) (capacity * triggerRatio);

                                if (curAmount >= trigger && curAmount > 0) {
                                    int keep = (int) (capacity * retainRatio);
                                    int safeKeep = Math.max(crate.reserved(), keep);
                                    if (curAmount > safeKeep) {
                                        int voided = curAmount - safeKeep;
                                        crate.amountSet(safeKeep);
                                        config.addVoided(voided);
                                        cratesPurged++;
                                        itemsVoided += voided;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new int[]{cratesPurged, itemsVoided};
    }

    public static boolean isResourceQualifies(RESOURCE res, VoidConfig config) {
        if (res == null || res.key == null) return false;
        String k = res.key.toUpperCase();

        if (config.isAutoVoidStone() && k.contains("STONE")) return true;
        if (config.isAutoVoidClay() && (k.contains("CLAY") || k.contains("MUD"))) return true;
        if (config.isAutoVoidCoal() && k.contains("COAL")) return true;
        if (config.isAutoVoidLivestock() && (k.contains("LIVESTOCK") || k.contains("DUNG") || k.contains("WASTE"))) return true;
        if (config.isAutoVoidWood() && k.contains("WOOD")) return true;
        if (config.isAutoVoidOres() && k.contains("ORE")) return true;

        return false;
    }

    public void save(FilePutter file) {
        ensureInitialized();
        file.i(designatedCount);
        if (designatedVoidCrates != null) {
            file.bool(true);
            designatedVoidCrates.save(file);
        } else {
            file.bool(false);
        }
    }

    public void load(FileGetter file) throws IOException {
        ensureInitialized();
        designatedCount = file.i();
        boolean hasMap = file.bool();
        if (hasMap) {
            ensureInitialized();
            if (designatedVoidCrates != null) {
                designatedVoidCrates.load(file);
            }
        }
    }

    public int getDesignatedCount() {
        return designatedCount;
    }
}
