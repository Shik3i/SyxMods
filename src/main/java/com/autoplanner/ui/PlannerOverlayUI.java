package com.autoplanner.ui;

import com.autoplanner.config.PlannerConfig;
import com.autoplanner.solver.AutoFurnishSolver;
import com.autoplanner.solver.AutoStrutSolver;
import com.autoplanner.solver.PlacerBridge;
import com.autoplanner.util.I18n;

import init.constant.C;
import init.sprite.UI.UI;
import settlement.main.SETT;
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
 * Dynamically surfaces when the player is drawing/editing a room.
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
        if (placer == null || placer.blueprint() == null || PlacerBridge.getArea(placer) <= 0) {
            return;
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

        // Header: Room Name + Tile Count
        CharSequence roomName = placer.blueprint().info.name;
        String header = I18n.get("ui.overlay.room", roomName, PlacerBridge.getArea(placer));
        UI.FONT().S.render(r, header, panelX + 10, panelY + 4);

        // Buttons Row
        int btnY = panelY + 22;
        int btnH = 26;

        // Button 1: Auto-Strut
        int btn1W = 105;
        int btn1X = panelX + 8;
        btnStrutBounds.set(btn1X, btn1X + btn1W, btnY, btnY + btnH);
        renderButton(r, btnStrutBounds, I18n.get("btn.strut"));

        // Button 2: Auto-Furnish
        int btn2W = 110;
        int btn2X = btn1X + btn1W + 6;
        btnFurnishBounds.set(btn2X, btn2X + btn2W, btnY, btnY + btnH);
        renderButton(r, btnFurnishBounds, I18n.get("btn.furnish"));

        // Button 3: Quick Build
        int btn3W = 110;
        int btn3X = btn2X + btn2W + 6;
        btnQuickBounds.set(btn3X, btn3X + btn3W, btnY, btnY + btnH);
        renderButton(r, btnQuickBounds, I18n.get("btn.quickfill"));

        // Button 4: Clear
        int btn4W = 85;
        int btn4X = btn3X + btn3W + 6;
        btnClearBounds.set(btn4X, btn4X + btn4W, btnY, btnY + btnH);
        renderButton(r, btnClearBounds, I18n.get("btn.clear"));

        // Button 5: Config / Options
        int btn5W = 65;
        int btn5X = btn4X + btn4W + 6;
        btnConfigBounds.set(btn5X, btn5X + btn5W, btnY, btnY + btnH);
        renderButton(r, btnConfigBounds, I18n.get("btn.settings"));

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
        if (placer == null || placer.blueprint() == null || PlacerBridge.getArea(placer) <= 0) {
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
        AutoStrutSolver.Result res = strutSolver.solve(placer);
        if (res.pillarsPlaced > 0) {
            setStatus(I18n.get("msg.strut_success", res.pillarsPlaced));
        } else {
            setStatus(I18n.get("msg.strut_none_needed"));
        }
    }

    public void triggerAutoFurnish(RoomPlacer placer) {
        AutoFurnishSolver.Result res = furnishSolver.solve(placer, config);
        if (res.success) {
            setStatus(I18n.get("msg.furnish_success", res.workstations, res.efficiencyModules, res.efficiencyPercent));
        } else {
            setStatus(I18n.get(res.errorMessage));
        }
    }

    public void triggerQuickBuild(RoomPlacer placer) {
        AutoStrutSolver.Result sRes = strutSolver.solve(placer);
        AutoFurnishSolver.Result fRes = furnishSolver.solve(placer, config);

        if (fRes.success) {
            setStatus(I18n.get("msg.furnish_success", fRes.workstations, fRes.efficiencyModules, fRes.efficiencyPercent));
        } else {
            setStatus(I18n.get(fRes.errorMessage));
        }
    }

    public void triggerClearFurniture(RoomPlacer placer) {
        furnishSolver.clearExistingFurniture(placer);
        setStatus(I18n.get("msg.clear_success"));
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
