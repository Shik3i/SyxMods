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
import snake2d.CORE;
import snake2d.MButt;
import snake2d.SPRITE_RENDERER;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.misc.ACTION;
import snake2d.util.misc.CLAMP;
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
 * - Native GPanel.setBig() stone-framed modal
 * - Zero font clipping: all buttons use native Font M / S (no giant H2 truncation)
 * - Crystal-clear multi-state tooltips explaining current state and full cycle progression
 * - Symmetrical tab navigation using GButt.selectedSet()
 * - Clean Label-on-Left / Action-on-Right column rows
 */
public final class ArchitectUI {

    private static boolean isOpen = false;
    private static int activeTab = 0; // 0 = City & Economy, 1 = World & Population
    private static int selectedRaceIndex = 0;
    private static int windowX = -1;
    private static int windowY = -1;

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
        return isOpen && VIEW.inters() != null && VIEW.inters().section != null && VIEW.inters().section.isActivated();
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
        int contentW = 790;

        ACTION closeAction = new ACTION() {
            @Override
            public void exe() {
                close();
            }
        };

        final GPanel[] panelRef = new GPanel[1];

        GuiSection root = new GuiSection() {
            private boolean isDragging = false;
            private int dragAnchorX = 0;
            private int dragAnchorY = 0;

            @Override
            public void render(SPRITE_RENDERER r, float ds) {
                COORDINATE m = CORE.getInput().getMouse().getCoo();
                if (m != null) {
                    if (MButt.LEFT.isDown()) {
                        if (!isDragging) {
                            // Title bar drag region: top 40px excluding top-right [X] close button
                            int titleY1 = body().y1();
                            int titleY2 = body().y1() + 40;
                            int closeX = body().x2() - 40;
                            if (m.y() >= titleY1 && m.y() <= titleY2 && m.x() >= body().x1() && m.x() <= closeX) {
                                isDragging = true;
                                dragAnchorX = m.x() - body().x1();
                                dragAnchorY = m.y() - body().y1();
                            }
                        } else {
                            int targetX = CLAMP.i(m.x() - dragAnchorX, 10, Math.max(10, C.WIDTH() - body().width() - 10));
                            int targetY = CLAMP.i(m.y() - dragAnchorY, 10, Math.max(10, C.HEIGHT() - body().height() - 10));
                            if (targetX != (int) body().x1() || targetY != (int) body().y1()) {
                                windowX = targetX;
                                windowY = targetY;
                                body().moveX1Y1(targetX, targetY);
                                if (panelRef[0] != null) {
                                    // Compensate vanilla GPanel$1.moveY1 bug (missing +8 closeAction offset)
                                    panelRef[0].inner().incrY(8);
                                }
                            }
                        }
                    } else {
                        isDragging = false;
                    }
                }
                super.render(r, ds);
            }
        };

        // =========================================================================
        // 1. TOP BAR: Native Tabs + Language + Manual
        // =========================================================================
        GuiSection topBar = new GuiSection();
        int tabW = (contentW - 18) / 4; // 193px each

        // Tab 0: City & Economy
        GButt.ButtPanel tabCity = createBtnM(I18n.get("ui.tab.city"), tabW, 30);
        tabCity.selectedSet(activeTab == 0);
        tabCity.hoverTitleSet(I18n.get("ui.tab.city"));
        tabCity.hoverInfoSet(I18n.getLanguage() == I18n.Language.DE
            ? "Oeffnet Bau-, Wartungs-, Wissenschafts-, Logistik- und Sicherheits-Werkzeuge."
            : "Switches to construction, maintenance, science, logistics, and security modules.");
        tabCity.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 0;
                open(state, callback);
            }
        });
        topBar.add(tabCity);

        // Tab 1: World & Population
        GButt.ButtPanel tabWorld = createBtnM(I18n.get("ui.tab.world"), tabW, 30);
        tabWorld.selectedSet(activeTab == 1);
        tabWorld.hoverTitleSet(I18n.get("ui.tab.world"));
        tabWorld.hoverInfoSet(I18n.getLanguage() == I18n.Language.DE
            ? "Oeffnet Bevoelkerungs-Manager, Einwanderung, Atmosphaere und Natur-Tools."
            : "Switches to population manager, immigration, atmosphere, and nature controls.");
        tabWorld.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                activeTab = 1;
                open(state, callback);
            }
        });
        topBar.addRight(6, tabWorld);

        // Language toggle
        GButt.ButtPanel btnLang = createBtnM("[" + I18n.getLanguage().code.toUpperCase() + "] " + I18n.get("btn.language"), tabW, 30);
        btnLang.hoverTitleSet(I18n.getLanguage() == I18n.Language.DE ? "Sprache wechseln" : "Switch Language");
        btnLang.hoverInfoSet(I18n.getLanguage() == I18n.Language.DE
            ? "Schaltet die Benutzeroberflaeche und Tooltips zwischen Deutsch und Englisch um."
            : "Toggles user interface and tooltips between English and German.");
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
        GButt.ButtPanel btnTut = createBtnM(I18n.get("btn.tutorial"), tabW, 30);
        btnTut.hoverTitleSet(I18n.get("btn.tutorial"));
        btnTut.hoverInfoSet(I18n.getLanguage() == I18n.Language.DE
            ? "Oeffnet das ausfuehrliche Handbuch mit Beschreibungen aller Sandbox-Module."
            : "Opens the in-game manual detailing all sandbox and creative features.");
        btnTut.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                TutorialModal.open();
            }
        });
        topBar.addRight(6, btnTut);

        root.add(topBar, 0, 0);
        root.addDown(6, GFrame.separator(contentW));

        if (activeTab == 0) {
            buildCityTab(root, contentW, state, callback, closeAction);
        } else {
            buildWorldTab(root, contentW, state, callback, closeAction);
        }

        root.pad(20, 16);

        // Authentic grand game panel wrapping the modal content
        GPanel panel = new GPanel(root.body()).setBig();
        panel.setTitle(I18n.get("ui.title"));
        panel.setCloseAction(closeAction);
        panelRef[0] = panel;

        root.add(panel);
        root.moveLastToBack();

        int totalW = root.body().width();
        int totalH = root.body().height();

        if (windowX < 0 || windowY < 0) {
            windowX = (C.DIM() != null) ? (C.WIDTH() - totalW) / 2 : 100;
            windowY = (C.DIM() != null) ? (C.HEIGHT() - totalH) / 2 : 100;
        }

        if (C.DIM() != null) {
            windowX = CLAMP.i(windowX, 10, Math.max(10, C.WIDTH() - totalW - 10));
            windowY = CLAMP.i(windowY, 10, Math.max(10, C.HEIGHT() - totalH - 10));
        }

        root.body().moveX1Y1(windowX, windowY);
        // Compensate vanilla GPanel$1.moveY1 bug (missing +8 closeAction offset)
        panel.inner().incrY(8);
        VIEW.inters().section.activate(root);
    }

    /**
     * Builds Tab 0: City, Economy, Maintenance, Boosts & Instant Actions.
     */
    private static void buildCityTab(GuiSection content, int contentW, ArchitectState state, UICallback callback, ACTION closeAction) {
        boolean de = I18n.getLanguage() == I18n.Language.DE;

        // --- 1. Quick Presets Section ---
        GHeader presetHeader = new GHeader(I18n.get("ui.presets")).subify();
        content.addDown(8, presetHeader);

        GuiSection presetRow = new GuiSection();
        int btnW3 = (contentW - 16) / 3; // 268px each

        // Preset 1: God Mode
        GButt.ButtPanel btnGod = createBtnM(I18n.get("preset.god"), btnW3, 30);
        btnGod.hoverTitleSet(I18n.get("preset.god"));
        btnGod.hoverInfoSet(de
            ? "Aktiviert maximale Fuelle:\n- Sofortbau fuer alle Raeume, Strassen & Mauern\n- 0% Verschleiss (100% Zustand eingefroren)\n- 1.000.000 Innovationspunkte (alle Techs)\n- Alle Lagerkisten automatisch auf 80% gefuellt\n- 1.000.000 Denari bar in der Schatzkammer\n- 0% Nahrungsverderb & 100% Loyalitaet\n- Immunitat gegen feindliche Raids"
            : "Activates supreme abundance:\n- Instant build for all structures\n- Zero decay (100% condition frozen)\n- 1,000,000 innovation points (all techs)\n- All storage crates stocked to 80%\n- 1,000,000 Denari in treasury\n- Zero food spoilage & 100% loyalty\n- Complete raid immunity");
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
        GButt.ButtPanel btnLiving = createBtnM(I18n.get("preset.living"), btnW3, 30);
        btnLiving.hoverTitleSet(I18n.get("preset.living"));
        btnLiving.hoverInfoSet(de
            ? "Perfekt fuer lebendigen Schoenbau:\n- Gratis Baumaterialien, echte Arbeiter bauen animiert\n- Hausmeister reparieren ohne Materialkosten\n- Erforschtes Wissen verfaellt niemals\n- Rohstoffe werden automatisch auf 80% gehalten\n- Unendliche Denari & Raid-Schutz aktiv\n- Nahrung verdirbt normal fuer realistisches Stadtleben"
            : "Tailored for aesthetic building:\n- Free building materials with active worker animations\n- Janitors repair without material consumption\n- Unlocked knowledge never decays\n- Raw materials auto-stocked to 80%\n- Infinite Denari & raid immunity enabled\n- Food spoilage enabled for organic city dynamics");
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
        GButt.ButtPanel btnReset = createBtnM(I18n.get("preset.vanilla"), btnW3, 30);
        btnReset.hoverTitleSet(I18n.get("preset.vanilla"));
        btnReset.hoverInfoSet(de
            ? "Setzt alle Einstellungen, Boosts und Cheats zurueck auf die Standard-Spielregeln von Songs of Syx."
            : "Resets all cheats, tiers, and sandbox boosts back to original vanilla game rules.");
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

        // --- 2. Main 2-Column Grid (387px per column) ---
        int colW = (contentW - 16) / 2; // 387px
        int ctrlW = 170; // Control button width
        int ctrlH = 28;

        GuiSection gridRow = new GuiSection();

        // === LEFT COLUMN: Construction, Maintenance, Knowledge, Shelf Life ===
        GuiSection leftCol = new GuiSection();
        GHeader leftHeader = new GHeader(I18n.get("ui.tab.construction") + " & " + I18n.get("ui.tab.boosts")).subify();
        leftCol.addDown(0, leftHeader);
        leftCol.addDown(4, GFrame.separator(colW));

        // Row 1: Construction Tier
        GButt.ButtPanel btnConstr = createBtnM(state.constructionTier.getLabel(), ctrlW, ctrlH);
        btnConstr.hoverTitleSet(I18n.get("cheat.construction") + " [" + state.constructionTier.getLabel() + "]");
        btnConstr.hoverInfoSet(state.constructionTier.getDetailedTooltip());
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
        GButt.ButtPanel btnMaint = createBtnM(state.maintenanceTier.getLabel(), ctrlW, ctrlH);
        btnMaint.hoverTitleSet(I18n.get("cheat.maintenance") + " [" + state.maintenanceTier.getLabel() + "]");
        btnMaint.hoverInfoSet(state.maintenanceTier.getDetailedTooltip());
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
        GButt.ButtPanel btnKnow = createBtnM(state.knowledgeTier.getLabel(), ctrlW, ctrlH);
        btnKnow.hoverTitleSet(I18n.get("cheat.knowledge") + " [" + state.knowledgeTier.getLabel() + "]");
        btnKnow.hoverInfoSet(state.knowledgeTier.getDetailedTooltip());
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
        GButt.ButtPanel btnSpoil = createBtnM(state.spoilageDisabled ? I18n.get("status.active") : I18n.get("status.inactive"), ctrlW, ctrlH);
        btnSpoil.selectedSet(state.spoilageDisabled);
        btnSpoil.hoverTitleSet(I18n.get("cheat.spoilage") + " [" + (state.spoilageDisabled ? I18n.get("status.active") : I18n.get("status.inactive")) + "]");
        btnSpoil.hoverInfoSet(de
            ? (state.spoilageDisabled
                ? "Status: AKTIV (Gueter und Nahrung verderben niemals).\nKlick schaltet die Funktion AUS (normaler Verfall)."
                : "Status: AUS (Normaler Verderb).\nKlick schaltet unbegrenzte Haltbarkeit fuer alle Lager EIN.")
            : (state.spoilageDisabled
                ? "Status: ACTIVE (Goods and food never spoil).\nClick to turn OFF (normal spoiling)."
                : "Status: OFF (Normal spoilage).\nClick to turn infinite shelf life ON."));
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
        GButt.ButtPanel btnLog = createBtnM(state.logisticsTier.getLabel(), ctrlW, ctrlH);
        btnLog.hoverTitleSet(I18n.get("cheat.logistics") + " [" + state.logisticsTier.getLabel() + "]");
        btnLog.hoverInfoSet(state.logisticsTier.getDetailedTooltip());
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
        GButt.ButtPanel btnDenari = createBtnM(state.infiniteDenari ? I18n.get("status.active") : I18n.get("status.inactive"), ctrlW, ctrlH);
        btnDenari.selectedSet(state.infiniteDenari);
        btnDenari.hoverTitleSet(I18n.get("cheat.infinite_denari") + " [" + (state.infiniteDenari ? I18n.get("status.active") : I18n.get("status.inactive")) + "]");
        btnDenari.hoverInfoSet(de
            ? (state.infiniteDenari
                ? "Status: AKTIV (Schatzkammer wird auf mind. 1.000.000 Denari gehalten).\nKlick schaltet die Funktion AUS."
                : "Status: AUS (Normale Steuereinnahmen & Handel).\nKlick aktiviert automatische 1.000.000 Denari.")
            : (state.infiniteDenari
                ? "Status: ACTIVE (Treasury auto-replenished to 1,000,000+ Denari).\nClick to turn OFF."
                : "Status: OFF (Standard economy).\nClick to activate infinite Denari."));
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
        GButt.ButtPanel btnLoyalty = createBtnM(state.loyaltyBoost ? I18n.get("status.active") : I18n.get("status.inactive"), ctrlW, ctrlH);
        btnLoyalty.selectedSet(state.loyaltyBoost);
        btnLoyalty.hoverTitleSet(I18n.get("cheat.loyalty") + " [" + (state.loyaltyBoost ? I18n.get("status.active") : I18n.get("status.inactive")) + "]");
        btnLoyalty.hoverInfoSet(de
            ? (state.loyaltyBoost
                ? "Status: AKTIV (Loyalitaet & Gehorsam maximiert, Aufstaende unmoeglich).\nKlick schaltet die Funktion AUS."
                : "Status: AUS (Normale Buergerlaunen & Aufstandsrisiken).\nKlick schaltet 100% Loyalitaet EIN.")
            : (state.loyaltyBoost
                ? "Status: ACTIVE (Loyalty maximized, riots and rebellions prevented).\nClick to turn OFF."
                : "Status: OFF (Standard citizen happiness dynamics).\nClick to turn maximum loyalty ON."));
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
        GButt.ButtPanel btnRaid = createBtnM(state.raidImmunity ? I18n.get("status.active") : I18n.get("status.inactive"), ctrlW, ctrlH);
        btnRaid.selectedSet(state.raidImmunity);
        btnRaid.hoverTitleSet(I18n.get("cheat.raid_protection") + " [" + (state.raidImmunity ? I18n.get("status.active") : I18n.get("status.inactive")) + "]");
        btnRaid.hoverInfoSet(de
            ? (state.raidImmunity
                ? "Status: AKTIV (Feindliche Raids und Invasionen werden sofort friedlich beendet).\nKlick schaltet die Funktion AUS."
                : "Status: AUS (Normale feindliche Angriffe).\nKlick schaltet Raid-Schutz EIN.")
            : (state.raidImmunity
                ? "Status: ACTIVE (Hostile raids and invasions automatically dismissed).\nClick to turn OFF."
                : "Status: OFF (Hostile invasions occur normally).\nClick to turn raid protection ON."));
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
        int btnW4 = (contentW - 18) / 4; // 200px each

        // 1-Click Instant Build
        GButt.ButtPanel btnInstantBuild = createBtnM(I18n.get("btn.instant_build_now"), btnW4, 30);
        btnInstantBuild.hoverTitleSet(I18n.get("btn.instant_build_now"));
        btnInstantBuild.hoverInfoSet(de
            ? "Stellt alle derzeit geplanten Raeume, Strassen, Mauern und Daecher kartenweit sofort fertig."
            : "Instantly finishes all currently planned rooms, roads, walls, and roofs across the map.");
        btnInstantBuild.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onInstantBuildRequested();
            }
        });
        actionSection.add(btnInstantBuild);

        // 1-Click Fill All Crates to 100%
        GButt.ButtPanel btnFillCrates = createBtnM(I18n.get("btn.fill_crates_100"), btnW4, 30);
        btnFillCrates.hoverTitleSet(I18n.get("btn.fill_crates_100"));
        btnFillCrates.hoverInfoSet(de
            ? "Fuellt alle zugewiesenen Lagerkisten kartenweit sofort auf 100% Maximalkapazitaet."
            : "Tops up every assigned storage crate map-wide to 100% maximum capacity.");
        btnFillCrates.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onFillCratesRequested();
            }
        });
        actionSection.addRight(6, btnFillCrates);

        // 1-Click Add 1,000,000 Denari
        GButt.ButtPanel btnAddDenari = createBtnM(I18n.get("btn.add_denari_1m"), btnW4, 30);
        btnAddDenari.hoverTitleSet(I18n.get("btn.add_denari_1m"));
        btnAddDenari.hoverInfoSet(de
            ? "Zahlt der staedtischen Schatzkammer sofort 1.000.000 Denari bar ein."
            : "Deposits 1,000,000 Denari cash directly into the city treasury.");
        btnAddDenari.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                callback.onAddDenariRequested();
            }
        });
        actionSection.addRight(6, btnAddDenari);

        // 1-Click Combat Auto-Win
        GButt.ButtPanel btnWinCombat = createBtnM(I18n.get("btn.win_combat_now"), btnW4, 30);
        btnWinCombat.hoverTitleSet(I18n.get("btn.win_combat_now"));
        btnWinCombat.hoverInfoSet(de
            ? "Bricht die Moral aller feindlichen Armeen auf allen aktiven Schlachtfeldern sofort fuer garantierten Sieg."
            : "Breaks hostile enemy army morale on all active battlefields for an unconditional instant victory.");
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
        GButt.ButtPanel btnClose = createBtnM(I18n.get("btn.close"), contentW, 30);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet(de
            ? "Schliesst das Kontrollfenster. Kann jederzeit mit F10 oder dem ARCHITECT-Button wieder geoeffnet werden."
            : "Closes this window. Can be re-opened anytime via F10 or the ARCHITECT HUD button.");
        btnClose.clickActionSet(closeAction);
        content.addDown(6, btnClose);
    }

    /**
     * Builds Tab 1: World, Population Manager, Environment & Nature.
     */
    private static void buildWorldTab(GuiSection content, int contentW, ArchitectState state, UICallback callback, ACTION closeAction) {
        boolean de = I18n.getLanguage() == I18n.Language.DE;

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

            GButt.ButtPanel btnPrev = createBtnM("<", 44, 30);
            btnPrev.hoverTitleSet(de ? "Vorherige Spezies" : "Previous Species");
            btnPrev.hoverInfoSet(de ? "Waehlt die vorherige Spezies in der Liste." : "Cycles to previous species.");
            btnPrev.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    selectedRaceIndex = (selectedRaceIndex - 1 + finalRaces.size()) % finalRaces.size();
                    open(state, callback);
                }
            });
            raceRow.add(btnPrev);

            GButt.ButtPanel lblRace = createBtnM(I18n.get("world.population.species") + "  " + raceName, contentW - 100, 30);
            lblRace.hoverTitleSet(raceName);
            lblRace.hoverInfoSet(de ? "Aktuell ausgewaehlte Rasse fuer Sofort-Einwanderung." : "Currently selected species for immigration.");
            raceRow.addRight(6, lblRace);

            GButt.ButtPanel btnNext = createBtnM(">", 44, 30);
            btnNext.hoverTitleSet(de ? "Naechste Spezies" : "Next Species");
            btnNext.hoverInfoSet(de ? "Waehlt die naechste Spezies in der Liste." : "Cycles to next species.");
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
            int btnW4 = (contentW - 18) / 4; // 200px each

            GButt.ButtPanel btn10 = createBtnM(I18n.get("world.btn.spawn10"), btnW4, 30);
            btn10.hoverTitleSet("+10 " + raceName);
            btn10.hoverInfoSet(de ? "Spawnt sofort 10 Einwanderer der Spezies " + raceName + "." : "Instantly spawns 10 immigrants of " + raceName + ".");
            btn10.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 10);
                }
            });
            spawnRow.add(btn10);

            GButt.ButtPanel btn50 = createBtnM(I18n.get("world.btn.spawn50"), btnW4, 30);
            btn50.hoverTitleSet("+50 " + raceName);
            btn50.hoverInfoSet(de ? "Spawnt sofort 50 Einwanderer der Spezies " + raceName + "." : "Instantly spawns 50 immigrants of " + raceName + ".");
            btn50.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 50);
                }
            });
            spawnRow.addRight(6, btn50);

            GButt.ButtPanel btn100 = createBtnM(I18n.get("world.btn.spawn100"), btnW4, 30);
            btn100.hoverTitleSet("+100 " + raceName);
            btn100.hoverInfoSet(de ? "Spawnt sofort 100 Einwanderer der Spezies " + raceName + "." : "Instantly spawns 100 immigrants of " + raceName + ".");
            btn100.clickActionSet(new ACTION() {
                @Override
                public void exe() {
                    callback.onSpawnImmigrantsRequested(currentRace, 100);
                }
            });
            spawnRow.addRight(6, btn100);

            GButt.ButtPanel btnAll25 = createBtnM(I18n.get("world.btn.spawn_all_25"), btnW4, 30);
            btnAll25.hoverTitleSet(de ? "+25 Alle Rassen" : "+25 All Races");
            btnAll25.hoverInfoSet(de ? "Spawnt sofort je 25 Buerger fuer jede spielbare Rasse zugleich." : "Instantly spawns 25 citizens for each playable species simultaneously.");
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

        int colW = (contentW - 16) / 2; // 387px
        int ctrlW = 170;
        int ctrlH = 28;

        GuiSection envGrid = new GuiSection();

        // Left Column: Aging & Weather
        GuiSection envLeft = new GuiSection();

        GButt.ButtPanel btnAging = createBtnM(state.disableAging ? I18n.get("status.active") : I18n.get("status.inactive"), ctrlW, ctrlH);
        btnAging.selectedSet(state.disableAging);
        btnAging.hoverTitleSet(I18n.get("world.toggle.aging") + " [" + (state.disableAging ? I18n.get("status.active") : I18n.get("status.inactive")) + "]");
        btnAging.hoverInfoSet(de
            ? (state.disableAging
                ? "Status: AKTIV (Alterung vollstaendig gestoppt, Buerger sterben nicht an Altersschwaeche).\nKlick schaltet die Funktion AUS."
                : "Status: AUS (Normale Alterung).\nKlick schaltet Stoppen der Alterung EIN.")
            : (state.disableAging
                ? "Status: ACTIVE (Aging disabled, citizens never die of old age).\nClick to turn OFF."
                : "Status: OFF (Standard aging dynamics).\nClick to disable aging."));
        btnAging.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.disableAging = !state.disableAging;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        envLeft.addDown(0, createOptionRow(colW, I18n.get("world.toggle.aging"), btnAging));

        GButt.ButtPanel btnWeather = createBtnM(state.freezeNiceWeather ? I18n.get("status.active") : I18n.get("status.inactive"), ctrlW, ctrlH);
        btnWeather.selectedSet(state.freezeNiceWeather);
        btnWeather.hoverTitleSet(I18n.get("world.toggle.weather") + " [" + (state.freezeNiceWeather ? I18n.get("status.active") : I18n.get("status.inactive")) + "]");
        btnWeather.hoverInfoSet(de
            ? (state.freezeNiceWeather
                ? "Status: AKTIV (Klares Tageslicht dauerhaft eingefroren, kein Regen oder Schnee).\nKlick schaltet die Funktion AUS."
                : "Status: AUS (Normaler Wetterwechsel).\nKlick friert klares Schoenwetter EIN.")
            : (state.freezeNiceWeather
                ? "Status: ACTIVE (Clear daylight frozen permanently, zero precipitation).\nClick to turn OFF."
                : "Status: OFF (Standard weather dynamics).\nClick to freeze clear weather."));
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

        GButt.ButtPanel btnFertility = createBtnM(state.maxFertility ? I18n.get("status.active") : I18n.get("status.inactive"), ctrlW, ctrlH);
        btnFertility.selectedSet(state.maxFertility);
        btnFertility.hoverTitleSet(I18n.get("world.toggle.fertility") + " [" + (state.maxFertility ? I18n.get("status.active") : I18n.get("status.inactive")) + "]");
        btnFertility.hoverInfoSet(de
            ? (state.maxFertility
                ? "Status: AKTIV (Bodenfeuchte wird kartenweit permanent auf 100% gehalten).\nKlick schaltet die Funktion AUS."
                : "Status: AUS (Normale Bodenfeuchte).\nKlick aktiviert permanente 100% Bodenfeuchte.")
            : (state.maxFertility
                ? "Status: ACTIVE (Soil moisture locked at 100% map-wide permanently).\nClick to turn OFF."
                : "Status: OFF (Natural soil moisture dynamics).\nClick to lock soil moisture to 100%."));
        btnFertility.clickActionSet(new ACTION() {
            @Override
            public void exe() {
                state.maxFertility = !state.maxFertility;
                callback.onStateUpdated();
                open(state, callback);
            }
        });
        envRight.addDown(0, createOptionRow(colW, I18n.get("world.toggle.fertility"), btnFertility));

        GButt.ButtPanel btnFertilityNow = createBtnM(I18n.get("btn.apply"), ctrlW, ctrlH);
        btnFertilityNow.hoverTitleSet(I18n.get("world.btn.fertility_now"));
        btnFertilityNow.hoverInfoSet(de
            ? "Setzt sofort in diesem Moment alle Bodenkacheln der Karte auf 100% Feuchtigkeit."
            : "Instantly sets moisture across all map tiles to 100%.");
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
        GButt.ButtPanel btnClose = createBtnM(I18n.get("btn.close"), contentW, 30);
        btnClose.hoverTitleSet(I18n.get("btn.close"));
        btnClose.hoverInfoSet(de
            ? "Schliesst das Kontrollfenster. Kann jederzeit mit F10 oder dem ARCHITECT-Button wieder geoeffnet werden."
            : "Closes this window. Can be re-opened anytime via F10 or the ARCHITECT HUD button.");
        btnClose.clickActionSet(closeAction);
        content.addDown(6, btnClose);
    }

    /**
     * Standardized helper creating a GButt.ButtPanel with Font M (clean, compact, zero clipping).
     */
    public static GButt.ButtPanel createBtnM(CharSequence text, int width, int height) {
        GText t = new GText(UI.FONT().M, text);
        GButt.ButtPanel b = new GButt.ButtPanel((snake2d.util.sprite.SPRITE) t);
        b.setDim(width, height);
        b.align(DIR.C);
        return b;
    }

    /**
     * Standardized helper creating a GButt.ButtPanel with Font S for extra compact areas.
     */
    public static GButt.ButtPanel createBtnS(CharSequence text, int width, int height) {
        GText t = new GText(UI.FONT().S, text);
        GButt.ButtPanel b = new GButt.ButtPanel((snake2d.util.sprite.SPRITE) t);
        b.setDim(width, height);
        b.align(DIR.C);
        return b;
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
        int ctrlX = width - control.body().width() - 6;
        lbl.setMaxWidth(ctrlX - 12);
        row.add(lbl, 6, (32 - lbl.height()) / 2);

        row.add(control, ctrlX, (32 - control.body().height()) / 2);
        return row;
    }
}
