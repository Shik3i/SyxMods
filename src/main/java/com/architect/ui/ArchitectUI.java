package com.architect.ui;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.MaintenanceTier;
import com.architect.config.CheatTiers.MultiplierTier;
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
 * Main GUI control center for Syx Architect.
 * Provides live controls for sandbox modules, cheats, and language toggles.
 */
public final class ArchitectUI {

    private ArchitectUI() {}

    public interface UICallback {
        void onStateUpdated();
        void onInstantBuildRequested();
    }

    public static void open(ArchitectState state, UICallback callback) {
        int width = 560;
        int height = 580;

        GuiSection section = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setTitle(I18n.get("ui.title"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                VIEW.inters().section.close();
            }
        };
        panel.setCloseAction(closeAction);

        GuiSection content = new GuiSection();
        content.pad(15);

        // Subtitle
        GText sub = new GText(UI.FONT().S, I18n.get("mod.desc"));
        content.addDown(0, sub);

        // 1. Knowledge Tier
        String knowledgeLabel = I18n.get("cheat.knowledge") + ": " + state.knowledgeTier.getLabel();
        GButt.ButtPanel btnKnowledge = new GButt.ButtPanel(knowledgeLabel);
        btnKnowledge.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.knowledgeTier = state.knowledgeTier.next();
                callback.onStateUpdated();
                open(state, callback); // Re-render updated labels
            }
        });
        content.addDown(12, btnKnowledge);

        // 2. Building Wear (Maintenance)
        String maintLabel = I18n.get("cheat.maintenance") + ": " + (state.maintenanceTier == MaintenanceTier.ZERO_DEGRADATION ? I18n.get("status.active") : state.maintenanceTier.getLabel());
        GButt.ButtPanel btnMaint = new GButt.ButtPanel(maintLabel);
        btnMaint.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.maintenanceTier = state.maintenanceTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(8, btnMaint);

        // 3. Spoilage Toggle
        String spoilLabel = I18n.get("cheat.spoilage") + ": " + (state.spoilageDisabled ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnSpoil = new GButt.ButtPanel(spoilLabel);
        btnSpoil.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.spoilageDisabled = !state.spoilageDisabled;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(8, btnSpoil);

        // 4. Loyalty & Peace Toggle
        String loyaltyLabel = I18n.get("cheat.loyalty") + ": " + (state.loyaltyBoost ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnLoyalty = new GButt.ButtPanel(loyaltyLabel);
        btnLoyalty.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.loyaltyBoost = !state.loyaltyBoost;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(8, btnLoyalty);

        // 5. Instant Construction Toggle
        String buildToggleLabel = I18n.get("cheat.instant_build") + " (Auto): " + (state.instantBuildActive ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnBuildToggle = new GButt.ButtPanel(buildToggleLabel);
        btnBuildToggle.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.instantBuildActive = !state.instantBuildActive;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(8, btnBuildToggle);

        // 6. One-shot Instant Build Trigger
        GButt.ButtPanel btnBuildNow = new GButt.ButtPanel("⚡ " + I18n.get("cheat.instant_build") + " (One-Shot)");
        btnBuildNow.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onInstantBuildRequested();
            }
        });
        content.addDown(8, btnBuildNow);

        // 7. Raid Immunity Toggle
        String raidLabel = I18n.get("cheat.raid_protection") + ": " + (state.raidImmunity ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnRaid = new GButt.ButtPanel(raidLabel);
        btnRaid.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.raidImmunity = !state.raidImmunity;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(8, btnRaid);

        // Separator / Footer Controls
        GuiSection footer = new GuiSection();

        // Language toggle
        GButt.ButtPanel btnLang = new GButt.ButtPanel(I18n.get("btn.language"));
        btnLang.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                I18n.toggleLanguage();
                state.language = I18n.getLanguage().code;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        footer.add(btnLang);

        // Tutorial button
        GButt.ButtPanel btnTutorial = new GButt.ButtPanel(I18n.get("btn.tutorial"));
        btnTutorial.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                TutorialModal.open();
            }
        });
        footer.addRight(10, btnTutorial);

        // Close button
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.clickActionSet(closeAction);
        footer.addRight(10, btnClose);

        content.addDown(18, footer);

        section.add(panel);
        section.add(content);

        // Center on screen
        if (C.DIM() != null) {
            section.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(section);
    }
}
