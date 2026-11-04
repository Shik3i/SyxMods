package com.architect.config;

import com.architect.util.I18n;

/**
 * Semantic creative and sandbox tiers decoupling work/simulation from material costs.
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
    }

    /**
     * Warehouse and stockpile logistics automation tiers.
     */
    public enum LogisticsTier {
        VANILLA("tier.logistics.vanilla"),
        RAW_MATERIALS_ONLY("tier.logistics.raw_only"),
        ALL_ASSIGNED_CRATES("tier.logistics.all_crates");

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
    }
}
