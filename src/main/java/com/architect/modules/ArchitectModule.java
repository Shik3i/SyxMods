package com.architect.modules;

import com.architect.config.ArchitectState;

/**
 * Common lifecycle interface for modular sandbox subsystems.
 */
public interface ArchitectModule {
    /**
     * Called once during instance initialization or post-load.
     */
    void init();

    /**
     * Called on each game simulation tick (60 Hz).
     * Implementations must follow the zero-allocation rule: no new object creations!
     *
     * @param ds In-game delta time elapsed in seconds.
     * @param state Active persistent configuration state.
     */
    void update(double ds, ArchitectState state);

    /**
     * Called whenever a setting or cheat tier is modified via the UI.
     */
    void onStateChanged(ArchitectState state);
}
