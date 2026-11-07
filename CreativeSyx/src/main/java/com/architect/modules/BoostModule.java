package com.architect.modules;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.KnowledgeTier;
import com.architect.config.CheatTiers.LogisticsTier;
import com.architect.config.CheatTiers.MaintenanceTier;

import game.GAME;
import game.boosting.BOOSTABLES;
import game.boosting.BSourceInfo;
import game.boosting.BoostSpec;
import game.boosting.Boostable;
import game.boosting.Booster;
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

    private long lastTechCheckMs = 0;
    private static final long TECH_CHECK_INTERVAL_MS = 1500; // 1.5 real seconds

    public static class DynamicBooster extends BoosterImp {
        private double activeValue;

        public DynamicBooster(BSourceInfo info, boolean isMul) {
            this(info, isMul ? 1.0 : 0.0, isMul);
        }

        public DynamicBooster(BSourceInfo info, double initialValue, boolean isMul) {
            super(info, initialValue, isMul);
            this.activeValue = initialValue;
        }

        public void setValue(double val) {
            this.activeValue = val;
        }

        public double getValue() {
            return activeValue;
        }

        @Override
        public double getValue(double v) {
            return activeValue;
        }

        @Override
        public double vGet(Player player) {
            return activeValue;
        }

        @Override
        public double vGet(init.type.HCLASS_RACE hclass) {
            return activeValue;
        }

        @Override
        public double vGet(world.map.regions.Region region) {
            return activeValue;
        }

        @Override
        public double vGet(settlement.stats.Induvidual ind) {
            return activeValue;
        }

        @Override
        public double vGet(game.battle.div.Div div) {
            return activeValue;
        }

        @Override
        public double vGet(game.faction.npc.FactionNPC npc) {
            return activeValue;
        }
    }

    private static void registerBooster(Boostable boostable, Booster booster) {
        if (boostable != null && booster != null && !boostable.contains(booster)) {
            boostable.addFactor(new BoostSpec(booster, boostable, null));
        }
    }

    private void ensureBoostersRegistered() {
        if (innovationBooster == null) {
            init();
        }

        try {
            if (BOOSTABLES.CIVICS() != null) {
                registerBooster(BOOSTABLES.CIVICS().INNOVATION, innovationBooster);
                registerBooster(BOOSTABLES.CIVICS().MAINTENANCE, maintenanceBooster);
                registerBooster(BOOSTABLES.CIVICS().SPOILAGE, spoilageBooster);
            }
        } catch (Throwable ignored) {
        }

        try {
            if (BOOSTABLES.BEHAVIOUR() != null) {
                registerBooster(BOOSTABLES.BEHAVIOUR().LOYALTY, loyaltyBooster);
                registerBooster(BOOSTABLES.BEHAVIOUR().HAPPI, happinessBooster);
            }
        } catch (Throwable ignored) {
        }

        try {
            if (GAME.player() != null && GAME.player().tech != null && GAME.player().tech.currs() != null) {
                for (int i = 0; i < GAME.player().tech.currs().size(); i++) {
                    PTech.TechCurr tc = GAME.player().tech.currs().get(i);
                    if (tc != null && tc.cu != null && tc.cu.bo != null) {
                        registerBooster(tc.cu.bo, innovationBooster);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static final snake2d.util.sprite.SPRITE DUMMY_SPRITE = new snake2d.util.sprite.SPRITE() {
        @Override
        public void render(snake2d.SPRITE_RENDERER r, int x1, int x2, int y1, int y2) {}
        @Override
        public void renderTextured(snake2d.util.sprite.TextureCoords t, int x1, int x2, int y1, int y2) {}
        @Override
        public int width() { return 16; }
        @Override
        public int height() { return 16; }
    };

    @Override
    public void init() {
        if (innovationBooster == null) {
            snake2d.util.sprite.SPRITE icon = (UI.icons() != null && UI.icons().s != null) ? UI.icons().s.cog : DUMMY_SPRITE;
            BSourceInfo source = new BSourceInfo("Syx Architect", icon);
            // Innovation is ADDITIVE (isMul = false) so it adds research points directly even with 0 labs!
            innovationBooster = new DynamicBooster(source, 0.0, false);
            maintenanceBooster = new DynamicBooster(source, 1.0, true);
            spoilageBooster = new DynamicBooster(source, 1.0, true);
            // Loyalty is ADDITIVE (isMul = false) because base value is 0.0 (+1.0 = +100%)
            loyaltyBooster = new DynamicBooster(source, 0.0, false);
            happinessBooster = new DynamicBooster(source, 1.0, true);
        }

        ensureBoostersRegistered();
    }

    @Override
    public void update(double ds, ArchitectState state) {
        long now = System.currentTimeMillis();
        if (now - lastTechCheckMs >= TECH_CHECK_INTERVAL_MS) {
            lastTechCheckMs = now;
            ensureBoostersRegistered();
            if (state.knowledgeTier == KnowledgeTier.NO_FORGETTING) {
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
                    int baseTotal = (int) Math.max(0, currentTotal - innovationBooster.getValue());
                    if (state.highWatermarkInnovation > 0 && baseTotal < state.highWatermarkInnovation) {
                        // Compensate so unlocked technologies are never forgotten due to laboratory worker shifts
                        double needed = (double) (state.highWatermarkInnovation - baseTotal);
                        innovationBooster.setValue(needed);
                    } else if (state.knowledgeTier != KnowledgeTier.OMNISCIENCE) {
                        innovationBooster.setValue(0.0);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Protected against uninitialized game states
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        ensureBoostersRegistered();

        // 1. Knowledge Tier
        if (state.knowledgeTier == KnowledgeTier.OMNISCIENCE) {
            innovationBooster.setValue(1_000_000.0);
        } else if (state.knowledgeTier == KnowledgeTier.NO_FORGETTING) {
            checkHighWatermark(state);
        } else {
            innovationBooster.setValue(0.0);
        }

        // 2. Maintenance Tier
        if (state.maintenanceTier == MaintenanceTier.NO_DECAY) {
            maintenanceBooster.setValue(100.0);
        } else {
            // Vanilla or JANITOR_WORK_FREE_MATS (let buildings age naturally so janitors walk around)
            maintenanceBooster.setValue(1.0);
        }

        // 3. Spoilage (Disabled if explicitly checked OR if ALL_ASSIGNED_CRATES / NO_DECREASE is on)
        boolean noSpoil = state.spoilageDisabled || (state.logisticsTier == LogisticsTier.ALL_ASSIGNED_CRATES) || (state.logisticsTier == LogisticsTier.NO_DECREASE);
        spoilageBooster.setValue(noSpoil ? 0.0 : 1.0);

        // 4. Loyalty & Happiness
        loyaltyBooster.setValue(state.loyaltyBoost ? 10.0 : 0.0);
        happinessBooster.setValue(state.loyaltyBoost ? 10.0 : 1.0);
    }
}
