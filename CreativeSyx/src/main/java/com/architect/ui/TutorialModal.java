package com.architect.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.architect.util.I18n;

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

/**
 * In-game bilingual tutorial and feature guide modal.
 * Uses canonical Single-GuiSection-Pattern with GPanel, title-bar dragging, and multiline text wrapping.
 */
public final class TutorialModal {

    private static int modalX = -1;
    private static int modalY = -1;
    private static GuiSection activeModal = null;

    private TutorialModal() {}

    public static GuiSection getActiveModal() {
        return activeModal;
    }

    public static boolean isOpen() {
        return activeModal != null && VIEW.inters() != null && VIEW.inters().section != null && VIEW.inters().section.isActivated() && VIEW.inters().section.current() == activeModal;
    }

    /**
     * Loads tutorial text from packaged JAR resources with multiple path fallbacks.
     */
    public static String getTutorialText(I18n.Language lang) {
        String filename = "tutorial_" + lang.code + ".txt";
        String[] candidatePaths = {
            "/" + filename,
            "/assets/text/" + filename,
            filename,
            "assets/text/" + filename
        };
        for (String path : candidatePaths) {
            try {
                InputStream in = TutorialModal.class.getResourceAsStream(path);
                if (in == null && TutorialModal.class.getClassLoader() != null) {
                    in = TutorialModal.class.getClassLoader().getResourceAsStream(path);
                }
                if (in != null) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            sb.append(I18n.sanitizeAscii(line)).append("\n");
                        }
                        return sb.toString();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static void open() {
        if (VIEW.inters() == null || VIEW.inters().section == null) {
            return;
        }

        int contentW = 770;
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
                            // Title bar drag region: top 40px excluding top-right [X] close button
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
                                modalX = targetX;
                                modalY = targetY;
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

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                activeModal = null;
                ArchitectUI.close();
            }
        };

        // Section Title
        GHeader header = new GHeader(I18n.get("mod.name") + " - " + I18n.get("mod.desc")).subify();
        modal.add(header, 0, 0);
        modal.addDown(6, GFrame.separator(contentW));

        // 2-Column Guide Content (377px each)
        int colW = (contentW - 16) / 2;
        GuiSection grid = new GuiSection();

        // Left Column
        GuiSection leftCol = new GuiSection();
        String[] leftKeys = {
            "cheat.construction",
            "cheat.maintenance",
            "cheat.knowledge",
            "cheat.spoilage"
        };
        for (int i = 0; i < leftKeys.length; i++) {
            String key = leftKeys[i];
            GText head = new GText(UI.FONT().M, I18n.get(key));
            head.lablify();
            leftCol.addDown(i == 0 ? 0 : 8, head);

            GText desc = new GText(UI.FONT().S, I18n.get(key + ".desc")).normalify();
            desc.setMultipleLines(true);
            desc.setMaxWidth(colW - 8);
            desc.adjustWidth();
            leftCol.addDown(2, desc);
        }
        grid.add(leftCol);

        // Right Column
        GuiSection rightCol = new GuiSection();
        String[] rightKeys = {
            "cheat.logistics",
            "cheat.infinite_denari",
            "cheat.loyalty",
            "cheat.raid_protection"
        };
        for (int i = 0; i < rightKeys.length; i++) {
            String key = rightKeys[i];
            GText head = new GText(UI.FONT().M, I18n.get(key));
            head.lablify();
            rightCol.addDown(i == 0 ? 0 : 8, head);

            GText desc = new GText(UI.FONT().S, I18n.get(key + ".desc")).normalify();
            desc.setMultipleLines(true);
            desc.setMaxWidth(colW - 8);
            desc.adjustWidth();
            rightCol.addDown(2, desc);
        }
        grid.addRight(16, rightCol);

        modal.addDown(8, grid);
        modal.addDown(12, GFrame.separator(contentW));

        // Close button at bottom
        GText btnCloseText = new GText(UI.FONT().M, I18n.get("btn.close"));
        GButt.ButtPanel btnClose = new GButt.ButtPanel((snake2d.util.sprite.SPRITE) btnCloseText);
        btnClose.setDim(contentW, 30);
        btnClose.align(DIR.C);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet(I18n.getLanguage() == I18n.Language.DE
            ? "Schliesst diese Modul-Uebersicht."
            : "Closes this module manual.");
        btnClose.clickActionSet(closeAction);
        modal.addDown(6, btnClose);

        modal.pad(20, 16);

        // Canonical Single-GuiSection-Pattern with GPanel
        GPanel panel = new GPanel(modal.body()).setBig();
        panel.setTitle(I18n.get("ui.title") + " - " + I18n.get("btn.tutorial"));
        panel.setCloseAction(closeAction);
        panelRef[0] = panel;

        modal.add(panel);
        modal.moveLastToBack();

        int totalW = modal.body().width();
        int totalH = modal.body().height();

        if (modalX < 0 || modalY < 0) {
            modalX = (C.DIM() != null) ? (C.WIDTH() - totalW) / 2 : 100;
            modalY = (C.DIM() != null) ? (C.HEIGHT() - totalH) / 2 : 100;
        }

        if (C.DIM() != null) {
            modalX = CLAMP.i(modalX, 10, Math.max(10, C.WIDTH() - totalW - 10));
            modalY = CLAMP.i(modalY, 10, Math.max(10, C.HEIGHT() - totalH - 10));
        }

        modal.body().moveX1Y1(modalX, modalY);
        // Compensate vanilla GPanel$1.moveY1 bug (missing +8 closeAction offset)
        panel.inner().incrY(8);
        activeModal = modal;
        VIEW.inters().section.activate(modal);
    }
}
