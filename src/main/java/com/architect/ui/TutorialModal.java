package com.architect.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.architect.util.I18n;

import init.constant.C;
import init.sprite.UI.UI;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * In-game bilingual tutorial and feature guide modal.
 * Can load tutorial texts directly from packaged JAR ClassLoader resources.
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
                            sb.append(line).append("\n");
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
        int width = 640;
        int height = 540;

        GuiSection section = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setTitle(I18n.get("btn.tutorial"));

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
        content.pad(15);

        // Header
        GText title = new GText(UI.FONT().H1, I18n.get("ui.title"));
        content.addDown(0, title);

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
            GText head = new GText(UI.FONT().M, "• " + I18n.get(key));
            head.lablify();
            content.addDown(10, head);

            GText desc = new GText(UI.FONT().S, "  " + I18n.get(key + ".desc"));
            content.addDown(3, desc);
        }

        // Close button
        GButt.ButtPanel closeBtn = new GButt.ButtPanel(I18n.get("btn.close"));
        closeBtn.clickActionSet(closeAction);
        content.addDown(16, closeBtn);

        section.add(panel);
        section.add(content);

        // Center on screen
        if (C.DIM() != null) {
            section.body().centerIn(C.DIM());
        }

        if (VIEW.inters() != null && VIEW.inters().section != null) {
            VIEW.inters().section.activate(section);
        }
    }
}
