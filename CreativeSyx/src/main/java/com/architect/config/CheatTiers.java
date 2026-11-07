package com.architect.config;

import com.architect.util.I18n;

/**
 * Semantic creative and sandbox tiers decoupling work/simulation from material costs.
 * Provides crystal-clear multi-state tooltips explaining current state and full cycle progression.
 */
public final class CheatTiers {

    private CheatTiers() {}

    /**
     * Building maintenance and degradation tiers.
     */
    public enum MaintenanceTier {
        VANILLA("tier.maintenance.vanilla"),
        JANITOR_WORK_FREE_MATS("tier.maintenance.janitors_free"),
        NO_DECAY("tier.maintenance.no_decay");

        public final String i18nKey;

        MaintenanceTier(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public String getLabel() {
            return I18n.get(i18nKey);
        }

        public MaintenanceTier next() {
            MaintenanceTier[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static MaintenanceTier fromOrdinal(int ord) {
            MaintenanceTier[] values = values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return VANILLA;
        }

        public String getDetailedTooltip() {
            boolean de = I18n.getLanguage() == I18n.Language.DE;
            StringBuilder sb = new StringBuilder();
            if (de) {
                sb.append("AKTUELL: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Standard Verschleiss. Hausmeister benoetigen Material und Zeit zur Instandhaltung.");
                        break;
                    case JANITOR_WORK_FREE_MATS:
                        sb.append("Hausmeister reparieren alle Raeume und Strassen gratis ohne Materialkosten.");
                        break;
                    case NO_DECAY:
                        sb.append("Null Verfall. Alle Raeume, Strassen und Mauern bleiben permanent bei 100%.");
                        break;
                }
                sb.append("\n\nKlick schaltet zur naechsten Stufe:");
                sb.append("\n 1. ").append(I18n.get("tier.maintenance.vanilla")).append(" (Normaler Verfall)");
                sb.append("\n 2. ").append(I18n.get("tier.maintenance.janitors_free")).append(" (Gratis Reparatur)");
                sb.append("\n 3. ").append(I18n.get("tier.maintenance.no_decay")).append(" (Null Verfall)");
            } else {
                sb.append("CURRENT: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Standard structure wear. Janitors require materials and time for repairs.");
                        break;
                    case JANITOR_WORK_FREE_MATS:
                        sb.append("Janitors repair all structures without consuming materials.");
                        break;
                    case NO_DECAY:
                        sb.append("Zero decay. All structures freeze at 100% condition permanently.");
                        break;
                }
                sb.append("\n\nClick cycles to next tier:");
                sb.append("\n 1. ").append(I18n.get("tier.maintenance.vanilla")).append(" (Default wear)");
                sb.append("\n 2. ").append(I18n.get("tier.maintenance.janitors_free")).append(" (Free repairs)");
                sb.append("\n 3. ").append(I18n.get("tier.maintenance.no_decay")).append(" (Zero decay)");
            }
            return sb.toString();
        }
    }

    /**
     * Construction site automation and material requirement tiers.
     */
    public enum ConstructionTier {
        VANILLA("tier.construction.vanilla"),
        FREE_MATERIALS_KEEP_WORK("tier.construction.free_work"),
        FAST_AND_FREE("tier.construction.fast_free"),
        INSTANT_BUILD("tier.construction.instant");

        public final String i18nKey;

        ConstructionTier(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public String getLabel() {
            return I18n.get(i18nKey);
        }

        public ConstructionTier next() {
            ConstructionTier[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static ConstructionTier fromOrdinal(int ord) {
            ConstructionTier[] values = values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return VANILLA;
        }

        public String getDetailedTooltip() {
            boolean de = I18n.getLanguage() == I18n.Language.DE;
            StringBuilder sb = new StringBuilder();
            if (de) {
                sb.append("AKTUELL: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Normaler Bauaufwand. Arbeiter benoetigen alle Materialien und Standardzeit.");
                        break;
                    case FREE_MATERIALS_KEEP_WORK:
                        sb.append("Gratis Materialien! Echte Arbeiter bauen animiert ohne Materialkosten (Schoenbau).");
                        break;
                    case FAST_AND_FREE:
                        sb.append("10-fache Baugeschwindigkeit bei null Materialkosten mit echten Arbeitern.");
                        break;
                    case INSTANT_BUILD:
                        sb.append("Sofortige Fertigstellung aller Raeume, Strassen und Mauern in der Platzierungssekunde.");
                        break;
                }
                sb.append("\n\nKlick schaltet zur naechsten Stufe:");
                sb.append("\n 1. ").append(I18n.get("tier.construction.vanilla")).append(" (Standard)");
                sb.append("\n 2. ").append(I18n.get("tier.construction.free_work")).append(" (Gratis Material)");
                sb.append("\n 3. ").append(I18n.get("tier.construction.fast_free")).append(" (10x Tempo & Gratis)");
                sb.append("\n 4. ").append(I18n.get("tier.construction.instant")).append(" (Sofortbau)");
            } else {
                sb.append("CURRENT: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Default construction rules. Workers require all materials and build time.");
                        break;
                    case FREE_MATERIALS_KEEP_WORK:
                        sb.append("Zero material cost! Workers build with animations (Living city).");
                        break;
                    case FAST_AND_FREE:
                        sb.append("10x building speed with zero resource costs and animated workers.");
                        break;
                    case INSTANT_BUILD:
                        sb.append("Instant completion of all rooms, roads, and walls upon placement.");
                        break;
                }
                sb.append("\n\nClick cycles to next tier:");
                sb.append("\n 1. ").append(I18n.get("tier.construction.vanilla")).append(" (Default)");
                sb.append("\n 2. ").append(I18n.get("tier.construction.free_work")).append(" (Free Materials)");
                sb.append("\n 3. ").append(I18n.get("tier.construction.fast_free")).append(" (10x Speed & Free)");
                sb.append("\n 4. ").append(I18n.get("tier.construction.instant")).append(" (Instant Build)");
            }
            return sb.toString();
        }
    }

    /**
     * Innovation and technology progression tiers.
     */
    public enum KnowledgeTier {
        VANILLA("tier.knowledge.vanilla"),
        NO_FORGETTING("tier.knowledge.no_forgetting"),
        OMNISCIENCE("tier.knowledge.omniscience");

        public final String i18nKey;

        KnowledgeTier(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public String getLabel() {
            return I18n.get(i18nKey);
        }

        public KnowledgeTier next() {
            KnowledgeTier[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static KnowledgeTier fromOrdinal(int ord) {
            KnowledgeTier[] values = values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return VANILLA;
        }

        public String getDetailedTooltip() {
            boolean de = I18n.getLanguage() == I18n.Language.DE;
            StringBuilder sb = new StringBuilder();
            if (de) {
                sb.append("AKTUELL: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Normaler Forschungsfortschritt. Wissen verfaellt bei zu wenigen Gelehrten.");
                        break;
                    case NO_FORGETTING:
                        sb.append("Erforschte Technologien gehen niemals verloren, selbst ohne Universitaeten.");
                        break;
                    case OMNISCIENCE:
                        sb.append("1.000.000 Innovationspunkte. Alle Technologien sind sofort freigeschaltet.");
                        break;
                }
                sb.append("\n\nKlick schaltet zur naechsten Stufe:");
                sb.append("\n 1. ").append(I18n.get("tier.knowledge.vanilla")).append(" (Standard)");
                sb.append("\n 2. ").append(I18n.get("tier.knowledge.no_forgetting")).append(" (Nie vergessen)");
                sb.append("\n 3. ").append(I18n.get("tier.knowledge.omniscience")).append(" (Allwissenheit)");
            } else {
                sb.append("CURRENT: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Default tech progression. Knowledge decays without enough scholars.");
                        break;
                    case NO_FORGETTING:
                        sb.append("Unlocked technologies never degrade or get forgotten.");
                        break;
                    case OMNISCIENCE:
                        sb.append("+1,000,000 innovation points. All tech trees are unlocked instantly.");
                        break;
                }
                sb.append("\n\nClick cycles to next tier:");
                sb.append("\n 1. ").append(I18n.get("tier.knowledge.vanilla")).append(" (Default)");
                sb.append("\n 2. ").append(I18n.get("tier.knowledge.no_forgetting")).append(" (No Forgetting)");
                sb.append("\n 3. ").append(I18n.get("tier.knowledge.omniscience")).append(" (Omniscience)");
            }
            return sb.toString();
        }
    }

    /**
     * Warehouse and stockpile logistics automation tiers.
     */
    public enum LogisticsTier {
        VANILLA("tier.logistics.vanilla"),
        RAW_MATERIALS_ONLY("tier.logistics.raw_only"),
        ALL_ASSIGNED_CRATES("tier.logistics.all_crates"),
        NO_DECREASE("tier.logistics.no_decrease");

        public final String i18nKey;

        LogisticsTier(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public String getLabel() {
            return I18n.get(i18nKey);
        }

        public LogisticsTier next() {
            LogisticsTier[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static LogisticsTier fromOrdinal(int ord) {
            LogisticsTier[] values = values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return VANILLA;
        }

        public String getDetailedTooltip() {
            boolean de = I18n.getLanguage() == I18n.Language.DE;
            StringBuilder sb = new StringBuilder();
            if (de) {
                sb.append("AKTUELL: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Lager fuellen sich nur durch normale Transport- und Ernteauftraege.");
                        break;
                    case RAW_MATERIALS_ONLY:
                        sb.append("Alle Rohstoffe (Holz, Stein, Erze, Kohle, Lehm) werden auf ca. 80% gehalten.");
                        break;
                    case ALL_ASSIGNED_CRATES:
                        sb.append("Jede zugewiesene Kiste kartenweit wird automatisch auf ca. 80% gefuellt.");
                        break;
                    case NO_DECREASE:
                        sb.append("Bodenlose Kisten: Waren muessen normal eingelagert werden, aber Entnahmen sind unbegrenzt gratis (Bestand sinkt nie).");
                        break;
                }
                sb.append("\n\nKlick schaltet zur naechsten Stufe:");
                sb.append("\n 1. ").append(I18n.get("tier.logistics.vanilla")).append(" (Standard)");
                sb.append("\n 2. ").append(I18n.get("tier.logistics.raw_only")).append(" (Rohstoffe ~80%)");
                sb.append("\n 3. ").append(I18n.get("tier.logistics.all_crates")).append(" (Alle Kisten ~80%)");
                sb.append("\n 4. ").append(I18n.get("tier.logistics.no_decrease")).append(" (Bodenlos / Kein Schwund)");
            } else {
                sb.append("CURRENT: ").append(getLabel()).append("\n");
                switch (this) {
                    case VANILLA:
                        sb.append("Stockpiles fill strictly through normal hauling and harvests.");
                        break;
                    case RAW_MATERIALS_ONLY:
                        sb.append("All raw materials (wood, stone, ores, coal, clay) are kept filled to ~80%.");
                        break;
                    case ALL_ASSIGNED_CRATES:
                        sb.append("Every assigned storage crate map-wide is kept topped up to ~80%.");
                        break;
                    case NO_DECREASE:
                        sb.append("Bottomless Crates: Goods must be hauled in normally, but pickup is infinite (stock never drops below stored amount).");
                        break;
                }
                sb.append("\n\nClick cycles to next tier:");
                sb.append("\n 1. ").append(I18n.get("tier.logistics.vanilla")).append(" (Default)");
                sb.append("\n 2. ").append(I18n.get("tier.logistics.raw_only")).append(" (Raw Materials ~80%)");
                sb.append("\n 3. ").append(I18n.get("tier.logistics.all_crates")).append(" (All Crates ~80%)");
                sb.append("\n 4. ").append(I18n.get("tier.logistics.no_decrease")).append(" (Bottomless / No Depletion)");
            }
            return sb.toString();
        }
    }

    /**
     * Resource yield behavior for instant clearing and demolition jobs.
     */
    public enum JobYieldTier {
        DROP_ON_GROUND("tier.job_yield.drop"),
        VOID_DISCARD("tier.job_yield.void"),
        STOCKPILE("tier.job_yield.stockpile");

        public final String i18nKey;

        JobYieldTier(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public String getLabel() {
            return I18n.get(i18nKey);
        }

        public JobYieldTier next() {
            JobYieldTier[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static JobYieldTier fromOrdinal(int ord) {
            JobYieldTier[] values = values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return DROP_ON_GROUND;
        }

        public String getDetailedTooltip() {
            boolean de = I18n.getLanguage() == I18n.Language.DE;
            StringBuilder sb = new StringBuilder();
            if (de) {
                sb.append("AKTUELL: ").append(getLabel()).append("\n");
                switch (this) {
                    case DROP_ON_GROUND:
                        sb.append("Ressourcen (Holz, Stein, Erze) spawnen wie gewohnt auf dem Boden am Abbauort zum spaeteren Abtransport.");
                        break;
                    case VOID_DISCARD:
                        sb.append("True Creative: Ressourcen werden vollstaendig verworfen und geloescht. Kein Muell, kein Boden-Chaos.");
                        break;
                    case STOCKPILE:
                        sb.append("Ressourcen werden sofort direkt dem staedtischen Hauptlager gutgeschrieben.");
                        break;
                }
                sb.append("\n\nKlick schaltet zur naechsten Stufe:");
                sb.append("\n- ").append(DROP_ON_GROUND.getLabel()).append(": Boden-Spawn (wie beim Abbauen)");
                sb.append("\n- ").append(VOID_DISCARD.getLabel()).append(": True Creative (einfach weg)");
                sb.append("\n- ").append(STOCKPILE.getLabel()).append(": Direkt ins Lager");
            } else {
                sb.append("CURRENT: ").append(getLabel()).append("\n");
                switch (this) {
                    case DROP_ON_GROUND:
                        sb.append("Resources (wood, stone, ore) drop onto the ground tiles at clearing sites as loose crates.");
                        break;
                    case VOID_DISCARD:
                        sb.append("True Creative: Resources vanish completely upon clearing. Zero floor clutter, zero mess.");
                        break;
                    case STOCKPILE:
                        sb.append("Resources are deposited directly into the city stockpile.");
                        break;
                }
                sb.append("\n\nClick to cycle to the next tier:");
                sb.append("\n- ").append(DROP_ON_GROUND.getLabel()).append(": Drop on ground (like vanilla)");
                sb.append("\n- ").append(VOID_DISCARD.getLabel()).append(": True Creative (discarded)");
                sb.append("\n- ").append(STOCKPILE.getLabel()).append(": Direct to stockpile");
            }
            return sb.toString();
        }
    }
}
