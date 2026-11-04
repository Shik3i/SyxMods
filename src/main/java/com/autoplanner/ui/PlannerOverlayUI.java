package com.autoplanner.ui;

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
import view.main.VIEW;

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

    // Draggable Box Coordinates
    private int boxX = -1;
    private int boxY = -1;
    private boolean isDragging = false;
    private int dragAnchorX = 0;
    private int dragAnchorY = 0;

    // Native Toolbar Section (compact 2-row card with authentic GButt.ButtPanel buttons)
    private final ToolbarSection toolbarSection = new ToolbarSection();
    private Language cachedLanguage = null;

    public PlannerOverlayUI(PlannerConfig config, AutoStrutSolver strutSolver, AutoFurnishSolver furnishSolver) {
        this.config = config;
        this.strutSolver = strutSolver;
        this.furnishSolver = furnishSolver;
        buildToolbar();
    }

    /**
     * Initializes default start position at lower-mid-left, completely clear of top bars and room frame.
     */
    private void initDefaultPosition() {
        int defaultX = Math.max(40, (int) (C.WIDTH() * 0.12));
        int defaultY = Math.max(120, (int) (C.HEIGHT() * 0.42));
        boxX = defaultX;
        boxY = defaultY;
    }

    /**
     * Rebuilds the native toolbar as a compact 2-row card with localized labels and official icons.
     * Called only on initialization and when the interface language changes.
     */
    private void buildToolbar() {
        cachedLanguage = I18n.getLanguage();
        toolbarSection.clear();

        // --- Drag Header Bar ---
        GuiSection headerBar = new GuiSection();
        RENDEROBJ.Sprite dragIcon = new RENDEROBJ.Sprite(SPRITES.icons().m.openscroll);
        headerBar.add(dragIcon, 0, 0);
        GText titleText = new GText(UI.FONT().S, "Syx Auto-Planner").lablify();
        headerBar.addRightC(6, titleText);

        // --- Row 1 Buttons (Primary Builders) ---
        // Button 1: Auto-Strut (Pillars)
        GButt.ButtPanel btnStrut = new GButt.ButtPanel(I18n.get("btn.strut")) {
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
        GButt.ButtPanel btnFurnish = new GButt.ButtPanel(I18n.get("btn.furnish")) {
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
        GButt.ButtPanel btnQuick = new GButt.ButtPanel(I18n.get("btn.quickfill")) {
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

        GuiSection row1 = new GuiSection();
        row1.add(btnStrut, 0, 0);
        row1.addRightC(4, btnFurnish);
        row1.addRightC(4, btnQuick);

        // --- Row 2 Buttons (Secondary / Config) ---
        // Button 4: Clear Furniture
        GButt.ButtPanel btnClear = new GButt.ButtPanel(I18n.get("btn.clear")) {
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
        GButt.ButtPanel btnConfig = new GButt.ButtPanel(I18n.get("btn.settings")) {
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

        GuiSection row2 = new GuiSection();
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
     * an area-based room blueprint is being drafted, at least 1 tile has been drawn,
     * and NO modal popup / menu is blocking the screen.
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
        if (!placer.blueprint().constructor().usesArea()) {
            return false;
        }
        return placer.size() > 0;
    }

    public void render(Renderer r, float ds) {
        if (!isPlacerActive()) {
            isDragging = false;
            return;
        }

        if (cachedLanguage != I18n.getLanguage()) {
            buildToolbar();
        }

        if (boxX < 0 || boxY < 0) {
            initDefaultPosition();
        }

        COORDINATE m = CORE.getInput().getMouse().getCoo();

        // Handle active drag
        if (isDragging) {
            if (m != null && MButt.LEFT.isDown()) {
                boxX = CLAMP.i(m.x() - dragAnchorX, 10, C.WIDTH() - toolbarSection.body().width() - 10);
                boxY = CLAMP.i(m.y() - dragAnchorY, 34, C.HEIGHT() - toolbarSection.body().height() - 10);
            } else {
                isDragging = false;
            }
        }

        toolbarSection.body().moveX1(boxX);
        toolbarSection.body().moveY1(boxY);

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
        if (button != MButt.LEFT || !isPlacerActive()) {
            return false;
        }

        COORDINATE m = CORE.getInput().getMouse().getCoo();
        if (m == null) {
            return false;
        }

        if (toolbarSection.body().holdsPoint(m.x(), m.y())) {
            toolbarSection.hover(m);

            // First check if an individual button consumed the click
            boolean consumed = toolbarSection.click();
            if (consumed) {
                CORE.getInput().clearAllInput();
                return true;
            }

            // Otherwise, start dragging the box!
            isDragging = true;
            dragAnchorX = m.x() - boxX;
            dragAnchorY = m.y() - boxY;
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

    /**
     * Opens or refreshes the ornate Songs of Syx Configuration Center modal dialog.
     */
    public void openConfigModal() {
        GuiSection root = new GuiSection();
        GuiSection content = new GuiSection();
        int contentWidth = 500;

        // Header Subtitle
        GText subTitle = new GText(UI.FONT().S, I18n.get("ui.panel.subtitle")).normalify();
        content.add(subTitle, 0, 0);

        // Divider
        content.addDown(8, new HorizontalSeparator(contentWidth));

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
        btnStyle.body().setWidth(160);
        content.addDown(8, createOptionRow(SPRITES.icons().m.place_fill, 
                I18n.get("ui.panel.style"), 
                I18n.get("ui.panel.style.desc"), 
                btnStyle, contentWidth));

        // Divider
        content.addDown(8, new HorizontalSeparator(contentWidth));

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
        btnEff.body().setWidth(160);
        content.addDown(8, createOptionRow(SPRITES.icons().m.ok, 
                I18n.get("ui.panel.target_eff"), 
                I18n.get("ui.panel.target_eff.desc"), 
                btnEff, contentWidth));

        // Divider
        content.addDown(8, new HorizontalSeparator(contentWidth));

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
        btnStorage.body().setWidth(160);
        content.addDown(8, createOptionRow(SPRITES.icons().m.storage_push, 
                I18n.get("ui.panel.storage"), 
                I18n.get("ui.panel.storage.desc"), 
                btnStorage, contentWidth));

        // Divider
        content.addDown(8, new HorizontalSeparator(contentWidth));

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
        btnLang.body().setWidth(160);
        content.addDown(8, createOptionRow(SPRITES.icons().m.openscroll, 
                I18n.get("ui.panel.language"), 
                I18n.get("ui.panel.language.desc"), 
                btnLang, contentWidth));

        // Divider
        content.addDown(8, new HorizontalSeparator(contentWidth));

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
                VIEW.inters().section.close();
            }
        }.icon(SPRITES.icons().m.ok);

        footer.add(btnReset, 0, 0);
        btnClose.body().moveX2(contentWidth);
        footer.add(btnClose);

        content.addDown(12, footer);
        content.pad(16, 12);

        // Ornate Grand Game Panel
        GPanel panel = new GPanel().setBig();
        panel.inner().set(content.body());
        panel.setTitle(I18n.get("ui.panel.title"), UI.FONT().H1);

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                VIEW.inters().section.close();
            }
        };
        panel.setCloseAction(closeAction);

        root.add(panel);
        content.body().centerIn(panel.inner());
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(root);
    }

    private GuiSection createOptionRow(SPRITE icon, String title, String desc, GButt.ButtPanel actionBtn, int width) {
        GuiSection row = new GuiSection();
        row.body().setWidth(width);

        // Left Information Block (Icon + Title + Description)
        GuiSection leftBlock = new GuiSection();
        RENDEROBJ.Sprite iconObj = new RENDEROBJ.Sprite(icon);
        leftBlock.add(iconObj, 0, 0);

        GuiSection textBlock = new GuiSection();
        GHeader header = new GHeader(title, UI.FONT().H2);
        textBlock.add(header, 0, 0);

        GText descText = new GText(UI.FONT().S, desc).normalify();
        textBlock.addDown(2, descText);

        leftBlock.addRightC(10, textBlock);
        row.add(leftBlock, 0, 0);

        // Right Control Button
        actionBtn.body().moveX2(width);
        actionBtn.body().centerY(leftBlock.body());
        row.add(actionBtn);

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
