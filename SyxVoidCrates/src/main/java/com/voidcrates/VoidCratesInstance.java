package com.voidcrates;

import com.voidcrates.config.VoidConfig;
import com.voidcrates.manager.VoidManager;
import com.voidcrates.ui.VoidUI;
import com.voidcrates.util.I18n;

import game.GAME;
import init.sprite.SPRITES;
import script.SCRIPT.SCRIPT_INSTANCE;
import snake2d.CORE;
import snake2d.KEYCODES;
import snake2d.MButt;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.gui.misc.GBox;
import view.interrupter.InterManager;
import view.keyboard.KEYS;
import view.main.VIEW;
import view.ui.top.UIPanelTop;

import java.io.IOException;
import java.lang.reflect.Field;

public final class VoidCratesInstance implements SCRIPT_INSTANCE {

    private final VoidConfig config = new VoidConfig();
    private final VoidManager manager = new VoidManager();

    private boolean wasF9Down = false;
    private double f9Cooldown = 0.0;

    private boolean attachedToTopPanel = false;
    private UIPanelTop.Butt hudButton = null;
    private GuiSection topBarSection = null;
    private UIPanelTop lastAttachedTop = null;

    private final VoidUI.UICallback uiCallback = new VoidUI.UICallback() {
        @Override
        public void onConfigUpdated() {
            // Config updated
        }

        @Override
        public void onPurgeNowRequested() {
            int[] res = manager.purgeNow(config);
            boolean de = I18n.getLanguage() == I18n.Language.DE;
            GAME.Notify(de
                ? "Schwarzes Loch: " + res[0] + " Kisten geleert (" + res[1] + " Gueter vernichtet)!"
                : "Black Hole: " + res[0] + " crates purged (" + res[1] + " items destroyed)!");
        }

        @Override
        public void onClearMarksRequested() {
            manager.clearAllDesignations();
            boolean de = I18n.getLanguage() == I18n.Language.DE;
            GAME.Notify(de
                ? "Alle Kisten-Markierungen als Schwarzes Loch wurden aufgehoben."
                : "All black hole crate designations cleared.");
        }
    };

    public VoidCratesInstance() {
        I18n.setLanguage(I18n.Language.fromCode(config.getLanguage()));
    }

    private void ensureHudButton() {
        if (hudButton == null && SPRITES.icons() != null && SPRITES.icons().m != null && SPRITES.icons().m.trash != null) {
            hudButton = new UIPanelTop.Butt(SPRITES.icons().m.trash);
            hudButton.hoverTitleSet("Void Crates [F9]");
            hudButton.hoverInfoSet(I18n.getLanguage() == I18n.Language.DE
                ? "Oeffnet das Kontrollzentrum fuer Schwarze-Loch-Kisten und automatischen Ueberschuss-Abbau (ESC schliesst)."
                : "Opens the black hole and void crates management modal (ESC closes).");
            hudButton.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    VoidUI.toggle(config, manager, uiCallback);
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
            }
            top.addRightRight(topBarSection);
            lastAttachedTop = top;
            attachedToTopPanel = true;
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
            VoidUI.toggle(config, manager, uiCallback);
            f9Cooldown = 0.3; // 300ms cooldown
        }
        wasF9Down = f9;

        // 2. ESC key closes modal if open
        if (VoidUI.isOpen()) {
            boolean esc = CORE.getInput().getKeyboard().isPressed(KEYCODES.KEY_ESCAPE);
            if (esc) {
                VoidUI.close();
            }
        }

        // 3. Ensure native top bar docking whenever entering settlement view
        if (!attachedToTopPanel || lastAttachedTop == null) {
            UIPanelTop top = findTopPanel();
            if (top != null) {
                attachToTopPanel(top);
            }
        }

        // 4. Update HUD button selection and visibility (clean, no activeSet)
        if (hudButton != null) {
            hudButton.selectedSet(VoidUI.isOpen());
            if (topBarSection != null) {
                topBarSection.visableSet(config.isShowHudButton());
            }
        }

        // 5. Periodic Void Crate checking and automated overflow purge
        manager.update(config);
    }

    @Override
    public void mouseClick(MButt button) {}

    @Override
    public void hoverTimer(double mouseTimer, GBox text) {}

    @Override
    public void hover(COORDINATE mCoo, boolean mouseHasMoved) {}

    @Override
    public void keyPush(KEYS key) {}

    @Override
    public void save(FilePutter file) {
        config.save(file);
        manager.save(file);
    }

    @Override
    public void load(FileGetter file) throws IOException {
        config.load(file);
        manager.load(file);
        I18n.setLanguage(I18n.Language.fromCode(config.getLanguage()));
    }
}
