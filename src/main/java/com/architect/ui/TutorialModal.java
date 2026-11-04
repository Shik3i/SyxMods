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
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * In-game bilingual tutorial and feature guide modal.
 * Spacious 840px wide layout with clean typography.
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
        int contentW = width - 40;

        GuiSection root = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setTitle("Syx Architect - " + I18n.get("btn.tutorial"));

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

        // Header
        GText title = new GText(UI.FONT().H1, I18n.get("ui.title"));
        content.addDown(0, title);

        GText sub = new GText(UI.FONT().M, "Creative, Schoenbau & Sandbox Suite");
        sub.lablify();
        content.addDown(4, sub);

        // Body items
        String[] keys = new String[]{
            "cheat.knowledge",
            "cheat.maintenance",
            "cheat.construction",
            "cheat.logistics",
            "cheat.infinite_denari",
            "cheat.spoilage",
            "cheat.loyalty",
            "cheat.raid_protection",
            "cheat.battle_autowin"
        };

        for (String key : keys) {
            GText head = new GText(UI.FONT().M, "* " + I18n.get(key));
            head.lablify();
            content.addDown(6, head);

            GText desc = new GText(UI.FONT().S, "  " + I18n.get(key + ".desc"));
            desc.setMaxWidth(contentW - 20);
            content.addDown(2, desc);
        }

        // Close button at bottom
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.setDim(contentW, 32);
        btnClose.align(DIR.C);
        btnClose.clickActionSet(closeAction);
        content.addDown(12, btnClose);

        root.add(panel);
        content.body().moveX1Y1(panel.inner().x1() + 16, panel.inner().y1() + 8);
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(root);
    }
}
