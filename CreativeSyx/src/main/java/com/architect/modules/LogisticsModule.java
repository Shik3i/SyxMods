package com.architect.modules;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.LogisticsTier;

import game.GAME;
import game.faction.FCredits;
import game.faction.FResources.RTYPE;
import init.resources.RESOURCE;
import settlement.main.SETT;
import settlement.room.infra.stockpile.ROOM_STOCKPILE;
import settlement.room.infra.stockpile.StockpileInstance;
import settlement.room.main.Room;
import settlement.room.main.job.StorageCrate;
import settlement.thing.THINGS;
import settlement.thing.ThingsResources;
import settlement.thing.ThingsResources.ScatteredResource;
import snake2d.util.datatypes.RECTANGLE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Logistics, storage, and treasury management subsystem.
 * Handles automatic warehouse refilling for raw materials or all assigned crates,
 * treasury maintenance (Infinite Denari >= 1,000,000), and 1-click inventory/coin top-ups.
 *
 * PERFORMANCE:
 * Guaranteed 100% zero-allocation: avoids RECTANGLE iterator allocations by using
 * direct integer coordinate bounds (x1, x2, y1, y2).
 */
public final class LogisticsModule implements ArchitectModule {

    private long lastRefillMs = 0;
    private static final long REFILL_INTERVAL_MS = 1500; // Refills at most once every 1.5 real seconds

    // High-watermark storage buffer for NO_DECREASE tier
    private short[] crateWatermark = null;
    private short[] crateResourceIndex = null;

    private static Field thingsResourcesAllField;
    private static Method thingsResourcesAllMethod;
    private static boolean thingsResourcesReflectionFailed = false;

    private static Field thingsRubbishHolderField;
    private static Field rubbishGoreField;
    private static boolean rubbishReflectionFailed = false;

    private static Field corpsesAllField;
    private static boolean corpsesReflectionFailed = false;

    @Override
    public void init() {
        // Scaffolding initialization
    }

    @Override
    public void update(double ds, ArchitectState state) {
        // 1. Treasury / Denari Management
        if (state.infiniteDenari && GAME.player() != null && GAME.player().credits() != null) {
            try {
                if (GAME.player().credits().credits() < 1_000_000.0) {
                    GAME.player().credits().set(1_000_000.0);
                }
            } catch (Throwable ignored) {
            }
        }

        if (state.logisticsTier == LogisticsTier.VANILLA) {
            return;
        }

        // Wall-clock rate limiting: exactly 1 refill check per 1.5 real-world seconds regardless of game speed
        long now = System.currentTimeMillis();
        if (now - lastRefillMs < REFILL_INTERVAL_MS) {
            return;
        }
        lastRefillMs = now;

        if (state.logisticsTier == LogisticsTier.NO_DECREASE) {
            processNoDecrease();
        } else {
            refillCrates(state.logisticsTier == LogisticsTier.RAW_MATERIALS_ONLY, 0.8);
        }
    }

    /**
     * Bottomless Crates / High-Watermark Storage:
     * Goods must be hauled in normally by player workers, but pickup by consumers or workshops
     * is infinite (stock never drops below the recorded high-watermark level).
     */
    private void processNoDecrease() {
        if (!SETT.exists() || SETT.ROOMS() == null || SETT.ROOMS().STOCKPILE == null) {
            return;
        }

        ROOM_STOCKPILE stockpile = SETT.ROOMS().STOCKPILE;
        int count = stockpile.instancesSize();
        if (count == 0) return;

        int mapArea = SETT.TAREA;
        if (crateWatermark == null || crateWatermark.length != mapArea) {
            crateWatermark = new short[mapArea];
            crateResourceIndex = new short[mapArea];
        }

        int mapW = SETT.TWIDTH;
        for (int i = 0; i < count; i++) {
            StockpileInstance instance = stockpile.getInstance(i);
            if (instance == null || instance.body() == null) {
                continue;
            }

            RECTANGLE body = instance.body();
            int x1 = body.x1();
            int x2 = body.x2();
            int y1 = body.y1();
            int y2 = body.y2();

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    if (instance.is(x, y)) {
                        StorageCrate crate = instance.crate(x, y);
                        int tileIdx = y * mapW + x;
                        if (crate != null && crate.resource() != null) {
                            short resIdx = (short) (crate.resource().index() + 1);
                            if (crateResourceIndex[tileIdx] != resIdx) {
                                crateResourceIndex[tileIdx] = resIdx;
                                crateWatermark[tileIdx] = (short) Math.min(32767, crate.amount());
                            } else {
                                int cur = crate.amount();
                                int mark = crateWatermark[tileIdx] & 0xFFFF;
                                if (cur > mark) {
                                    crateWatermark[tileIdx] = (short) Math.min(32767, cur);
                                } else if (cur < mark) {
                                    crate.amountSet(mark);
                                }
                            }
                        } else {
                            crateWatermark[tileIdx] = 0;
                            crateResourceIndex[tileIdx] = 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * Refills warehouse crates based on current tier and target capacity.
     * Zero-allocation primitive integer loop.
     */
    public void refillCrates(boolean rawOnly, double targetRatio) {
        if (!SETT.exists() || SETT.ROOMS() == null || SETT.ROOMS().STOCKPILE == null) {
            return;
        }

        ROOM_STOCKPILE stockpile = SETT.ROOMS().STOCKPILE;
        int count = stockpile.instancesSize();

        for (int i = 0; i < count; i++) {
            StockpileInstance instance = stockpile.getInstance(i);
            if (instance == null || instance.body() == null) {
                continue;
            }

            RECTANGLE body = instance.body();
            int x1 = body.x1();
            int x2 = body.x2();
            int y1 = body.y1();
            int y2 = body.y2();

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    if (instance.is(x, y)) {
                        StorageCrate crate = instance.crate(x, y);
                        if (crate != null && crate.resource() != null) {
                            RESOURCE res = crate.resource();
                            if (rawOnly && !isRawMaterial(res)) {
                                continue;
                            }

                            int capacity = instance.crateSize(res);
                            int target = (int) (capacity * targetRatio);
                            if (crate.amount() < target) {
                                crate.amountSet(target);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 1-Click Action: Immediately tops up every single assigned crate in all warehouses to 100%.
     */
    public void fillAllCratesToMax() {
        refillCrates(false, 1.0);
    }

    /**
     * 1-Click Action: Adds 1,000,000 Denari directly to the city treasury.
     */
    public void addDenari(double amount) {
        if (GAME.player() != null && GAME.player().credits() != null && amount > 0) {
            try {
                GAME.player().credits().inc(amount, FCredits.CTYPE.MISC);
            } catch (Throwable t) {
                GAME.player().credits().set(GAME.player().credits().credits() + amount);
            }
        }
    }

    /**
     * Checks whether a map coordinate is a dedicated warehouse, hauler, depot,
     * or active room storage tile where items should NOT be removed by ground cleanup.
     */
    public static boolean isProtectedStorageTile(int tx, int ty) {
        if (!SETT.exists() || !SETT.IN_BOUNDS(tx, ty) || SETT.ROOMS() == null || SETT.ROOMS().map == null) {
            return false;
        }
        Room room = SETT.ROOMS().map.get(tx, ty);
        if (room == null) {
            return false;
        }
        if (SETT.ROOMS().STOCKPILE != null && room.blueprint() == SETT.ROOMS().STOCKPILE) {
            return true;
        }
        if (SETT.ROOMS().HAULER != null && room.blueprint() == SETT.ROOMS().HAULER) {
            return true;
        }
        if (SETT.ROOMS().IMPORT != null && room.blueprint() == SETT.ROOMS().IMPORT) {
            return true;
        }
        if (SETT.ROOMS().EXPORT != null && room.blueprint() == SETT.ROOMS().EXPORT) {
            return true;
        }
        if (SETT.ROOMS().SUPPLY != null && room.blueprint() == SETT.ROOMS().SUPPLY) {
            return true;
        }
        try {
            if (room.storage(tx, ty) != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (room.resourceTile(tx, ty) != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static ScatteredResource[] getScatteredResourcesArray() {
        if (!SETT.exists() || SETT.THINGS() == null || SETT.THINGS().resources == null) {
            return null;
        }
        if (thingsResourcesReflectionFailed) {
            return null;
        }
        try {
            if (thingsResourcesAllField == null && thingsResourcesAllMethod == null) {
                try {
                    thingsResourcesAllField = ThingsResources.class.getDeclaredField("all");
                    thingsResourcesAllField.setAccessible(true);
                } catch (Throwable t1) {
                    thingsResourcesAllMethod = ThingsResources.class.getDeclaredMethod("all");
                    thingsResourcesAllMethod.setAccessible(true);
                }
            }
            if (thingsResourcesAllField != null) {
                return (ScatteredResource[]) thingsResourcesAllField.get(SETT.THINGS().resources);
            }
            if (thingsResourcesAllMethod != null) {
                return (ScatteredResource[]) thingsResourcesAllMethod.invoke(SETT.THINGS().resources);
            }
        } catch (Throwable t) {
            thingsResourcesReflectionFailed = true;
        }
        return null;
    }

    /**
     * Cleans up all loose resource stacks on the ground (outside of warehouses and room storages).
     * @param toStockpile if true, resources are added to player faction inventory before being removed.
     * @return int array [stacksRemoved, totalUnits]
     */
    public int[] clearGroundResources(boolean toStockpile) {
        ScatteredResource[] all = getScatteredResourcesArray();
        if (all == null) {
            return new int[]{0, 0};
        }

        int stacksCount = 0;
        int totalUnits = 0;

        for (int i = 0; i < all.length; i++) {
            ScatteredResource sr = all[i];
            if (sr != null && !sr.isRemoved()) {
                int tx = sr.x();
                int ty = sr.y();
                if (isProtectedStorageTile(tx, ty)) {
                    continue;
                }
                try {
                    int am = sr.amount();
                    RESOURCE res = sr.resource();
                    if (toStockpile && res != null && am > 0 && GAME.player() != null && GAME.player().res() != null) {
                        GAME.player().res().inc(res, RTYPE.PRODUCED, am);
                        totalUnits += am;
                    }
                    sr.remove();
                    stacksCount++;
                } catch (Throwable ignored) {
                }
            }
        }

        return new int[]{stacksCount, totalUnits};
    }

    /**
     * Cleans up all waste, rubbish, and corpses across the map.
     * @return number of debris items removed
     */
    public int clearWaste() {
        if (!SETT.exists() || SETT.THINGS() == null) {
            return 0;
        }
        int removedCount = 0;

        // 1. Rubbish
        if (SETT.THINGS().rubbish != null) {
            try {
                if (!rubbishReflectionFailed && thingsRubbishHolderField == null) {
                    try {
                        thingsRubbishHolderField = settlement.thing.ThingsRubbish.class.getDeclaredField("rubbish");
                        thingsRubbishHolderField.setAccessible(true);
                        Class<?> holderClass = thingsRubbishHolderField.getType();
                        rubbishGoreField = holderClass.getDeclaredField("gore");
                        rubbishGoreField.setAccessible(true);
                    } catch (Throwable t) {
                        rubbishReflectionFailed = true;
                    }
                }
                if (thingsRubbishHolderField != null && rubbishGoreField != null) {
                    Object holder = thingsRubbishHolderField.get(SETT.THINGS().rubbish);
                    if (holder != null) {
                        Object[] gore = (Object[]) rubbishGoreField.get(holder);
                        if (gore != null) {
                            for (int i = 0; i < gore.length; i++) {
                                if (gore[i] instanceof THINGS.Thing) {
                                    THINGS.Thing t = (THINGS.Thing) gore[i];
                                    if (!t.isRemoved()) {
                                        t.remove();
                                        removedCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 2. Corpses
        if (SETT.THINGS().corpses != null) {
            try {
                if (corpsesAllField == null && !corpsesReflectionFailed) {
                    try {
                        corpsesAllField = settlement.thing.ThingsCorpses.class.getDeclaredField("corpses");
                        corpsesAllField.setAccessible(true);
                    } catch (Throwable t) {
                        corpsesReflectionFailed = true;
                    }
                }
                if (corpsesAllField != null) {
                    Object[] corpses = (Object[]) corpsesAllField.get(SETT.THINGS().corpses);
                    if (corpses != null) {
                        for (int i = 0; i < corpses.length; i++) {
                            if (corpses[i] instanceof THINGS.Thing) {
                                THINGS.Thing t = (THINGS.Thing) corpses[i];
                                if (!t.isRemoved()) {
                                    t.remove();
                                    removedCount++;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return removedCount;
    }

    private static boolean isRawMaterial(RESOURCE res) {
        if (res == null || res.key == null) return false;
        String k = res.key;
        return k.equalsIgnoreCase("WOOD") || k.equalsIgnoreCase("STONE") ||
               k.equalsIgnoreCase("COAL") || k.equalsIgnoreCase("CLAY") ||
               k.equalsIgnoreCase("GRAIN") || k.equalsIgnoreCase("COTTON") ||
               k.equalsIgnoreCase("LEATHER") || k.contains("ORE") || k.contains("ore") ||
               k.contains("FIBER") || k.contains("fiber");
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        if (state.infiniteDenari && GAME.player() != null && GAME.player().credits() != null) {
            try {
                if (GAME.player().credits().credits() < 1_000_000.0) {
                    GAME.player().credits().set(1_000_000.0);
                }
            } catch (Throwable ignored) {
            }
        }
        if (state.logisticsTier == LogisticsTier.NO_DECREASE) {
            processNoDecrease();
        } else if (state.logisticsTier != LogisticsTier.VANILLA) {
            refillCrates(state.logisticsTier == LogisticsTier.RAW_MATERIALS_ONLY, 0.8);
        }
    }
}
