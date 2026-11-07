package com.treezones;

import java.io.IOException;
import java.lang.reflect.Field;

import com.treezones.config.TreeZoneConfig;
import com.treezones.ui.TreeZonesOverlayUI;
import com.treezones.util.I18n;
import com.treezones.zone.TreeZoneManager;

import init.sprite.SPRITES;
import script.SCRIPT.SCRIPT_INSTANCE;
import snake2d.CORE;
import snake2d.KEYCODES;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import view.interrupter.InterManager;
import view.keyboard.KEYS;
import view.main.VIEW;
import view.ui.top.UIPanelTop;

/**
 * Active runtime instance for Syx Tree Zones.
 * Guarantees zero heap allocations during the 60Hz update loop.
 */
public final class TreeZonesInstance implements SCRIPT_INSTANCE {

    private final TreeZoneConfig config = new TreeZoneConfig();
    private final TreeZoneManager zoneManager = new TreeZoneManager();
    private final TreeZonesOverlayUI overlayUI = new TreeZonesOverlayUI(config, zoneManager);

    // Native Top Bar HUD Button & State Tracking
    private GButt.ButtPanel hudButton;
    private GuiSection topBarSection;
    private boolean attachedToTopPanel = false;
    private UIPanelTop lastAttachedTop = null;

    // F8 Hotkey Edge-Trigger with Debounce Tracking
    private boolean wasF8Down = false;
    private double f8Cooldown = 0.0;

    private void ensureHudButton() {
        if (hudButton == null && SPRITES.icons() != null && SPRITES.icons().m != null && SPRITES.icons().m.wallceiling != null) {
            hudButton = new UIPanelTop.Butt(SPRITES.icons().m.wallceiling);
            hudButton.hoverTitleSet(I18n.get("hud.tooltip_title"));
            hudButton.hoverInfoSet(I18n.get("hud.tooltip_desc"));
            hudButton.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    overlayUI.toggleOpen();
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

    @Override
    public void update(double ds) {
        // 1. F8 Hotkey Edge-Trigger with debounce
        if (f8Cooldown > 0.0) {
            f8Cooldown -= ds;
        }

        boolean f8 = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_F8);
        if (f8 && !wasF8Down && f8Cooldown <= 0.0) {
            overlayUI.toggleOpen();
            f8Cooldown = 0.3; // 300ms cooldown
        }
        wasF8Down = f8;

        // 2. ESC key closes toolbar if open
        if (overlayUI.isOpen()) {
            boolean esc = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_ESCAPE);
            if (esc) {
                overlayUI.setOpen(false);
            }
        }

        // 3. Ensure native top bar docking whenever entering settlement view
        if (!attachedToTopPanel || lastAttachedTop == null) {
            UIPanelTop top = findTopPanel();
            if (top != null) {
                attachToTopPanel(top);
            }
        }

        // 4. Ensure Interrupter is registered in SettView.uiManager to absorb clicks
        if (VIEW.s() != null && VIEW.s().uiManager != null) {
            overlayUI.ensureInterrupterAttached(VIEW.s().uiManager);
        }

        // 5. Update button active/highlighted and visibility state
        if (hudButton != null) {
            hudButton.selectedSet(overlayUI.isOpen());
            if (topBarSection != null) {
                topBarSection.visableSet(config.isShowHudButton());
            }
            hudButton.activeSet(config.isShowHudButton());
        }

        // 6. Update overlay simulation and periodic zone enforcement
        overlayUI.update(ds);
        zoneManager.update();
    }

    @Override
    public void render(Renderer r, float ds) {
        if (!overlayUI.isInterrupterActive()) {
            overlayUI.renderOverlay(r, ds);
        }
    }

    @Override
    public void mouseClick(MButt button) {
        if (!overlayUI.isInterrupterActive()) {
            overlayUI.handleInterrupterClick(button);
        }
    }

    @Override
    public void hoverTimer(double mouseTimer, GBox text) {
        // Interrupter handles tooltips in uiManager
    }

    @Override
    public void hover(snake2d.util.datatypes.COORDINATE mCoo, boolean mouseHasMoved) {
        // Handled by Interrupter in uiManager
    }

    @Override
    public void keyPush(KEYS key) {
        // Handled via update loop
    }

    @Override
    public void save(FilePutter file) {
        config.save(file);
        zoneManager.save(file);
    }

    @Override
    public void load(FileGetter file) throws IOException {
        config.load(file);
        zoneManager.load(file);
    }

    public TreeZoneConfig getConfig() {
        return config;
    }

    public TreeZoneManager getZoneManager() {
        return zoneManager;
    }

    public TreeZonesOverlayUI getOverlayUI() {
        return overlayUI;
    }
}
