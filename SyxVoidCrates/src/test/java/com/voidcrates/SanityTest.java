package com.voidcrates;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import com.voidcrates.config.VoidConfig;
import com.voidcrates.manager.VoidManager;
import com.voidcrates.util.I18n;

import script.SCRIPT;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.file.Json;

public class SanityTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   SYX VOID CRATES - LOGIC, LOCALIZATION & SERIALIZATION VERIFICATION SUITE     ");
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
            Class<?> clazz = Class.forName("com.voidcrates.VoidCratesScript");
            SCRIPT script = (SCRIPT) clazz.getDeclaredConstructor().newInstance();
            if (script.name() == null || script.name().length() == 0) {
                throw new IllegalStateException("Script name is null or empty");
            }
            SCRIPT.SCRIPT_INSTANCE instance = script.createInstance();
            if (!(instance instanceof VoidCratesInstance)) {
                throw new IllegalStateException("createInstance did not return VoidCratesInstance");
            }
            System.out.println("[PASS] Check 2: VoidCratesScript reflection & SCRIPT lifecycle verified.");
            passed++;

            // Check 3: Localization symmetry
            Properties deProps = loadProps("assets/text/strings_de.properties");
            Properties enProps = loadProps("assets/text/strings_en.properties");
            Set<String> deKeys = deProps.stringPropertyNames();
            Set<String> enKeys = enProps.stringPropertyNames();
            if (!deKeys.equals(enKeys)) {
                throw new IllegalStateException("Localization key mismatch between DE and EN! DE count=" + deKeys.size() + ", EN count=" + enKeys.size());
            }
            System.out.println("[PASS] Check 3: Localization symmetry verified (" + deKeys.size() + " keys match).");
            passed++;

            // Check 4: VoidConfig binary save/load fidelity
            VoidConfig cfg = new VoidConfig();
            cfg.setActive(false);
            cfg.setShowHudButton(false);
            cfg.setLanguage("en");
            cfg.setAutoVoidStone(false);
            cfg.setAutoVoidClay(true);
            cfg.setAutoVoidCoal(true);
            cfg.setAutoVoidLivestock(true);
            cfg.setAutoVoidWood(true);
            cfg.setAutoVoidOres(true);
            cfg.setThresholdIndex(2);
            cfg.setRetainBufferIndex(1);
            cfg.setDesignatedOnly(true);
            cfg.addVoided(123456L);

            Path tempPath = java.nio.file.Files.createTempFile("void_cfg_test", ".bin");
            try {
                FilePutter putter = new FilePutter(tempPath, 64 * 1024);
                cfg.save(putter);
                putter.save();

                FileGetter getter = new FileGetter(tempPath);
                VoidConfig loadedCfg = new VoidConfig();
                loadedCfg.load(getter);
                getter.close();

                if (loadedCfg.isActive() != cfg.isActive()
                        || loadedCfg.isShowHudButton() != cfg.isShowHudButton()
                        || !loadedCfg.getLanguage().equals(cfg.getLanguage())
                        || loadedCfg.isAutoVoidStone() != cfg.isAutoVoidStone()
                        || loadedCfg.isAutoVoidClay() != cfg.isAutoVoidClay()
                        || loadedCfg.isAutoVoidCoal() != cfg.isAutoVoidCoal()
                        || loadedCfg.isAutoVoidLivestock() != cfg.isAutoVoidLivestock()
                        || loadedCfg.isAutoVoidWood() != cfg.isAutoVoidWood()
                        || loadedCfg.isAutoVoidOres() != cfg.isAutoVoidOres()
                        || loadedCfg.getThresholdIndex() != cfg.getThresholdIndex()
                        || loadedCfg.getRetainBufferIndex() != cfg.getRetainBufferIndex()
                        || loadedCfg.isDesignatedOnly() != cfg.isDesignatedOnly()
                        || loadedCfg.getTotalItemsVoided() != cfg.getTotalItemsVoided()) {
                    throw new IllegalStateException("VoidConfig serialization fidelity check failed!");
                }
            } finally {
                java.nio.file.Files.deleteIfExists(tempPath);
            }
            System.out.println("[PASS] Check 4: VoidConfig binary save/load fidelity verified.");
            passed++;

            // Check 5: VoidManager save/load & trigger calculations
            if (cfg.getTriggerRatio() != 0.75) {
                throw new IllegalStateException("Trigger ratio for threshold index 2 should be 0.75, got " + cfg.getTriggerRatio());
            }
            if (cfg.getRetainRatio() != 0.20) {
                throw new IllegalStateException("Retain ratio for retain index 1 should be 0.20, got " + cfg.getRetainRatio());
            }

            VoidManager vm = new VoidManager();
            vm.ensureInitialized(256, 256);
            Path vmTemp = java.nio.file.Files.createTempFile("void_mgr_test", ".bin");
            try {
                FilePutter p = new FilePutter(vmTemp, 256 * 1024);
                vm.save(p);
                p.save();

                FileGetter g = new FileGetter(vmTemp);
                VoidManager loadedVm = new VoidManager();
                loadedVm.ensureInitialized(256, 256);
                loadedVm.load(g);
                g.close();

                if (loadedVm.getDesignatedCount() != vm.getDesignatedCount()) {
                    throw new IllegalStateException("VoidManager serialization count mismatch!");
                }
            } finally {
                java.nio.file.Files.deleteIfExists(vmTemp);
            }
            System.out.println("[PASS] Check 5: VoidManager serialization & ratios verified.");
            passed++;

            System.out.println("================================================================================");
            System.out.println("VOID CRATES TEST RESULTS: " + passed + " passed, 0 failed.");
            System.out.println("================================================================================");
        } catch (Throwable t) {
            System.err.println("[FAIL] " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static Properties loadProps(String relPath) throws Exception {
        File f = new File("mod-files/" + relPath);
        if (!f.exists()) {
            throw new IllegalStateException("File not found: " + f.getAbsolutePath());
        }
        Properties props = new Properties();
        try (InputStream is = new java.io.FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            props.load(isr);
        }
        return props;
    }
}
