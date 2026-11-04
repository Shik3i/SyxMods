package com.architect.ui;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.ConstructionTier;
import com.architect.config.CheatTiers.KnowledgeTier;
import com.architect.config.CheatTiers.LogisticsTier;
import com.architect.config.CheatTiers.MaintenanceTier;
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
 * Provides live controls for sandbox modules, semantic Schönbau presets, 1-click actions,
 * and seamless in-game bilingual localization.
 */
public final class ArchitectUI {

    private static boolean isOpen = false;

    private ArchitectUI() {}

    public interface UICallback {
        void onStateUpdated();
        void onInstantBuildRequested();
        void onFillCratesRequested();
        void onWinCombatRequested();
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void close() {
        if (isOpen) {
            isOpen = false;
            VIEW.inters().section.close();
        }
    }

    public static void toggle(ArchitectState state, UICallback callback) {
        if (isOpen) {
            close();
        } else {
            open(state, callback);
        }
    }

    public static void open(ArchitectState state, UICallback callback) {
        isOpen = true;
        int width = 620;
        int height = 660;

        GuiSection root = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setTitle(I18n.get("ui.title"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                isOpen = false;
                VIEW.inters().section.close();
            }
        };
        panel.setCloseAction(closeAction);

        GuiSection content = new GuiSection();
        content.pad(14);

        // Top Header Line: Subtitle + Language Toggle [DE | EN] + [?] Tutorial Button
        GuiSection topHeader = new GuiSection();
        GText sub = new GText(UI.FONT().S, I18n.get("mod.desc"));
        topHeader.add(sub, 0, 4);

        GButt.ButtPanel btnLang = new GButt.ButtPanel("[" + I18n.getLanguage().code.toUpperCase() + "]");
        btnLang.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                I18n.toggleLanguage();
                state.language = I18n.getLanguage().code;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        topHeader.addRight(160, btnLang);

        GButt.ButtPanel btnTut = new GButt.ButtPanel("[?]");
        btnTut.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                TutorialModal.open();
            }
        });
        topHeader.addRight(8, btnTut);

        content.addDown(0, topHeader);

        // --- 1. Quick Presets Section ---
        GText presetTitle = new GText(UI.FONT().M, I18n.get("ui.presets"));
        presetTitle.lablify();
        content.addDown(12, presetTitle);

        GuiSection presetRow = new GuiSection();

        // Preset 1: God Mode
        GButt.ButtPanel btnGod = new GButt.ButtPanel("🌟 " + I18n.get("preset.god"));
        btnGod.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.constructionTier = ConstructionTier.INSTANT_BUILD;
                state.maintenanceTier = MaintenanceTier.NO_DECAY;
                state.knowledgeTier = KnowledgeTier.OMNISCIENCE;
                state.logisticsTier = LogisticsTier.ALL_ASSIGNED_CRATES;
                state.spoilageDisabled = true;
                state.loyaltyBoost = true;
                state.raidImmunity = true;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.add(btnGod);

        // Preset 2: Living City
        GButt.ButtPanel btnLiving = new GButt.ButtPanel("🏗️ " + I18n.get("preset.living"));
        btnLiving.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.constructionTier = ConstructionTier.FREE_MATERIALS_KEEP_WORK;
                state.maintenanceTier = MaintenanceTier.JANITOR_WORK_FREE_MATS;
                state.knowledgeTier = KnowledgeTier.NO_FORGETTING;
                state.logisticsTier = LogisticsTier.RAW_MATERIALS_ONLY;
                state.spoilageDisabled = false;
                state.loyaltyBoost = true;
                state.raidImmunity = true;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.addDown(6, btnLiving);

        // Preset 3: Vanilla Reset
        GButt.ButtPanel btnReset = new GButt.ButtPanel("🔄 " + I18n.get("preset.vanilla"));
        btnReset.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.resetToDefaults();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.addDown(6, btnReset);

        content.addDown(6, presetRow);

        // --- 2. Semantic Modules & Cheat Toggles ---
        // Construction Tier
        String constrLabel = I18n.get("cheat.construction") + ": " + state.constructionTier.getLabel();
        GButt.ButtPanel btnConstr = new GButt.ButtPanel(constrLabel);
        btnConstr.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.constructionTier = state.constructionTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(12, btnConstr);

        // Maintenance Tier
        String maintLabel = I18n.get("cheat.maintenance") + ": " + state.maintenanceTier.getLabel();
        GButt.ButtPanel btnMaint = new GButt.ButtPanel(maintLabel);
        btnMaint.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.maintenanceTier = state.maintenanceTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(6, btnMaint);

        // Knowledge Tier
        String knowLabel = I18n.get("cheat.knowledge") + ": " + state.knowledgeTier.getLabel();
        GButt.ButtPanel btnKnow = new GButt.ButtPanel(knowLabel);
        btnKnow.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.knowledgeTier = state.knowledgeTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(6, btnKnow);

        // Logistics Tier
        String logLabel = I18n.get("cheat.logistics") + ": " + state.logisticsTier.getLabel();
        GButt.ButtPanel btnLog = new GButt.ButtPanel(logLabel);
        btnLog.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.logisticsTier = state.logisticsTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(6, btnLog);

        // Loyalty & Happiness Toggle
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
        content.addDown(6, btnLoyalty);

        // Spoilage Toggle
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
        content.addDown(6, btnSpoil);

        // Raid Immunity Toggle
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
        content.addDown(6, btnRaid);

        // --- 3. Instant Action Buttons ---
        GuiSection actionSection = new GuiSection();

        // 1-Click Instant Build All
        GButt.ButtPanel btnInstantBuild = new GButt.ButtPanel(I18n.get("btn.instant_build_now"));
        btnInstantBuild.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onInstantBuildRequested();
            }
        });
        actionSection.add(btnInstantBuild);

        // 1-Click Fill All Crates to 100%
        GButt.ButtPanel btnFillCrates = new GButt.ButtPanel(I18n.get("btn.fill_crates_100"));
        btnFillCrates.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onFillCratesRequested();
            }
        });
        actionSection.addRight(8, btnFillCrates);

        content.addDown(10, actionSection);

        // 1-Click Combat Auto-Win Button
        GButt.ButtPanel btnWinCombat = new GButt.ButtPanel(I18n.get("btn.win_combat_now"));
        btnWinCombat.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onWinCombatRequested();
            }
        });
        content.addDown(6, btnWinCombat);

        // Footer: Close Button
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.clickActionSet(closeAction);
        content.addDown(12, btnClose);

        root.add(panel);
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(root);
    }
}
