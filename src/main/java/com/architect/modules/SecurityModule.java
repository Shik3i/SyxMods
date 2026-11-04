package com.architect.modules;

import com.architect.config.ArchitectState;

import settlement.battle.invasion.Invador;
import settlement.main.SETT;

/**
 * Military defense and peaceful sandbox security subsystem.
 * Handles raid auto-cancellation and peace enforcement.
 */
public final class SecurityModule implements ArchitectModule {

    private double timer = 0.0;
    private static final double INTERVAL = 1.0;

    @Override
    public void init() {
        // Scaffolding initialization
    }

    @Override
    public void update(double ds, ArchitectState state) {
        if (!state.raidImmunity) {
            return;
        }

        timer += ds;
        if (timer < INTERVAL) {
            return;
        }
        timer = 0.0;

        // Neutralize approaching invasions
        try {
            if (SETT.exists() && SETT.INVADOR() != null) {
                Invador invador = SETT.INVADOR();
                if (invador.invading()) {
                    invador.cancel(0);
                }
            }
        } catch (Exception ignored) {
            // Safe guard against uninitialized settlement states
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        // React to raid immunity toggles
    }
}
