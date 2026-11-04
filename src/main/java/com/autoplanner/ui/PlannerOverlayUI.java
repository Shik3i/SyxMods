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
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * Native-integrated Room Editor Toolbar & Config Center for Syx Auto-Planner.
 * Strictly docks above the active room constructor frame, completely isolated from top bar elements.
 * Strictly guarantees ZERO object allocations during 60Hz render and update loops.
 */
public final class PlannerOverlayUI {

    private final PlannerConfig config;
    private final AutoStrutSolver strutSolver;
    private final AutoFurnishSolver furnishSolver;

    private static boolean isModalOpen = false;
    private String statusMessage = "";
    private double statusTimer = 0.0;

    // Fixed toolbar geometry
    private static final int TOOLBAR_WIDTH = 460;
    private static final int TOOLBAR_HEIGHT = 32;

    // HUD Button Bounds (in screen pixels)
    private final Rec panelBounds = new Rec();
    private final Rec btnStrutBounds = new Rec();
    private final Rec btnFurnishBounds = new Rec();
    private final Rec btnQuickBounds = new Rec();
    private final Rec btnClearBounds = new Rec();
    private final Rec btnConfigBounds = new Rec();

    // Zero-GC String Cache for 60Hz Render Loop
    private I18n.Language cachedLanguage = null;
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
        if (SETT.ROOMS() == null || SETT.ROOMS().placement == null) {
            return false;
        }
        RoomPlacer placer = SETT.ROOMS().placement.placer;
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
            return;
        }

        I18n.Language currentLang = I18n.getLanguage();
        if (cachedLanguage != currentLang) {
            cachedLanguage = currentLang;
            cachedBtnStrut = I18n.get("btn.strut");
            cachedBtnFurnish = I18n.get("btn.furnish");
            cachedBtnQuick = I18n.get("btn.quickfill");
            cachedBtnClear = I18n.get("btn.clear");
            cachedBtnConfig = I18n.get("btn.settings");
        }

        // Compute docking position:
        // Native Songs of Syx RoomPlacer frame sits at y1 = C.SG * 80.
        // We dock directly 4px above it, completely clear of the top HUD bar (y <= 30).
        int screenWidth = C.WIDTH();
        int nativeFrameY1 = (int) (C.SG * 80);

        int panelW = TOOLBAR_WIDTH;
        int panelH = TOOLBAR_HEIGHT;
        int panelX = (screenWidth - panelW) / 2;
        int panelY = Math.max(34, nativeFrameY1 - panelH - 4);

        panelBounds.set(panelX, panelX + panelW, panelY, panelY + panelH);

        // Render Background Panel Box
        COLOR.BLACK.render(r, panelBounds);
        COLOR.WHITE35.render(r, panelBounds, 1);

        // Buttons Row (Vertically centered at panelY + 4, height 24px)
        int btnY = panelY + 4;
        int btnH = 24;

        // Button 1: Auto-Strut (92px)
        int btn1W = 92;
        int btn1X = panelX + 6;
        btnStrutBounds.set(btn1X, btn1X + btn1W, btnY, btnY + btnH);
        renderButton(r, btnStrutBounds, cachedBtnStrut);

        // Button 2: Auto-Furnish (96px)
        int btn2W = 96;
        int btn2X = btn1X + btn1W + 6;
        btnFurnishBounds.set(btn2X, btn2X + btn2W, btnY, btnY + btnH);
        renderButton(r, btnFurnishBounds, cachedBtnFurnish);

        // Button 3: Quick Build (92px)
        int btn3W = 92;
        int btn3X = btn2X + btn2W + 6;
        btnQuickBounds.set(btn3X, btn3X + btn3W, btnY, btnY + btnH);
        renderButton(r, btnQuickBounds, cachedBtnQuick);

        // Button 4: Clear (70px)
        int btn4W = 70;
        int btn4X = btn3X + btn3W + 6;
        btnClearBounds.set(btn4X, btn4X + btn4W, btnY, btnY + btnH);
        renderButton(r, btnClearBounds, cachedBtnClear);

        // Button 5: Config (74px)
        int btn5W = 74;
        int btn5X = btn4X + btn4W + 6;
        btnConfigBounds.set(btn5X, btn5X + btn5W, btnY, btnY + btnH);
        renderButton(r, btnConfigBounds, cachedBtnConfig);

        // Status Feedback Message (Floating centered directly above the toolbar)
        if (!statusMessage.isEmpty()) {
            GCOLOR.MAP().OK.bind();
            int msgW = UI.FONT().S.width(statusMessage);
            int msgX = panelX + (panelW - msgW) / 2;
            int msgY = panelY - 15;
            UI.FONT().S.render(r, statusMessage, msgX, msgY);
            COLOR.unbind();
        }
    }

    private void renderButton(Renderer r, Rec b, String text) {
        COORDINATE m = CORE.getInput().getMouse().getCoo();
        boolean hover = m != null && b.holdsPoint(m.x(), m.y());

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

        if (!isPlacerActive()) {
            return false;
        }

        RoomPlacer placer = SETT.ROOMS().placement.placer;
        COORDINATE m = CORE.getInput().getMouse().getCoo();
        if (m == null) {
            return false;
        }
        int mx = m.x();
        int my = m.y();

        if (panelBounds.holdsPoint(mx, my)) {
            if (btnStrutBounds.holdsPoint(mx, my)) {
                triggerAutoStrut(placer);
            } else if (btnFurnishBounds.holdsPoint(mx, my)) {
                triggerAutoFurnish(placer);
            } else if (btnQuickBounds.holdsPoint(mx, my)) {
                triggerQuickBuild(placer);
            } else if (btnClearBounds.holdsPoint(mx, my)) {
                triggerClearFurniture(placer);
            } else if (btnConfigBounds.holdsPoint(mx, my)) {
                openConfigModal();
            }

            // Consume click so downstream settlement clicks or tile placements are suppressed
            CORE.getInput().clearAllInput();
            return true;
        }

        return false;
    }

    public void hoverTimer(double mouseTimer, GBox text) {
        if (!isPlacerActive() || text == null) {
            return;
        }
        COORDINATE m = CORE.getInput().getMouse().getCoo();
        if (m == null) return;
        int mx = m.x();
        int my = m.y();

        if (btnStrutBounds.holdsPoint(mx, my)) {
            text.title(I18n.get("btn.strut"));
            text.text(I18n.get("tooltip.strut"));
        } else if (btnFurnishBounds.holdsPoint(mx, my)) {
            text.title(I18n.get("btn.furnish"));
            text.text(I18n.get("tooltip.furnish"));
        } else if (btnQuickBounds.holdsPoint(mx, my)) {
            text.title(I18n.get("btn.quickfill"));
            text.text(I18n.get("tooltip.quickfill"));
        } else if (btnClearBounds.holdsPoint(mx, my)) {
            text.title(I18n.get("btn.clear"));
            text.text(I18n.get("tooltip.clear"));
        } else if (btnConfigBounds.holdsPoint(mx, my)) {
            text.title(I18n.get("btn.settings"));
            text.text(I18n.get("tooltip.settings"));
        }
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
            setStatus("[Error] Strut solver failed");
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
            setStatus("[Error] Furnish solver failed");
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
            setStatus("[Error] QuickBuild failed");
        }
    }

    public void triggerClearFurniture(RoomPlacer placer) {
        try {
            furnishSolver.clearExistingFurniture(placer);
            setStatus(I18n.get("msg.clear_success"));
        } catch (Throwable t) {
            snake2d.LOG.err("AutoPlanner: ClearFurniture exception: " + t.getMessage());
            t.printStackTrace();
            setStatus("[Error] ClearFurniture failed");
        }
    }

    private void setStatus(String msg) {
        this.statusMessage = msg;
        this.statusTimer = 4.0; // Show for 4 seconds
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
