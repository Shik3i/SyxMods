package com.architect.modules;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.MultiplierTier;

import settlement.main.SETT;
import settlement.room.infra.stockpile.ROOM_STOCKPILE;
import settlement.room.infra.stockpile.StockpileInstance;

/**
 * Logistics and storage management subsystem.
 * Handles stockpile crate capacities and resource hauling multipliers.
 */
public final class LogisticsModule implements ArchitectModule {

    private double updateTimer = 0.0;
    private static final double CHECK_INTERVAL = 2.0; // Check periodically every 2 seconds

    @Override
    public void init() {
        // Scaffolding initialization
    }

    @Override
    public void update(double ds, ArchitectState state) {
        if (state.infiniteStorageTier == MultiplierTier.OFF) {
            return;
        }

        updateTimer += ds;
        if (updateTimer < CHECK_INTERVAL) {
            return;
        }
        updateTimer = 0.0;

        // Periodic logistics enforcement (zero-allocation)
        if (SETT.ROOMS() == null || SETT.ROOMS().STOCKPILE == null) {
            return;
        }

        ROOM_STOCKPILE stockpile = SETT.ROOMS().STOCKPILE;
        int count = stockpile.instancesSize();
        for (int i = 0; i < count; i++) {
            StockpileInstance instance = stockpile.getInstance(i);
            if (instance != null) {
                // Future expansion: dynamic crate capacity expansion
            }
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        // React to storage tier changes
    }
}
