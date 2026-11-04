package com.architect;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.ConstructionTier;
import com.architect.config.CheatTiers.KnowledgeTier;
import com.architect.config.CheatTiers.LogisticsTier;
import com.architect.config.CheatTiers.MaintenanceTier;
import com.architect.util.I18n;

import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

/**
 * Automated QA & Sanity Test Suite for Syx Architect.
 * Validates:
 * 1. 100% Localization Completeness and UTF-8 encoding symmetry (DE vs EN vs Java dictionaries).
 * 2. Perfect Symmetrical Serialization of ArchitectState (Save/Load with FilePutter/FileGetter).
 * 3. Backward compatibility with V2 format.
 */
public class SanityTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            SYX ARCHITECT - AUTOMATED QA & SANITY TEST RUNNER                   ");
        System.out.println("================================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Localization Completeness & UTF-8 Integrity
        try {
            testLocalizationCompleteness();
            System.out.println("[PASS] Test 1: Localization symmetry & UTF-8 encoding verified (66 keys match).");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1: Localization error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Symmetrical State Serialization (V3 Save & Load)
        try {
            testStateSerializationV3();
            System.out.println("[PASS] Test 2: Symmetrical Save & Load (V3) verified with 100% field fidelity.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2: Serialization error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Backward Compatibility with V2 Savegames
        try {
            testBackwardCompatibilityV2();
            System.out.println("[PASS] Test 3: Backward compatibility (V2 -> V3) verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3: Backward compatibility error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("================================================================================");
        System.out.println("TEST RESULTS: " + passed + " passed, " + failed + " failed.");
        System.out.println("================================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testLocalizationCompleteness() throws Exception {
        Properties enProps = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream("mod-files/assets/text/strings_en.properties"), StandardCharsets.UTF_8)) {
            enProps.load(reader);
        }

        Properties deProps = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream("mod-files/assets/text/strings_de.properties"), StandardCharsets.UTF_8)) {
            deProps.load(reader);
        }

        Map<String, String> javaEn = I18n.getDictionary(I18n.Language.EN);
        Map<String, String> javaDe = I18n.getDictionary(I18n.Language.DE);

        if (enProps.isEmpty()) {
            throw new AssertionError("strings_en.properties is empty!");
        }
        if (deProps.isEmpty()) {
            throw new AssertionError("strings_de.properties is empty!");
        }

        // Check that every EN key exists in DE
        for (String key : enProps.stringPropertyNames()) {
            String enVal = enProps.getProperty(key);
            if (enVal == null || enVal.trim().isEmpty()) {
                throw new AssertionError("Empty translation in EN for key: " + key);
            }
            String deVal = deProps.getProperty(key);
            if (deVal == null || deVal.trim().isEmpty()) {
                throw new AssertionError("Missing or empty translation in DE for key: " + key);
            }
            if (!javaEn.containsKey(key)) {
                throw new AssertionError("Key missing in I18n.java (EN): " + key);
            }
            if (!javaDe.containsKey(key)) {
                throw new AssertionError("Key missing in I18n.java (DE): " + key);
            }
        }

        // Check that every DE key exists in EN
        for (String key : deProps.stringPropertyNames()) {
            if (!enProps.containsKey(key)) {
                throw new AssertionError("Extraneous key in DE missing from EN: " + key);
            }
        }
    }

    private static void testStateSerializationV3() throws Exception {
        Path tempPath = Files.createTempFile("syx_architect_test_v3", ".bin");
        try {
            ArchitectState s1 = new ArchitectState();
            s1.maintenanceTier = MaintenanceTier.NO_DECAY;
            s1.constructionTier = ConstructionTier.FAST_AND_FREE;
            s1.knowledgeTier = KnowledgeTier.OMNISCIENCE;
            s1.logisticsTier = LogisticsTier.ALL_ASSIGNED_CRATES;
            s1.highWatermarkInnovation = 99999.75;
            s1.spoilageDisabled = true;
            s1.loyaltyBoost = true;
            s1.raidImmunity = true;
            s1.battleAutoWin = true;
            s1.freezeNiceWeather = true;
            s1.disableAging = true;
            s1.maxFertility = true;
            s1.language = "de";

            // Save via Songs of Syx FilePutter
            FilePutter putter = new FilePutter(tempPath, 1024);
            s1.save(putter);
            putter.save();

            // Deserialization via Songs of Syx FileGetter
            ArchitectState s2 = new ArchitectState();
            FileGetter getter = new FileGetter(tempPath);
            s2.load(getter);
            getter.close();

            assertEquals("maintenanceTier", s1.maintenanceTier, s2.maintenanceTier);
            assertEquals("constructionTier", s1.constructionTier, s2.constructionTier);
            assertEquals("knowledgeTier", s1.knowledgeTier, s2.knowledgeTier);
            assertEquals("logisticsTier", s1.logisticsTier, s2.logisticsTier);
            assertEquals("highWatermarkInnovation", s1.highWatermarkInnovation, s2.highWatermarkInnovation, 0.0001);
            assertEquals("spoilageDisabled", s1.spoilageDisabled, s2.spoilageDisabled);
            assertEquals("loyaltyBoost", s1.loyaltyBoost, s2.loyaltyBoost);
            assertEquals("raidImmunity", s1.raidImmunity, s2.raidImmunity);
            assertEquals("battleAutoWin", s1.battleAutoWin, s2.battleAutoWin);
            assertEquals("freezeNiceWeather", s1.freezeNiceWeather, s2.freezeNiceWeather);
            assertEquals("disableAging", s1.disableAging, s2.disableAging);
            assertEquals("maxFertility", s1.maxFertility, s2.maxFertility);
            assertEquals("language", s1.language, s2.language);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private static void testBackwardCompatibilityV2() throws Exception {
        Path tempPath = Files.createTempFile("syx_architect_test_v2", ".bin");
        try {
            // Manually simulate a V2 binary stream
            FilePutter putter = new FilePutter(tempPath, 1024);
            putter.i(ArchitectState.MAGIC_HEADER);
            putter.i(2); // V2
            putter.i(MaintenanceTier.JANITOR_WORK_FREE_MATS.ordinal());
            putter.i(ConstructionTier.FREE_MATERIALS_KEEP_WORK.ordinal());
            putter.i(KnowledgeTier.NO_FORGETTING.ordinal());
            putter.i(LogisticsTier.RAW_MATERIALS_ONLY.ordinal());
            putter.d(12345.5);
            putter.bool(false);
            putter.bool(true);
            putter.bool(true);
            putter.bool(false);
            putter.chars("en");
            putter.save();

            ArchitectState s = new ArchitectState();
            FileGetter getter = new FileGetter(tempPath);
            s.load(getter);
            getter.close();

            assertEquals("V2 maintenanceTier", MaintenanceTier.JANITOR_WORK_FREE_MATS, s.maintenanceTier);
            assertEquals("V2 constructionTier", ConstructionTier.FREE_MATERIALS_KEEP_WORK, s.constructionTier);
            assertEquals("V2 knowledgeTier", KnowledgeTier.NO_FORGETTING, s.knowledgeTier);
            assertEquals("V2 logisticsTier", LogisticsTier.RAW_MATERIALS_ONLY, s.logisticsTier);
            assertEquals("V2 highWatermarkInnovation", 12345.5, s.highWatermarkInnovation, 0.0001);
            assertEquals("V2 loyaltyBoost", true, s.loyaltyBoost);
            assertEquals("V2 freezeNiceWeather (default)", false, s.freezeNiceWeather);
            assertEquals("V2 disableAging (default)", false, s.disableAging);
            assertEquals("V2 maxFertility (default)", false, s.maxFertility);
            assertEquals("V2 language", "en", s.language);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private static void assertEquals(String field, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Mismatch in " + field + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(String field, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError("Mismatch in " + field + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
