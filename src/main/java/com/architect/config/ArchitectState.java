package com.architect.config;

import java.io.IOException;

import com.architect.config.CheatTiers.MaintenanceTier;
import com.architect.config.CheatTiers.MultiplierTier;
import com.architect.util.I18n;

import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

/**
 * Savegame-persistent state container for Syx Architect.
 * Serializes and deserializes cleanly using Songs of Syx FilePutter/FileGetter protocol.
 */
public final class ArchitectState {

    public static final int MAGIC_HEADER = 0x59584152; // 'SXAR'
    public static final int CURRENT_VERSION = 1;

    // Cheat and sandbox settings
    public MultiplierTier knowledgeTier = MultiplierTier.OFF;
    public MaintenanceTier maintenanceTier = MaintenanceTier.VANILLA;
    public boolean spoilageDisabled = false;
    public boolean loyaltyBoost = false;
    public boolean instantBuildActive = false;
    public MultiplierTier infiniteStorageTier = MultiplierTier.OFF;
    public boolean raidImmunity = false;
    public boolean battleAutoWin = false;

    // Preferences
    public String language = "en";

    public ArchitectState() {}

    /**
     * Resets all cheats and configurations back to clean vanilla defaults.
     */
    public void resetToDefaults() {
        knowledgeTier = MultiplierTier.OFF;
        maintenanceTier = MaintenanceTier.VANILLA;
        spoilageDisabled = false;
        loyaltyBoost = false;
        instantBuildActive = false;
        infiniteStorageTier = MultiplierTier.OFF;
        raidImmunity = false;
        battleAutoWin = false;
        language = "en";
        I18n.setLanguage(I18n.Language.EN);
    }

    /**
     * Serializes state to the Songs of Syx binary savegame stream.
     */
    public void save(FilePutter file) {
        file.i(MAGIC_HEADER);
        file.i(CURRENT_VERSION);

        file.i(knowledgeTier.ordinal());
        file.i(maintenanceTier.ordinal());
        file.bool(spoilageDisabled);
        file.bool(loyaltyBoost);
        file.bool(instantBuildActive);
        file.i(infiniteStorageTier.ordinal());
        file.bool(raidImmunity);
        file.bool(battleAutoWin);

        file.chars(language != null ? language : "en");
    }

    /**
     * Deserializes state from the Songs of Syx binary savegame stream.
     */
    public void load(FileGetter file) throws IOException {
        int magic = file.i();
        if (magic != MAGIC_HEADER) {
            // Not a Syx Architect stream or corrupt header, reset to defaults
            resetToDefaults();
            return;
        }

        int version = file.i();
        if (version >= 1) {
            knowledgeTier = MultiplierTier.fromOrdinal(file.i());
            maintenanceTier = MaintenanceTier.fromOrdinal(file.i());
            spoilageDisabled = file.bool();
            loyaltyBoost = file.bool();
            instantBuildActive = file.bool();
            infiniteStorageTier = MultiplierTier.fromOrdinal(file.i());
            raidImmunity = file.bool();
            battleAutoWin = file.bool();

            language = file.chars();
            I18n.setLanguage(I18n.Language.fromCode(language));
        }
    }
}
