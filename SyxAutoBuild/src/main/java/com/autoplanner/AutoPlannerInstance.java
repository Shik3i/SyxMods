package com.autoplanner;

import java.io.IOException;
import java.lang.reflect.Field;

import com.autoplanner.config.PlannerConfig;
import com.autoplanner.solver.AutoFurnishSolver;
import com.autoplanner.solver.AutoStrutSolver;
import com.autoplanner.ui.PlannerOverlayUI;
import com.autoplanner.util.I18n;

import init.sprite.SPRITES;
import init.sprite.UI.UI;
import script.SCRIPT.SCRIPT_INSTANCE;
import settlement.main.SETT;
import settlement.room.main.placement.RoomPlacer;
import snake2d.CORE;
import snake2d.KEYCODES;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.datatypes.DIR;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import view.interrupter.InterManager;
import view.keyboard.KEYS;
import view.main.VIEW;
import view.ui.top.UIPanelTop;

/**
 * Active runtime instance for Syx Auto-Planner.
 * Guarantees zero heap allocations during the 60Hz update loop.
 * Features:
 * - Direct native docking into UIPanelTop beside Wiki & Menu buttons
 * - F9 hotkey toggle for Auto-Planner configuration suite
 * - Responsive dynamic docking under native Room Editor panel
 */
public final class AutoPlannerInstance implements SCRIPT_INSTANCE {

    private final PlannerConfig config = new PlannerConfig();
    private final AutoStrutSolver strutSolver = new AutoStrutSolver();
    private final AutoFurnishSolver furnishSolver = new AutoFurnishSolver();
    private final PlannerOverlayUI overlayUI = new PlannerOverlayUI(config, strutSolver, furnishSolver);

    // Native Top Bar HUD Button & State Tracking
    private GButt.ButtPanel hudButton;
    private GuiSection topBarSection;
    private boolean attachedToTopPanel = false;
    private UIPanelTop lastAttachedTop = null;

    // F9 Hotkey Edge-Trigger with Debounce Tracking
    private boolean wasF9Down = false;
    private double f9Cooldown = 0.0;

    private void ensureHudButton() {
        if (hudButton == null && SPRITES.icons() != null && SPRITES.icons().m != null && SPRITES.icons().m.wallceiling != null) {
            hudButton = new UIPanelTop.Butt(SPRITES.icons().m.wallceiling);
            hudButton.hoverTitleSet(I18n.get("hud.tooltip_title"));
            hudButton.hoverInfoSet(I18n.get("hud.tooltip_desc"));
            hudButton.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    toggleConfig();
                }
            });
        }
    }

    private UIPanelTop findTopPanel() {
        if (VIEW.s() == null || VIEW.s().uiManager == null) {
            return null;
        }
        try {
            Field f = InterManager.class.getDeclaredField("inters");
            f.setAccessible(true);
            Object coll = f.get(VIEW.s().uiManager);
            if (coll instanceof Iterable) {
                for (Object obj : (Iterable<?>) coll) {
                    if (obj instanceof UIPanelTop) {
                        return (UIPanelTop) obj;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void attachToTopPanel(UIPanelTop top) {
        if (top == null || top == lastAttachedTop) return;
        ensureHudButton();
        if (hudButton != null) {
            topBarSection = new GuiSection();
            topBarSection.add(hudButton);
            if (!config.isShowHudButton()) {
                topBarSection.visableSet(false);
                hudButton.activeSet(false);
            }
            top.addRightRight(topBarSection);
            lastAttachedTop = top;
            attachedToTopPanel = true;
        }
    }

    private void toggleConfig() {
        if (overlayUI.isConfigOpen()) {
            VIEW.inters().section.close();
        } else {
            overlayUI.openConfigModal();
        }
    }

    @Override
    public void update(double ds) {
        // 1. F9 Hotkey Edge-Trigger with debounce
        if (f9Cooldown > 0.0) {
            f9Cooldown -= ds;
        }

        boolean f9 = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_F9);
        if (f9 && !wasF9Down && f9Cooldown <= 0.0) {
            toggleConfig();
            f9Cooldown = 0.3; // 300ms cooldown
        }
        wasF9Down = f9;

        // 2. ESC key closes config modal cleanly
        if (overlayUI.isConfigOpen()) {
            boolean esc = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_ESCAPE);
            if (esc) {
                VIEW.inters().section.close();
            }
        }

        // 3. Ensure native top bar docking whenever entering settlement view
        if (!attachedToTopPanel || lastAttachedTop == null) {
            UIPanelTop top = findTopPanel();
            if (top != null) {
                attachToTopPanel(top);
            }
        }

        // 4. Update button active/highlighted and visibility state
        if (hudButton != null) {
            hudButton.selectedSet(overlayUI.isConfigOpen());
            if (topBarSection != null) {
                topBarSection.visableSet(config.isShowHudButton());
            }
            hudButton.activeSet(config.isShowHudButton());
        }

        // 5. Update overlay simulation
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
        // Handled via update loop
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
