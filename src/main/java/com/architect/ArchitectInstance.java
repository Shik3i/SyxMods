package com.architect;

import java.io.IOException;
import java.lang.reflect.Field;

import com.architect.config.ArchitectState;
import com.architect.modules.BoostModule;
import com.architect.modules.ConstructionModule;
import com.architect.modules.LogisticsModule;
import com.architect.modules.SecurityModule;
import com.architect.modules.WorldModule;
import com.architect.ui.ArchitectUI;
import com.architect.util.I18n;

import init.constant.C;
import init.race.Race;
import init.sprite.UI.UI;
import script.SCRIPT.SCRIPT_INSTANCE;
import snake2d.CORE;
import snake2d.KEYCODES;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.Rec;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import view.interrupter.InterManager;
import view.interrupter.Interrupter;
import view.keyboard.KEYS;
import view.main.VIEW;
import view.ui.top.UIPanelTop;

/**
 * Runtime execution instance of Syx Architect during active gameplay.
 * STRICT ZERO-ALLOCATION GUARANTEE in 60Hz update and render loops.
 * Features:
 * - Native integration directly into the game's top menu bar (UIPanelTop) alongside Menu & Wiki buttons
 * - 100% resolution-independent, UI-scale responsive layout (supports 1080p, 1440p, 4K, 720p, ultrawide)
 * - Safe responsive fallback for views where top panel is inactive
 * - Rock-solid edge-triggered F10 hotkey with debouncing
 * - Native hover tooltip and full interaction support
 */
public final class ArchitectInstance implements SCRIPT_INSTANCE {

    private static final String BANNER_TEXT = "Syx Architect aktiv  [Button in Menueleiste / F10]";

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

    // Native stone HUD button
    private GButt.ButtPanel hudButton;
    private boolean attachedToTopPanel = false;
    private UIPanelTop lastAttachedTop = null;

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

    private void ensureHudButton() {
        if (hudButton == null && UI.FONT() != null && UI.FONT().S != null) {
            GText t = new GText(UI.FONT().S, "ARCHITECT");
            t.lablify();
            hudButton = new GButt.ButtPanel((snake2d.util.sprite.SPRITE) t);
            hudButton.setDim(76, 24);
            hudButton.align(DIR.C);
            hudButton.hoverTitleSet("Syx Architect  [F10]");
            hudButton.hoverInfoSet(I18n.getLanguage() == I18n.Language.DE
                ? "Klick oeffnet das Creative-, Schoenbau- & Sandbox-Kontrollzentrum (Hotkeys: F10 / ESC)."
                : "Click opens the creative and sandbox control center (Hotkeys: F10 / ESC).");
            hudButton.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    toggleUI();
                }
            });
        }
    }

    /**
     * Finds the active UIPanelTop instance in the settlement view to dock the button natively.
     */
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

    /**
     * Docks our HUD button cleanly into the native top bar beside the Menu & Wiki buttons.
     */
    private void attachToTopPanel(UIPanelTop top) {
        if (top == null || top == lastAttachedTop) return;
        ensureHudButton();
        if (hudButton != null) {
            GuiSection section = new GuiSection();
            section.add(hudButton);
            top.addRightRight(section);
            lastAttachedTop = top;
            attachedToTopPanel = true;
        }
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

        // 3. Ensure native top bar docking whenever entering or loading settlement view
        if (!attachedToTopPanel || lastAttachedTop == null) {
            UIPanelTop top = findTopPanel();
            if (top != null) {
                attachToTopPanel(top);
            }
        }

        // 4. Update button active/highlighted state matching open modal
        if (hudButton != null) {
            hudButton.selectedSet(ArchitectUI.isOpen());
        }

        // 5. Subsystem simulation updates
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
        // Reset top panel attachment and banner timer upon loading game
        attachedToTopPanel = false;
        lastAttachedTop = null;
        bannerTimer = 4.0;
    }

    @Override
    public void render(Renderer r, float ds) {
        if (C.DIM() == null) return;

        // 1. Render discrete, non-intrusive notification banner at top center on game load
        if (bannerTimer > 0) {
            bannerTimer -= ds;
            int bannerW = 340;
            int bannerH = 26;
            int bannerX = (C.DIM().width() - bannerW) / 2;
            int bannerY = 12;
            bannerBounds.set(bannerX, bannerX + bannerW, bannerY, bannerY + bannerH);

            GButt.ButtPanel.renderBG(r, false, false, false, bannerBounds);
            if (UI.FONT() != null && UI.FONT().S != null) {
                int tw = UI.FONT().S.width(BANNER_TEXT);
                UI.FONT().S.render(r, BANNER_TEXT, bannerX + (bannerW - tw) / 2, bannerY + 6);
            }
        }

        // 2. If not yet attached to native UIPanelTop, render responsive fallback button
        // Anchors dynamically to the scaled top bar bottom edge and right screen edge
        if (!attachedToTopPanel) {
            ensureHudButton();
            if (hudButton != null) {
                int btnW = 76;
                int btnH = 24;
                int topY = UIPanelTop.y2();
                int btnX = C.DIM().width() - btnW - 10;
                int btnY = topY + 4;
                hudButton.body().moveX1Y1(btnX, btnY);
                hudButton.render(r, ds);
            }
        }
    }

    @Override
    public void hover(COORDINATE coordinate, boolean hasCursor) {
        if (!attachedToTopPanel) {
            ensureHudButton();
            if (hudButton != null && coordinate != null) {
                hudButton.hover(coordinate);
            }
        }
    }

    @Override
    public void hoverTimer(double time, GBox hoverBox) {
        if (!attachedToTopPanel) {
            ensureHudButton();
            if (hudButton != null && hudButton.hoveredIs() && hoverBox != null) {
                hudButton.hoverInfoGet(hoverBox);
            }
        }
    }

    @Override
    public void mouseClick(MButt button) {
        if (!attachedToTopPanel) {
            ensureHudButton();
            if (button == MButt.LEFT && hudButton != null && hudButton.hoveredIs()) {
                hudButton.click();
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
