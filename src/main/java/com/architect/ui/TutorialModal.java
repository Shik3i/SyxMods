package com.architect.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.architect.util.I18n;

import init.constant.C;
import init.sprite.UI.UI;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.gui.misc.GButt;
import util.gui.misc.GHeader;
import util.gui.misc.GText;
import util.gui.panel.GFrame;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * In-game bilingual tutorial and feature guide modal.
 * Spacious 840px wide native Songs of Syx layout with clean 2-column typography.
 */
public final class TutorialModal {

    private TutorialModal() {}

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

        int width = 840;
        int height = 580;
        int contentW = 800;

        GuiSection root = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setBig();
        panel.setTitle(I18n.get("ui.title") + " - " + I18n.get("btn.tutorial"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                if (VIEW.inters() != null && VIEW.inters().section != null) {
                    VIEW.inters().section.close();
                }
            }
        };
        panel.setCloseAction(closeAction);

        GuiSection content = new GuiSection();

        // Section Title
        GHeader header = new GHeader(I18n.get("mod.name") + " - " + I18n.get("mod.desc")).subify();
        content.addDown(0, header);
        content.addDown(6, GFrame.separator(contentW));

        // 2-Column Guide Content (392px each)
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

            GText desc = new GText(UI.FONT().S, I18n.get(key + ".desc"));
            desc.normalify();
            desc.setMaxWidth(colW - 8);
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

            GText desc = new GText(UI.FONT().S, I18n.get(key + ".desc"));
            desc.normalify();
            desc.setMaxWidth(colW - 8);
            rightCol.addDown(2, desc);
        }
        grid.addRight(16, rightCol);

        content.addDown(8, grid);
        content.addDown(12, GFrame.separator(contentW));

        // Close button at bottom
        GText btnCloseText = new GText(UI.FONT().M, I18n.get("btn.close"));
        GButt.ButtPanel btnClose = new GButt.ButtPanel((snake2d.util.sprite.SPRITE) btnCloseText);
        btnClose.setDim(contentW, 30);
        btnClose.align(DIR.C);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet("Schliesst diese Modul-Uebersicht.");
        btnClose.clickActionSet(closeAction);
        content.addDown(6, btnClose);

        root.add(panel);
        content.body().moveX1Y1(panel.inner().x1() + 20, panel.inner().y1() + 28);
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(root);
    }
}
