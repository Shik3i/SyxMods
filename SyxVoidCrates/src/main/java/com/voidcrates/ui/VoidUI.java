package com.voidcrates.ui;

import com.voidcrates.config.VoidConfig;
import com.voidcrates.manager.VoidManager;
import com.voidcrates.util.I18n;

import game.GAME;
import init.constant.C;
import init.sprite.UI.UI;
import snake2d.CORE;
import snake2d.MButt;
import snake2d.SPRITE_RENDERER;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import snake2d.util.misc.CLAMP;
import util.gui.misc.GButt;
import util.gui.misc.GHeader;
import util.gui.misc.GText;
import util.gui.panel.GFrame;
import util.gui.panel.GPanel;
import view.main.VIEW;

public final class VoidUI {

    private static boolean isOpen = false;
    private static GuiSection activeRootSection = null;
    private static int activeTab = 0; // 0 = Rules, 1 = Crates & Actions
    private static int windowX = -1;
    private static int windowY = -1;

    private VoidUI() {}

    public interface UICallback {
        void onConfigUpdated();
        void onPurgeNowRequested();
        void onClearMarksRequested();
    }

    public static boolean isOpen() {
        if (!isOpen) return false;
        if (VIEW.inters() == null || VIEW.inters().section == null || !VIEW.inters().section.isActivated()) {
            isOpen = false;
            activeRootSection = null;
            return false;
        }
        GuiSection curr = VIEW.inters().section.current();
        if (curr != activeRootSection) {
            isOpen = false;
            activeRootSection = null;
            return false;
        }
        return true;
    }

    public static void close() {
        isOpen = false;
        activeRootSection = null;
        if (VIEW.inters() != null && VIEW.inters().section != null) {
            VIEW.inters().section.close();
        }
    }

    public static void toggle(VoidConfig config, VoidManager manager, UICallback callback) {
        if (isOpen()) {
            close();
        } else {
            open(config, manager, callback);
        }
    }

    public static void open(VoidConfig config, VoidManager manager, UICallback callback) {
        if (VIEW.inters() == null || VIEW.inters().section == null) {
            return;
        }

        isOpen = true;
        int contentW = 720;

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                close();
            }
        };

        final GPanel[] panelRef = new GPanel[1];

        GuiSection root = new GuiSection() {
            private boolean isDragging = false;
            private int dragAnchorX = 0;
            private int dragAnchorY = 0;

            @Override
            public void render(SPRITE_RENDERER r, float ds) {
                COORDINATE m = CORE.getInput().getMouse().getCoo();
                if (m != null) {
                    if (MButt.LEFT.isDown()) {
                        if (!isDragging) {
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
                                windowX = targetX;
                                windowY = targetY;
                                body().moveX1Y1(targetX, targetY);
                                if (panelRef[0] != null) {
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

        // --- 1. TOP BAR: Tabs + Language ---
        GuiSection topBar = new GuiSection();
        int tabW = (contentW - 12) / 3;

        GButt.ButtPanel tabRules = createBtnM(I18n.get("ui.tab.rules"), tabW, 30);
        tabRules.selectedSet(activeTab == 0);
        tabRules.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 0;
                open(config, manager, callback);
            }
        });
        topBar.add(tabRules);

        GButt.ButtPanel tabCrates = createBtnM(I18n.get("ui.tab.crates"), tabW, 30);
        tabCrates.selectedSet(activeTab == 1);
        tabCrates.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 1;
                open(config, manager, callback);
            }
        });
        topBar.addRight(6, tabCrates);

        GButt.ButtPanel btnLang = createBtnM("[" + I18n.getLanguage().code.toUpperCase() + "] " + I18n.get("btn.language"), tabW, 30);
        btnLang.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                I18n.toggleLanguage();
                config.setLanguage(I18n.getLanguage().code);
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        topBar.addRight(6, btnLang);

        root.add(topBar, 0, 0);
        root.addDown(6, GFrame.separator(contentW));

        if (activeTab == 0) {
            buildRulesTab(root, contentW, config, manager, callback);
        } else {
            buildCratesTab(root, contentW, config, manager, callback);
        }

        root.addDown(8, GFrame.separator(contentW));

        // Footer: Close Button
        GButt.ButtPanel btnClose = createBtnM(I18n.get("btn.close"), contentW, 30);
        btnClose.clickActionSet(closeAction);
        root.addDown(6, btnClose);

        root.pad(20, 16);

        GPanel panel = new GPanel(root.body()).setBig();
        panel.setTitle(I18n.get("ui.title"));
        panel.setCloseAction(closeAction);
        panelRef[0] = panel;

        root.add(panel);
        root.moveLastToBack();

        int totalW = root.body().width();
        int totalH = root.body().height();

        if (windowX < 0 || windowY < 0) {
            windowX = (C.DIM() != null) ? (C.WIDTH() - totalW) / 2 : 150;
            windowY = (C.DIM() != null) ? (C.HEIGHT() - totalH) / 2 : 120;
        }

        if (C.DIM() != null) {
            windowX = CLAMP.i(windowX, 10, Math.max(10, C.WIDTH() - totalW - 10));
            windowY = CLAMP.i(windowY, 10, Math.max(10, C.HEIGHT() - totalH - 10));
        }

        root.body().moveX1Y1(windowX, windowY);
        panel.inner().incrY(8);
        activeRootSection = root;
        VIEW.inters().section.activate(root);
    }

    private static void buildRulesTab(GuiSection content, int contentW, VoidConfig config, VoidManager manager, UICallback callback) {
        boolean de = I18n.getLanguage() == I18n.Language.DE;

        // Master switch & Live stats
        GuiSection headerRow = new GuiSection();
        int btnW2 = (contentW - 12) / 2;

        GButt.ButtPanel btnMaster = createBtnM(config.isActive() ? I18n.get("ui.status.active") : I18n.get("ui.status.inactive"), btnW2, 32);
        btnMaster.selectedSet(config.isActive());
        btnMaster.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setActive(!config.isActive());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        headerRow.add(btnMaster);

        GButt.ButtPanel lblStats = createBtnM(I18n.get("ui.stats", String.valueOf(config.getTotalItemsVoided())), btnW2, 32);
        headerRow.addRight(12, lblStats);
        content.addDown(6, headerRow);

        content.addDown(6, GFrame.separator(contentW));

        // Subheader: Byproduct Rules
        GHeader ruleHeader = new GHeader(I18n.get("ui.tab.rules")).subify();
        content.addDown(6, ruleHeader);

        int colW = (contentW - 16) / 2;
        int ctrlW = 150;
        int ctrlH = 28;

        GuiSection gridRow = new GuiSection();

        // Left Column: Stone, Clay, Coal
        GuiSection leftCol = new GuiSection();

        GButt.ButtPanel btnStone = createBtnM(config.isAutoVoidStone() ? "AKTIV" : "AUS", ctrlW, ctrlH);
        btnStone.selectedSet(config.isAutoVoidStone());
        btnStone.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setAutoVoidStone(!config.isAutoVoidStone());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        leftCol.addDown(0, createOptionRow(colW, I18n.get("res.stone"), btnStone));

        GButt.ButtPanel btnClay = createBtnM(config.isAutoVoidClay() ? "AKTIV" : "AUS", ctrlW, ctrlH);
        btnClay.selectedSet(config.isAutoVoidClay());
        btnClay.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setAutoVoidClay(!config.isAutoVoidClay());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        leftCol.addDown(4, createOptionRow(colW, I18n.get("res.clay"), btnClay));

        GButt.ButtPanel btnCoal = createBtnM(config.isAutoVoidCoal() ? "AKTIV" : "AUS", ctrlW, ctrlH);
        btnCoal.selectedSet(config.isAutoVoidCoal());
        btnCoal.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setAutoVoidCoal(!config.isAutoVoidCoal());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        leftCol.addDown(4, createOptionRow(colW, I18n.get("res.coal"), btnCoal));

        gridRow.add(leftCol);

        // Right Column: Livestock/Dung, Wood, Ores
        GuiSection rightCol = new GuiSection();

        GButt.ButtPanel btnLive = createBtnM(config.isAutoVoidLivestock() ? "AKTIV" : "AUS", ctrlW, ctrlH);
        btnLive.selectedSet(config.isAutoVoidLivestock());
        btnLive.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setAutoVoidLivestock(!config.isAutoVoidLivestock());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        rightCol.addDown(0, createOptionRow(colW, I18n.get("res.livestock"), btnLive));

        GButt.ButtPanel btnWood = createBtnM(config.isAutoVoidWood() ? "AKTIV" : "AUS", ctrlW, ctrlH);
        btnWood.selectedSet(config.isAutoVoidWood());
        btnWood.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setAutoVoidWood(!config.isAutoVoidWood());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        rightCol.addDown(4, createOptionRow(colW, I18n.get("res.wood"), btnWood));

        GButt.ButtPanel btnOres = createBtnM(config.isAutoVoidOres() ? "AKTIV" : "AUS", ctrlW, ctrlH);
        btnOres.selectedSet(config.isAutoVoidOres());
        btnOres.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setAutoVoidOres(!config.isAutoVoidOres());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        rightCol.addDown(4, createOptionRow(colW, I18n.get("res.ores"), btnOres));

        gridRow.addRight(16, rightCol);
        content.addDown(4, gridRow);

        content.addDown(6, GFrame.separator(contentW));

        // Threshold & Buffer controls row
        GuiSection settingsRow = new GuiSection();

        String threshLabel = de ? "Bei 100% (Voll)" : "At 100% (Full)";
        if (config.getThresholdIndex() == 1) threshLabel = de ? "Bei 90%" : "At 90%";
        if (config.getThresholdIndex() == 2) threshLabel = de ? "Bei 75%" : "At 75%";
        if (config.getThresholdIndex() == 3) threshLabel = de ? "Sofort" : "Instant";

        GButt.ButtPanel btnThresh = createBtnM(threshLabel, ctrlW, ctrlH);
        btnThresh.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.cycleThreshold();
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        settingsRow.add(createOptionRow(colW, I18n.get("setting.threshold"), btnThresh));

        String retainLabel = de ? "0% (Ganz leeren)" : "0% (Empty)";
        if (config.getRetainBufferIndex() == 1) retainLabel = de ? "20% Puffer" : "Keep 20%";
        if (config.getRetainBufferIndex() == 2) retainLabel = de ? "50% Puffer" : "Keep 50%";

        GButt.ButtPanel btnRetain = createBtnM(retainLabel, ctrlW, ctrlH);
        btnRetain.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.cycleRetainBuffer();
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        settingsRow.addRight(16, createOptionRow(colW, I18n.get("setting.retain"), btnRetain));

        content.addDown(4, settingsRow);

        // Scope row
        GuiSection scopeRow = new GuiSection();
        String scopeLabel = config.isDesignatedOnly() ? I18n.get("setting.mode.designated_only") : I18n.get("setting.mode.all");
        GButt.ButtPanel btnScope = createBtnM(scopeLabel, 350, ctrlH);
        btnScope.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                config.setDesignatedOnly(!config.isDesignatedOnly());
                callback.onConfigUpdated();
                open(config, manager, callback);
            }
        });
        scopeRow.add(createOptionRow(contentW, I18n.get("setting.mode"), btnScope));
        content.addDown(4, scopeRow);
    }

    private static void buildCratesTab(GuiSection content, int contentW, VoidConfig config, VoidManager manager, UICallback callback) {
        boolean de = I18n.getLanguage() == I18n.Language.DE;

        GHeader actionHeader = new GHeader(I18n.get("ui.tab.crates")).subify();
        content.addDown(6, actionHeader);

        // Purge Now Button (Full Width)
        GButt.ButtPanel btnPurge = createBtnM(I18n.get("btn.purge_now"), contentW, 34);
        btnPurge.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onPurgeNowRequested();
                open(config, manager, callback);
            }
        });
        content.addDown(6, btnPurge);

        content.addDown(8, GFrame.separator(contentW));

        // Designated Crates Count
        GuiSection infoSection = new GuiSection();
        GText lblCount = new GText(UI.FONT().M, (de ? "Als Schwarzes Loch markierte Kisten: " : "Designated Black Hole Crates: ") + manager.getDesignatedCount());
        lblCount.lablify();
        infoSection.add(lblCount, 6, 6);
        content.addDown(4, infoSection);

        // Clear All Designations
        GButt.ButtPanel btnClear = createBtnM(I18n.get("btn.clear_marks"), contentW, 30);
        btnClear.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onClearMarksRequested();
                open(config, manager, callback);
            }
        });
        content.addDown(6, btnClear);
    }

    public static GButt.ButtPanel createBtnM(CharSequence text, int width, int height) {
        GText t = new GText(UI.FONT().M, text);
        GButt.ButtPanel b = new GButt.ButtPanel((snake2d.util.sprite.SPRITE) t);
        b.setDim(width, height);
        b.align(DIR.C);
        return b;
    }

    private static GuiSection createOptionRow(int width, String title, GButt control) {
        GuiSection row = new GuiSection();
        row.body().setDim(width, 32);

        GText lbl = new GText(UI.FONT().M, title);
        lbl.lablify();
        int ctrlX = width - control.body().width() - 6;
        lbl.setMaxWidth(ctrlX - 12);
        row.add(lbl, 6, (32 - lbl.height()) / 2);

        row.add(control, ctrlX, (32 - control.body().height()) / 2);
        return row;
    }
}
