package com.architect.modules;

import com.architect.config.ArchitectState;

import game.GAME;
import game.battle.div.Div;
import settlement.battle.invasion.Invador;
import settlement.main.SETT;
import world.WORLD;
import world.entity.army.WArmy;
import world.entity.army.WArmyConstructor;

/**
 * Military defense and peaceful sandbox security subsystem.
 * Handles raid auto-cancellation, peace enforcement, and the 1-click combat instant victory.
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
            if (WORLD.ENTITIES() != null && WORLD.ENTITIES().armies != null) {
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
     * 1. Tactical Battle: routes all enemy divisions immediately.
     * 2. Settlement Invasion: cancels/resolves active city attack.
     * 3. World Map: disbands hostile siege armies.
     */
    public void instantWinCombat() {
        // 1. Tactical Battle
        try {
            if (GAME.ARMIES() != null && GAME.ARMIES().enemy() != null) {
                for (Div div : GAME.ARMIES().enemy().divisions()) {
                    if (div != null && div.active()) {
                        GAME.ARMIES().factors.reportRout(div);
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
            if (WORLD.ENTITIES() != null && WORLD.ENTITIES().armies != null) {
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
