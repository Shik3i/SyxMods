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
import util.gui.misc.GButt;
import view.keyboard.KEYS;
import view.main.VIEW;

/**
 * Runtime execution instance of Syx Architect during active gameplay.
 * STRICT ZERO-ALLOCATION GUARANTEE in update and render loops.
 * Features a permanent, fully-interactive top HUD button and non-intrusive start banner.
 */
public final class ArchitectInstance implements SCRIPT_INSTANCE {

    private static final String HUD_LABEL = "Syx Architect [F10]";
    private static final String BANNER_TEXT = "Syx Architect active  |  Press [F10] or Click Top Button";

    private final ArchitectState state = new ArchitectState();

    // Sandbox subsystems
    private final BoostModule boostModule = new BoostModule();
    private final LogisticsModule logisticsModule = new LogisticsModule();
    private final ConstructionModule constructionModule = new ConstructionModule();
    private final SecurityModule securityModule = new SecurityModule();
    private final WorldModule worldModule = new WorldModule();

    // Zero-allocation input state tracking
    private boolean wasF10Down = false;
    private boolean wasLeftMouseDown = false;

    // HUD button area (top-right screen position, right next to speed controls)
    private final Rec hudBounds = new Rec();
    private final Rec bannerBounds = new Rec();

    // Subtle welcome banner timer (in seconds) - fades away cleanly without popping up modals
    private double bannerTimer = 5.0;

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
        // 1. Direct F10 Hotkey polling (always active)
        boolean f10 = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_F10);
        if (f10 && !wasF10Down) {
            toggleUI();
        }
        wasF10Down = f10;

        // 2. Direct mouse click polling for HUD button (catches clicks even if interrupters consume callback)
        boolean isLeftDown = MButt.LEFT.isDown();
        if (isLeftDown && !wasLeftMouseDown) {
            COORDINATE mouseCoo = CORE.getInput().getMouse().getCoo();
            if (mouseCoo != null && hudBounds.holdsPoint(mouseCoo)) {
                toggleUI();
            }
        }
        wasLeftMouseDown = isLeftDown;

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
        bannerTimer = 5.0;
    }

    @Override
    public void render(Renderer r, float ds) {
        if (C.DIM() == null) return;

        // --- 1. Render permanent, native-styled HUD Button in Top Bar ---
        int hudW = 160;
        int hudH = 26;
        int hudX = Math.max(10, C.DIM().width() - 380);
        int hudY = 4;
        hudBounds.set(hudX, hudX + hudW, hudY, hudY + hudH);

        COORDINATE mouseCoo = CORE.getInput().getMouse().getCoo();
        boolean isHover = mouseCoo != null && hudBounds.holdsPoint(mouseCoo);
        boolean isPressed = isHover && MButt.LEFT.isDown();
        boolean isOpen = ArchitectUI.isOpen();

        // Native button styling matching Songs of Syx UI panels
        GButt.ButtPanel.renderBG(r, isHover, isPressed, isOpen, hudBounds);
        GButt.ButtPanel.renderFrame(r, isHover, isPressed, isOpen, hudBounds);

        if (UI.FONT() != null && UI.FONT().S != null) {
            int textW = UI.FONT().S.width(HUD_LABEL);
            int textX = hudBounds.x1() + (hudBounds.width() - textW) / 2;
            int textY = hudBounds.y1() + (hudBounds.height() - UI.FONT().S.height()) / 2;
            UI.FONT().S.render(r, HUD_LABEL, textX, textY);
        }

        // --- 2. Render discrete welcome banner on game load (Non-intrusive, fades after 5s) ---
        if (bannerTimer > 0) {
            bannerTimer -= ds;
            int bannerW = 420;
            int bannerH = 28;
            int bannerX = (C.DIM().width() - bannerW) / 2;
            int bannerY = 42;
            bannerBounds.set(bannerX, bannerX + bannerW, bannerY, bannerY + bannerH);

            COLOR.BLACK.render(r, bannerBounds, 0);
            COLOR.WHITE35.renderFrame(r, bannerBounds, 1, 0);
            if (UI.FONT() != null && UI.FONT().S != null) {
                int tw = UI.FONT().S.width(BANNER_TEXT);
                UI.FONT().S.render(r, BANNER_TEXT, bannerX + (bannerW - tw) / 2, bannerY + 6);
            }
        }
    }

    @Override
    public void mouseClick(MButt button) {
        if (button == MButt.LEFT) {
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
