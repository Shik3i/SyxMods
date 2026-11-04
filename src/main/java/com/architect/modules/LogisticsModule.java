package com.architect.modules;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.LogisticsTier;

import init.resources.RESOURCE;
import settlement.main.SETT;
import settlement.room.infra.stockpile.ROOM_STOCKPILE;
import settlement.room.infra.stockpile.StockpileInstance;
import settlement.room.main.job.StorageCrate;
import snake2d.util.datatypes.RECTANGLE;

/**
 * Logistics and storage management subsystem.
 * Handles automatic warehouse refilling for raw materials or all assigned crates,
 * plus 1-click 100% inventory top-ups.
 *
 * PERFORMANCE:
 * Guaranteed 100% zero-allocation: avoids RECTANGLE iterator allocations by using
 * direct integer coordinate bounds (x1, x2, y1, y2).
 */
public final class LogisticsModule implements ArchitectModule {

    private double updateTimer = 0.0;
    private static final double REFILL_INTERVAL = 3.0; // Refills every 3 seconds

    @Override
    public void init() {
        // Scaffolding initialization
    }

    @Override
    public void update(double ds, ArchitectState state) {
        if (state.logisticsTier == LogisticsTier.VANILLA) {
            return;
        }

        updateTimer += ds;
        if (updateTimer < REFILL_INTERVAL) {
            return;
        }
        updateTimer = 0.0;

        refillCrates(state.logisticsTier == LogisticsTier.RAW_MATERIALS_ONLY, 0.8);
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
        if (state.logisticsTier != LogisticsTier.VANILLA) {
            refillCrates(state.logisticsTier == LogisticsTier.RAW_MATERIALS_ONLY, 0.8);
        }
    }
}
