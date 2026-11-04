package com.architect.config;

import com.architect.util.I18n;

/**
 * Multi-tier configurations for cheats and creative features.
 */
public final class CheatTiers {

    private CheatTiers() {}

    public enum MultiplierTier {
        OFF(1.0, "tier.off"),
        NORMAL(2.0, "tier.normal"),
        HIGH(5.0, "tier.high"),
        EXTREME(10.0, "tier.extreme"),
        GODLIKE(100.0, "tier.god");

        public final double multiplier;
        public final String i18nKey;

        MultiplierTier(double multiplier, String i18nKey) {
            this.multiplier = multiplier;
            this.i18nKey = i18nKey;
        }

        public String getLabel() {
            return I18n.get(i18nKey);
        }

        public MultiplierTier next() {
            MultiplierTier[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static MultiplierTier fromOrdinal(int ord) {
            MultiplierTier[] values = values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return OFF;
        }
    }

    public enum MaintenanceTier {
        VANILLA(1.0, "tier.off"),
        REDUCED(0.2, "tier.normal"),
        ZERO_DEGRADATION(0.0, "tier.god");

        public final double degradeFactor;
        public final String i18nKey;

        MaintenanceTier(double degradeFactor, String i18nKey) {
            this.degradeFactor = degradeFactor;
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
}
