package com.autoplanner;

import java.io.IOException;

import com.autoplanner.config.PlannerConfig;
import com.autoplanner.solver.AutoFurnishSolver;
import com.autoplanner.solver.AutoStrutSolver;
import com.autoplanner.ui.PlannerOverlayUI;

import script.SCRIPT.SCRIPT_INSTANCE;
import settlement.main.SETT;
import settlement.room.main.placement.RoomPlacer;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import view.keyboard.KEYS;

/**
 * Active runtime instance for Syx Auto-Planner.
 * Guarantees zero heap allocations during the 60Hz update loop.
 */
public final class AutoPlannerInstance implements SCRIPT_INSTANCE {

    private final PlannerConfig config = new PlannerConfig();
    private final AutoStrutSolver strutSolver = new AutoStrutSolver();
    private final AutoFurnishSolver furnishSolver = new AutoFurnishSolver();
    private final PlannerOverlayUI overlayUI = new PlannerOverlayUI(config, strutSolver, furnishSolver);

    @Override
    public void update(double ds) {
        overlayUI.update(ds);
    }

    @Override
    public void render(Renderer r, float ds) {
        overlayUI.render(r, ds);
    }

    @Override
    public void mouseClick(MButt button) {
        overlayUI.mouseClick(button);
    }

    @Override
    public void hoverTimer(double mouseTimer, util.gui.misc.GBox text) {
        overlayUI.hoverTimer(mouseTimer, text);
    }

    @Override
    public void hover(snake2d.util.datatypes.COORDINATE mCoo, boolean mouseHasMoved) {
        overlayUI.hover(mCoo, mouseHasMoved);
    }

    @Override
    public void keyPush(KEYS key) {
        // Can be extended with custom key bindings if desired
    }

    @Override
    public void save(FilePutter file) {
        config.save(file);
    }

    @Override
    public void load(FileGetter file) throws IOException {
        config.load(file);
    }

    public PlannerConfig getConfig() {
        return config;
    }

    public AutoStrutSolver getStrutSolver() {
        return strutSolver;
    }

    public AutoFurnishSolver getFurnishSolver() {
        return furnishSolver;
    }

    public PlannerOverlayUI getOverlayUI() {
        return overlayUI;
    }
}
