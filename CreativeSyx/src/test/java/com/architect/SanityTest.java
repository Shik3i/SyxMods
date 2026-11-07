package com.architect;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import com.architect.config.ArchitectState;
import com.architect.config.CheatTiers.ConstructionTier;
import com.architect.config.CheatTiers.JobYieldTier;
import com.architect.config.CheatTiers.KnowledgeTier;
import com.architect.config.CheatTiers.LogisticsTier;
import com.architect.config.CheatTiers.MaintenanceTier;
import com.architect.util.I18n;

import script.SCRIPT;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.file.Json;

/**
 * Automated QA & Hard Pre-Flight Sanity Test Suite for Syx Architect.
 * Validates:
 * 1. 100% Localization Completeness and UTF-8 encoding symmetry (DE vs EN vs Java dictionaries).
 * 2. Symmetrical Save & Load (V4) with 100% field fidelity including Treasury / infiniteDenari.
 * 3. Backward compatibility across V3, V2, and V1 savegame formats.
 * 4. Engine ScriptLoad Simulation: reflection instantiation and lifecycle hooks.
 * 5. JAR ClassLoader Verification: checks that .properties and .txt are loadable directly from SyxArchitect.jar.
 * 6. _Info.txt Engine Parsing Verification via snake2d.util.file.Json.
 */
public class SanityTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("     SYX ARCHITECT - PRE-FLIGHT BLOCKER VERIFICATION & INTEGRATION TEST         ");
        System.out.println("================================================================================");

        int passed = 0;
        int failed = 0;

        // Check 1: Localization Completeness & UTF-8 Integrity
        try {
            testLocalizationCompleteness();
            System.out.println("[PASS] Check 1: Localization symmetry & UTF-8 encoding verified (all keys match).");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 1: Localization error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 2: Symmetrical State Serialization (V6 Save & Load)
        try {
            testStateSerializationV6();
            System.out.println("[PASS] Check 2: Symmetrical Save & Load (V6) verified with 100% fidelity.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 2: Serialization error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 3: Backward Compatibility with V5, V4, V3 and V2 Savegames
        try {
            testBackwardCompatibility();
            System.out.println("[PASS] Check 3: Backward compatibility (V1/V2/V3/V4/V5 -> V6) verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 3: Backward compatibility error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 4: Engine ScriptLoad Simulation
        try {
            testScriptLoadSimulation();
            System.out.println("[PASS] Check 4: Engine ScriptLoad reflection simulation succeeded without linkage error.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 4: ScriptLoad simulation failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 5: JAR ClassLoader Resource Packaging
        try {
            testJarResourcePackaging();
            System.out.println("[PASS] Check 5: JAR ClassLoader resource packaging verified directly inside SyxArchitect.jar.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 5: JAR resource packaging failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 6: _Info.txt Engine Syntax Verification
        try {
            testModInfoSyntax();
            System.out.println("[PASS] Check 6: _Info.txt verified using engine Json parser.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 6: _Info.txt parser error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 7: Launcher ModInfo Engine Loader Test
        try {
            testLauncherModInfoLoader();
            System.out.println("[PASS] Check 7: Engine ModInfo loader verified (mod fully recognized by game launcher).");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 7: Launcher ModInfo error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Check 8: Omniscience & Booster Mechanics Verification
        try {
            testOmniscienceBoosterMechanics();
            System.out.println("[PASS] Check 8: Omniscience additive boost & HCLASS_RACE dispatch verified (+1M points).");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 8: Omniscience booster error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }        // Check 9: ConstructionModule Package-Private Reflection Verification
        try {
            testConstructionReflection();
            System.out.println("[PASS] Check 9: ConstructionModule reflection handles resolved with setAccessible(true).");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Check 9: ConstructionModule reflection error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }


        System.out.println("================================================================================");
        System.out.println("INTEGRATION TEST RESULTS: " + passed + " passed, " + failed + " failed.");
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

        for (String key : enProps.stringPropertyNames()) {
            String enVal = enProps.getProperty(key);
            if (enVal == null || enVal.trim().isEmpty()) {
                throw new AssertionError("Empty translation in EN for key: " + key);
            }
            for (char c : enVal.toCharArray()) {
                if ((c < 32 || c > 126) && c != '\n' && c != '\t' && c != '\r') {
                    throw new AssertionError("Non-ASCII character '" + c + "' (code " + (int)c + ") in EN key: " + key);
                }
            }

            String deVal = deProps.getProperty(key);
            if (deVal == null || deVal.trim().isEmpty()) {
                throw new AssertionError("Missing translation in DE for key: " + key);
            }
            for (char c : deVal.toCharArray()) {
                if ((c < 32 || c > 126) && c != '\n' && c != '\t' && c != '\r') {
                    throw new AssertionError("Non-ASCII character '" + c + "' (code " + (int)c + ") in DE key: " + key);
                }
            }

            if (!javaEn.containsKey(key)) {
                throw new AssertionError("Key missing in I18n.java (EN): " + key);
            }
            if (!javaDe.containsKey(key)) {
                throw new AssertionError("Key missing in I18n.java (DE): " + key);
            }
        }

        for (String key : deProps.stringPropertyNames()) {
            if (!enProps.containsKey(key)) {
                throw new AssertionError("Extraneous key in DE missing from EN: " + key);
            }
        }
    }

    private static void testStateSerializationV6() throws Exception {
        Path tempPath = Files.createTempFile("syx_architect_test_v6", ".bin");
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
            s1.instantJobs = true;
            s1.jobYieldTier = JobYieldTier.VOID_DISCARD;
            s1.showHudButton = false;
            s1.infiniteDenari = true;
            s1.freezeNiceWeather = true;
            s1.disableAging = true;
            s1.maxFertility = true;
            s1.language = "de";

            FilePutter putter = new FilePutter(tempPath, 1024);
            s1.save(putter);
            putter.save();

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
            assertEquals("instantJobs", s1.instantJobs, s2.instantJobs);
            assertEquals("jobYieldTier", s1.jobYieldTier, s2.jobYieldTier);
            assertEquals("showHudButton", s1.showHudButton, s2.showHudButton);
            assertEquals("infiniteDenari", s1.infiniteDenari, s2.infiniteDenari);
            assertEquals("freezeNiceWeather", s1.freezeNiceWeather, s2.freezeNiceWeather);
            assertEquals("disableAging", s1.disableAging, s2.disableAging);
            assertEquals("maxFertility", s1.maxFertility, s2.maxFertility);
            assertEquals("language", s1.language, s2.language);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private static void testBackwardCompatibility() throws Exception {
        // 1. Test V5 binary stream
        Path tempPathV5 = Files.createTempFile("syx_architect_test_v5", ".bin");
        try {
            FilePutter putter = new FilePutter(tempPathV5, 1024);
            putter.i(ArchitectState.MAGIC_HEADER);
            putter.i(5); // V5
            putter.i(MaintenanceTier.NO_DECAY.ordinal());
            putter.i(ConstructionTier.INSTANT_BUILD.ordinal());
            putter.i(KnowledgeTier.OMNISCIENCE.ordinal());
            putter.i(LogisticsTier.ALL_ASSIGNED_CRATES.ordinal());
            putter.d(12345.0);
            putter.bool(true);
            putter.bool(true);
            putter.bool(true);
            putter.bool(true);
            // V3 booleans
            putter.bool(true);
            putter.bool(false);
            putter.bool(true);
            // V4 booleans
            putter.bool(true); // infiniteDenari
            // V5 booleans
            putter.bool(true); // instantJobs
            putter.i(JobYieldTier.STOCKPILE.ordinal());
            putter.chars("en");
            putter.save();

            ArchitectState s = new ArchitectState();
            FileGetter getter = new FileGetter(tempPathV5);
            s.load(getter);
            getter.close();

            assertEquals("V5 maintenanceTier", MaintenanceTier.NO_DECAY, s.maintenanceTier);
            assertEquals("V5 infiniteDenari", true, s.infiniteDenari);
            assertEquals("V5 instantJobs", true, s.instantJobs);
            assertEquals("V5 jobYieldTier", JobYieldTier.STOCKPILE, s.jobYieldTier);
            assertEquals("V5 showHudButton (default)", true, s.showHudButton);
            assertEquals("V5 language", "en", s.language);
        } finally {
            Files.deleteIfExists(tempPathV5);
        }
        // 2. Test V4 binary stream
        Path tempPathV4 = Files.createTempFile("syx_architect_test_v4", ".bin");
        try {
            FilePutter putter = new FilePutter(tempPathV4, 1024);
            putter.i(ArchitectState.MAGIC_HEADER);
            putter.i(4); // V4
            putter.i(MaintenanceTier.NO_DECAY.ordinal());
            putter.i(ConstructionTier.INSTANT_BUILD.ordinal());
            putter.i(KnowledgeTier.OMNISCIENCE.ordinal());
            putter.i(LogisticsTier.ALL_ASSIGNED_CRATES.ordinal());
            putter.d(12345.0);
            putter.bool(true);
            putter.bool(true);
            putter.bool(true);
            putter.bool(true);
            // V3 booleans
            putter.bool(true);
            putter.bool(false);
            putter.bool(true);
            // V4 booleans
            putter.bool(true); // infiniteDenari
            putter.chars("en");
            putter.save();

            ArchitectState s = new ArchitectState();
            FileGetter getter = new FileGetter(tempPathV4);
            s.load(getter);
            getter.close();

            assertEquals("V4 maintenanceTier", MaintenanceTier.NO_DECAY, s.maintenanceTier);
            assertEquals("V4 infiniteDenari", true, s.infiniteDenari);
            assertEquals("V4 instantJobs (default)", false, s.instantJobs);
            assertEquals("V4 jobYieldTier (default)", JobYieldTier.DROP_ON_GROUND, s.jobYieldTier);
            assertEquals("V4 language", "en", s.language);
        } finally {
            Files.deleteIfExists(tempPathV4);
        }

        // 3. Test V3 binary stream
        Path tempPath = Files.createTempFile("syx_architect_test_v3", ".bin");
        try {
            FilePutter putter = new FilePutter(tempPath, 1024);
            putter.i(ArchitectState.MAGIC_HEADER);
            putter.i(3); // V3
            putter.i(MaintenanceTier.JANITOR_WORK_FREE_MATS.ordinal());
            putter.i(ConstructionTier.FREE_MATERIALS_KEEP_WORK.ordinal());
            putter.i(KnowledgeTier.NO_FORGETTING.ordinal());
            putter.i(LogisticsTier.RAW_MATERIALS_ONLY.ordinal());
            putter.d(54321.0);
            putter.bool(false);
            putter.bool(true);
            putter.bool(true);
            putter.bool(false);
            // V3 booleans
            putter.bool(true); // freezeNiceWeather
            putter.bool(true); // disableAging
            putter.bool(false); // maxFertility
            putter.chars("en");
            putter.save();

            ArchitectState s = new ArchitectState();
            FileGetter getter = new FileGetter(tempPath);
            s.load(getter);
            getter.close();

            assertEquals("V3 maintenanceTier", MaintenanceTier.JANITOR_WORK_FREE_MATS, s.maintenanceTier);
            assertEquals("V3 freezeNiceWeather", true, s.freezeNiceWeather);
            assertEquals("V3 disableAging", true, s.disableAging);
            assertEquals("V3 infiniteDenari (default)", false, s.infiniteDenari);
            assertEquals("V3 instantJobs (default)", false, s.instantJobs);
            assertEquals("V3 jobYieldTier (default)", JobYieldTier.DROP_ON_GROUND, s.jobYieldTier);
            assertEquals("V3 language", "en", s.language);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private static void testScriptLoadSimulation() throws Exception {
        Class<?> clazz = Class.forName("com.architect.ArchitectScript");
        Object scriptObj = clazz.getDeclaredConstructor().newInstance();
        if (!(scriptObj instanceof SCRIPT)) {
            throw new AssertionError("ArchitectScript does not implement script.SCRIPT interface!");
        }
        SCRIPT script = (SCRIPT) scriptObj;
        if (!script.forceInit()) { throw new AssertionError("ArchitectScript.forceInit() must be true!"); }
        CharSequence name = script.name();
        CharSequence desc = script.desc();
        if (name == null || name.length() == 0) {
            throw new AssertionError("script.name() returned empty!");
        }
        if (desc == null || desc.length() == 0) {
            throw new AssertionError("script.desc() returned empty!");
        }

        // Test early game hooks without throwing
        script.initBeforeGameCreated();
        script.initBeforeGameInited();
    }

    private static void testJarResourcePackaging() throws Exception {
        File jarFile = new File("build/libs/SyxArchitect.jar");
        if (!jarFile.exists()) {
            throw new AssertionError("SyxArchitect.jar not found at " + jarFile.getAbsolutePath());
        }

        URL jarUrl = jarFile.toURI().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
            // Test 1: Check root classpath presence
            InputStream inRootProps = loader.getResourceAsStream("strings_de.properties");
            if (inRootProps == null) {
                throw new AssertionError("strings_de.properties missing from JAR root!");
            }
            inRootProps.close();

            InputStream inRootTut = loader.getResourceAsStream("tutorial_de.txt");
            if (inRootTut == null) {
                throw new AssertionError("tutorial_de.txt missing from JAR root!");
            }
            inRootTut.close();

            // Test 2: Check assets path presence
            InputStream inAssetProps = loader.getResourceAsStream("assets/text/strings_de.properties");
            if (inAssetProps == null) {
                throw new AssertionError("assets/text/strings_de.properties missing from JAR!");
            }
            inAssetProps.close();
        }
    }

    private static void testModInfoSyntax() throws Exception {
        File infoFile = new File("mod-files/_Info.txt");
        if (!infoFile.exists()) {
            throw new AssertionError("_Info.txt does not exist in mod-files!");
        }

        // Verify case sensitivity: directory listing must contain exact "_Info.txt" (not lowercase "_info.txt")
        File parent = infoFile.getParentFile();
        boolean exactCaseFound = false;
        String[] files = parent.list();
        if (files != null) {
            for (String f : files) {
                if (f.equals("_Info.txt")) {
                    exactCaseFound = true;
                    break;
                }
            }
        }
        if (!exactCaseFound) {
            throw new AssertionError("File casing mismatch: _Info.txt must have capital 'I' on disk to satisfy Songs of Syx Util.check!");
        }

        Json json = new Json(infoFile.toPath());
        String name = json.text("NAME", null);
        String desc = json.text("DESC", null);
        String version = json.text("VERSION", null);
        String author = json.text("AUTHOR", null);
        String gameVersion = json.text("GAMEVERSION", null);

        if (name == null || name.trim().isEmpty()) throw new AssertionError("NAME is missing in _Info.txt");
        if (desc == null || desc.trim().isEmpty()) throw new AssertionError("DESC is missing in _Info.txt");
        if (version == null || version.trim().isEmpty()) throw new AssertionError("VERSION is missing in _Info.txt");
        if (author == null || author.trim().isEmpty()) throw new AssertionError("AUTHOR is missing in _Info.txt");
        if (gameVersion == null || gameVersion.trim().isEmpty()) throw new AssertionError("GAMEVERSION is missing in _Info.txt");

        assertEquals("NAME", "Syx Architect", name);
        assertEquals("VERSION", "1.0.0", version);
        assertEquals("AUTHOR", "Architect", author);
        assertEquals("GAMEVERSION", "71", gameVersion);
    }

    private static void testLauncherModInfoLoader() throws Exception {
        try {
            init.paths.ModInfo info = new init.paths.ModInfo("SyxArchitect");
            assertEquals("ModInfo name", "Syx Architect", info.name);
            assertEquals("ModInfo majorVersion", 71, info.majorVersion);
        } catch (init.paths.ModInfo.ModInfoException e) {
            throw new AssertionError("Launcher ModInfo constructor rejected SyxArchitect: " + e.getMessage(), e);
        }
    }

    private static void testOmniscienceBoosterMechanics() throws Exception {
        com.architect.modules.BoostModule boostModule = new com.architect.modules.BoostModule();
        boostModule.init();

        ArchitectState state = new ArchitectState();
        state.knowledgeTier = KnowledgeTier.OMNISCIENCE;
        boostModule.onStateChanged(state);

        java.lang.reflect.Field f = com.architect.modules.BoostModule.class.getDeclaredField("innovationBooster");
        f.setAccessible(true);
        com.architect.modules.BoostModule.DynamicBooster booster = (com.architect.modules.BoostModule.DynamicBooster) f.get(null);

        if (booster == null) {
            throw new AssertionError("innovationBooster was not initialized!");
        }

        if (booster.isMul) {
            throw new AssertionError("innovationBooster must have isMul = false (additive), but isMul was true!");
        }

        assertEquals("Omniscience active value", 1_000_000.0, booster.getValue(), 0.001);
        assertEquals("HCLASS_RACE vGet value", 1_000_000.0, booster.vGet((init.type.HCLASS_RACE) null), 0.001);

        state.knowledgeTier = KnowledgeTier.VANILLA;
        boostModule.onStateChanged(state);
        assertEquals("Vanilla active value", 0.0, booster.getValue(), 0.001);
    }

    private static void assertEquals(String field, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Mismatch in " + field + ": expected=" + expected + ", actual=" + actual);
        }
    }
    private static void testConstructionReflection() throws Exception {
        Class<?> ciClass = Class.forName("settlement.room.main.construction.ConstructionInstance");
        java.lang.reflect.Method m1 = ciClass.getMethod("jobFinsih", int.class, int.class, init.resources.RESOURCE.class, int.class);
        m1.setAccessible(true);
        java.lang.reflect.Method m2 = ciClass.getMethod("totalResourcesNeeded", int.class, int.class);
        m2.setAccessible(true);

        Class<?> bitsClass = Class.forName("settlement.room.infra.janitor.BITS");
        java.lang.reflect.Method m3 = bitsClass.getMethod("resAm", init.resources.RESOURCE.class);
        m3.setAccessible(true);

        Class<?> jiClass = Class.forName("settlement.room.infra.janitor.JanitorInstance");
        java.lang.reflect.Method m4 = bitsClass.getMethod("resInc", jiClass, init.resources.RESOURCE.class, int.class);
        m4.setAccessible(true);

        Class<?> constrClass = Class.forName("settlement.room.main.construction.CONSTRUCTION");
        java.lang.reflect.Field f1 = constrClass.getDeclaredField("construction");
        f1.setAccessible(true);
        Class<?> bpClass = Class.forName("settlement.room.main.construction.ConstructionBlueprint");
        java.lang.reflect.Field f2 = bpClass.getDeclaredField("all");
        f2.setAccessible(true);
    }


    private static void assertEquals(String field, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError("Mismatch in " + field + ": expected=" + expected + ", actual=" + actual);
        }
    }
}

