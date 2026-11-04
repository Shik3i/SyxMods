package com.architect;

import java.io.IOException;

import com.architect.config.ArchitectState;
import com.architect.modules.BoostModule;
import com.architect.modules.ConstructionModule;
import com.architect.modules.LogisticsModule;
import com.architect.modules.SecurityModule;
import com.architect.ui.ArchitectUI;

import script.SCRIPT.SCRIPT_INSTANCE;
import snake2d.Renderer;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import view.keyboard.KEYS;

/**
 * Runtime execution instance of Syx Architect during active gameplay.
 * STRICT ZERO-ALLOCATION GUARANTEE: update(double ds) allocates zero heap objects.
 */
public final class ArchitectInstance implements SCRIPT_INSTANCE {

    private final ArchitectState state = new ArchitectState();

    // Sandbox subsystems
    private final BoostModule boostModule = new BoostModule();
    private final LogisticsModule logisticsModule = new LogisticsModule();
    private final ConstructionModule constructionModule = new ConstructionModule();
    private final SecurityModule securityModule = new SecurityModule();

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
    };

    public ArchitectInstance() {
        boostModule.init();
        logisticsModule.init();
        constructionModule.init();
        securityModule.init();
    }

    /**
     * Called 60 times per second during game simulation.
     * Guaranteed zero allocations.
     */
    @Override
    public void update(double ds) {
        boostModule.update(ds, state);
        logisticsModule.update(ds, state);
        constructionModule.update(ds, state);
        securityModule.update(ds, state);
    }

    /**
     * Propagates state modifications to all registered modules.
     */
    public void notifyStateChanged() {
        boostModule.onStateChanged(state);
        logisticsModule.onStateChanged(state);
        constructionModule.onStateChanged(state);
        securityModule.onStateChanged(state);
    }

    /**
     * Opens the in-game control interface.
     */
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
        // Zero-allocation rendering hook
    }

    @Override
    public void keyPush(KEYS key) {
        // Keyboard listening hook
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
