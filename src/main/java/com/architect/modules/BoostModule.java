package com.architect.modules;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.MaintenanceTier;
import com.architect.config.CheatTiers.MultiplierTier;

import game.boosting.BOOSTABLES;
import game.boosting.BSourceInfo;
import game.boosting.BoosterImp;
import game.faction.player.Player;
import init.sprite.UI.UI;

/**
 * Manages game-level stat multipliers and civic boosts.
 * Uses persistent Booster instances to maintain 100% zero-allocation during 60 Hz updates.
 */
public final class BoostModule implements ArchitectModule {

    private DynamicBooster innovationBooster;
    private DynamicBooster maintenanceBooster;
    private DynamicBooster spoilageBooster;
    private DynamicBooster loyaltyBooster;
    private DynamicBooster happinessBooster;

    private static class DynamicBooster extends BoosterImp {
        private double activeValue = 1.0;

        public DynamicBooster(BSourceInfo info, boolean isMul) {
            super(info, 1.0, isMul);
        }

        public void setValue(double val) {
            this.activeValue = val;
        }

        @Override
        public double getValue(double v) {
            return activeValue;
        }

        @Override
        public double vGet(Player player) {
            return activeValue;
        }
    }

    @Override
    public void init() {
        if (innovationBooster != null) {
            return;
        }

        BSourceInfo source = new BSourceInfo("Syx Architect", UI.icons().s.cog);

        innovationBooster = new DynamicBooster(source, true);
        maintenanceBooster = new DynamicBooster(source, true);
        spoilageBooster = new DynamicBooster(source, true);
        loyaltyBooster = new DynamicBooster(source, true);
        happinessBooster = new DynamicBooster(source, true);

        // Attach to core game boostables
        if (BOOSTABLES.CIVICS() != null) {
            if (BOOSTABLES.CIVICS().INNOVATION != null) {
                BOOSTABLES.CIVICS().INNOVATION.fGlobal.add(innovationBooster);
            }
            if (BOOSTABLES.CIVICS().MAINTENANCE != null) {
                BOOSTABLES.CIVICS().MAINTENANCE.fGlobal.add(maintenanceBooster);
            }
            if (BOOSTABLES.CIVICS().SPOILAGE != null) {
                BOOSTABLES.CIVICS().SPOILAGE.fGlobal.add(spoilageBooster);
            }
        }

        if (BOOSTABLES.BEHAVIOUR() != null) {
            if (BOOSTABLES.BEHAVIOUR().LOYALTY != null) {
                BOOSTABLES.BEHAVIOUR().LOYALTY.fGlobal.add(loyaltyBooster);
            }
            if (BOOSTABLES.BEHAVIOUR().HAPPI != null) {
                BOOSTABLES.BEHAVIOUR().HAPPI.fGlobal.add(happinessBooster);
            }
        }
    }

    @Override
    public void update(double ds, ArchitectState state) {
        // Zero allocation - boosters are evaluated reactively by the game's boosting engine
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        if (innovationBooster == null) {
            init();
        }

        // Apply Innovation multiplier
        innovationBooster.setValue(state.knowledgeTier.multiplier);

        // Apply Maintenance robustness (higher maintenance boost = lower degradation)
        if (state.maintenanceTier == MaintenanceTier.ZERO_DEGRADATION) {
            maintenanceBooster.setValue(100.0);
        } else if (state.maintenanceTier == MaintenanceTier.REDUCED) {
            maintenanceBooster.setValue(5.0);
        } else {
            maintenanceBooster.setValue(1.0);
        }

        // Apply Spoilage reduction (0.0 = no spoilage)
        spoilageBooster.setValue(state.spoilageDisabled ? 0.0 : 1.0);

        // Apply Loyalty / Happiness
        double loyaltyVal = state.loyaltyBoost ? 10.0 : 1.0;
        loyaltyBooster.setValue(loyaltyVal);
        happinessBooster.setValue(loyaltyVal);
    }
}
