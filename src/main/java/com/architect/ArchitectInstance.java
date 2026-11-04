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
import snake2d.util.datatypes.Rec;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import view.keyboard.KEYS;

/**
 * Runtime execution instance of Syx Architect during active gameplay.
 * STRICT ZERO-ALLOCATION GUARANTEE in 60Hz update and render loops.
 * Features a rock-solid, edge-triggered F10 debounced toggle and clean unobtrusive presentation.
 */
public final class ArchitectInstance implements SCRIPT_INSTANCE {

    private static final String BANNER_TEXT = "Syx Architect v1.0.0 active  [F10]";

    private final ArchitectState state = new ArchitectState();

    // Sandbox subsystems
    private final BoostModule boostModule = new BoostModule();
    private final LogisticsModule logisticsModule = new LogisticsModule();
    private final ConstructionModule constructionModule = new ConstructionModule();
    private final SecurityModule securityModule = new SecurityModule();
    private final WorldModule worldModule = new WorldModule();

    // Rock-solid edge-trigger and debounce tracking for F10 hotkey
    private boolean wasF10Down = false;
    private double f10Cooldown = 0.0;

    // Centered subtle welcome banner bounds (only visible for 4s on load)
    private final Rec bannerBounds = new Rec();
    private double bannerTimer = 4.0;

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
        // 1. Rock-solid F10 Hotkey Edge-Trigger with Debounce Cooldown
        if (f10Cooldown > 0.0) {
            f10Cooldown -= ds;
        }

        boolean f10 = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_F10);
        if (f10 && !wasF10Down && f10Cooldown <= 0.0) {
            toggleUI();
            f10Cooldown = 0.3; // 300ms debounce cooldown
        }
        wasF10Down = f10;

        // 2. ESC key closes open panel cleanly
        if (ArchitectUI.isOpen()) {
            boolean esc = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_ESCAPE);
            if (esc) {
                ArchitectUI.close();
            }
        }

        // 3. Subsystem simulation updates
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
        // Reset subtle banner timer upon loading game
        bannerTimer = 4.0;
    }

    @Override
    public void render(Renderer r, float ds) {
        if (C.DIM() == null) return;

        // Render discrete, non-intrusive notification banner at the top center on game load
        if (bannerTimer > 0) {
            bannerTimer -= ds;
            int bannerW = 320;
            int bannerH = 24;
            int bannerX = (C.DIM().width() - bannerW) / 2;
            int bannerY = 12;
            bannerBounds.set(bannerX, bannerX + bannerW, bannerY, bannerY + bannerH);

            COLOR.BLACK.render(r, bannerBounds, 0);
            COLOR.WHITE35.renderFrame(r, bannerBounds, 1, 0);
            if (UI.FONT() != null && UI.FONT().S != null) {
                int tw = UI.FONT().S.width(BANNER_TEXT);
                UI.FONT().S.render(r, BANNER_TEXT, bannerX + (bannerW - tw) / 2, bannerY + 5);
            }
        }
    }

    @Override
    public void mouseClick(MButt button) {
        // No floating HUD box to obstruct clicks
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
