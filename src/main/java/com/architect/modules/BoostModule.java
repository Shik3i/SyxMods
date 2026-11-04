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
 * Guards against duplicate booster injection on savegame reload and prevents
 * high-watermark pollution from temporary Omniscience boosts.
 */
public final class BoostModule implements ArchitectModule {

    private static DynamicBooster innovationBooster;
    private static DynamicBooster maintenanceBooster;
    private static DynamicBooster spoilageBooster;
    private static DynamicBooster loyaltyBooster;
    private static DynamicBooster happinessBooster;

    private double techCheckTimer = 0.0;
    private static final double TECH_CHECK_INTERVAL = 1.0;

    public static class DynamicBooster extends BoosterImp {
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
        if (innovationBooster == null) {
            BSourceInfo source = new BSourceInfo("Syx Architect", UI.icons().s.cog);
            innovationBooster = new DynamicBooster(source, true);
            maintenanceBooster = new DynamicBooster(source, true);
            spoilageBooster = new DynamicBooster(source, true);
            loyaltyBooster = new DynamicBooster(source, true);
            happinessBooster = new DynamicBooster(source, true);
        }

        // Attach to core game boostables if not already present
        if (BOOSTABLES.CIVICS() != null) {
            if (BOOSTABLES.CIVICS().INNOVATION != null && !BOOSTABLES.CIVICS().INNOVATION.fGlobal.contains(innovationBooster)) {
                BOOSTABLES.CIVICS().INNOVATION.fGlobal.add(innovationBooster);
            }
            if (BOOSTABLES.CIVICS().MAINTENANCE != null && !BOOSTABLES.CIVICS().MAINTENANCE.fGlobal.contains(maintenanceBooster)) {
                BOOSTABLES.CIVICS().MAINTENANCE.fGlobal.add(maintenanceBooster);
            }
            if (BOOSTABLES.CIVICS().SPOILAGE != null && !BOOSTABLES.CIVICS().SPOILAGE.fGlobal.contains(spoilageBooster)) {
                BOOSTABLES.CIVICS().SPOILAGE.fGlobal.add(spoilageBooster);
            }
        }

        if (BOOSTABLES.BEHAVIOUR() != null) {
            if (BOOSTABLES.BEHAVIOUR().LOYALTY != null && !BOOSTABLES.BEHAVIOUR().LOYALTY.fGlobal.contains(loyaltyBooster)) {
                BOOSTABLES.BEHAVIOUR().LOYALTY.fGlobal.add(loyaltyBooster);
            }
            if (BOOSTABLES.BEHAVIOUR().HAPPI != null && !BOOSTABLES.BEHAVIOUR().HAPPI.fGlobal.contains(happinessBooster)) {
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

    /**
     * Prevents high-watermark pollution from temporary Omniscience:
     * We measure allocated tech points (the actual cost of technologies unlocked)
     * rather than raw total boosted points.
     */
    private void checkHighWatermark(ArchitectState state) {
        try {
            if (GAME.player() != null && GAME.player().tech != null) {
                PTech tech = GAME.player().tech;
                if (tech.currs() != null && tech.currs().size() > 0) {
                    PTech.TechCurr innovationCurr = tech.currs().get(0);
                    int allocatedPoints = innovationCurr.allocated();
                    
                    // Track only legitimate allocated tech costs
                    if (allocatedPoints > state.highWatermarkInnovation) {
                        state.highWatermarkInnovation = allocatedPoints;
                    }

                    int currentTotal = innovationCurr.total();
                    if (state.highWatermarkInnovation > 0 && currentTotal < state.highWatermarkInnovation) {
                        // Compensate so unlocked technologies are never forgotten due to laboratory worker shifts
                        double ratio = (double) state.highWatermarkInnovation / Math.max(1.0, (double) currentTotal);
                        innovationBooster.setValue(ratio);
                    } else if (state.knowledgeTier != KnowledgeTier.OMNISCIENCE) {
                        innovationBooster.setValue(1.0);
                    }
                }
            }
        } catch (Exception ignored) {
            // Protected against uninitialized game states
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
