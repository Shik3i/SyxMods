package com.architect;

import java.io.IOException;

import com.architect.config.ArchitectState;
import com.architect.modules.BoostModule;
import com.architect.modules.ConstructionModule;
import com.architect.modules.LogisticsModule;
import com.architect.modules.SecurityModule;
import com.architect.modules.WorldModule;
import com.architect.ui.ArchitectUI;

import init.constant.C;
import init.race.Race;
import init.sprite.UI.UI;
import script.SCRIPT.SCRIPT_INSTANCE;
import snake2d.CORE;
import snake2d.KEYCODES;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Rec;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import view.keyboard.KEYS;
import view.main.VIEW;

/**
 * Runtime execution instance of Syx Architect during active gameplay.
 * STRICT ZERO-ALLOCATION GUARANTEE: update(double ds) and render(Renderer r, float ds)
 * allocate zero heap objects.
 */
public final class ArchitectInstance implements SCRIPT_INSTANCE {

    private static final String HUD_LABEL = "Syx Architect [F10]";

    private final ArchitectState state = new ArchitectState();

    // Sandbox subsystems
    private final BoostModule boostModule = new BoostModule();
    private final LogisticsModule logisticsModule = new LogisticsModule();
    private final ConstructionModule constructionModule = new ConstructionModule();
    private final SecurityModule securityModule = new SecurityModule();
    private final WorldModule worldModule = new WorldModule();

    // Zero-allocation input state
    private boolean wasF10Down = false;

    // HUD button area (top-right screen position, 140x24 pixels)
    private final Rec hudBounds = new Rec();

    // Reusable UI callback to prevent lambda allocation on each open
    private final ArchitectUI.UICallback uiCallback = new ArchitectUI.UICallback() {
        @Override
        public void onStateUpdated() {
            notifyStateChanged();
        }

        @Override
        public void onInstantBuildRequested() {
            constructionModule.triggerInstantBuildAll();
        }

        @Override
        public void onFillCratesRequested() {
            logisticsModule.fillAllCratesToMax();
        }

        @Override
        public void onAddDenariRequested() {
            logisticsModule.addDenari(1_000_000.0);
        }

        @Override
        public void onWinCombatRequested() {
            securityModule.instantWinCombat();
        }

        @Override
        public void onSpawnImmigrantsRequested(Race race, int count) {
            worldModule.spawnImmigrants(race, count);
        }

        @Override
        public void onSpawnAllPlayableRequested(int countPerRace) {
            worldModule.spawnAllPlayable(countPerRace);
        }

        @Override
        public void onMaxFertilityRequested() {
            worldModule.maximizeSoilMoistureNow();
        }
    };

    public ArchitectInstance() {
        boostModule.init();
        logisticsModule.init();
        constructionModule.init();
        securityModule.init();
        worldModule.init();
    }

    /**
     * Called 60 times per second during game simulation.
     * Guaranteed zero allocations.
     */
    @Override
    public void update(double ds) {
        // 1. Direct F10 Hotkey polling (always active, no debug mode required!)
        boolean f10 = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_F10);
        if (f10 && !wasF10Down) {
            toggleUI();
        }
        wasF10Down = f10;

        // 2. Subsystem simulation updates
        boostModule.update(ds, state);
        logisticsModule.update(ds, state);
        constructionModule.update(ds, state);
        securityModule.update(ds, state);
        worldModule.update(ds, state);
    }

    /**
     * Propagates state modifications to all registered modules.
     */
    public void notifyStateChanged() {
        boostModule.onStateChanged(state);
        logisticsModule.onStateChanged(state);
        constructionModule.onStateChanged(state);
        securityModule.onStateChanged(state);
        worldModule.onStateChanged(state);
    }

    /**
     * Opens or closes the in-game control interface.
     */
    public void toggleUI() {
        ArchitectUI.toggle(state, uiCallback);
    }

    public void openUI() {
        ArchitectUI.open(state, uiCallback);
    }

    @Override
    public void save(FilePutter file) {
        state.save(file);
    }

    @Override
    public void load(FileGetter file) throws IOException {
        state.load(file);
        notifyStateChanged();
    }

    @Override
    public void render(Renderer r, float ds) {
        // Render discreet HUD shortcut button in top bar (Zero allocation)
        if (C.DIM() != null) {
            int hudX = Math.max(0, C.DIM().width() - 150);
            int hudY = 4;
            int hudW = 140;
            int hudH = 24;
            hudBounds.set(hudX, hudX + hudW, hudY, hudY + hudH);

            COLOR.BLACK.render(r, hudBounds, 0);
            COLOR.WHITE20.renderFrame(r, hudBounds, 1, 0);
            if (UI.FONT() != null && UI.FONT().S != null) {
                UI.FONT().S.render(r, HUD_LABEL, hudX + 8, hudY + 5);
            }
        }
    }

    @Override
    public void mouseClick(MButt button) {
        if (button == MButt.LEFT) {
            // Guard: Ignore HUD clicks if any modal interrupter is active
            if (VIEW.inters() != null && VIEW.inters().section != null && VIEW.inters().section.isActivated()) {
                return;
            }
            COORDINATE mouseCoo = CORE.getInput().getMouse().getCoo();
            if (mouseCoo != null && hudBounds.holdsPoint(mouseCoo)) {
                toggleUI();
            }
        }
    }

    @Override
    public void keyPush(KEYS key) {
        // Auxiliary key handler
    }

    @Override
    public boolean handleBrokenSavedState() {
        state.resetToDefaults();
        notifyStateChanged();
        return true;
    }

    public ArchitectState getState() {
        return state;
    }
}
