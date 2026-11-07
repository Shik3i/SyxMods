package com.autoplanner.ui;

import java.lang.reflect.Field;
import com.autoplanner.config.PlannerConfig;
import com.autoplanner.config.PlannerConfig.LayoutStyle;
import com.autoplanner.solver.AutoFurnishSolver;
import com.autoplanner.solver.AutoStrutSolver;
import com.autoplanner.util.I18n;
import com.autoplanner.util.I18n.Language;

import init.constant.C;
import init.sprite.SPRITES;
import init.sprite.UI.UI;
import settlement.main.SETT;
import settlement.room.main.placement.RoomPlacer;
import snake2d.CORE;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Rec;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.misc.ACTION;
import snake2d.util.misc.CLAMP;
import snake2d.util.sprite.SPRITE;
import util.colors.GCOLOR;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GHeader;
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import view.interrupter.InterManager;
import view.interrupter.Interrupter;
import view.main.VIEW;
import view.tool.ToolConfig;

/**
 * 100% Native-Integrated Draggable Room Editor Box & Configuration Suite for Syx Auto-Planner.
 * Uses authentic Songs of Syx UI panels, buttons, sound effects, fonts and official icons.
 * Features a compact 2-row card layout, free drag-and-drop repositioning, and zero GC overhead.
 */
public final class PlannerOverlayUI {

    private final PlannerConfig config;
    private final AutoStrutSolver strutSolver;
    private final AutoFurnishSolver furnishSolver;

    private String statusMessage = "";
    private boolean statusIsError = false;
    private double statusTimer = 0.0;

    // Toast Notification Bounds
    private final Rec toastBounds = new Rec();

    // Toolbar Docking & Drag Coordinates
    private int boxX = -1;
    private int boxY = -1;
    private boolean isCustomPosition = false;
    private double relX = 0.02;
    private double relY = 0.08;
    private boolean isDragging = false;
    private int dragAnchorX = 0;
    private int dragAnchorY = 0;

    // Sub-sections and Buttons for direct click & hover dispatch
    private final GuiSection headerBar = new GuiSection();
    private final GuiSection row1 = new GuiSection();
    private final GuiSection row2 = new GuiSection();
    private GButt.ButtPanel btnSnap;
    private GButt.ButtPanel btnStrut;
    private GButt.ButtPanel btnFurnish;
    private GButt.ButtPanel btnQuick;
    private GButt.ButtPanel btnClear;
    private GButt.ButtPanel btnConfig;

    // Native Toolbar Section (compact 2-row card with authentic GButt.ButtPanel buttons)
    private final ToolbarSection toolbarSection = new ToolbarSection();
    private Language cachedLanguage = null;

    /**
     * Native Settlement Interrupter: absorbs hover, clicks, tooltips, and rendering.
     * When registered in SettView.uiManager, InterManager absorbs all clicks over the toolbar
     * so that clicks NEVER reach SettView.mouseClick or RoomPlacer!
     */
    private final class OverlayInterrupter extends Interrupter {
        private OverlayInterrupter() {
            super(true, true, false); // persistent = true, pinned = true, desturbingfuck = false
        }

        @Override
        protected boolean hover(COORDINATE m, boolean hasMoved) {
            if (!isPlacerActive()) {
                isDragging = false;
                return false;
            }
            if (m == null) {
                return false;
            }
            if (isDragging) {
                return true;
            }
            return holdsPoint(m.x(), m.y());
        }

        @Override
        protected void mouseClick(MButt button) {
            if (!isPlacerActive()) {
                return;
            }
            handleInterrupterClick(button);
        }

        @Override
        protected void hoverTimer(GBox text) {
            if (!isPlacerActive() || text == null || isDragging) {
                return;
            }
            if (toolbarSection.hoveredIs()) {
                toolbarSection.hoverInfoGet(text);
            }
        }

        @Override
        protected boolean render(Renderer r, float ds) {
            if (isPlacerActive()) {
                renderOverlay(r, ds);
            }
            return true;
        }

        @Override
        protected boolean update(float ds) {
            return true;
        }

        private void attach(InterManager manager) {
            if (manager == null) return;
            if (isActivated() && manager() == manager) {
                return;
            }
            if (isActivated()) {
                try {
                    hide();
                } catch (Throwable ignored) {}
            }
            try {
                show(manager);
            } catch (Throwable t) {
                snake2d.LOG.err("AutoPlanner: Failed to show Interrupter: " + t.getMessage());
            }
        }
    }

    private final OverlayInterrupter interrupter = new OverlayInterrupter();

    public boolean isInterrupterActive() {
        return interrupter.isActivated();
    }

    public void ensureInterrupterAttached(InterManager manager) {
        interrupter.attach(manager);
    }

    public boolean holdsPoint(int x, int y) {
        if (toolbarSection != null && toolbarSection.body().holdsPoint(x, y)) {
            return true;
        }
        if (statusTimer > 0.0 && !statusMessage.isEmpty() && toastBounds != null && toastBounds.holdsPoint(x, y)) {
            return true;
        }
        return false;
    }

    public boolean holdsPoint(COORDINATE m) {
        return m != null && holdsPoint(m.x(), m.y());
    }

    public PlannerOverlayUI(PlannerConfig config, AutoStrutSolver strutSolver, AutoFurnishSolver furnishSolver) {
        this.config = config;
        this.strutSolver = strutSolver;
        this.furnishSolver = furnishSolver;
        buildToolbar();
    }

    /**
     * Default docking anchor: Left-aligned ("links außen") below top HUD panel.
     * Leaves the entire center of the screen completely free for room drawing, wall selection, and floor choices.
     */
    public int getDefaultDockX() {
        return (int) (16 * C.SG);
    }

    public int getDefaultDockY() {
        return (int) (60 * C.SG);
    }

    /**
     * Updates toolbar position:
     * - Default: Docked on the left side of the screen ("links außen"), completely clear of wall selection.
     * - Custom: Preserves user's dragged position, scaled proportionally with C.WIDTH / C.HEIGHT.
     */
    public void updatePosition() {
        int targetX;
        int targetY;

        if (!isCustomPosition) {
            targetX = getDefaultDockX();
            targetY = getDefaultDockY();
        } else {
            targetX = CLAMP.i((int) (relX * C.WIDTH()), 10, Math.max(10, C.WIDTH() - toolbarSection.body().width() - 10));
            targetY = CLAMP.i((int) (relY * C.HEIGHT()), 34, Math.max(34, C.HEIGHT() - toolbarSection.body().height() - 10));
        }

        if (boxX != targetX || boxY != targetY || (int) toolbarSection.body().x1() != targetX || (int) toolbarSection.body().y1() != targetY) {
            boxX = targetX;
            boxY = targetY;
            toolbarSection.body().moveX1Y1(boxX, boxY);
        }
    }

    /**
     * Rebuilds the native toolbar as a compact 2-row card with localized labels and official icons.
     * Called only on initialization and when the interface language changes.
     */
    private void buildToolbar() {
        cachedLanguage = I18n.getLanguage();
        toolbarSection.clear();
        headerBar.clear();
        row1.clear();
        row2.clear();

        // --- Drag & Dock Header Bar ---
        RENDEROBJ.Sprite dragIcon = new RENDEROBJ.Sprite(SPRITES.icons().m.openscroll);
        headerBar.add(dragIcon, 0, 0);
        GText titleText = new GText(UI.FONT().S, "Syx Auto-Planner").lablify();
        headerBar.addRightC(6, titleText);

        // Snap / Re-Dock Button (Instantly snaps back docked to left outer edge)
        btnSnap = new GButt.ButtPanel(SPRITES.icons().m.crossair) {
            @Override
            protected void clickA() {
                isCustomPosition = false;
                boxX = getDefaultDockX();
                boxY = getDefaultDockY();
                relX = (double) boxX / Math.max(1, C.WIDTH());
                relY = (double) boxY / Math.max(1, C.HEIGHT());
                toolbarSection.body().moveX1Y1(boxX, boxY);
                setStatus(I18n.getLanguage() == Language.DE ? "Links angedockt" : "Docked to left", false);
            }
        };
        btnSnap.hoverTitleSet(I18n.get("btn.snap"));
        btnSnap.hoverInfoSet(I18n.get("tooltip.snap"));
        headerBar.addRightC(12, btnSnap);

        // --- Row 1 Buttons (Primary Builders) ---
        // Button 1: Auto-Strut (Pillars)
        btnStrut = new GButt.ButtPanel(I18n.get("btn.strut")) {
            @Override
            protected void clickA() {
                RoomPlacer placer = getActivePlacer();
                if (placer != null) {
                    triggerAutoStrut(placer);
                }
            }
        }.icon(SPRITES.icons().m.wallceiling);
        btnStrut.hoverTitleSet(I18n.get("btn.strut"));
        btnStrut.hoverInfoSet(I18n.get("tooltip.strut"));

        // Button 2: Auto-Furnish (Workstations & Auxiliaries)
        btnFurnish = new GButt.ButtPanel(I18n.get("btn.furnish")) {
            @Override
            protected void clickA() {
                RoomPlacer placer = getActivePlacer();
                if (placer != null) {
                    triggerAutoFurnish(placer);
                }
            }
        }.icon(SPRITES.icons().m.furniture);
        btnFurnish.hoverTitleSet(I18n.get("btn.furnish"));
        btnFurnish.hoverInfoSet(I18n.get("tooltip.furnish"));

        // Button 3: Quick-Build (Pillars + Furniture in one pass)
        btnQuick = new GButt.ButtPanel(I18n.get("btn.quickfill")) {
            @Override
            protected void clickA() {
                RoomPlacer placer = getActivePlacer();
                if (placer != null) {
                    triggerQuickBuild(placer);
                }
            }
        }.icon(SPRITES.icons().m.fast_forw);
        btnQuick.hoverTitleSet(I18n.get("btn.quickfill"));
        btnQuick.hoverInfoSet(I18n.get("tooltip.quickfill"));

        row1.add(btnStrut, 0, 0);
        row1.addRightC(4, btnFurnish);
        row1.addRightC(4, btnQuick);

        // --- Row 2 Buttons (Secondary / Config) ---
        // Button 4: Clear Furniture
        btnClear = new GButt.ButtPanel(I18n.get("btn.clear")) {
            @Override
            protected void clickA() {
                RoomPlacer placer = getActivePlacer();
                if (placer != null) {
                    triggerClearFurniture(placer);
                }
            }
        }.icon(SPRITES.icons().m.trash);
        btnClear.hoverTitleSet(I18n.get("btn.clear"));
        btnClear.hoverInfoSet(I18n.get("tooltip.clear"));

        // Button 5: Configuration Settings
        btnConfig = new GButt.ButtPanel(I18n.get("btn.settings")) {
            @Override
            protected void clickA() {
                if (VIEW.inters().section != null && VIEW.inters().section.isActivated()) {
                    VIEW.inters().section.close();
                } else {
                    openConfigModal();
                }
            }
        }.icon(SPRITES.icons().m.cog);
        btnConfig.hoverTitleSet(I18n.get("btn.settings"));
        btnConfig.hoverInfoSet(I18n.get("tooltip.settings"));

        row2.add(btnClear, 0, 0);
        row2.addRightC(8, btnConfig);

        // Assemble 2-row card with title
        toolbarSection.add(headerBar, 0, 0);
        toolbarSection.addDown(4, row1);

        // Center row2 underneath row1
        int row2Offset = Math.max(0, (row1.body().width() - row2.body().width()) / 2);
        row2.body().moveX1(row1.body().x1() + row2Offset);
        toolbarSection.addDown(4, row2);

        toolbarSection.pad(8, 6);

        // Initial left docking
        if (boxX == -1 || boxY == -1) {
            boxX = getDefaultDockX();
            boxY = getDefaultDockY();
            relX = (double) boxX / Math.max(1, C.WIDTH());
            relY = (double) boxY / Math.max(1, C.HEIGHT());
        }
        toolbarSection.body().moveX1Y1(boxX, boxY);
    }

    public void update(double ds) {
        if (statusTimer > 0.0) {
            statusTimer -= ds;
            if (statusTimer <= 0.0) {
                statusMessage = "";
            }
        }
    }

    /**
     * Strictest possible active-check:
     * Returns true ONLY if player is actively in settlement mode, has the RoomPlacer tool selected,
     * an area-based room blueprint is being drafted, and NO modal popup / menu is blocking the screen.
     */
    public boolean isPlacerActive() {
        if (!config.isShowOverlay()) {
            return false;
        }
        if (VIEW.s() == null || VIEW.s().tools == null || VIEW.s().tools.placer == null) {
            return false;
        }
        if (!VIEW.s().tools.placer.isActivated() || VIEW.s().tools.current() != VIEW.s().tools.placer) {
            return false;
        }
        if (VIEW.inters() != null && VIEW.inters().section != null && VIEW.inters().section.isActivated()) {
            return false;
        }
        RoomPlacer placer = getActivePlacer();
        if (placer == null || placer.blueprint() == null || placer.blueprint().constructor() == null) {
            return false;
        }
        return placer.blueprint().constructor().usesArea();
    }

    private boolean isConfigOpen = false;
    private GuiSection activeConfigModal = null;

    public boolean isConfigOpen() {
        if (!isConfigOpen) {
            return false;
        }
        if (VIEW.inters() == null || VIEW.inters().section == null || !VIEW.inters().section.isActivated()) {
            isConfigOpen = false;
            activeConfigModal = null;
            return false;
        }
        if (activeConfigModal != null && VIEW.inters().section.current() != activeConfigModal) {
            isConfigOpen = false;
            activeConfigModal = null;
            return false;
        }
        return true;
    }

    public void closeConfigModal() {
        isConfigOpen = false;
        activeConfigModal = null;
        if (VIEW.inters() != null && VIEW.inters().section != null) {
            VIEW.inters().section.close();
        }
    }

    public void render(Renderer r, float ds) {
        if (!isInterrupterActive()) {
            renderOverlay(r, ds);
        }
    }

    public void renderOverlay(Renderer r, float ds) {
        if (!isPlacerActive()) {
            isDragging = false;
            return;
        }

        if (cachedLanguage != I18n.getLanguage()) {
            buildToolbar();
        }

        COORDINATE m = CORE.getInput().getMouse().getCoo();

        // Handle active drag & drop
        if (m != null) {
            if (MButt.LEFT.isDown()) {
                if (!isDragging) {
                    // Check if mouse down in header bar (excluding btnSnap)
                    int hX1 = toolbarSection.body().x1();
                    int hX2 = (btnSnap != null) ? btnSnap.body().x1() - 4 : toolbarSection.body().x2();
                    int hY1 = toolbarSection.body().y1();
                    int hY2 = hY1 + (int) (32 * C.SG);

                    if (m.x() >= hX1 && m.x() <= hX2 && m.y() >= hY1 && m.y() <= hY2) {
                        isDragging = true;
                        isCustomPosition = true;
                        dragAnchorX = m.x() - (int) toolbarSection.body().x1();
                        dragAnchorY = m.y() - (int) toolbarSection.body().y1();
                    }
                } else {
                    int targetX = CLAMP.i(m.x() - dragAnchorX, 10, Math.max(10, C.WIDTH() - toolbarSection.body().width() - 10));
                    int targetY = CLAMP.i(m.y() - dragAnchorY, 34, Math.max(34, C.HEIGHT() - toolbarSection.body().height() - 10));
                    if (targetX != boxX || targetY != boxY) {
                        boxX = targetX;
                        boxY = targetY;
                        relX = (double) boxX / Math.max(1, C.WIDTH());
                        relY = (double) boxY / Math.max(1, C.HEIGHT());
                        toolbarSection.body().moveX1Y1(boxX, boxY);
                    }
                }
            } else {
                isDragging = false;
            }
        } else {
            isDragging = false;
        }

        if (!isDragging) {
            updatePosition();
        }

        // Keep hover state responsive
        if (m != null && !isDragging) {
            toolbarSection.hover(m);
        }

        // Render the authentic beveled 2-row button panel
        toolbarSection.render(r, ds);

        // Render polished floating status toast anchored directly above the box
        renderStatusToast(r, boxX, boxY);
    }

    private void renderStatusToast(Renderer r, int x, int y) {
        if (statusTimer <= 0.0 || statusMessage.isEmpty()) {
            return;
        }

        int textW = UI.FONT().S.width(statusMessage);
        int toastW = textW + 42;
        int toastH = 26;
        int toastX = x + (toolbarSection.body().width() - toastW) / 2;
        int toastY = y - toastH - 6;

        toastBounds.set(toastX, toastX + toastW, toastY, toastY + toastH);

        // Native thin panel frame
        UI.PANEL().thin.render(r, toastBounds, 0);

        // Status indicator icon
        SPRITE statusIcon = statusIsError ? SPRITES.icons().m.cancel : SPRITES.icons().m.ok;
        statusIcon.renderCY(r, toastX + 6, toastY + toastH / 2);

        // Text rendered in warm gold header font
        GCOLOR.T().H1.bind();
        UI.FONT().S.renderCY(r, toastX + 34, toastY + toastH / 2, statusMessage);
        COLOR.unbind();
    }

    public void hover(COORDINATE mCoo, boolean mouseHasMoved) {
        if (isPlacerActive() && mCoo != null && !isDragging) {
            toolbarSection.hover(mCoo);
        }
    }

    public boolean mouseClick(MButt button) {
        if (!isInterrupterActive()) {
            return handleInterrupterClick(button);
        }
        return false;
    }

    public boolean handleInterrupterClick(MButt button) {
        if (button != MButt.LEFT || !isPlacerActive()) {
            return false;
        }

        COORDINATE m = CORE.getInput().getMouse().getCoo();
        if (m == null) {
            return false;
        }

        if (toolbarSection.body().holdsPoint(m.x(), m.y())) {
            // If dragging, consume click so it doesn't click into the game world
            if (isDragging) {
                CORE.getInput().clearAllInput();
                return true;
            }

            // 1. Check Snap button
            if (btnSnap != null && btnSnap.body().holdsPoint(m.x(), m.y())) {
                btnSnap.hover(m);
                btnSnap.click();
                CORE.getInput().clearAllInput();
                return true;
            }

            // 2. Check Row 1 buttons
            if (btnStrut != null && btnStrut.body().holdsPoint(m.x(), m.y())) {
                btnStrut.hover(m);
                btnStrut.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnFurnish != null && btnFurnish.body().holdsPoint(m.x(), m.y())) {
                btnFurnish.hover(m);
                btnFurnish.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnQuick != null && btnQuick.body().holdsPoint(m.x(), m.y())) {
                btnQuick.hover(m);
                btnQuick.click();
                CORE.getInput().clearAllInput();
                return true;
            }

            // 3. Check Row 2 buttons
            if (btnClear != null && btnClear.body().holdsPoint(m.x(), m.y())) {
                btnClear.hover(m);
                btnClear.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnConfig != null && btnConfig.body().holdsPoint(m.x(), m.y())) {
                btnConfig.hover(m);
                btnConfig.click();
                CORE.getInput().clearAllInput();
                return true;
            }

            // 4. If clicked on header bar, initiate dragging immediately
            int hX1 = toolbarSection.body().x1();
            int hX2 = (btnSnap != null) ? btnSnap.body().x1() - 4 : toolbarSection.body().x2();
            int hY1 = toolbarSection.body().y1();
            int hY2 = hY1 + (int) (32 * C.SG);

            if (m.x() >= hX1 && m.x() <= hX2 && m.y() >= hY1 && m.y() <= hY2) {
                isDragging = true;
                isCustomPosition = true;
                dragAnchorX = m.x() - (int) toolbarSection.body().x1();
                dragAnchorY = m.y() - (int) toolbarSection.body().y1();
                CORE.getInput().clearAllInput();
                return true;
            }

            // Clicked inside panel padding / background: consume click
            CORE.getInput().clearAllInput();
            return true;
        }

        // Also consume click if inside status toast
        if (statusTimer > 0.0 && !statusMessage.isEmpty() && toastBounds.holdsPoint(m.x(), m.y())) {
            CORE.getInput().clearAllInput();
            return true;
        }

        return false;
    }

    public void hoverTimer(double mouseTimer, GBox text) {
        if (!isPlacerActive() || text == null || isDragging) {
            return;
        }
        if (toolbarSection.hoveredIs()) {
            toolbarSection.hoverInfoGet(text);
        }
    }

    public void triggerAutoStrut(RoomPlacer placer) {
        if (placer == null || placer.size() == 0) {
            setStatus(I18n.get("msg.draw_room_first"), true);
            return;
        }
        try {
            AutoStrutSolver.Result res = strutSolver.solve(placer);
            if (res.pillarsPlaced > 0) {
                setStatus(I18n.get("msg.strut_success", res.pillarsPlaced), false);
            } else if (res.initialYellow == 0) {
                setStatus(I18n.get("msg.strut_none_needed"), false);
            } else {
                setStatus(I18n.get("msg.strut_failed", res.initialYellow), true);
            }
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: AutoStrut exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] Auto-Strut solver failed", true);
        }
    }

    public void triggerAutoFurnish(RoomPlacer placer) {
        if (placer == null || placer.size() == 0) {
            setStatus(I18n.get("msg.draw_room_first"), true);
            return;
        }
        try {
            AutoFurnishSolver.Result res = furnishSolver.solve(placer, config);
            if (res.success) {
                if (res.virtualDoorUsed) {
                    setStatus(I18n.get("msg.furnish_virtual_door", res.workstations, res.efficiencyModules, res.efficiencyPercent, res.virtualDoorX, res.virtualDoorY), false);
                } else {
                    setStatus(I18n.get("msg.furnish_success", res.workstations, res.efficiencyModules, res.efficiencyPercent), false);
                }
            } else {
                setStatus(I18n.get(res.errorMessage), true);
            }
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: AutoFurnish exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] Auto-Furnish solver failed", true);
        }
    }

    public void triggerQuickBuild(RoomPlacer placer) {
        if (placer == null || placer.size() == 0) {
            setStatus(I18n.get("msg.draw_room_first"), true);
            return;
        }
        try {
            AutoStrutSolver.Result sRes = strutSolver.solve(placer);
            AutoFurnishSolver.Result fRes = furnishSolver.solve(placer, config);

            if (fRes.success) {
                if (fRes.virtualDoorUsed) {
                    setStatus(I18n.get("msg.furnish_virtual_door", fRes.workstations, fRes.efficiencyModules, fRes.efficiencyPercent, fRes.virtualDoorX, fRes.virtualDoorY), false);
                } else {
                    setStatus(I18n.get("msg.furnish_success", fRes.workstations, fRes.efficiencyModules, fRes.efficiencyPercent), false);
                }
            } else {
                setStatus(I18n.get(fRes.errorMessage), true);
            }
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: QuickBuild exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] Quick-Build solver failed", true);
        }
    }

    public void triggerClearFurniture(RoomPlacer placer) {
        try {
            furnishSolver.clearExistingFurniture(placer);
            setStatus(I18n.get("msg.clear_success"), false);
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: ClearFurniture exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] Clear-Furniture failed", true);
        }
    }

    private void setStatus(String msg, boolean isError) {
        this.statusMessage = msg;
        this.statusIsError = isError;
        this.statusTimer = 4.0;
    }

    private RoomPlacer getActivePlacer() {
        if (SETT.ROOMS() == null || SETT.ROOMS().placement == null) {
            return null;
        }
        return SETT.ROOMS().placement.placer;
    }

    private static int configModalX = -1;
    private static int configModalY = -1;

    /**
     * Opens or refreshes the ornate Songs of Syx Configuration Center modal dialog.
     * Uses canonical single GuiSection architecture with GPanel background.
     */
    public void openConfigModal() {
        int contentWidth = 750;

        final GPanel[] panelRef = new GPanel[1];

        GuiSection modal = new GuiSection() {
            private boolean isDragging = false;
            private int dragAnchorX = 0;
            private int dragAnchorY = 0;

            @Override
            public void render(SPRITE_RENDERER r, float ds) {
                COORDINATE m = CORE.getInput().getMouse().getCoo();
                if (m != null) {
                    if (MButt.LEFT.isDown()) {
                        if (!isDragging) {
                            // Title bar drag zone: top 40px of modal excluding top-right [X] close button
                            int titleY1 = body().y1();
                            int titleY2 = body().y1() + 40;
                            int closeX = body().x2() - 40;
                            if (m.y() >= titleY1 && m.y() <= titleY2 && m.x() >= body().x1() && m.x() <= closeX) {
                                isDragging = true;
                                dragAnchorX = m.x() - body().x1();
                                dragAnchorY = m.y() - body().y1();
                            }
                        } else {
                            int targetX = CLAMP.i(m.x() - dragAnchorX, 10, Math.max(10, C.WIDTH() - body().width() - 10));
                            int targetY = CLAMP.i(m.y() - dragAnchorY, 10, Math.max(10, C.HEIGHT() - body().height() - 10));
                            if (targetX != (int) body().x1() || targetY != (int) body().y1()) {
                                configModalX = targetX;
                                configModalY = targetY;
                                body().moveX1Y1(targetX, targetY);
                                if (panelRef[0] != null) {
                                    // Compensate vanilla GPanel$1.moveY1 bug (missing +8 closeAction offset)
                                    panelRef[0].inner().incrY(8);
                                }
                            }
                        }
                    } else {
                        isDragging = false;
                    }
                }
                super.render(r, ds);
            }
        };

        // Header Subtitle
        GText subTitle = new GText(UI.FONT().S, I18n.get("ui.panel.subtitle")).normalify();
        modal.add(subTitle, 0, 0);

        // Divider
        modal.addDown(8, new HorizontalSeparator(contentWidth));

        // Row 1: Packing Strategy
        String styleLabel = config.getStyle() == LayoutStyle.COMPACT 
                ? I18n.get("ui.panel.style.compact") 
                : I18n.get("ui.panel.style.balanced");
        GButt.ButtPanel btnStyle = new GButt.ButtPanel(styleLabel) {
            @Override
            protected void clickA() {
                config.cycleStyle();
                openConfigModal();
            }
        };
        btnStyle.body().setWidth(250);
        btnStyle.body().setHeight(32);
        modal.addDown(8, createOptionRow(SPRITES.icons().m.place_fill, 
                I18n.get("ui.panel.style"), 
                I18n.get("ui.panel.style.desc"), 
                btnStyle, contentWidth));

        // Divider
        modal.addDown(8, new HorizontalSeparator(contentWidth));

        // Row 2: Target Room Efficiency
        int effPct = (int) Math.round(config.getTargetEfficiency() * 100);
        String effLabel = I18n.get("btn.target_eff", effPct);
        GButt.ButtPanel btnEff = new GButt.ButtPanel(effLabel) {
            @Override
            protected void clickA() {
                config.cycleTargetEfficiency();
                openConfigModal();
            }
        };
        btnEff.body().setWidth(250);
        btnEff.body().setHeight(32);
        modal.addDown(8, createOptionRow(SPRITES.icons().m.ok, 
                I18n.get("ui.panel.target_eff"), 
                I18n.get("ui.panel.target_eff.desc"), 
                btnEff, contentWidth));

        // Divider
        modal.addDown(8, new HorizontalSeparator(contentWidth));

        // Row 3: Internal Storage Ratio
        String storageLabel = config.getStorageRatio() <= 0.001 
                ? I18n.get("ui.panel.storage.none") 
                : I18n.get("btn.storage_ratio", (int) Math.round(config.getStorageRatio() * 100));
        GButt.ButtPanel btnStorage = new GButt.ButtPanel(storageLabel) {
            @Override
            protected void clickA() {
                config.cycleStorageRatio();
                openConfigModal();
            }
        };
        btnStorage.body().setWidth(250);
        btnStorage.body().setHeight(32);
        modal.addDown(8, createOptionRow(SPRITES.icons().m.storage_push, 
                I18n.get("ui.panel.storage"), 
                I18n.get("ui.panel.storage.desc"), 
                btnStorage, contentWidth));

        // Divider
        modal.addDown(8, new HorizontalSeparator(contentWidth));

        // Row 4: Interface Language
        String langLabel = I18n.getLanguage() == Language.DE ? "Deutsch [DE]" : "English [EN]";
        GButt.ButtPanel btnLang = new GButt.ButtPanel(langLabel) {
            @Override
            protected void clickA() {
                I18n.toggleLanguage();
                buildToolbar();
                openConfigModal();
            }
        };
        btnLang.body().setWidth(250);
        btnLang.body().setHeight(32);
        modal.addDown(8, createOptionRow(SPRITES.icons().m.openscroll, 
                I18n.get("ui.panel.language"), 
                I18n.get("ui.panel.language.desc"), 
                btnLang, contentWidth));

        // Divider
        modal.addDown(8, new HorizontalSeparator(contentWidth));

        // Row 5: Top-Bar HUD Button
        String hudLabel = config.isShowHudButton()
                ? (I18n.getLanguage() == Language.DE ? "Aktiviert" : "Enabled")
                : (I18n.getLanguage() == Language.DE ? "Ausgeblendet" : "Hidden");
        GButt.ButtPanel btnHud = new GButt.ButtPanel(hudLabel) {
            @Override
            protected void clickA() {
                config.toggleHudButton();
                openConfigModal();
            }
        };
        btnHud.selectedSet(config.isShowHudButton());
        btnHud.body().setWidth(250);
        btnHud.body().setHeight(32);
        modal.addDown(8, createOptionRow(SPRITES.icons().m.wallceiling,
                I18n.get("ui.panel.hud_button"),
                I18n.get("ui.panel.hud_button.desc"),
                btnHud, contentWidth));

        // Divider
        modal.addDown(8, new HorizontalSeparator(contentWidth));

        // Footer Action Bar: Reset Defaults & Close/Done
        GuiSection footer = new GuiSection();
        footer.body().setWidth(contentWidth);

        GButt.ButtPanel btnReset = new GButt.ButtPanel(I18n.get("btn.reset")) {
            @Override
            protected void clickA() {
                config.resetDefaults();
                openConfigModal();
            }
        }.icon(SPRITES.icons().m.trash);

        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close")) {
            @Override
            protected void clickA() {
                closeConfigModal();
            }
        }.icon(SPRITES.icons().m.ok);

        footer.add(btnReset, 0, 0);
        btnClose.body().moveX2(contentWidth);
        footer.add(btnClose);

        modal.addDown(12, footer);
        modal.pad(20, 16);

        // Ornate Grand Game Panel fitted tightly around modal
        GPanel panel = new GPanel(modal.body()).setBig();
        panel.setTitle(I18n.get("ui.panel.title"));
        panel.setCloseAction(new ACTION() {
            @Override
            public void exe() {
                closeConfigModal();
            }
        });
        panelRef[0] = panel;

        // Add panel to modal and send behind all interactive elements
        modal.add(panel);
        modal.moveLastToBack();

        int totalW = modal.body().width();
        int totalH = modal.body().height();

        if (configModalX < 0 || configModalY < 0) {
            configModalX = C.DIM() != null ? (C.WIDTH() - totalW) / 2 : 100;
            configModalY = C.DIM() != null ? (C.HEIGHT() - totalH) / 2 : 100;
        }

        // Clamp to screen
        if (C.DIM() != null) {
            configModalX = CLAMP.i(configModalX, 10, Math.max(10, C.WIDTH() - totalW - 10));
            configModalY = CLAMP.i(configModalY, 10, Math.max(10, C.HEIGHT() - totalH - 10));
        }

        modal.body().moveX1Y1(configModalX, configModalY);
        // Compensate vanilla GPanel$1.moveY1 bug (missing +8 closeAction offset)
        panel.inner().incrY(8);
        isConfigOpen = true;
        activeConfigModal = modal;
        VIEW.inters().section.activate(modal);
    }

    private GuiSection createOptionRow(SPRITE icon, String title, String desc, GButt.ButtPanel actionBtn, int width) {
        GuiSection row = new GuiSection();
        row.body().setWidth(width);

        // Left Information Block (Icon + Title + Description)
        GuiSection leftBlock = new GuiSection();
        RENDEROBJ.Sprite iconObj = new RENDEROBJ.Sprite(icon);
        leftBlock.add(iconObj, 0, 4);

        GuiSection textBlock = new GuiSection();
        GHeader header = new GHeader(title, UI.FONT().H2);
        textBlock.add(header, 0, 0);

        GText descText = new GText(UI.FONT().S, desc).normalify();
        descText.setMultipleLines(true);
        int btnWidth = actionBtn.body().width();
        descText.setMaxWidth(width - btnWidth - 50);
        descText.adjustWidth();
        textBlock.addDown(4, descText);

        leftBlock.addRightC(12, textBlock);
        row.add(leftBlock, 0, 0);

        // Right Control Button
        actionBtn.body().moveX2(width);
        actionBtn.body().centerY(leftBlock.body());
        actionBtn.hoverTitleSet(title);
        actionBtn.hoverInfoSet(desc);
        row.add(actionBtn);

        int rowHeight = Math.max(leftBlock.body().height(), actionBtn.body().height());
        row.body().setHeight(rowHeight);

        return row;
    }

    /**
     * Native Toolbar Panel container styled with UI.PANEL().butt.
     */
    private static final class ToolbarSection extends GuiSection {
        @Override
        public void render(SPRITE_RENDERER r, float ds) {
            UI.PANEL().butt.render(r, body(), 0);
            super.render(r, ds);
        }
    }

    /**
     * Authentic 2px horizontal separator line using native UI border color.
     */
    private static final class HorizontalSeparator extends SPRITE.Imp {
        public HorizontalSeparator(int width) {
            super(width, 2);
        }

        @Override
        public void render(SPRITE_RENDERER r, int X1, int X2, int Y1, int Y2) {
            GCOLOR.UI().border(r, X1, X2, Y1, Y2);
        }
    }
}
