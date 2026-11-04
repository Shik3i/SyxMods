package com.architect.ui;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.ConstructionTier;
import com.architect.config.CheatTiers.KnowledgeTier;
import com.architect.config.CheatTiers.LogisticsTier;
import com.architect.config.CheatTiers.MaintenanceTier;
import com.architect.util.I18n;

import init.constant.C;
import init.race.RACES;
import init.race.Race;
import init.sprite.UI.UI;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import snake2d.util.sets.LIST;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * Main GUI control center for Syx Architect.
 * Elegant, strictly-structured 2-column grid layout with fixed button widths,
 * zero text clipping, zero emoji/encoding errors, and complete memory-safe lifecycle management.
 */
public final class ArchitectUI {

    private static boolean isOpen = false;
    private static int activeTab = 0; // 0 = City & Economy, 1 = World & Population
    private static int selectedRaceIndex = 0;

    private ArchitectUI() {}

    public interface UICallback {
        void onStateUpdated();
        void onInstantBuildRequested();
        void onFillCratesRequested();
        void onAddDenariRequested();
        void onWinCombatRequested();
        void onSpawnImmigrantsRequested(Race race, int count);
        void onSpawnAllPlayableRequested(int countPerRace);
        void onMaxFertilityRequested();
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void close() {
        isOpen = false;
        if (VIEW.inters() != null && VIEW.inters().section != null) {
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
        if (VIEW.inters() == null || VIEW.inters().section == null) {
            return;
        }

        isOpen = true;
        int screenH = (C.DIM() != null) ? C.DIM().height() : 720;
        int width = 720;
        int height = Math.min(680, Math.max(520, screenH - 40));
        int contentW = width - 40;

        GuiSection root = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setTitle(I18n.get("ui.title"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                close();
            }
        };
        panel.setCloseAction(closeAction);

        GuiSection content = new GuiSection();

        // --- Top Bar: Tab Switcher + Language Toggle + Tutorial [?] ---
        GuiSection topBar = new GuiSection();

        // Tab 0: City & Economy
        GButt.ButtPanel tabCity = new GButt.ButtPanel((activeTab == 0 ? ">> " : "") + I18n.get("ui.tab.city"));
        tabCity.setDim(180, 28);
        tabCity.align(DIR.C);
        tabCity.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 0;
                open(state, callback);
            }
        });
        topBar.add(tabCity);

        // Tab 1: World & Population
        GButt.ButtPanel tabWorld = new GButt.ButtPanel((activeTab == 1 ? ">> " : "") + I18n.get("ui.tab.world"));
        tabWorld.setDim(180, 28);
        tabWorld.align(DIR.C);
        tabWorld.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 1;
                open(state, callback);
            }
        });
        topBar.addRight(8, tabWorld);

        // Language toggle [EN] or [DE]
        GButt.ButtPanel btnLang = new GButt.ButtPanel("[" + I18n.getLanguage().code.toUpperCase() + "] " + I18n.get("btn.language"));
        btnLang.setDim(160, 28);
        btnLang.align(DIR.C);
        btnLang.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                I18n.toggleLanguage();
                state.language = I18n.getLanguage().code;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        topBar.addRight(12, btnLang);

        // Tutorial modal [? Guide]
        GButt.ButtPanel btnTut = new GButt.ButtPanel("[? Guide]");
        btnTut.setDim(100, 28);
        btnTut.align(DIR.C);
        btnTut.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                TutorialModal.open();
            }
        });
        topBar.addRight(8, btnTut);

        content.addDown(0, topBar);

        if (activeTab == 0) {
            buildCityTab(content, contentW, state, callback, closeAction);
        } else {
            buildWorldTab(content, contentW, state, callback, closeAction);
        }

        root.add(panel);
        // Position content cleanly inside the inner boundary of the GPanel
        content.body().moveX1Y1(panel.inner().x1() + 16, panel.inner().y1() + 8);
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(root);
    }

    private static void buildCityTab(GuiSection content, int contentW, ArchitectState state, UICallback callback, ACTION closeAction) {
        // --- 1. Quick Presets Section (3 Columns) ---
        GText presetTitle = new GText(UI.FONT().M, I18n.get("ui.presets"));
        presetTitle.lablify();
        content.addDown(10, presetTitle);

        GuiSection presetRow = new GuiSection();
        int btnW3 = (contentW - 16) / 3;

        // Preset 1: God Mode
        GButt.ButtPanel btnGod = new GButt.ButtPanel("[*] " + I18n.get("preset.god"));
        btnGod.setDim(btnW3, 30);
        btnGod.align(DIR.C);
        btnGod.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.constructionTier = ConstructionTier.INSTANT_BUILD;
                state.maintenanceTier = MaintenanceTier.NO_DECAY;
                state.knowledgeTier = KnowledgeTier.OMNISCIENCE;
                state.logisticsTier = LogisticsTier.ALL_ASSIGNED_CRATES;
                state.infiniteDenari = true;
                state.spoilageDisabled = true;
                state.loyaltyBoost = true;
                state.raidImmunity = true;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.add(btnGod);

        // Preset 2: Living City
        GButt.ButtPanel btnLiving = new GButt.ButtPanel("[+] " + I18n.get("preset.living"));
        btnLiving.setDim(btnW3, 30);
        btnLiving.align(DIR.C);
        btnLiving.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.constructionTier = ConstructionTier.FREE_MATERIALS_KEEP_WORK;
                state.maintenanceTier = MaintenanceTier.JANITOR_WORK_FREE_MATS;
                state.knowledgeTier = KnowledgeTier.NO_FORGETTING;
                state.logisticsTier = LogisticsTier.RAW_MATERIALS_ONLY;
                state.infiniteDenari = true;
                state.spoilageDisabled = false;
                state.loyaltyBoost = true;
                state.raidImmunity = true;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.addRight(8, btnLiving);

        // Preset 3: Vanilla Reset
        GButt.ButtPanel btnReset = new GButt.ButtPanel("[R] " + I18n.get("preset.vanilla"));
        btnReset.setDim(btnW3, 30);
        btnReset.align(DIR.C);
        btnReset.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.resetToDefaults();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.addRight(8, btnReset);

        content.addDown(4, presetRow);

        // --- 2. Semantic Modules & Cheat Toggles (Clean 2-Column Grid) ---
        int btnW2 = (contentW - 12) / 2;

        GuiSection gridRow = new GuiSection();

        // Left Column: Construction, Maintenance, Knowledge, Spoilage
        GuiSection leftCol = new GuiSection();

        GText lblLeft = new GText(UI.FONT().M, "--- " + I18n.get("ui.tab.construction") + " & " + I18n.get("ui.tab.boosts") + " ---");
        lblLeft.lablify();
        leftCol.addDown(0, lblLeft);

        // Construction Tier Button
        String constrLabel = I18n.get("cheat.construction") + ": " + state.constructionTier.getLabel();
        GButt.ButtPanel btnConstr = new GButt.ButtPanel(constrLabel);
        btnConstr.setDim(btnW2, 30);
        btnConstr.align(DIR.W);
        btnConstr.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.constructionTier = state.constructionTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        leftCol.addDown(4, btnConstr);

        // Maintenance Tier Button
        String maintLabel = I18n.get("cheat.maintenance") + ": " + state.maintenanceTier.getLabel();
        GButt.ButtPanel btnMaint = new GButt.ButtPanel(maintLabel);
        btnMaint.setDim(btnW2, 30);
        btnMaint.align(DIR.W);
        btnMaint.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.maintenanceTier = state.maintenanceTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        leftCol.addDown(4, btnMaint);

        // Knowledge Tier Button
        String knowLabel = I18n.get("cheat.knowledge") + ": " + state.knowledgeTier.getLabel();
        GButt.ButtPanel btnKnow = new GButt.ButtPanel(knowLabel);
        btnKnow.setDim(btnW2, 30);
        btnKnow.align(DIR.W);
        btnKnow.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.knowledgeTier = state.knowledgeTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        leftCol.addDown(4, btnKnow);

        // Spoilage Toggle Button
        String spoilLabel = I18n.get("cheat.spoilage") + ": " + (state.spoilageDisabled ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnSpoil = new GButt.ButtPanel(spoilLabel);
        btnSpoil.setDim(btnW2, 30);
        btnSpoil.align(DIR.W);
        btnSpoil.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.spoilageDisabled = !state.spoilageDisabled;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        leftCol.addDown(4, btnSpoil);

        gridRow.add(leftCol);

        // Right Column: Logistics, Treasury, Loyalty, Raid Immunity
        GuiSection rightCol = new GuiSection();

        GText lblRight = new GText(UI.FONT().M, "--- " + I18n.get("ui.tab.logistics") + " & " + I18n.get("ui.tab.security") + " ---");
        lblRight.lablify();
        rightCol.addDown(0, lblRight);

        // Logistics Tier Button
        String logLabel = I18n.get("cheat.logistics") + ": " + state.logisticsTier.getLabel();
        GButt.ButtPanel btnLog = new GButt.ButtPanel(logLabel);
        btnLog.setDim(btnW2, 30);
        btnLog.align(DIR.W);
        btnLog.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.logisticsTier = state.logisticsTier.next();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        rightCol.addDown(4, btnLog);

        // Treasury / Infinite Denari Button
        String denariLabel = I18n.get("cheat.infinite_denari") + ": " + (state.infiniteDenari ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnDenari = new GButt.ButtPanel(denariLabel);
        btnDenari.setDim(btnW2, 30);
        btnDenari.align(DIR.W);
        btnDenari.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.infiniteDenari = !state.infiniteDenari;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        rightCol.addDown(4, btnDenari);

        // Loyalty & Happiness Button
        String loyaltyLabel = I18n.get("cheat.loyalty") + ": " + (state.loyaltyBoost ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnLoyalty = new GButt.ButtPanel(loyaltyLabel);
        btnLoyalty.setDim(btnW2, 30);
        btnLoyalty.align(DIR.W);
        btnLoyalty.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.loyaltyBoost = !state.loyaltyBoost;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        rightCol.addDown(4, btnLoyalty);

        // Raid Immunity Button
        String raidLabel = I18n.get("cheat.raid_protection") + ": " + (state.raidImmunity ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnRaid = new GButt.ButtPanel(raidLabel);
        btnRaid.setDim(btnW2, 30);
        btnRaid.align(DIR.W);
        btnRaid.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.raidImmunity = !state.raidImmunity;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        rightCol.addDown(4, btnRaid);

        gridRow.addRight(12, rightCol);
        content.addDown(10, gridRow);

        // --- 3. Instant 1-Click Action Buttons (4 Columns) ---
        GText lblActions = new GText(UI.FONT().M, "--- Instant 1-Click Actions ---");
        lblActions.lablify();
        content.addDown(10, lblActions);

        GuiSection actionSection = new GuiSection();
        int btnW4 = (contentW - 18) / 4;

        // 1-Click Instant Build
        GButt.ButtPanel btnInstantBuild = new GButt.ButtPanel(I18n.get("btn.instant_build_now"));
        btnInstantBuild.setDim(btnW4, 30);
        btnInstantBuild.align(DIR.C);
        btnInstantBuild.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onInstantBuildRequested();
            }
        });
        actionSection.add(btnInstantBuild);

        // 1-Click Fill All Crates to 100%
        GButt.ButtPanel btnFillCrates = new GButt.ButtPanel(I18n.get("btn.fill_crates_100"));
        btnFillCrates.setDim(btnW4, 30);
        btnFillCrates.align(DIR.C);
        btnFillCrates.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onFillCratesRequested();
            }
        });
        actionSection.addRight(6, btnFillCrates);

        // 1-Click Add 1,000,000 Denari
        GButt.ButtPanel btnAddDenari = new GButt.ButtPanel(I18n.get("btn.add_denari_1m"));
        btnAddDenari.setDim(btnW4, 30);
        btnAddDenari.align(DIR.C);
        btnAddDenari.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onAddDenariRequested();
            }
        });
        actionSection.addRight(6, btnAddDenari);

        // 1-Click Combat Auto-Win
        GButt.ButtPanel btnWinCombat = new GButt.ButtPanel(I18n.get("btn.win_combat_now"));
        btnWinCombat.setDim(btnW4, 30);
        btnWinCombat.align(DIR.C);
        btnWinCombat.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onWinCombatRequested();
            }
        });
        actionSection.addRight(6, btnWinCombat);

        content.addDown(4, actionSection);

        // --- 4. Footer: Close Button ---
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.setDim(contentW, 30);
        btnClose.align(DIR.C);
        btnClose.clickActionSet(closeAction);
        content.addDown(10, btnClose);
    }

    private static void buildWorldTab(GuiSection content, int contentW, ArchitectState state, UICallback callback, ACTION closeAction) {
        // --- 1. Population Manager Section ---
        GText popTitle = new GText(UI.FONT().M, "--- " + I18n.get("world.population.title") + " ---");
        popTitle.lablify();
        content.addDown(10, popTitle);

        LIST<Race> races = null;
        try {
            races = (RACES.playable() != null && RACES.playable().size() > 0) ? RACES.playable() : RACES.all();
        } catch (Exception ignored) {
        }

        if (races != null && races.size() > 0) {
            if (selectedRaceIndex >= races.size() || selectedRaceIndex < 0) {
                selectedRaceIndex = 0;
            }
            final Race currentRace = races.get(selectedRaceIndex);
            final LIST<Race> finalRaces = races;

            String raceName = currentRace.info != null ? currentRace.info.name.toString() : currentRace.key;
            raceName = I18n.sanitizeAscii(raceName);

            GuiSection raceRow = new GuiSection();

            GButt.ButtPanel btnPrev = new GButt.ButtPanel("<<");
            btnPrev.setDim(44, 28);
            btnPrev.align(DIR.C);
            btnPrev.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    selectedRaceIndex = (selectedRaceIndex - 1 + finalRaces.size()) % finalRaces.size();
                    open(state, callback);
                }
            });
            raceRow.add(btnPrev);

            GButt.ButtPanel lblRace = new GButt.ButtPanel(I18n.get("world.population.species") + " " + raceName);
            lblRace.setDim(contentW - 96, 28);
            lblRace.align(DIR.C);
            raceRow.addRight(4, lblRace);

            GButt.ButtPanel btnNext = new GButt.ButtPanel(">>");
            btnNext.setDim(44, 28);
            btnNext.align(DIR.C);
            btnNext.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    selectedRaceIndex = (selectedRaceIndex + 1) % finalRaces.size();
                    open(state, callback);
                }
            });
            raceRow.addRight(4, btnNext);

            content.addDown(4, raceRow);

            // Spawn Buttons in a single clean row
            GuiSection spawnRow = new GuiSection();
            int btnW4 = (contentW - 18) / 4;

            GButt.ButtPanel btn10 = new GButt.ButtPanel(I18n.get("world.btn.spawn10"));
            btn10.setDim(btnW4, 30);
            btn10.align(DIR.C);
            btn10.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 10);
                }
            });
            spawnRow.add(btn10);

            GButt.ButtPanel btn50 = new GButt.ButtPanel(I18n.get("world.btn.spawn50"));
            btn50.setDim(btnW4, 30);
            btn50.align(DIR.C);
            btn50.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 50);
                }
            });
            spawnRow.addRight(6, btn50);

            GButt.ButtPanel btn100 = new GButt.ButtPanel(I18n.get("world.btn.spawn100"));
            btn100.setDim(btnW4, 30);
            btn100.align(DIR.C);
            btn100.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 100);
                }
            });
            spawnRow.addRight(6, btn100);

            GButt.ButtPanel btnAll25 = new GButt.ButtPanel(I18n.get("world.btn.spawn_all_25"));
            btnAll25.setDim(btnW4, 30);
            btnAll25.align(DIR.C);
            btnAll25.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnAllPlayableRequested(25);
                }
            });
            spawnRow.addRight(6, btnAll25);

            content.addDown(4, spawnRow);
        }

        // --- 2. Atmosphere, Longevity & Soil Environment ---
        GText envTitle = new GText(UI.FONT().M, "--- " + I18n.get("world.env.title") + " ---");
        envTitle.lablify();
        content.addDown(12, envTitle);

        int btnW2 = (contentW - 12) / 2;
        GuiSection envGrid = new GuiSection();

        // Left: Aging & Weather
        GuiSection envLeft = new GuiSection();

        String agingLabel = I18n.get("world.toggle.aging") + ": " + (state.disableAging ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnAging = new GButt.ButtPanel(agingLabel);
        btnAging.setDim(btnW2, 30);
        btnAging.align(DIR.W);
        btnAging.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.disableAging = !state.disableAging;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        envLeft.addDown(0, btnAging);

        String weatherLabel = I18n.get("world.toggle.weather") + ": " + (state.freezeNiceWeather ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnWeather = new GButt.ButtPanel(weatherLabel);
        btnWeather.setDim(btnW2, 30);
        btnWeather.align(DIR.W);
        btnWeather.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.freezeNiceWeather = !state.freezeNiceWeather;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        envLeft.addDown(4, btnWeather);

        envGrid.add(envLeft);

        // Right: Soil Fertility
        GuiSection envRight = new GuiSection();

        String fertilityLabel = I18n.get("world.toggle.fertility") + ": " + (state.maxFertility ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnFertility = new GButt.ButtPanel(fertilityLabel);
        btnFertility.setDim(btnW2, 30);
        btnFertility.align(DIR.W);
        btnFertility.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.maxFertility = !state.maxFertility;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        envRight.addDown(0, btnFertility);

        GButt.ButtPanel btnFertilityNow = new GButt.ButtPanel(I18n.get("world.btn.fertility_now"));
        btnFertilityNow.setDim(btnW2, 30);
        btnFertilityNow.align(DIR.W);
        btnFertilityNow.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onMaxFertilityRequested();
            }
        });
        envRight.addDown(4, btnFertilityNow);

        envGrid.addRight(12, envRight);
        content.addDown(4, envGrid);

        // --- 3. Footer: Close Button ---
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.setDim(contentW, 30);
        btnClose.align(DIR.C);
        btnClose.clickActionSet(closeAction);
        content.addDown(12, btnClose);
    }
}
