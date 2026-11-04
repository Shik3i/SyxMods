package com.architect.modules;

import com.architect.config.ArchitectState;

import game.boosting.BOOSTABLES;
import game.boosting.BSourceInfo;
import init.race.RACES;
import init.race.Race;
import init.sprite.UI.UI;
import init.type.HTYPES;
import settlement.main.SETT;
import settlement.weather.SWEATHER;
import snake2d.util.sets.LIST;

/**
 * World, Environment, and Population management subsystem.
 * Supports:
 * - Instant immigration waves (spawning citizens by species or all playable races)
 * - Anti-Aging / Longevity lock (neutralizes deaths by old age)
 * - Clear Building Weather & Daylight lock (ideal lighting for screenshots and construction)
 * - Soil Fertility & Moisture maximization (total freedom for farms and orchards)
 *
 * PERFORMANCE:
 * Background moisture synchronization is chunked into 48-row slices to prevent frame lag.
 */
public final class WorldModule implements ArchitectModule {

    private static BoostModule.DynamicBooster deathAgeBooster;

    private double weatherTimer = 0.0;
    private static final double WEATHER_INTERVAL = 0.5;

    private double fertilityTimer = 0.0;
    private static final double FERTILITY_INTERVAL = 0.1; // Smooth rolling slice check
    private int moistureRowCursor = 0;
    private static final int MOISTURE_ROWS_PER_TICK = 48;

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
        if (deathAgeBooster == null) {
            snake2d.util.sprite.SPRITE icon = (UI.icons() != null && UI.icons().s != null) ? UI.icons().s.death : DUMMY_SPRITE;
            BSourceInfo source = new BSourceInfo("Syx Architect", icon);
            deathAgeBooster = new BoostModule.DynamicBooster(source, true);
        }

        try {
            if (BOOSTABLES.PHYSICS() != null && BOOSTABLES.PHYSICS().DEATH_AGE != null) {
                if (!BOOSTABLES.PHYSICS().DEATH_AGE.contains(deathAgeBooster)) {
                    BOOSTABLES.PHYSICS().DEATH_AGE.addFactor(new game.boosting.BoostSpec(deathAgeBooster, BOOSTABLES.PHYSICS().DEATH_AGE, null));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void update(double ds, ArchitectState state) {
        if (!SETT.exists()) {
            return;
        }

        // 1. Weather & Daylight freezing
        if (state.freezeNiceWeather) {
            weatherTimer += ds;
            if (weatherTimer >= WEATHER_INTERVAL) {
                weatherTimer = 0.0;
                applyNiceWeather();
            }
        }

        // 2. Continuous soil moisture enforcement (chunked rolling slice)
        if (state.maxFertility) {
            fertilityTimer += ds;
            if (fertilityTimer >= FERTILITY_INTERVAL) {
                fertilityTimer = 0.0;
                updateSoilMoistureSlice();
            }
        }
    }

    /**
     * Locks clear weather, 0 clouds, 0 rain/snow, and pleasant neutral daylight.
     */
    public void applyNiceWeather() {
        if (!SETT.exists() || SETT.WEATHER() == null) {
            return;
        }
        try {
            SWEATHER weather = SETT.WEATHER();
            if (weather.clouds != null) {
                weather.clouds.setD(0.0);
                weather.clouds.setTarget(0.0);
            }
            if (weather.rain != null) {
                weather.rain.setD(0.0);
                weather.rain.setTarget(0.0);
            }
            if (weather.snow != null) {
                weather.snow.setD(0.0);
            }
            if (weather.lightColor() != null) {
                weather.lightColor().set(1.0, 1.0, 1.0);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Budgeted row slice scan for continuous soil moisture synchronization.
     */
    private void updateSoilMoistureSlice() {
        if (!SETT.exists() || SETT.GROUND() == null || SETT.GROUND().MOISTURE_CURRENT == null) {
            return;
        }
        try {
            int width = SETT.TWIDTH;
            int height = SETT.THEIGHT;
            int startY = moistureRowCursor;
            int endY = Math.min(height, startY + MOISTURE_ROWS_PER_TICK);

            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < width; x++) {
                    if (SETT.GROUND().MOISTURE_CURRENT.get(x, y) < 1.0) {
                        SETT.GROUND().MOISTURE_CURRENT.set(x, y, 1.0);
                    }
                }
            }
            moistureRowCursor = (endY >= height) ? 0 : endY;
        } catch (Exception ignored) {
        }
    }

    /**
     * Maximizes soil moisture map-wide immediately (for 1-click action or toggle-on).
     */
    public void maximizeSoilMoistureNow() {
        if (!SETT.exists() || SETT.GROUND() == null || SETT.GROUND().MOISTURE_CURRENT == null) {
            return;
        }
        try {
            int width = SETT.TWIDTH;
            int height = SETT.THEIGHT;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (SETT.GROUND().MOISTURE_CURRENT.get(x, y) < 1.0) {
                        SETT.GROUND().MOISTURE_CURRENT.set(x, y, 1.0);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Spawns an immediate immigration wave for a specific race.
     */
    public void spawnImmigrants(Race race, int count) {
        if (!SETT.exists() || SETT.ENTRY() == null || race == null || count <= 0) {
            return;
        }
        try {
            SETT.ENTRY().add(race, HTYPES.SUBJECT(), count);
        } catch (Exception ignored) {
        }
    }

    /**
     * Spawns equal immigration waves for all playable races (Zero-allocation indexed loop).
     */
    public void spawnAllPlayable(int countPerRace) {
        if (!SETT.exists() || SETT.ENTRY() == null || countPerRace <= 0) {
            return;
        }
        try {
            LIST<Race> playable = RACES.playable();
            if (playable != null) {
                int count = playable.size();
                for (int i = 0; i < count; i++) {
                    Race race = playable.get(i);
                    if (race != null) {
                        SETT.ENTRY().add(race, HTYPES.SUBJECT(), countPerRace);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStateChanged(ArchitectState state) {
        if (deathAgeBooster == null) {
            init();
        }

        // Anti-Aging: 1000x life expectancy completely eliminates deaths by old age
        deathAgeBooster.setValue(state.disableAging ? 1000.0 : 1.0);

        if (state.freezeNiceWeather) {
            applyNiceWeather();
        }
        if (state.maxFertility) {
            maximizeSoilMoistureNow();
        }
    }
}
