package com.architect.modules;

import com.architect.config.ArchitectState;

import game.GAME;
import game.battle.div.Div;
import settlement.battle.invasion.Invador;
import settlement.main.SETT;
import snake2d.util.sets.LIST;
import world.WORLD;
import world.entity.army.WArmy;
import world.entity.army.WArmyConstructor;

/**
 * Military defense and peaceful sandbox security subsystem.
 * Handles raid auto-cancellation, peace enforcement, and the 1-click combat instant victory.
 *
 * PERFORMANCE:
 * Guaranteed 100% zero-allocation: uses indexed loops over LIST collections instead of foreach iterators.
 */
public final class SecurityModule implements ArchitectModule {

    private long lastCheckMs = 0;
    private static final long INTERVAL_MS = 1500; // 1.5 real seconds

    @Override
    public void init() {
        // Scaffolding initialization
    }

    @Override
    public void update(double ds, ArchitectState state) {
        if (!state.raidImmunity) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCheckMs < INTERVAL_MS) {
            return;
        }
        lastCheckMs = now;

        cancelHostileRaids();
    }

    /**
     * Neutralizes all approaching raids, settlement invasions, and world map sieges against the player.
     */
    public void cancelHostileRaids() {
        // 1. Settlement invasions
        try {
            if (SETT.exists() && SETT.INVADOR() != null) {
                Invador invador = SETT.INVADOR();
                if (invador.invading() || invador.invadingPending()) {
                    invador.cancel(0);
                }
            }
        } catch (Exception ignored) {
        }

        // 2. World map sieges targeting player
        try {
            if (WORLD.ENTITIES() != null && WORLD.ENTITIES().armies != null && GAME.player() != null) {
                WArmyConstructor ac = WORLD.ENTITIES().armies;
                int count = ac.armies();
                for (int i = 0; i < count; i++) {
                    WArmy army = ac.get(i);
                    if (army != null && army.faction() != GAME.player()) {
                        if (army.besieging() != null || army.raiding()) {
                            army.stop();
                            army.disband();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 1-Click Action: Instantly wins active combat across all 3 layers:
     * 1. Tactical Battle: routes all enemy divisions immediately (zero-allocation loop).
     * 2. Settlement Invasion: cancels/resolves active city attack.
     * 3. World Map: disbands hostile siege armies.
     */
    public void instantWinCombat() {
        // 1. Tactical Battle
        try {
            if (GAME.ARMIES() != null && GAME.ARMIES().enemy() != null) {
                LIST<Div> divs = GAME.ARMIES().enemy().divisions();
                if (divs != null) {
                    int count = divs.size();
                    for (int i = 0; i < count; i++) {
                        Div div = divs.get(i);
                        if (div != null && div.active()) {
                            GAME.ARMIES().factors.reportRout(div);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Settlement Invasion
        try {
            if (SETT.exists() && SETT.INVADOR() != null) {
                Invador invador = SETT.INVADOR();
                if (invador.invading() || invador.invadingPending()) {
                    invador.cancel(0);
                }
            }
        } catch (Exception ignored) {
        }

        // 3. World Map Hostile Armies
        try {
            if (WORLD.ENTITIES() != null && WORLD.ENTITIES().armies != null && GAME.player() != null) {
                WArmyConstructor ac = WORLD.ENTITIES().armies;
                int count = ac.armies();
                for (int i = 0; i < count; i++) {
                    WArmy army = ac.get(i);
                    if (army != null && army.faction() != GAME.player()) {
                        if (army.besieging() != null || army.raiding()) {
                            army.stop();
                            army.disband();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        if (state.raidImmunity) {
            cancelHostileRaids();
        }
    }
}
