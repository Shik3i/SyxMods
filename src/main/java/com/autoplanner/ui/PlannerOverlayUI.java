package com.autoplanner.ui;

import com.autoplanner.config.PlannerConfig;
import com.autoplanner.solver.AutoFurnishSolver;
import com.autoplanner.solver.AutoStrutSolver;
import com.autoplanner.util.I18n;

import init.constant.C;
import init.sprite.UI.UI;
import settlement.main.SETT;
import settlement.room.main.RoomBlueprintImp;
import settlement.room.main.placement.RoomPlacer;
import snake2d.CORE;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Rec;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.colors.GCOLOR;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * Compact HUD Overlay & Modal Configuration Center for Syx Auto-Planner.
 * Dynamically surfaces when the player is drawing/editing a furnisher room.
 * Strictly guarantees ZERO object allocations during 60Hz render and update loops.
 */
public final class PlannerOverlayUI {

    private final PlannerConfig config;
    private final AutoStrutSolver strutSolver;
    private final AutoFurnishSolver furnishSolver;

    private static boolean isModalOpen = false;
    private String statusMessage = "";
    private double statusTimer = 0.0;

    // HUD Button Bounds (in screen pixels)
    private final Rec panelBounds = new Rec();
    private final Rec btnStrutBounds = new Rec();
    private final Rec btnFurnishBounds = new Rec();
    private final Rec btnQuickBounds = new Rec();
    private final Rec btnClearBounds = new Rec();
    private final Rec btnConfigBounds = new Rec();

    // Zero-GC String Cache for 60Hz Render Loop
    private RoomBlueprintImp cachedBlueprint = null;
    private int cachedArea = -1;
    private I18n.Language cachedLanguage = null;
    private String cachedHeader = "";
    private String cachedBtnStrut = "";
    private String cachedBtnFurnish = "";
    private String cachedBtnQuick = "";
    private String cachedBtnClear = "";
    private String cachedBtnConfig = "";

    public PlannerOverlayUI(PlannerConfig config, AutoStrutSolver strutSolver, AutoFurnishSolver furnishSolver) {
        this.config = config;
        this.strutSolver = strutSolver;
        this.furnishSolver = furnishSolver;
    }

    public void update(double ds) {
        if (statusTimer > 0.0) {
            statusTimer -= ds;
            if (statusTimer <= 0.0) {
                statusMessage = "";
            }
        }
    }

    public void render(Renderer r, float ds) {
        if (!config.isShowOverlay()) {
            return;
        }

        RoomPlacer placer = getActivePlacer();
        if (placer == null || placer.blueprint() == null || placer.blueprint().constructor() == null) {
            return;
        }

        // Only active for actual area-based furnisher rooms (ignores pastures, roads, canals, farms without area constructor)
        if (!placer.blueprint().constructor().usesArea()) {
            return;
        }

        int currentArea = placer.size();
        if (currentArea <= 0) {
            return;
        }

        RoomBlueprintImp bp = placer.blueprint();
        I18n.Language currentLang = I18n.getLanguage();

        // Zero-GC cache update: Only re-format strings when area, room blueprint or language changes
        if (cachedBlueprint != bp || cachedArea != currentArea || cachedLanguage != currentLang) {
            cachedBlueprint = bp;
            cachedArea = currentArea;
            cachedLanguage = currentLang;

            CharSequence roomName = bp.info != null ? bp.info.name : "";
            cachedHeader = I18n.get("ui.overlay.room", roomName, currentArea);
            cachedBtnStrut = I18n.get("btn.strut");
            cachedBtnFurnish = I18n.get("btn.furnish");
            cachedBtnQuick = I18n.get("btn.quickfill");
            cachedBtnClear = I18n.get("btn.clear");
            cachedBtnConfig = I18n.get("btn.settings");
        }

        // Compute HUD Bar position (Top Center of screen)
        int screenWidth = C.WIDTH();
        int panelWidth = 510;
        int panelHeight = statusMessage.isEmpty() ? 56 : 74;
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = 12;

        panelBounds.set(panelX, panelX + panelWidth, panelY, panelY + panelHeight);

        // Render Background Panel Box
        COLOR.BLACK.render(r, panelBounds);
        COLOR.WHITE50.render(r, panelBounds, 1);

        // Header: Room Name + Tile Count (Cached string)
        UI.FONT().S.render(r, cachedHeader, panelX + 10, panelY + 4);

        // Buttons Row
        int btnY = panelY + 22;
        int btnH = 26;

        // Button 1: Auto-Strut
        int btn1W = 105;
        int btn1X = panelX + 8;
        btnStrutBounds.set(btn1X, btn1X + btn1W, btnY, btnY + btnH);
        renderButton(r, btnStrutBounds, cachedBtnStrut);

        // Button 2: Auto-Furnish
        int btn2W = 110;
        int btn2X = btn1X + btn1W + 6;
        btnFurnishBounds.set(btn2X, btn2X + btn2W, btnY, btnY + btnH);
        renderButton(r, btnFurnishBounds, cachedBtnFurnish);

        // Button 3: Quick Build
        int btn3W = 110;
        int btn3X = btn2X + btn2W + 6;
        btnQuickBounds.set(btn3X, btn3X + btn3W, btnY, btnY + btnH);
        renderButton(r, btnQuickBounds, cachedBtnQuick);

        // Button 4: Clear
        int btn4W = 85;
        int btn4X = btn3X + btn3W + 6;
        btnClearBounds.set(btn4X, btn4X + btn4W, btnY, btnY + btnH);
        renderButton(r, btnClearBounds, cachedBtnClear);

        // Button 5: Config / Options
        int btn5W = 65;
        int btn5X = btn4X + btn4W + 6;
        btnConfigBounds.set(btn5X, btn5X + btn5W, btnY, btnY + btnH);
        renderButton(r, btnConfigBounds, cachedBtnConfig);

        // Status Feedback Message (if active)
        if (!statusMessage.isEmpty()) {
            GCOLOR.MAP().OK.bind();
            UI.FONT().S.render(r, statusMessage, panelX + 10, panelY + 52);
            COLOR.unbind();
        }
    }

    private void renderButton(Renderer r, Rec b, String text) {
        COORDINATE m = CORE.getInput().getMouse().getCoo();
        boolean hover = b.holdsPoint(m.x(), m.y());

        if (hover) {
            COLOR.WHITE35.render(r, b);
            COLOR.WHITE100.render(r, b, 1);
        } else {
            COLOR.WHITE15.render(r, b);
            COLOR.WHITE50.render(r, b, 1);
        }

        int textW = UI.FONT().S.width(text);
        int tx = b.x1() + (b.width() - textW) / 2;
        int ty = b.y1() + (b.height() - UI.FONT().S.height()) / 2;
        UI.FONT().S.render(r, text, tx, ty);
    }

    public boolean mouseClick(MButt button) {
        if (button != MButt.LEFT) {
            return false;
        }

        RoomPlacer placer = getActivePlacer();
        if (placer == null || placer.blueprint() == null || placer.blueprint().constructor() == null || placer.size() <= 0) {
            return false;
        }

        COORDINATE m = CORE.getInput().getMouse().getCoo();
        int mx = m.x();
        int my = m.y();

        if (btnStrutBounds.holdsPoint(mx, my)) {
            triggerAutoStrut(placer);
            return true;
        }

        if (btnFurnishBounds.holdsPoint(mx, my)) {
            triggerAutoFurnish(placer);
            return true;
        }

        if (btnQuickBounds.holdsPoint(mx, my)) {
            triggerQuickBuild(placer);
            return true;
        }

        if (btnClearBounds.holdsPoint(mx, my)) {
            triggerClearFurniture(placer);
            return true;
        }

        if (btnConfigBounds.holdsPoint(mx, my)) {
            openConfigModal();
            return true;
        }

        return panelBounds.holdsPoint(mx, my);
    }

    public void triggerAutoStrut(RoomPlacer placer) {
        try {
            AutoStrutSolver.Result res = strutSolver.solve(placer);
            if (res.pillarsPlaced > 0) {
                setStatus(I18n.get("msg.strut_success", res.pillarsPlaced));
            } else {
                setStatus(I18n.get("msg.strut_none_needed"));
            }
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: AutoStrut exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] Strut solver encountered an error");
        }
    }

    public void triggerAutoFurnish(RoomPlacer placer) {
        try {
            AutoFurnishSolver.Result res = furnishSolver.solve(placer, config);
            if (res.success) {
                if (res.virtualDoorUsed) {
                    setStatus(I18n.get("msg.furnish_virtual_door", res.workstations, res.efficiencyModules, res.efficiencyPercent, res.virtualDoorX, res.virtualDoorY));
                } else {
                    setStatus(I18n.get("msg.furnish_success", res.workstations, res.efficiencyModules, res.efficiencyPercent));
                }
            } else {
                setStatus(I18n.get(res.errorMessage));
            }
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: AutoFurnish exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] Furnish solver encountered an error");
        }
    }

    public void triggerQuickBuild(RoomPlacer placer) {
        try {
            AutoStrutSolver.Result sRes = strutSolver.solve(placer);
            AutoFurnishSolver.Result fRes = furnishSolver.solve(placer, config);

            if (fRes.success) {
                if (fRes.virtualDoorUsed) {
                    setStatus(I18n.get("msg.furnish_virtual_door", fRes.workstations, fRes.efficiencyModules, fRes.efficiencyPercent, fRes.virtualDoorX, fRes.virtualDoorY));
                } else {
                    setStatus(I18n.get("msg.furnish_success", fRes.workstations, fRes.efficiencyModules, fRes.efficiencyPercent));
                }
            } else {
                setStatus(I18n.get(fRes.errorMessage));
            }
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: QuickBuild exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] QuickBuild encountered an error");
        }
    }

    public void triggerClearFurniture(RoomPlacer placer) {
        try {
            furnishSolver.clearExistingFurniture(placer);
            setStatus(I18n.get("msg.clear_success"));
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: ClearFurniture exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] ClearFurniture encountered an error");
        }
    }

    private void setStatus(String msg) {
        this.statusMessage = msg;
        this.statusTimer = 5.0; // Show for 5 seconds
    }

    private RoomPlacer getActivePlacer() {
        if (SETT.ROOMS() == null || SETT.ROOMS().placement == null) {
            return null;
        }
        return SETT.ROOMS().placement.placer;
    }

    public void openConfigModal() {
        if (isModalOpen) {
            VIEW.inters().section.close();
            isModalOpen = false;
            return;
        }

        isModalOpen = true;
        GuiSection root = new GuiSection();
        GPanel panel = new GPanel(450, 300);
        panel.setTitle(I18n.get("ui.panel.title"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                isModalOpen = false;
                VIEW.inters().section.close();
            }
        };
        panel.setCloseAction(closeAction);

        GuiSection content = new GuiSection();
        content.pad(16);

        // Row 1: Layout Style Toggle
        GText lblStyle = new GText(UI.FONT().M, I18n.get("ui.panel.style"));
        content.add(lblStyle, 0, 0);

        String styleKey = config.getStyle() == PlannerConfig.LayoutStyle.COMPACT 
                ? "ui.panel.style.compact" : "ui.panel.style.balanced";
        GButt.ButtPanel btnStyle = new GButt.ButtPanel(I18n.get(styleKey));
        btnStyle.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.cycleStyle();
                openConfigModal();
            }
        });
        content.addRight(12, btnStyle);

        // Row 2: Target Efficiency
        content.addDown(16, new GText(UI.FONT().M, I18n.get("ui.panel.target_eff", (int) Math.round(config.getTargetEfficiency() * 100))));
        GButt.ButtPanel btnEff = new GButt.ButtPanel(I18n.get("btn.target_eff", (int) Math.round(config.getTargetEfficiency() * 100)));
        btnEff.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.cycleTargetEfficiency();
                openConfigModal();
            }
        });
        content.addRight(12, btnEff);

        // Row 3: Storage Allowance
        content.addDown(16, new GText(UI.FONT().M, I18n.get("ui.panel.storage_ratio", (int) Math.round(config.getStorageRatio() * 100))));
        GButt.ButtPanel btnStorage = new GButt.ButtPanel(I18n.get("btn.storage_ratio", (int) Math.round(config.getStorageRatio() * 100)));
        btnStorage.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.cycleStorageRatio();
                openConfigModal();
            }
        });
        content.addRight(12, btnStorage);

        // Row 4: Language Toggle
        content.addDown(20, new GText(UI.FONT().M, I18n.get("btn.language")));
        GButt.ButtPanel btnLang = new GButt.ButtPanel("[" + I18n.getLanguage().code.toUpperCase() + "]");
        btnLang.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                I18n.toggleLanguage();
                cachedLanguage = null; // force cache refresh
                openConfigModal();
            }
        });
        content.addRight(12, btnLang);

        // Footer: Close Button
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.clickActionSet(closeAction);
        content.addDown(16, btnClose);

        root.add(panel);
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(root);
    }
}
