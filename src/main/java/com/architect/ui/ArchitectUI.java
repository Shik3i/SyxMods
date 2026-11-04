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
import snake2d.util.gui.GuiSection;
import snake2d.util.misc.ACTION;
import snake2d.util.sets.LIST;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * Main GUI control center for Syx Architect.
 * Provides live controls for sandbox modules, semantic Schönbau presets, 1-click actions,
 * world & population management, and seamless in-game bilingual localization.
 *
 * LAYOUT:
 * Clamped and budgeted for 100% visibility on all resolutions from 720p to 4K.
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
        void onWinCombatRequested();
        void onSpawnImmigrantsRequested(Race race, int count);
        void onSpawnAllPlayableRequested(int countPerRace);
        void onMaxFertilityRequested();
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void close() {
        if (isOpen) {
            isOpen = false;
            if (VIEW.inters() != null && VIEW.inters().section != null) {
                VIEW.inters().section.close();
            }
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
        int screenH = (C.DIM() != null) ? C.DIM().height() : 720;
        int width = 630;
        int height = Math.min(670, Math.max(500, screenH - 30));

        GuiSection root = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setTitle(I18n.get("ui.title"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                isOpen = false;
                if (VIEW.inters() != null && VIEW.inters().section != null) {
                    VIEW.inters().section.close();
                }
            }
        };
        panel.setCloseAction(closeAction);

        GuiSection content = new GuiSection();
        content.pad(12);

        // --- Top Bar: Tab Switcher + Language Toggle + Tutorial [?] ---
        GuiSection topBar = new GuiSection();

        // Tab 0: City & Economy
        GButt.ButtPanel tabCity = new GButt.ButtPanel((activeTab == 0 ? "▶ " : "") + I18n.get("ui.tab.city"));
        tabCity.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 0;
                open(state, callback);
            }
        });
        topBar.add(tabCity);

        // Tab 1: World & Population
        GButt.ButtPanel tabWorld = new GButt.ButtPanel((activeTab == 1 ? "▶ " : "") + I18n.get("ui.tab.world"));
        tabWorld.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 1;
                open(state, callback);
            }
        });
        topBar.addRight(8, tabWorld);

        // Language toggle [EN] or [DE]
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
        topBar.addRight(110, btnLang);

        // Tutorial modal [?]
        GButt.ButtPanel btnTut = new GButt.ButtPanel("[?]");
        btnTut.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                TutorialModal.open();
            }
        });
        topBar.addRight(8, btnTut);

        content.addDown(0, topBar);

        if (activeTab == 0) {
            buildCityTab(content, state, callback, closeAction);
        } else {
            buildWorldTab(content, state, callback, closeAction);
        }

        root.add(panel);
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        if (VIEW.inters() != null && VIEW.inters().section != null) {
            VIEW.inters().section.activate(root);
        }
    }

    private static void buildCityTab(GuiSection content, ArchitectState state, UICallback callback, ACTION closeAction) {
        // --- 1. Quick Presets Section ---
        GText presetTitle = new GText(UI.FONT().M, I18n.get("ui.presets"));
        presetTitle.lablify();
        content.addDown(10, presetTitle);

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
        presetRow.addDown(4, btnLiving);

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
        presetRow.addDown(4, btnReset);

        content.addDown(4, presetRow);

        // --- 2. Semantic Modules & Cheat Toggles ---
        // Construction Tier (covers rooms, roads, walls, roofs, fortifications)
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
        content.addDown(8, btnConstr);

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
        content.addDown(4, btnMaint);

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
        content.addDown(4, btnKnow);

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
        content.addDown(4, btnLog);

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
        content.addDown(4, btnLoyalty);

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
        content.addDown(4, btnSpoil);

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
        content.addDown(4, btnRaid);

        // --- 3. Instant Action Buttons ---
        GuiSection actionSection = new GuiSection();

        // 1-Click Instant Build All (Rooms, roads, walls)
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

        content.addDown(8, actionSection);

        // 1-Click Combat Auto-Win Button
        GButt.ButtPanel btnWinCombat = new GButt.ButtPanel(I18n.get("btn.win_combat_now"));
        btnWinCombat.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onWinCombatRequested();
            }
        });
        content.addDown(4, btnWinCombat);

        // Footer: Close Button
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.clickActionSet(closeAction);
        content.addDown(8, btnClose);
    }

    private static void buildWorldTab(GuiSection content, ArchitectState state, UICallback callback, ACTION closeAction) {
        // --- 1. Population Manager Section ---
        GText popTitle = new GText(UI.FONT().M, I18n.get("world.population.title"));
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

            CharSequence raceName = currentRace.info != null ? currentRace.info.name : currentRace.key;

            GuiSection raceRow = new GuiSection();

            GButt.ButtPanel btnSelectRace = new GButt.ButtPanel(I18n.get("world.population.species") + " [ " + raceName + " ]");
            btnSelectRace.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    selectedRaceIndex = (selectedRaceIndex + 1) % finalRaces.size();
                    open(state, callback);
                }
            });
            raceRow.add(btnSelectRace);

            GButt.ButtPanel btnSpawn10 = new GButt.ButtPanel(I18n.get("world.btn.spawn10"));
            btnSpawn10.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 10);
                }
            });
            raceRow.addRight(6, btnSpawn10);

            GButt.ButtPanel btnSpawn50 = new GButt.ButtPanel(I18n.get("world.btn.spawn50"));
            btnSpawn50.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 50);
                }
            });
            raceRow.addRight(6, btnSpawn50);

            GButt.ButtPanel btnSpawn100 = new GButt.ButtPanel(I18n.get("world.btn.spawn100"));
            btnSpawn100.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 100);
                }
            });
            raceRow.addRight(6, btnSpawn100);

            content.addDown(6, raceRow);

            // Button: Cosmopolitan Wave (spawns 25 citizens of EVERY playable race)
            GButt.ButtPanel btnSpawnAll = new GButt.ButtPanel(I18n.get("world.btn.spawn_all_25"));
            btnSpawnAll.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnAllPlayableRequested(25);
                }
            });
            content.addDown(4, btnSpawnAll);
        }

        // --- 2. Environment & Longevity Toggles ---
        GText envTitle = new GText(UI.FONT().M, I18n.get("world.env.title"));
        envTitle.lablify();
        content.addDown(12, envTitle);

        // Anti-Aging Toggle
        String agingLabel = I18n.get("world.toggle.aging") + ": " + (state.disableAging ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnAging = new GButt.ButtPanel(agingLabel);
        btnAging.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.disableAging = !state.disableAging;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(6, btnAging);

        // Nice Weather & Daylight Toggle
        String weatherLabel = I18n.get("world.toggle.weather") + ": " + (state.freezeNiceWeather ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnWeather = new GButt.ButtPanel(weatherLabel);
        btnWeather.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.freezeNiceWeather = !state.freezeNiceWeather;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(4, btnWeather);

        // Max Soil Moisture & Fertility Toggle
        String ferLabel = I18n.get("world.toggle.fertility") + ": " + (state.maxFertility ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnFer = new GButt.ButtPanel(ferLabel);
        btnFer.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.maxFertility = !state.maxFertility;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        content.addDown(4, btnFer);

        // 1-Click Soil Moisture Immediate Action
        GButt.ButtPanel btnFerNow = new GButt.ButtPanel(I18n.get("world.btn.fertility_now"));
        btnFerNow.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onMaxFertilityRequested();
            }
        });
        content.addDown(8, btnFerNow);

        // Footer: Close Button
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.clickActionSet(closeAction);
        content.addDown(10, btnClose);
    }
}
