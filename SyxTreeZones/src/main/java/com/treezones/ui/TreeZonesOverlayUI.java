package com.treezones.ui;

import com.treezones.config.TreeZoneConfig;
import com.treezones.tools.AreaClearPlacer;
import com.treezones.tools.ForestPlacer;
import com.treezones.tools.NoTreePlacer;
import com.treezones.tools.ResetPlacer;
import com.treezones.util.I18n;
import com.treezones.zone.GardenerActions;
import com.treezones.zone.TreeZoneManager;
import init.constant.C;
import init.sprite.SPRITES;
import init.sprite.UI.UI;
import settlement.main.SETT;
import snake2d.CORE;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.color.ColorImp;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.datatypes.Rec;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.misc.CLAMP;
import snake2d.util.sprite.SPRITE;
import util.colors.GCOLOR;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import view.interrupter.InterManager;
import view.interrupter.Interrupter;
import view.main.VIEW;
import view.subview.GameWindow;

/**
 * 100% Native-Integrated Draggable Zone Control Box & Map Overlay for Syx Tree Zones.
 * Built with authentic Songs of Syx bevels, fonts, official sprites, and zero-GC rendering.
 */
public final class TreeZonesOverlayUI {

    private final TreeZoneConfig config;
    private final TreeZoneManager zoneManager;

    private final NoTreePlacer noTreePlacer;
    private final ForestPlacer forestPlacer;
    private final ResetPlacer resetPlacer;
    private final AreaClearPlacer areaClearPlacer;

    // Toast Notification Bounds & State
    private String statusMessage = "";
    private boolean statusIsError = false;
    private double statusTimer = 0.0;
    private final Rec toastBounds = new Rec();

    // Toolbar Docking & Drag State
    private int boxX = -1;
    private int boxY = -1;
    private boolean isCustomPosition = false;
    private double relX = 0.02;
    private double relY = 0.16;
    private boolean isDragging = false;
    private int dragAnchorX = 0;
    private int dragAnchorY = 0;

    private boolean isOpen = false;

    // GUI Sections
    private final ToolbarSection toolbarSection = new ToolbarSection();
    private final GuiSection headerBar = new GuiSection();
    private final GuiSection row1 = new GuiSection();
    private final GuiSection row2 = new GuiSection();
    private final GuiSection row3 = new GuiSection();

    // Buttons - Row 1: Tools
    private GButt.ButtPanel btnSnap;
    private GButt.ButtPanel btnNoTree;
    private GButt.ButtPanel btnForest;
    private GButt.ButtPanel btnClearArea;
    private GButt.ButtPanel btnReset;

    // Buttons - Row 2: Global Clear Actions
    private GButt.ButtPanel btnClearTrees;
    private GButt.ButtPanel btnClearFlowers;
    private GButt.ButtPanel btnClearBushes;
    private GButt.ButtPanel btnClearMushrooms;
    private GButt.ButtPanel btnClearCrops;
    private GButt.ButtPanel btnClearAll;

    // Buttons - Row 3: Toggles
    private GButt.ButtPanel btnDrop;
    private GButt.ButtPanel btnAutoNoGrowth;
    private GButt.ButtPanel btnOverlay;
    private GButt.ButtPanel btnInstant;

    // Overlay Colors
    private static final ColorImp COLOR_FOREST = new ColorImp(50, 220, 80);
    private static final ColorImp COLOR_NOTREE = new ColorImp(240, 50, 50);

    // Native Settlement Interrupter to prevent click-through
    private final class OverlayInterrupter extends Interrupter {
        private OverlayInterrupter() {
            super(true, true, false); // persistent = true, pinned = true, desturbingfuck = false
        }

        @Override
        protected boolean hover(COORDINATE m, boolean hasMoved) {
            if (!isOpen || !isInSettlementView()) {
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
            if (!isOpen || !isInSettlementView()) {
                return;
            }
            handleInterrupterClick(button);
        }

        @Override
        protected void hoverTimer(GBox text) {
            if (!isOpen || !isInSettlementView() || text == null || isDragging) {
                return;
            }
            if (toolbarSection.hoveredIs()) {
                toolbarSection.hoverInfoGet(text);
            }
        }

        @Override
        protected boolean render(Renderer r, float ds) {
            if (isInSettlementView()) {
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
                snake2d.LOG.err("TreeZones: Failed to show Interrupter: " + t.getMessage());
            }
        }
    }

    private final OverlayInterrupter interrupter = new OverlayInterrupter();

    public TreeZonesOverlayUI(TreeZoneConfig config, TreeZoneManager zoneManager) {
        this.config = config;
        this.zoneManager = zoneManager;

        SPRITE fallbackIcon = new SPRITE.Imp(16, 16) {
            @Override
            public void render(SPRITE_RENDERER r, int X1, int X2, int Y1, int Y2) {}
        };
        SPRITE cancelIcon = (SPRITES.icons() != null && SPRITES.icons().m != null) ? SPRITES.icons().m.cancel : fallbackIcon;
        SPRITE forestIcon = (SPRITES.icons() != null && SPRITES.icons().m != null) ? SPRITES.icons().m.wallceiling : fallbackIcon;
        SPRITE trashIcon = (SPRITES.icons() != null && SPRITES.icons().m != null) ? SPRITES.icons().m.trash : fallbackIcon;
        SPRITE clearFoodIcon = (SPRITES.icons() != null && SPRITES.icons().m != null) ? SPRITES.icons().m.place_brush : fallbackIcon;

        this.noTreePlacer = new NoTreePlacer(zoneManager, config, cancelIcon);
        this.forestPlacer = new ForestPlacer(zoneManager, config, forestIcon);
        this.resetPlacer = new ResetPlacer(zoneManager, trashIcon);
        this.areaClearPlacer = new AreaClearPlacer(config, zoneManager, clearFoodIcon);

        buildToolbar();
    }

    public boolean isInterrupterActive() {
        return interrupter.isActivated();
    }

    public void ensureInterrupterAttached(InterManager manager) {
        interrupter.attach(manager);
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public void toggleOpen() {
        this.isOpen = !this.isOpen;
    }

    public boolean isInSettlementView() {
        return VIEW.s() != null && VIEW.current() == VIEW.s();
    }

    public int getDefaultDockX() {
        return (int) (16 * C.SG);
    }

    public int getDefaultDockY() {
        return (int) (125 * C.SG);
    }

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

    private void buildToolbar() {
        if (SPRITES.icons() == null || SPRITES.icons().m == null || UI.FONT() == null) {
            return;
        }
        toolbarSection.clear();
        headerBar.clear();
        row1.clear();
        row2.clear();
        row3.clear();

        // 1. Header Bar with Title & Snap Button
        RENDEROBJ.Sprite dragIcon = new RENDEROBJ.Sprite(SPRITES.icons().m.openscroll);
        headerBar.add(dragIcon, 0, 0);
        GText titleText = new GText(UI.FONT().S, I18n.get("ui.title")).lablify();
        headerBar.addRightC(6, titleText);

        btnSnap = new GButt.ButtPanel(SPRITES.icons().m.crossair) {
            @Override
            protected void clickA() {
                isCustomPosition = false;
                boxX = getDefaultDockX();
                boxY = getDefaultDockY();
                relX = (double) boxX / Math.max(1, C.WIDTH());
                relY = (double) boxY / Math.max(1, C.HEIGHT());
                toolbarSection.body().moveX1Y1(boxX, boxY);
                setStatus("Links angedockt", false);
            }
        };
        btnSnap.hoverTitleSet("Dock to Left");
        headerBar.addRightC(12, btnSnap);

        // 2. Row 1: The Four Main Placement & Brush Tools
        // Button 1: Sperr-Zone
        btnNoTree = new GButt.ButtPanel(I18n.get("btn.notree")) {
            @Override
            protected void clickA() {
                if (VIEW.s() != null && VIEW.s().tools != null) {
                    VIEW.s().tools.place(noTreePlacer);
                    setStatus(I18n.get("status.notree_placed"), false);
                }
            }
        }.icon(SPRITES.icons().m.cancel);
        btnNoTree.hoverTitleSet(I18n.get("tool.notree.name"));
        btnNoTree.hoverInfoSet(I18n.get("tool.notree.desc"));

        // Button 2: Forst-Zone
        btnForest = new GButt.ButtPanel(I18n.get("btn.forest")) {
            @Override
            protected void clickA() {
                if (VIEW.s() != null && VIEW.s().tools != null) {
                    VIEW.s().tools.place(forestPlacer);
                    setStatus(I18n.get("status.forest_placed"), false);
                }
            }
        }.icon(SPRITES.icons().m.wallceiling);
        btnForest.hoverTitleSet(I18n.get("tool.forest.name"));
        btnForest.hoverInfoSet(I18n.get("tool.forest.desc"));

        // Button 3: Bereich roden (Gärtner-Pinsel)
        btnClearArea = new GButt.ButtPanel(I18n.get("btn.clear_area")) {
            @Override
            protected void clickA() {
                if (VIEW.s() != null && VIEW.s().tools != null) {
                    VIEW.s().tools.place(areaClearPlacer);
                    setStatus(I18n.get("tool.clear_area.name"), false);
                }
            }
        }.icon(SPRITES.icons().m.place_brush);
        btnClearArea.hoverTitleSet(I18n.get("tool.clear_area.name"));
        btnClearArea.hoverInfoSet(I18n.get("tool.clear_area.desc"));

        // Button 4: Zone loeschen
        btnReset = new GButt.ButtPanel(I18n.get("btn.reset")) {
            @Override
            protected void clickA() {
                if (VIEW.s() != null && VIEW.s().tools != null) {
                    VIEW.s().tools.place(resetPlacer);
                    setStatus(I18n.get("status.reset_placed"), false);
                }
            }
        }.icon(SPRITES.icons().m.trash);
        btnReset.hoverTitleSet(I18n.get("tool.reset.name"));
        btnReset.hoverInfoSet(I18n.get("tool.reset.desc"));

        row1.add(btnNoTree, 0, 0);
        row1.addRightC(4, btnForest);
        row1.addRightC(4, btnClearArea);
        row1.addRightC(4, btnReset);

        // 3. Row 2: Global Clear Actions (Map-Wide Instant Flora Clearing)
        btnClearTrees = new GButt.ButtPanel(I18n.get("btn.clear_trees")) {
            @Override
            protected void clickA() {
                int count = GardenerActions.clearAllTrees(config.isDropWood(), config.isAutoNoGrowth(), zoneManager);
                if (count > 0) {
                    setStatus(I18n.get("status.cleared_trees", count), false);
                } else {
                    setStatus(I18n.get("status.none_found"), false);
                }
            }
        }.icon(SPRITES.icons().m.clear_food);
        btnClearTrees.hoverTitleSet("Alle B\u00e4ume f\u00e4llen");
        btnClearTrees.hoverInfoSet("F\u00e4llt alle B\u00e4ume auf der gesamten Karte.");

        btnClearFlowers = new GButt.ButtPanel(I18n.get("btn.clear_flowers")) {
            @Override
            protected void clickA() {
                int count = GardenerActions.clearAllFlowers(config.isDropWood(), config.isAutoNoGrowth(), zoneManager);
                if (count > 0) {
                    setStatus(I18n.get("status.cleared_flowers", count), false);
                } else {
                    setStatus(I18n.get("status.none_found"), false);
                }
            }
        }.icon(SPRITES.icons().m.fertility);
        btnClearFlowers.hoverTitleSet("Alle Blumen entfernen");
        btnClearFlowers.hoverInfoSet("Entfernt alle wilden Blumen auf der gesamten Karte.");

        btnClearBushes = new GButt.ButtPanel(I18n.get("btn.clear_bushes")) {
            @Override
            protected void clickA() {
                int count = GardenerActions.clearAllBushes(config.isDropWood(), config.isAutoNoGrowth(), zoneManager);
                if (count > 0) {
                    setStatus(I18n.get("status.cleared_bushes", count), false);
                } else {
                    setStatus(I18n.get("status.none_found"), false);
                }
            }
        }.icon(SPRITES.icons().m.wildlife);
        btnClearBushes.hoverTitleSet("Alle B\u00fcsche entfernen");
        btnClearBushes.hoverInfoSet("Entfernt alle wilden B\u00fcsche auf der gesamten Karte.");

        btnClearMushrooms = new GButt.ButtPanel(I18n.get("btn.clear_mushrooms")) {
            @Override
            protected void clickA() {
                int count = GardenerActions.clearAllMushrooms(config.isDropWood(), config.isAutoNoGrowth(), zoneManager);
                if (count > 0) {
                    setStatus(I18n.get("status.cleared_mushrooms", count), false);
                } else {
                    setStatus(I18n.get("status.none_found"), false);
                }
            }
        }.icon(SPRITES.icons().m.agriculture);
        btnClearMushrooms.hoverTitleSet("Alle Pilze entfernen");
        btnClearMushrooms.hoverInfoSet("Entfernt alle wilden Pilze auf der gesamten Karte.");

        btnClearCrops = new GButt.ButtPanel(I18n.get("btn.clear_crops")) {
            @Override
            protected void clickA() {
                int count = GardenerActions.clearAllCrops(config.isDropWood(), config.isAutoNoGrowth(), zoneManager);
                if (count > 0) {
                    setStatus(I18n.get("status.cleared_crops", count), false);
                } else {
                    setStatus(I18n.get("status.none_found"), false);
                }
            }
        }.icon(SPRITES.icons().m.clear_food);
        btnClearCrops.hoverTitleSet("Alle Wildpflanzen entfernen");
        btnClearCrops.hoverInfoSet("Entfernt alle wilden Kr\u00e4uter und Feldfr\u00fcchte auf der gesamten Karte.");

        btnClearAll = new GButt.ButtPanel(I18n.get("btn.clear_all")) {
            @Override
            protected void clickA() {
                int count = GardenerActions.clearAllFlora(config.isDropWood(), config.isAutoNoGrowth(), zoneManager);
                if (count > 0) {
                    setStatus(I18n.get("status.cleared_all", count), false);
                } else {
                    setStatus(I18n.get("status.none_found"), false);
                }
            }
        }.icon(SPRITES.icons().m.trash);
        btnClearAll.hoverTitleSet("Alles roden");
        btnClearAll.hoverInfoSet("Entfernt ausnahmslos alle B\u00e4ume, Blumen, B\u00fcsche, Pilze und Kr\u00e4uter auf der gesamten Karte.");

        row2.add(btnClearTrees, 0, 0);
        row2.addRightC(4, btnClearFlowers);
        row2.addRightC(4, btnClearBushes);
        row2.addRightC(4, btnClearMushrooms);
        row2.addRightC(4, btnClearCrops);
        row2.addRightC(4, btnClearAll);

        // 4. Row 3: Toggles & Mod Preferences
        // Drop Wood Toggle
        btnDrop = new GButt.ButtPanel(I18n.get("btn.drop_wood", config.isDropWood() ? "AN" : "AUS")) {
            @Override
            protected void clickA() {
                config.toggleDropWood();
                selectedSet(config.isDropWood());
                replaceLabel(new GText(UI.FONT().S, I18n.get("btn.drop_wood", config.isDropWood() ? "AN" : "AUS")), null);
                setStatus("Drops: " + (config.isDropWood() ? "AN (Loot auf Boden)" : "AUS (Clean Void)"), false);
            }
        }.icon(SPRITES.icons().m.raw_materials);
        btnDrop.selectedSet(config.isDropWood());
        btnDrop.hoverTitleSet("Ressourcen-Drops");
        btnDrop.hoverInfoSet("AN: Holz/Nahrung f\u00e4llt auf den Boden. AUS: Saubere L\u00f6schung ohne Drops (Void Delete wie im Creative Mode).");

        // Auto-NoGrowth Toggle
        btnAutoNoGrowth = new GButt.ButtPanel(I18n.get("btn.auto_nogrowth", config.isAutoNoGrowth() ? "AN" : "AUS")) {
            @Override
            protected void clickA() {
                config.toggleAutoNoGrowth();
                selectedSet(config.isAutoNoGrowth());
                replaceLabel(new GText(UI.FONT().S, I18n.get("btn.auto_nogrowth", config.isAutoNoGrowth() ? "AN" : "AUS")), null);
                setStatus("Auto-Sperre: " + (config.isAutoNoGrowth() ? "AN (Verhindert Nachwuchs)" : "AUS"), false);
            }
        }.icon(SPRITES.icons().m.lock);
        btnAutoNoGrowth.selectedSet(config.isAutoNoGrowth());
        btnAutoNoGrowth.hoverTitleSet("Nachwuchs-Sperre (Auto-Lock)");
        btnAutoNoGrowth.hoverInfoSet("AN: Gerodete Kacheln werden sofort als Sperrzone verriegelt, sodass nie wieder etwas nachw\u00e4chst.");

        // Overlay Toggle
        btnOverlay = new GButt.ButtPanel(I18n.get("btn.overlay", config.isShowOverlay() ? "AN" : "AUS")) {
            @Override
            protected void clickA() {
                config.toggleOverlay();
                selectedSet(config.isShowOverlay());
                replaceLabel(new GText(UI.FONT().S, I18n.get("btn.overlay", config.isShowOverlay() ? "AN" : "AUS")), null);
                setStatus("Overlay: " + (config.isShowOverlay() ? "Aktiv" : "Aus"), false);
            }
        }.icon(SPRITES.icons().m.search);
        btnOverlay.selectedSet(config.isShowOverlay());
        btnOverlay.hoverTitleSet("Zonen-Overlay");
        btnOverlay.hoverInfoSet("Schaltet die farbige Anzeige der Zonen auf der Weltkarte ein oder aus.");

        // Instant Action Toggle
        btnInstant = new GButt.ButtPanel(I18n.get("btn.instant_clear", config.isInstantClear() ? "AN" : "AUS")) {
            @Override
            protected void clickA() {
                config.toggleInstantClear();
                config.setInstantPlant(config.isInstantClear());
                selectedSet(config.isInstantClear());
                replaceLabel(new GText(UI.FONT().S, I18n.get("btn.instant_clear", config.isInstantClear() ? "AN" : "AUS")), null);
                setStatus("Sofort-Aktion: " + (config.isInstantClear() ? "Aktiv" : "Aus"), false);
            }
        }.icon(SPRITES.icons().m.fast_forw);
        btnInstant.selectedSet(config.isInstantClear());
        btnInstant.hoverTitleSet("Sofort-Ausf\u00fchrung");
        btnInstant.hoverInfoSet("AN: Zonenwerkzeuge f\u00e4llen oder pflanzen Kacheln sofort beim Ziehen.");

        row3.add(btnDrop, 0, 0);
        row3.addRightC(4, btnAutoNoGrowth);
        row3.addRightC(4, btnOverlay);
        row3.addRightC(4, btnInstant);

        // Assemble toolbar card
        toolbarSection.add(headerBar, 0, 0);
        toolbarSection.addDown(4, row1);
        toolbarSection.addDown(4, row2);
        toolbarSection.addDown(4, row3);

        toolbarSection.pad(8, 6);

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

    public void setStatus(String msg, boolean isError) {
        this.statusMessage = msg;
        this.statusIsError = isError;
        this.statusTimer = 3.5;
    }

    public boolean holdsPoint(int x, int y) {
        if (!isOpen) return false;
        if (toolbarSection != null && toolbarSection.body().holdsPoint(x, y)) {
            return true;
        }
        if (statusTimer > 0.0 && !statusMessage.isEmpty() && toastBounds.holdsPoint(x, y)) {
            return true;
        }
        return false;
    }

    public boolean holdsPoint(COORDINATE m) {
        return m != null && holdsPoint(m.x(), m.y());
    }

    public void renderOverlay(Renderer r, float ds) {
        // 1. Render map overlay highlights for active zones
        if (config.isShowOverlay() && VIEW.s() != null && VIEW.s().getWindow() != null) {
            renderMapZones(r);
        }

        // 2. If toolbar is open, render toolbar card
        if (!isOpen || !isInSettlementView()) {
            isDragging = false;
            return;
        }

        if (btnNoTree == null) {
            buildToolbar();
        }

        COORDINATE m = CORE.getInput().getMouse().getCoo();

        // Handle active drag & drop
        if (m != null) {
            if (MButt.LEFT.isDown()) {
                if (isDragging) {
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

        if (m != null && !isDragging) {
            toolbarSection.hover(m);
        }

        toolbarSection.render(r, ds);
        renderStatusToast(r, boxX, boxY);
    }

    /**
     * Renders high-performance colored outlines and tints over visible tiles in the viewport.
     */
    private void renderMapZones(Renderer r) {
        GameWindow win = VIEW.s().getWindow();
        if (win == null) return;
        RECTANGLE tiles = win.tiles();
        if (tiles == null) return;

        int x1 = Math.max(0, tiles.x1());
        int x2 = Math.min(SETT.TWIDTH - 1, tiles.x2());
        int y1 = Math.max(0, tiles.y1());
        int y2 = Math.min(SETT.THEIGHT - 1, tiles.y2());

        for (int ty = y1; ty <= y2; ty++) {
            for (int tx = x1; tx <= x2; tx++) {
                boolean isF = zoneManager.isForest(tx, ty);
                boolean isN = zoneManager.isNoTree(tx, ty);

                if (!isF && !isN) continue;

                int px1 = win.pixels().screenX(tx * 16);
                int py1 = win.pixels().screenY(ty * 16);
                int px2 = win.pixels().screenX((tx + 1) * 16);
                int py2 = win.pixels().screenY((ty + 1) * 16);

                if (px2 < 0 || px1 > C.WIDTH() || py2 < 0 || py1 > C.HEIGHT()) {
                    continue;
                }

                if (isF) {
                    COLOR_FOREST.renderFrame(r, px1, px2, py1, py2, 0, 1);
                } else {
                    COLOR_NOTREE.renderFrame(r, px1, px2, py1, py2, 0, 1);
                }
            }
        }
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
        UI.PANEL().thin.render(r, toastBounds, 0);

        SPRITE statusIcon = statusIsError ? SPRITES.icons().m.cancel : SPRITES.icons().m.ok;
        statusIcon.renderCY(r, toastX + 6, toastY + toastH / 2);

        GCOLOR.T().H1.bind();
        UI.FONT().S.renderCY(r, toastX + 34, toastY + toastH / 2, statusMessage);
        COLOR.unbind();
    }

    public boolean handleInterrupterClick(MButt button) {
        if (button != MButt.LEFT || !isOpen) {
            return false;
        }

        COORDINATE m = CORE.getInput().getMouse().getCoo();
        if (m == null) {
            return false;
        }

        if (toolbarSection.body().holdsPoint(m.x(), m.y())) {
            if (isDragging) {
                CORE.getInput().clearAllInput();
                return true;
            }

            // Snap button
            if (btnSnap != null && btnSnap.body().holdsPoint(m.x(), m.y())) {
                btnSnap.hover(m);
                btnSnap.click();
                CORE.getInput().clearAllInput();
                return true;
            }

            // Row 1: Tools
            if (btnNoTree != null && btnNoTree.body().holdsPoint(m.x(), m.y())) {
                btnNoTree.hover(m);
                btnNoTree.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnForest != null && btnForest.body().holdsPoint(m.x(), m.y())) {
                btnForest.hover(m);
                btnForest.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnClearArea != null && btnClearArea.body().holdsPoint(m.x(), m.y())) {
                btnClearArea.hover(m);
                btnClearArea.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnReset != null && btnReset.body().holdsPoint(m.x(), m.y())) {
                btnReset.hover(m);
                btnReset.click();
                CORE.getInput().clearAllInput();
                return true;
            }

            // Row 2: Global Clear Actions
            if (btnClearTrees != null && btnClearTrees.body().holdsPoint(m.x(), m.y())) {
                btnClearTrees.hover(m);
                btnClearTrees.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnClearFlowers != null && btnClearFlowers.body().holdsPoint(m.x(), m.y())) {
                btnClearFlowers.hover(m);
                btnClearFlowers.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnClearBushes != null && btnClearBushes.body().holdsPoint(m.x(), m.y())) {
                btnClearBushes.hover(m);
                btnClearBushes.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnClearMushrooms != null && btnClearMushrooms.body().holdsPoint(m.x(), m.y())) {
                btnClearMushrooms.hover(m);
                btnClearMushrooms.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnClearCrops != null && btnClearCrops.body().holdsPoint(m.x(), m.y())) {
                btnClearCrops.hover(m);
                btnClearCrops.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnClearAll != null && btnClearAll.body().holdsPoint(m.x(), m.y())) {
                btnClearAll.hover(m);
                btnClearAll.click();
                CORE.getInput().clearAllInput();
                return true;
            }

            // Row 3: Toggles & Preferences
            if (btnDrop != null && btnDrop.body().holdsPoint(m.x(), m.y())) {
                btnDrop.hover(m);
                btnDrop.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnAutoNoGrowth != null && btnAutoNoGrowth.body().holdsPoint(m.x(), m.y())) {
                btnAutoNoGrowth.hover(m);
                btnAutoNoGrowth.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnOverlay != null && btnOverlay.body().holdsPoint(m.x(), m.y())) {
                btnOverlay.hover(m);
                btnOverlay.click();
                CORE.getInput().clearAllInput();
                return true;
            }
            if (btnInstant != null && btnInstant.body().holdsPoint(m.x(), m.y())) {
                btnInstant.hover(m);
                btnInstant.click();
                CORE.getInput().clearAllInput();
                return true;
            }

            // Drag zone on header bar
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

            CORE.getInput().clearAllInput();
            return true;
        }

        if (statusTimer > 0.0 && !statusMessage.isEmpty() && toastBounds.holdsPoint(m.x(), m.y())) {
            CORE.getInput().clearAllInput();
            return true;
        }

        return false;
    }

    private static final class ToolbarSection extends GuiSection {
        @Override
        public void render(SPRITE_RENDERER r, float ds) {
            UI.PANEL().butt.render(r, body(), 0);
            super.render(r, ds);
        }
    }
}
