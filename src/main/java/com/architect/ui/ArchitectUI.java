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
 * Generous 840px wide native Songs of Syx panel architecture.
 * Features clean 2-column layout, dynamic button sizing, zero text clipping,
 * and native Snake2D hover tooltips on every interactive element.
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
        int width = 840;
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

        // --- Top Bar: Tab Switcher + Language Toggle + Tutorial Guide ---
        GuiSection topBar = new GuiSection();

        // Tab 0: City & Economy
        GButt.ButtPanel tabCity = new GButt.ButtPanel((activeTab == 0 ? ">> " : "") + I18n.get("ui.tab.city"));
        tabCity.setDim(210, 30);
        tabCity.align(DIR.C);
        tabCity.hoverTitleSet(I18n.get("ui.tab.city"));
        tabCity.hoverInfoSet("Schaltet zu Bau, Wartung, Logistik, Wirtschaft und Sicherheits-Modulen.");
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
        tabWorld.setDim(210, 30);
        tabWorld.align(DIR.C);
        tabWorld.hoverTitleSet(I18n.get("ui.tab.world"));
        tabWorld.hoverInfoSet("Schaltet zu Bevoelkerungs-Manager, Einwanderung, Atmosphaere und Natur.");
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
        btnLang.setDim(180, 30);
        btnLang.align(DIR.C);
        btnLang.hoverTitleSet("Sprache / Language");
        btnLang.hoverInfoSet("Schaltet die Mod-Sprache zwischen Deutsch und Englisch um.");
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
        GButt.ButtPanel btnTut = new GButt.ButtPanel(I18n.get("btn.tutorial"));
        btnTut.setDim(110, 30);
        btnTut.align(DIR.C);
        btnTut.hoverTitleSet(I18n.get("btn.tutorial"));
        btnTut.hoverInfoSet("Oeffnet das ausfuehrliche In-Game Handbuch mit Erklaerungen zu allen Tiers.");
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
        // Position content cleanly inside panel inner boundaries
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
        GButt.ButtPanel btnGod = new GButt.ButtPanel(I18n.get("preset.god"));
        btnGod.setDim(btnW3, 32);
        btnGod.align(DIR.C);
        btnGod.hoverTitleSet(I18n.get("preset.god"));
        btnGod.hoverInfoSet(I18n.get("preset.god.desc"));
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
        GButt.ButtPanel btnLiving = new GButt.ButtPanel(I18n.get("preset.living"));
        btnLiving.setDim(btnW3, 32);
        btnLiving.align(DIR.C);
        btnLiving.hoverTitleSet(I18n.get("preset.living"));
        btnLiving.hoverInfoSet(I18n.get("preset.living.desc"));
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
        GButt.ButtPanel btnReset = new GButt.ButtPanel(I18n.get("preset.vanilla"));
        btnReset.setDim(btnW3, 32);
        btnReset.align(DIR.C);
        btnReset.hoverTitleSet(I18n.get("preset.vanilla"));
        btnReset.hoverInfoSet(I18n.get("preset.vanilla.desc"));
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

        // --- 2. Main 2-Column Grid (392px per column - generous & readable) ---
        int colW = (contentW - 16) / 2;

        GuiSection gridRow = new GuiSection();

        // Left Column: Construction, Maintenance, Knowledge, Spoilage
        GuiSection leftCol = new GuiSection();

        GText lblLeft = new GText(UI.FONT().M, "--- " + I18n.get("ui.tab.construction") + " & " + I18n.get("ui.tab.boosts") + " ---");
        lblLeft.lablify();
        leftCol.addDown(0, lblLeft);

        // Construction Tier Button
        String constrLabel = I18n.get("cheat.construction") + ": " + state.constructionTier.getLabel();
        GButt.ButtPanel btnConstr = new GButt.ButtPanel(constrLabel);
        btnConstr.setDim(colW, 32);
        btnConstr.align(DIR.W);
        btnConstr.hoverTitleSet(I18n.get("cheat.construction"));
        btnConstr.hoverInfoSet(I18n.get("cheat.construction.desc"));
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
        btnMaint.setDim(colW, 32);
        btnMaint.align(DIR.W);
        btnMaint.hoverTitleSet(I18n.get("cheat.maintenance"));
        btnMaint.hoverInfoSet(I18n.get("cheat.maintenance.desc"));
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
        btnKnow.setDim(colW, 32);
        btnKnow.align(DIR.W);
        btnKnow.hoverTitleSet(I18n.get("cheat.knowledge"));
        btnKnow.hoverInfoSet(I18n.get("cheat.knowledge.desc"));
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
        btnSpoil.setDim(colW, 32);
        btnSpoil.align(DIR.W);
        btnSpoil.hoverTitleSet(I18n.get("cheat.spoilage"));
        btnSpoil.hoverInfoSet(I18n.get("cheat.spoilage.desc"));
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
        btnLog.setDim(colW, 32);
        btnLog.align(DIR.W);
        btnLog.hoverTitleSet(I18n.get("cheat.logistics"));
        btnLog.hoverInfoSet(I18n.get("cheat.logistics.desc"));
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
        btnDenari.setDim(colW, 32);
        btnDenari.align(DIR.W);
        btnDenari.hoverTitleSet(I18n.get("cheat.infinite_denari"));
        btnDenari.hoverInfoSet(I18n.get("cheat.infinite_denari.desc"));
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
        btnLoyalty.setDim(colW, 32);
        btnLoyalty.align(DIR.W);
        btnLoyalty.hoverTitleSet(I18n.get("cheat.loyalty"));
        btnLoyalty.hoverInfoSet(I18n.get("cheat.loyalty.desc"));
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
        btnRaid.setDim(colW, 32);
        btnRaid.align(DIR.W);
        btnRaid.hoverTitleSet(I18n.get("cheat.raid_protection"));
        btnRaid.hoverInfoSet(I18n.get("cheat.raid_protection.desc"));
        btnRaid.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.raidImmunity = !state.raidImmunity;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        rightCol.addDown(4, btnRaid);

        gridRow.add(leftCol);
        gridRow.addRight(16, rightCol);
        content.addDown(10, gridRow);

        // --- 3. Instant 1-Click Action Buttons (4 Columns, ~190px each) ---
        GText lblActions = new GText(UI.FONT().M, "--- Instant 1-Click Actions ---");
        lblActions.lablify();
        content.addDown(10, lblActions);

        GuiSection actionSection = new GuiSection();
        int btnW4 = (contentW - 18) / 4;

        // 1-Click Instant Build
        GButt.ButtPanel btnInstantBuild = new GButt.ButtPanel(I18n.get("btn.instant_build_now"));
        btnInstantBuild.setDim(btnW4, 32);
        btnInstantBuild.align(DIR.C);
        btnInstantBuild.hoverTitleSet(I18n.get("btn.instant_build_now"));
        btnInstantBuild.hoverInfoSet("Stellt alle derzeit geplanten Raeume, Strassen, Mauern und Daecher sofort fertig.");
        btnInstantBuild.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onInstantBuildRequested();
            }
        });
        actionSection.add(btnInstantBuild);

        // 1-Click Fill All Crates to 100%
        GButt.ButtPanel btnFillCrates = new GButt.ButtPanel(I18n.get("btn.fill_crates_100"));
        btnFillCrates.setDim(btnW4, 32);
        btnFillCrates.align(DIR.C);
        btnFillCrates.hoverTitleSet(I18n.get("btn.fill_crates_100"));
        btnFillCrates.hoverInfoSet("Fuellt alle zugewiesenen Lagerkisten kartenweit sofort auf 100% Kapazitaet.");
        btnFillCrates.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onFillCratesRequested();
            }
        });
        actionSection.addRight(6, btnFillCrates);

        // 1-Click Add 1,000,000 Denari
        GButt.ButtPanel btnAddDenari = new GButt.ButtPanel(I18n.get("btn.add_denari_1m"));
        btnAddDenari.setDim(btnW4, 32);
        btnAddDenari.align(DIR.C);
        btnAddDenari.hoverTitleSet(I18n.get("btn.add_denari_1m"));
        btnAddDenari.hoverInfoSet("Zahlt der staedtischen Schatzkammer sofort 1.000.000 Denari bar ein.");
        btnAddDenari.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onAddDenariRequested();
            }
        });
        actionSection.addRight(6, btnAddDenari);

        // 1-Click Combat Auto-Win
        GButt.ButtPanel btnWinCombat = new GButt.ButtPanel(I18n.get("btn.win_combat_now"));
        btnWinCombat.setDim(btnW4, 32);
        btnWinCombat.align(DIR.C);
        btnWinCombat.hoverTitleSet(I18n.get("btn.win_combat_now"));
        btnWinCombat.hoverInfoSet("Bricht die Moral aller feindlichen Armeen auf allen aktiven Schlachtfeldern augenblicklich.");
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
        btnClose.setDim(contentW, 32);
        btnClose.align(DIR.C);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet("Schliesst das Syx Architect Kontrollfenster. Kann jederzeit mit F10 wieder geoeffnet werden.");
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
            btnPrev.setDim(50, 32);
            btnPrev.align(DIR.C);
            btnPrev.hoverTitleSet("Vorherige Spezies");
            btnPrev.hoverInfoSet("Waehlt die vorherige Spezies in der Liste.");
            btnPrev.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    selectedRaceIndex = (selectedRaceIndex - 1 + finalRaces.size()) % finalRaces.size();
                    open(state, callback);
                }
            });
            raceRow.add(btnPrev);

            GButt.ButtPanel lblRace = new GButt.ButtPanel(I18n.get("world.population.species") + " [ " + raceName + " ]");
            lblRace.setDim(contentW - 108, 32);
            lblRace.align(DIR.C);
            lblRace.hoverTitleSet(raceName);
            lblRace.hoverInfoSet("Aktuell ausgewaehlte Rasse fuer Sofort-Einwanderung.");
            raceRow.addRight(4, lblRace);

            GButt.ButtPanel btnNext = new GButt.ButtPanel(">>");
            btnNext.setDim(50, 32);
            btnNext.align(DIR.C);
            btnNext.hoverTitleSet("Naechste Spezies");
            btnNext.hoverInfoSet("Waehlt die naechste Spezies in der Liste.");
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
            btn10.setDim(btnW4, 32);
            btn10.align(DIR.C);
            btn10.hoverTitleSet("+10 Buerger");
            btn10.hoverInfoSet("Spawnt sofort 10 Einwanderer der ausgewaehlten Spezies.");
            btn10.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 10);
                }
            });
            spawnRow.add(btn10);

            GButt.ButtPanel btn50 = new GButt.ButtPanel(I18n.get("world.btn.spawn50"));
            btn50.setDim(btnW4, 32);
            btn50.align(DIR.C);
            btn50.hoverTitleSet("+50 Buerger");
            btn50.hoverInfoSet("Spawnt sofort 50 Einwanderer der ausgewaehlten Spezies.");
            btn50.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 50);
                }
            });
            spawnRow.addRight(6, btn50);

            GButt.ButtPanel btn100 = new GButt.ButtPanel(I18n.get("world.btn.spawn100"));
            btn100.setDim(btnW4, 32);
            btn100.align(DIR.C);
            btn100.hoverTitleSet("+100 Buerger");
            btn100.hoverInfoSet("Spawnt sofort 100 Einwanderer der ausgewaehlten Spezies.");
            btn100.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 100);
                }
            });
            spawnRow.addRight(6, btn100);

            GButt.ButtPanel btnAll25 = new GButt.ButtPanel(I18n.get("world.btn.spawn_all_25"));
            btnAll25.setDim(btnW4, 32);
            btnAll25.align(DIR.C);
            btnAll25.hoverTitleSet("+25 Alle Rassen");
            btnAll25.hoverInfoSet("Spawnt sofort 25 Buerger fuer jede spielbare Rasse zugleich.");
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

        int colW = (contentW - 16) / 2;
        GuiSection envGrid = new GuiSection();

        // Left Column: Aging & Weather
        GuiSection envLeft = new GuiSection();

        String agingLabel = I18n.get("world.toggle.aging") + ": " + (state.disableAging ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnAging = new GButt.ButtPanel(agingLabel);
        btnAging.setDim(colW, 32);
        btnAging.align(DIR.W);
        btnAging.hoverTitleSet(I18n.get("world.toggle.aging"));
        btnAging.hoverInfoSet("Verhindert Alterstod voellig durch extreme Lebenszeit-Verlaengerung.");
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
        btnWeather.setDim(colW, 32);
        btnWeather.align(DIR.W);
        btnWeather.hoverTitleSet(I18n.get("world.toggle.weather"));
        btnWeather.hoverInfoSet("Friert klares Tageslicht ein und vertreibt Regen, Schnee und Gewitter.");
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

        // Right Column: Soil Fertility
        GuiSection envRight = new GuiSection();

        String fertilityLabel = I18n.get("world.toggle.fertility") + ": " + (state.maxFertility ? I18n.get("status.active") : I18n.get("status.inactive"));
        GButt.ButtPanel btnFertility = new GButt.ButtPanel(fertilityLabel);
        btnFertility.setDim(colW, 32);
        btnFertility.align(DIR.W);
        btnFertility.hoverTitleSet(I18n.get("world.toggle.fertility"));
        btnFertility.hoverInfoSet("Haelt die Bodenfeuchte kartenweit permanent auf 100% fuer maximale Farm-Ertaege.");
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
        btnFertilityNow.setDim(colW, 32);
        btnFertilityNow.align(DIR.W);
        btnFertilityNow.hoverTitleSet(I18n.get("world.btn.fertility_now"));
        btnFertilityNow.hoverInfoSet("Setzt sofort alle Bodenkacheln der Karte auf 100% Feuchtigkeit.");
        btnFertilityNow.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onMaxFertilityRequested();
            }
        });
        envRight.addDown(4, btnFertilityNow);

        envGrid.addRight(16, envRight);
        content.addDown(4, envGrid);

        // --- 3. Footer: Close Button ---
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.setDim(contentW, 32);
        btnClose.align(DIR.C);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet("Schliesst das Syx Architect Kontrollfenster. Kann jederzeit mit F10 wieder geoeffnet werden.");
        btnClose.clickActionSet(closeAction);
        content.addDown(10, btnClose);
    }
}
