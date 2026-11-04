package com.architect.modules;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.KnowledgeTier;
import com.architect.config.CheatTiers.LogisticsTier;
import com.architect.config.CheatTiers.MaintenanceTier;

import game.GAME;
import game.boosting.BOOSTABLES;
import game.boosting.BSourceInfo;
import game.boosting.BoosterImp;
import game.faction.player.PTech;
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

    private double techCheckTimer = 0.0;
    private static final double TECH_CHECK_INTERVAL = 1.0;

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
        // High-watermark check for NO_FORGETTING (every 1s)
        if (state.knowledgeTier == KnowledgeTier.NO_FORGETTING) {
            techCheckTimer += ds;
            if (techCheckTimer >= TECH_CHECK_INTERVAL) {
                techCheckTimer = 0.0;
                checkHighWatermark(state);
            }
        }
    }

    private void checkHighWatermark(ArchitectState state) {
        try {
            if (GAME.player() != null && GAME.player().tech != null) {
                PTech tech = GAME.player().tech;
                if (tech.currs() != null && tech.currs().size() > 0) {
                    PTech.TechCurr innovationCurr = tech.currs().get(0);
                    int currentTotal = innovationCurr.total();
                    if (currentTotal > state.highWatermarkInnovation) {
                        state.highWatermarkInnovation = currentTotal;
                        innovationBooster.setValue(1.0);
                    } else if (state.highWatermarkInnovation > 0 && currentTotal < state.highWatermarkInnovation) {
                        // Compensate the difference so techs are never forgotten
                        double ratio = state.highWatermarkInnovation / Math.max(1.0, (double) currentTotal);
                        innovationBooster.setValue(ratio);
                    }
                }
            }
        } catch (Exception ignored) {
            // Safe guard against uninitialized game states
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        if (innovationBooster == null) {
            init();
        }

        // 1. Knowledge Tier
        if (state.knowledgeTier == KnowledgeTier.OMNISCIENCE) {
            innovationBooster.setValue(1_000_000.0);
        } else if (state.knowledgeTier == KnowledgeTier.NO_FORGETTING) {
            checkHighWatermark(state);
        } else {
            innovationBooster.setValue(1.0);
        }

        // 2. Maintenance Tier
        if (state.maintenanceTier == MaintenanceTier.NO_DECAY) {
            maintenanceBooster.setValue(100.0);
        } else {
            // Vanilla or JANITOR_WORK_FREE_MATS (let buildings age naturally so janitors walk around)
            maintenanceBooster.setValue(1.0);
        }

        // 3. Spoilage (Disabled if explicitly checked OR if ALL_ASSIGNED_CRATES is on)
        boolean noSpoil = state.spoilageDisabled || (state.logisticsTier == LogisticsTier.ALL_ASSIGNED_CRATES);
        spoilageBooster.setValue(noSpoil ? 0.0 : 1.0);

        // 4. Loyalty & Happiness
        double loyaltyVal = state.loyaltyBoost ? 10.0 : 1.0;
        loyaltyBooster.setValue(loyaltyVal);
        happinessBooster.setValue(loyaltyVal);
    }
}
