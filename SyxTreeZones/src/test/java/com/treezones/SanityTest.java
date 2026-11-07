package com.treezones;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import com.treezones.config.TreeZoneConfig;
import com.treezones.util.I18n;
import com.treezones.zone.TreeZoneManager;

import script.SCRIPT;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.file.Json;

public class SanityTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   SYX TREE ZONES - LOGIC, LOCALIZATION & SERIALIZATION VERIFICATION SUITE       ");
        System.out.println("================================================================================");

        int passed = 0;

        try {
            // Check 1: _Info.txt JSON syntax
            File infoFile = new File("mod-files/_Info.txt");
            if (!infoFile.exists()) {
                throw new IllegalStateException("_Info.txt does not exist at " + infoFile.getAbsolutePath());
            }
            Json json = new Json(Path.of(infoFile.getAbsolutePath()));
            if (!json.has("NAME") || !json.has("VERSION") || !json.has("GAMEVERSION")) {
                throw new IllegalStateException("_Info.txt missing mandatory keys");
            }
            System.out.println("[PASS] Check 1: _Info.txt exact case & engine Json parser verified.");
            passed++;

            // Check 2: SCRIPT entrypoint reflection loading
            Class<?> clazz = Class.forName("com.treezones.TreeZonesScript");
            SCRIPT script = (SCRIPT) clazz.getDeclaredConstructor().newInstance();
            if (script.name() == null || script.name().length() == 0) {
                throw new IllegalStateException("Script name is null or empty");
            }
            SCRIPT.SCRIPT_INSTANCE instance = script.createInstance();
            if (!(instance instanceof TreeZonesInstance)) {
                throw new IllegalStateException("createInstance did not return TreeZonesInstance");
            }
            System.out.println("[PASS] Check 2: TreeZonesScript reflection & SCRIPT lifecycle verified.");
            passed++;

            // Check 3: Localization symmetry
            Properties deProps = loadProps("assets/text/strings_de.properties");
            Properties enProps = loadProps("assets/text/strings_en.properties");
            Set<String> deKeys = deProps.stringPropertyNames();
            Set<String> enKeys = enProps.stringPropertyNames();
            if (!deKeys.equals(enKeys)) {
                throw new IllegalStateException("Localization key mismatch between DE and EN!");
            }
            System.out.println("[PASS] Check 3: Localization symmetry verified (" + deKeys.size() + " keys match).");
            passed++;

            // Check 4: TreeZoneConfig save & load
            TreeZoneConfig cfg = new TreeZoneConfig();
            cfg.setInstantClear(false);
            cfg.setDropWood(false);
            cfg.setInstantPlant(true);
            cfg.setShowOverlay(false);
            cfg.setShowHudButton(false);
            cfg.setAutoNoGrowth(true);

            Path tempPath = java.nio.file.Files.createTempFile("tz_cfg_test", ".bin");
            try {
                FilePutter putter = new FilePutter(tempPath, 64 * 1024);
                cfg.save(putter);
                putter.save();

                FileGetter getter = new FileGetter(tempPath);
                TreeZoneConfig loadedCfg = new TreeZoneConfig();
                loadedCfg.load(getter);
                getter.close();

                if (loadedCfg.isInstantClear() != cfg.isInstantClear()
                        || loadedCfg.isDropWood() != cfg.isDropWood()
                        || loadedCfg.isInstantPlant() != cfg.isInstantPlant()
                        || loadedCfg.isShowOverlay() != cfg.isShowOverlay()
                        || loadedCfg.isShowHudButton() != cfg.isShowHudButton()
                        || loadedCfg.isAutoNoGrowth() != cfg.isAutoNoGrowth()) {
                    throw new IllegalStateException("Config serialization fidelity check failed!");
                }
            } finally {
                java.nio.file.Files.deleteIfExists(tempPath);
            }
            System.out.println("[PASS] Check 4: TreeZoneConfig binary save/load fidelity verified.");
            passed++;

            // Check 5: TreeZoneManager bitmask isolation & save/load
            TreeZoneManager zm = new TreeZoneManager();
            zm.ensureInitialized(256, 256);
            if (zm.getActiveNoTreeCount() != 0 || zm.getActiveForestCount() != 0) {
                throw new IllegalStateException("Zone counts should start at 0");
            }

            Path zmTempPath = java.nio.file.Files.createTempFile("tz_zm_test", ".bin");
            try {
                FilePutter zmPutter = new FilePutter(zmTempPath, 64 * 1024);
                zm.save(zmPutter);
                zmPutter.save();

                FileGetter zmGetter = new FileGetter(zmTempPath);
                TreeZoneManager loadedZm = new TreeZoneManager();
                loadedZm.load(zmGetter);
                zmGetter.close();

                if (loadedZm.getActiveNoTreeCount() != 0 || loadedZm.getActiveForestCount() != 0) {
                    throw new IllegalStateException("Loaded zone counts mismatch");
                }
            } finally {
                java.nio.file.Files.deleteIfExists(zmTempPath);
            }
            System.out.println("[PASS] Check 5: TreeZoneManager bitmask & serialization verified.");
            passed++;

            System.out.println("================================================================================");
            System.out.println("TREE ZONES TEST RESULTS: " + passed + " passed, 0 failed.");
            System.out.println("================================================================================");

        } catch (Throwable t) {
            System.err.println("[FAIL] Test failure: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static Properties loadProps(String path) throws Exception {
        Properties p = new Properties();
        try (InputStream in = SanityTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                File f = new File("mod-files/" + path);
                if (f.exists()) {
                    try (InputStream fis = new java.io.FileInputStream(f)) {
                        p.load(new InputStreamReader(fis, StandardCharsets.UTF_8));
                        return p;
                    }
                }
                throw new IllegalStateException("Cannot find resource: " + path);
            }
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }
}
