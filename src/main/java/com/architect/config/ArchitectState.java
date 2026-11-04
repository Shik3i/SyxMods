package com.architect.config;

import java.io.IOException;

import com.architect.config.CheatTiers.ConstructionTier;
import com.architect.config.CheatTiers.KnowledgeTier;
import com.architect.config.CheatTiers.LogisticsTier;
import com.architect.config.CheatTiers.MaintenanceTier;
import com.architect.util.I18n;

import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

/**
 * Savegame-persistent state container for Syx Architect.
 * Serializes and deserializes cleanly using Songs of Syx FilePutter/FileGetter protocol.
 */
public final class ArchitectState {

    public static final int MAGIC_HEADER = 0x59584152; // 'SXAR'
    public static final int CURRENT_VERSION = 2;

    // Semantic Schönbau and sandbox settings
    public MaintenanceTier maintenanceTier = MaintenanceTier.VANILLA;
    public ConstructionTier constructionTier = ConstructionTier.VANILLA;
    public KnowledgeTier knowledgeTier = KnowledgeTier.VANILLA;
    public LogisticsTier logisticsTier = LogisticsTier.VANILLA;

    // High-watermark for No-Forgetting research lock
    public double highWatermarkInnovation = 0.0;

    // General toggles
    public boolean spoilageDisabled = false;
    public boolean loyaltyBoost = false;
    public boolean raidImmunity = false;
    public boolean battleAutoWin = false;

    // Preferences
    public String language = "en";

    public ArchitectState() {}

    /**
     * Resets all cheats and configurations back to clean vanilla defaults.
     */
    public void resetToDefaults() {
        maintenanceTier = MaintenanceTier.VANILLA;
        constructionTier = ConstructionTier.VANILLA;
        knowledgeTier = KnowledgeTier.VANILLA;
        logisticsTier = LogisticsTier.VANILLA;
        highWatermarkInnovation = 0.0;
        spoilageDisabled = false;
        loyaltyBoost = false;
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

        file.i(maintenanceTier.ordinal());
        file.i(constructionTier.ordinal());
        file.i(knowledgeTier.ordinal());
        file.i(logisticsTier.ordinal());

        file.d(highWatermarkInnovation);

        file.bool(spoilageDisabled);
        file.bool(loyaltyBoost);
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
            resetToDefaults();
            return;
        }

        int version = file.i();
        if (version >= 2) {
            maintenanceTier = MaintenanceTier.fromOrdinal(file.i());
            constructionTier = ConstructionTier.fromOrdinal(file.i());
            knowledgeTier = KnowledgeTier.fromOrdinal(file.i());
            logisticsTier = LogisticsTier.fromOrdinal(file.i());

            highWatermarkInnovation = file.d();

            spoilageDisabled = file.bool();
            loyaltyBoost = file.bool();
            raidImmunity = file.bool();
            battleAutoWin = file.bool();

            language = file.chars();
            I18n.setLanguage(I18n.Language.fromCode(language));
        } else if (version == 1) {
            // Version 1 backward-compatibility fallback
            file.i(); // old knowledge
            file.i(); // old maintenance
            file.bool(); // spoilage
            file.bool(); // loyalty
            boolean oldInstant = file.bool();
            if (oldInstant) {
                constructionTier = ConstructionTier.INSTANT_BUILD;
            }
            file.i(); // storage tier
            file.bool();
            file.bool();
            language = file.chars();
            I18n.setLanguage(I18n.Language.fromCode(language));
        }
    }
}
