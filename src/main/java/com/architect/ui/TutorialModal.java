package com.architect.ui;

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
 */
public final class TutorialModal {

    private TutorialModal() {}

    public static void open() {
        int width = 640;
        int height = 520;

        GuiSection section = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setTitle(I18n.get("btn.tutorial"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                VIEW.inters().section.close();
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
            "cheat.spoilage",
            "cheat.loyalty",
            "cheat.instant_build",
            "cheat.infinite_crates",
            "cheat.raid_protection"
        };

        for (String key : keys) {
            GText head = new GText(UI.FONT().M, "• " + I18n.get(key));
            head.lablify();
            content.addDown(12, head);

            GText desc = new GText(UI.FONT().S, "  " + I18n.get(key + ".desc"));
            content.addDown(4, desc);
        }

        // Close button
        GButt.ButtPanel closeBtn = new GButt.ButtPanel(I18n.get("btn.close"));
        closeBtn.clickActionSet(closeAction);
        content.addDown(20, closeBtn);

        section.add(panel);
        section.add(content);

        // Center on screen
        if (C.DIM() != null) {
            section.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(section);
    }
}
