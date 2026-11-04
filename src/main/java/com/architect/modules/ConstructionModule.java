package com.architect.modules;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.MaintenanceTier;

import settlement.main.SETT;
import settlement.maintenance.MAINTENANCE;
import settlement.room.main.construction.CONSTRUCTION;

/**
 * Creative construction and maintenance automation subsystem.
 * Supports instant room/structure completion and zero-degradation auto-repair.
 */
public final class ConstructionModule implements ArchitectModule {

    private double tickTimer = 0.0;
    private static final double TICK_STEP = 0.5; // Every half second

    @Override
    public void init() {
        // Scaffolding initialization
    }

    @Override
    public void update(double ds, ArchitectState state) {
        tickTimer += ds;
        if (tickTimer < TICK_STEP) {
            return;
        }
        tickTimer = 0.0;

        // 1. Instant Construction
        if (state.instantBuildActive && SETT.ROOMS() != null && SETT.ROOMS().construction != null) {
            CONSTRUCTION construction = SETT.ROOMS().construction;
            if (construction.instances() > 0 && construction.isser != null) {
                int width = SETT.TWIDTH;
                int height = SETT.THEIGHT;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (construction.isser.is(x, y)) {
                            construction.construct(x, y);
                        }
                    }
                }
            }
        }

        // 2. Zero-Degradation Maintenance
        if (state.maintenanceTier == MaintenanceTier.ZERO_DEGRADATION && SETT.MAINTENANCE() != null) {
            MAINTENANCE maintenance = SETT.MAINTENANCE();
            if (maintenance.needs != null) {
                int width = SETT.TWIDTH;
                int height = SETT.THEIGHT;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (maintenance.needs.is(x, y)) {
                            maintenance.maintain(x, y);
                        }
                    }
                }
            }
        }
    }

    /**
     * Triggers a one-shot instant completion of all currently planned construction sites.
     */
    public void triggerInstantBuildAll() {
        if (SETT.ROOMS() == null || SETT.ROOMS().construction == null) {
            return;
        }
        CONSTRUCTION construction = SETT.ROOMS().construction;
        if (construction.isser == null) {
            return;
        }

        int width = SETT.TWIDTH;
        int height = SETT.THEIGHT;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (construction.isser.is(x, y)) {
                    construction.construct(x, y);
                }
            }
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        if (state.instantBuildActive) {
            triggerInstantBuildAll();
        }
    }
}
