package com.architect.ui;

import com.architect.config.ArchitectState;
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
import util.gui.misc.GHeader;
import util.gui.misc.GText;
import util.gui.panel.GFrame;
import util.gui.panel.GPanel;
import view.main.VIEW;

/**
 * Main GUI control center for Syx Architect.
 * Built to the highest native Songs of Syx visual standards:
 * - Native GPanel.setBig() dark-stone framed modal
 * - Symmetrical tab navigation using GButt.selectedSet()
 * - Native GHeader section dividers with stone horizontal separators
 * - Clean Label-on-Left / Action-on-Right column rows
 * - Zero text clipping, zero command-line symbols, 100% immersive
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
        int height = Math.min(620, Math.max(540, screenH - 40));
        int contentW = 800;

        GuiSection root = new GuiSection();
        GPanel panel = new GPanel(width, height);
        panel.setBig();
        panel.setTitle(I18n.get("ui.title"));

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                close();
            }
        };
        panel.setCloseAction(closeAction);

        GuiSection content = new GuiSection();

        // =========================================================================
        // 1. TOP BAR: Native Tabs + Language + Manual
        // =========================================================================
        GuiSection topBar = new GuiSection();
        int tabW = (contentW - 18) / 4; // 195px each

        // Tab 0: City & Economy
        GButt.ButtPanel tabCity = new GButt.ButtPanel(I18n.get("ui.tab.city"));
        tabCity.setDim(tabW, 32);
        tabCity.align(DIR.C);
        tabCity.selectedSet(activeTab == 0);
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
        GButt.ButtPanel tabWorld = new GButt.ButtPanel(I18n.get("ui.tab.world"));
        tabWorld.setDim(tabW, 32);
        tabWorld.align(DIR.C);
        tabWorld.selectedSet(activeTab == 1);
        tabWorld.hoverTitleSet(I18n.get("ui.tab.world"));
        tabWorld.hoverInfoSet("Schaltet zu Bevoelkerungs-Manager, Einwanderung, Atmosphaere und Natur.");
        tabWorld.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 1;
                open(state, callback);
            }
        });
        topBar.addRight(6, tabWorld);

        // Language toggle
        GButt.ButtPanel btnLang = new GButt.ButtPanel("[" + I18n.getLanguage().code.toUpperCase() + "] " + I18n.get("btn.language"));
        btnLang.setDim(tabW, 32);
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
        topBar.addRight(6, btnLang);

        // Manual / Tutorial modal
        GButt.ButtPanel btnTut = new GButt.ButtPanel(I18n.get("btn.tutorial"));
        btnTut.setDim(tabW, 32);
        btnTut.align(DIR.C);
        btnTut.hoverTitleSet(I18n.get("btn.tutorial"));
        btnTut.hoverInfoSet("Oeffnet das ausfuehrliche In-Game Handbuch mit Erklaerungen zu allen Tiers.");
        btnTut.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                TutorialModal.open();
            }
        });
        topBar.addRight(6, btnTut);

        content.addDown(0, topBar);
        content.addDown(6, GFrame.separator(contentW));

        if (activeTab == 0) {
            buildCityTab(content, contentW, state, callback, closeAction);
        } else {
            buildWorldTab(content, contentW, state, callback, closeAction);
        }

        root.add(panel);
        // Position content cleanly inside big panel frame
        content.body().moveX1Y1(panel.inner().x1() + 20, panel.inner().y1() + 28);
        root.add(content);

        if (C.DIM() != null) {
            root.body().centerIn(C.DIM());
        }

        VIEW.inters().section.activate(root);
    }

    /**
     * Builds Tab 0: City, Economy, Maintenance, Boosts & Instant Actions.
     */
    private static void buildCityTab(GuiSection content, int contentW, ArchitectState state, UICallback callback, ACTION closeAction) {
        // --- 1. Quick Presets Section ---
        GHeader presetHeader = new GHeader(I18n.get("ui.presets")).subify();
        content.addDown(8, presetHeader);

        GuiSection presetRow = new GuiSection();
        int btnW3 = (contentW - 16) / 3; // 261px each

        // Preset 1: God Mode
        GButt.ButtPanel btnGod = new GButt.ButtPanel(I18n.get("preset.god"));
        btnGod.setDim(btnW3, 30);
        btnGod.align(DIR.C);
        btnGod.hoverTitleSet(I18n.get("preset.god"));
        btnGod.hoverInfoSet(I18n.get("preset.god.desc"));
        btnGod.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.applyGodMode();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.add(btnGod);

        // Preset 2: Living City
        GButt.ButtPanel btnLiving = new GButt.ButtPanel(I18n.get("preset.living"));
        btnLiving.setDim(btnW3, 30);
        btnLiving.align(DIR.C);
        btnLiving.hoverTitleSet(I18n.get("preset.living"));
        btnLiving.hoverInfoSet(I18n.get("preset.living.desc"));
        btnLiving.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.applyLivingCity();
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        presetRow.addRight(8, btnLiving);

        // Preset 3: Vanilla Reset
        GButt.ButtPanel btnReset = new GButt.ButtPanel(I18n.get("preset.vanilla"));
        btnReset.setDim(btnW3, 30);
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
        content.addDown(8, GFrame.separator(contentW));

        // --- 2. Main 2-Column Grid (392px per column) ---
        int colW = (contentW - 16) / 2; // 392px
        int ctrlW = 175; // Control button width
        int ctrlH = 28;

        GuiSection gridRow = new GuiSection();

        // === LEFT COLUMN: Construction, Maintenance, Knowledge, Shelf Life ===
        GuiSection leftCol = new GuiSection();
        GHeader leftHeader = new GHeader(I18n.get("ui.tab.construction") + " & " + I18n.get("ui.tab.boosts")).subify();
        leftCol.addDown(0, leftHeader);
        leftCol.addDown(4, GFrame.separator(colW));

        // Row 1: Construction Tier
        GButt.ButtPanel btnConstr = new GButt.ButtPanel(state.constructionTier.getLabel());
        btnConstr.setDim(ctrlW, ctrlH);
        btnConstr.align(DIR.C);
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
        leftCol.addDown(4, createOptionRow(colW, I18n.get("cheat.construction"), btnConstr));

        // Row 2: Maintenance Tier
        GButt.ButtPanel btnMaint = new GButt.ButtPanel(state.maintenanceTier.getLabel());
        btnMaint.setDim(ctrlW, ctrlH);
        btnMaint.align(DIR.C);
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
        leftCol.addDown(4, createOptionRow(colW, I18n.get("cheat.maintenance"), btnMaint));

        // Row 3: Knowledge Tier
        GButt.ButtPanel btnKnow = new GButt.ButtPanel(state.knowledgeTier.getLabel());
        btnKnow.setDim(ctrlW, ctrlH);
        btnKnow.align(DIR.C);
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
        leftCol.addDown(4, createOptionRow(colW, I18n.get("cheat.knowledge"), btnKnow));

        // Row 4: Shelf Life / Spoilage Toggle
        GButt.ButtPanel btnSpoil = new GButt.ButtPanel(state.spoilageDisabled ? I18n.get("status.active") : I18n.get("status.inactive"));
        btnSpoil.setDim(ctrlW, ctrlH);
        btnSpoil.align(DIR.C);
        btnSpoil.selectedSet(state.spoilageDisabled);
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
        leftCol.addDown(4, createOptionRow(colW, I18n.get("cheat.spoilage"), btnSpoil));

        gridRow.add(leftCol);

        // === RIGHT COLUMN: Logistics, Treasury, Loyalty, Raid Immunity ===
        GuiSection rightCol = new GuiSection();
        GHeader rightHeader = new GHeader(I18n.get("ui.tab.logistics") + " & " + I18n.get("ui.tab.security")).subify();
        rightCol.addDown(0, rightHeader);
        rightCol.addDown(4, GFrame.separator(colW));

        // Row 1: Logistics Tier
        GButt.ButtPanel btnLog = new GButt.ButtPanel(state.logisticsTier.getLabel());
        btnLog.setDim(ctrlW, ctrlH);
        btnLog.align(DIR.C);
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
        rightCol.addDown(4, createOptionRow(colW, I18n.get("cheat.logistics"), btnLog));

        // Row 2: Treasury / Infinite Denari Toggle
        GButt.ButtPanel btnDenari = new GButt.ButtPanel(state.infiniteDenari ? I18n.get("status.active") : I18n.get("status.inactive"));
        btnDenari.setDim(ctrlW, ctrlH);
        btnDenari.align(DIR.C);
        btnDenari.selectedSet(state.infiniteDenari);
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
        rightCol.addDown(4, createOptionRow(colW, I18n.get("cheat.infinite_denari"), btnDenari));

        // Row 3: Loyalty & Happiness Toggle
        GButt.ButtPanel btnLoyalty = new GButt.ButtPanel(state.loyaltyBoost ? I18n.get("status.active") : I18n.get("status.inactive"));
        btnLoyalty.setDim(ctrlW, ctrlH);
        btnLoyalty.align(DIR.C);
        btnLoyalty.selectedSet(state.loyaltyBoost);
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
        rightCol.addDown(4, createOptionRow(colW, I18n.get("cheat.loyalty"), btnLoyalty));

        // Row 4: Raid Immunity Toggle
        GButt.ButtPanel btnRaid = new GButt.ButtPanel(state.raidImmunity ? I18n.get("status.active") : I18n.get("status.inactive"));
        btnRaid.setDim(ctrlW, ctrlH);
        btnRaid.align(DIR.C);
        btnRaid.selectedSet(state.raidImmunity);
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
        rightCol.addDown(4, createOptionRow(colW, I18n.get("cheat.raid_protection"), btnRaid));

        gridRow.addRight(16, rightCol);
        content.addDown(8, gridRow);
        content.addDown(8, GFrame.separator(contentW));

        // --- 3. Instant 1-Click Action Buttons ---
        GHeader actionHeader = new GHeader(I18n.get("ui.actions")).subify();
        content.addDown(6, actionHeader);

        GuiSection actionSection = new GuiSection();
        int btnW4 = (contentW - 18) / 4; // 195px each

        // 1-Click Instant Build
        GButt.ButtPanel btnInstantBuild = new GButt.ButtPanel(I18n.get("btn.instant_build_now"));
        btnInstantBuild.setDim(btnW4, 30);
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
        btnFillCrates.setDim(btnW4, 30);
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
        btnAddDenari.setDim(btnW4, 30);
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
        btnWinCombat.setDim(btnW4, 30);
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
        content.addDown(8, GFrame.separator(contentW));

        // --- 4. Footer: Close Button ---
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.setDim(contentW, 30);
        btnClose.align(DIR.C);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet("Schliesst das Syx Architect Kontrollfenster. Kann jederzeit mit F10 wieder geoeffnet werden.");
        btnClose.clickActionSet(closeAction);
        content.addDown(6, btnClose);
    }

    /**
     * Builds Tab 1: World, Population Manager, Environment & Nature.
     */
    private static void buildWorldTab(GuiSection content, int contentW, ArchitectState state, UICallback callback, ACTION closeAction) {
        // --- 1. Population Manager Section ---
        GHeader popHeader = new GHeader(I18n.get("world.population.title")).subify();
        content.addDown(8, popHeader);
        content.addDown(4, GFrame.separator(contentW));

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

            // Race selector row
            GuiSection raceRow = new GuiSection();

            GButt.ButtPanel btnPrev = new GButt.ButtPanel("<");
            btnPrev.setDim(44, 30);
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

            GButt.ButtPanel lblRace = new GButt.ButtPanel(I18n.get("world.population.species") + "  " + raceName);
            lblRace.setDim(contentW - 100, 30);
            lblRace.align(DIR.C);
            lblRace.hoverTitleSet(raceName);
            lblRace.hoverInfoSet("Aktuell ausgewaehlte Rasse fuer Sofort-Einwanderung.");
            raceRow.addRight(6, lblRace);

            GButt.ButtPanel btnNext = new GButt.ButtPanel(">");
            btnNext.setDim(44, 30);
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
            raceRow.addRight(6, btnNext);

            content.addDown(4, raceRow);

            // Immigrant Spawn Buttons Row
            GuiSection spawnRow = new GuiSection();
            int btnW4 = (contentW - 18) / 4; // 195px each

            GButt.ButtPanel btn10 = new GButt.ButtPanel(I18n.get("world.btn.spawn10"));
            btn10.setDim(btnW4, 30);
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
            btn50.setDim(btnW4, 30);
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
            btn100.setDim(btnW4, 30);
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
            btnAll25.setDim(btnW4, 30);
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

            content.addDown(6, spawnRow);
            content.addDown(8, GFrame.separator(contentW));
        }

        // --- 2. Atmosphere, Longevity & Soil Environment ---
        GHeader envHeader = new GHeader(I18n.get("world.env.title")).subify();
        content.addDown(8, envHeader);
        content.addDown(4, GFrame.separator(contentW));

        int colW = (contentW - 16) / 2; // 392px
        int ctrlW = 175;
        int ctrlH = 28;

        GuiSection envGrid = new GuiSection();

        // Left Column: Aging & Weather
        GuiSection envLeft = new GuiSection();

        GButt.ButtPanel btnAging = new GButt.ButtPanel(state.disableAging ? I18n.get("status.active") : I18n.get("status.inactive"));
        btnAging.setDim(ctrlW, ctrlH);
        btnAging.align(DIR.C);
        btnAging.selectedSet(state.disableAging);
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
        envLeft.addDown(0, createOptionRow(colW, I18n.get("world.toggle.aging"), btnAging));

        GButt.ButtPanel btnWeather = new GButt.ButtPanel(state.freezeNiceWeather ? I18n.get("status.active") : I18n.get("status.inactive"));
        btnWeather.setDim(ctrlW, ctrlH);
        btnWeather.align(DIR.C);
        btnWeather.selectedSet(state.freezeNiceWeather);
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
        envLeft.addDown(4, createOptionRow(colW, I18n.get("world.toggle.weather"), btnWeather));

        envGrid.add(envLeft);

        // Right Column: Soil Fertility
        GuiSection envRight = new GuiSection();

        GButt.ButtPanel btnFertility = new GButt.ButtPanel(state.maxFertility ? I18n.get("status.active") : I18n.get("status.inactive"));
        btnFertility.setDim(ctrlW, ctrlH);
        btnFertility.align(DIR.C);
        btnFertility.selectedSet(state.maxFertility);
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
        envRight.addDown(0, createOptionRow(colW, I18n.get("world.toggle.fertility"), btnFertility));

        GButt.ButtPanel btnFertilityNow = new GButt.ButtPanel(I18n.get("btn.apply"));
        btnFertilityNow.setDim(ctrlW, ctrlH);
        btnFertilityNow.align(DIR.C);
        btnFertilityNow.hoverTitleSet(I18n.get("world.btn.fertility_now"));
        btnFertilityNow.hoverInfoSet("Setzt sofort alle Bodenkacheln der Karte auf 100% Feuchtigkeit.");
        btnFertilityNow.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onMaxFertilityRequested();
            }
        });
        envRight.addDown(4, createOptionRow(colW, I18n.get("world.btn.fertility_now"), btnFertilityNow));

        envGrid.addRight(16, envRight);
        content.addDown(4, envGrid);
        content.addDown(8, GFrame.separator(contentW));

        // --- 3. Footer: Close Button ---
        GButt.ButtPanel btnClose = new GButt.ButtPanel(I18n.get("btn.close"));
        btnClose.setDim(contentW, 30);
        btnClose.align(DIR.C);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet("Schliesst das Syx Architect Kontrollfenster. Kann jederzeit mit F10 wieder geoeffnet werden.");
        btnClose.clickActionSet(closeAction);
        content.addDown(6, btnClose);
    }

    /**
     * Creates a standardized native option row:
     * Left side: Gold-tinted label (GText.lablify()), vertically centered.
     * Right side: Interactive control button (GButt), perfectly right-aligned.
     */
    private static GuiSection createOptionRow(int width, String title, GButt control) {
        GuiSection row = new GuiSection();
        row.body().setDim(width, 32);

        GText lbl = new GText(UI.FONT().M, title);
        lbl.lablify();
        lbl.setMaxWidth(width - control.body().width() - 16);
        row.add(lbl, 4, (32 - lbl.height()) / 2);

        int ctrlX = width - control.body().width() - 4;
        row.add(control, ctrlX, (32 - control.body().height()) / 2);
        return row;
    }
}
