package com.voidcrates.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class I18n {

    public enum Language {
        DE("de", "Deutsch"),
        EN("en", "English");

        public final String code;
        public final String label;

        Language(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public static Language fromCode(String code) {
            if (code != null && code.equalsIgnoreCase("en")) {
                return EN;
            }
            return DE;
        }
    }

    private static Language currentLanguage = Language.DE;

    private static final Map<String, String> DE_STRINGS = new HashMap<>();
    private static final Map<String, String> EN_STRINGS = new HashMap<>();

    static {
        loadProperties("strings_de.properties", DE_STRINGS);
        loadProperties("strings_en.properties", EN_STRINGS);

        initFallbackDictionaries();
    }

    private static void loadProperties(String fileName, Map<String, String> targetMap) {
        try {
            InputStream is = I18n.class.getResourceAsStream("/" + fileName);
            if (is == null) {
                is = I18n.class.getResourceAsStream("/assets/text/" + fileName);
            }
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Properties props = new Properties();
                    props.load(reader);
                    for (String key : props.stringPropertyNames()) {
                        targetMap.put(key, props.getProperty(key));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void initFallbackDictionaries() {
        // EN
        EN_STRINGS.putIfAbsent("mod.name", "Syx Void Crates");
        EN_STRINGS.putIfAbsent("mod.desc", "Black hole and automated overflow voiding for byproduct management");
        EN_STRINGS.putIfAbsent("ui.title", "Void Crates - Black Hole");
        EN_STRINGS.putIfAbsent("ui.subtitle", "Automated overflow voiding for byproduct management");
        EN_STRINGS.putIfAbsent("ui.tab.rules", "Void Policies");
        EN_STRINGS.putIfAbsent("ui.tab.crates", "Crate Management");
        EN_STRINGS.putIfAbsent("ui.stats", "Total Voided: {0} Items");
        EN_STRINGS.putIfAbsent("ui.status.active", "Automatic Voiding: ACTIVE");
        EN_STRINGS.putIfAbsent("ui.status.inactive", "Automatic Voiding: OFF");
        EN_STRINGS.putIfAbsent("btn.toggle_active", "Master Switch");
        EN_STRINGS.putIfAbsent("btn.purge_now", "Purge Full Crates Now");
        EN_STRINGS.putIfAbsent("btn.clear_marks", "Clear All Designations");
        EN_STRINGS.putIfAbsent("btn.mark_all_current", "Mark All Crates of Selected Resource");
        EN_STRINGS.putIfAbsent("btn.language", "Language: EN");
        EN_STRINGS.putIfAbsent("btn.close", "Close (ESC / F9)");
        EN_STRINGS.putIfAbsent("res.stone", "Stone (Mine Byproduct)");
        EN_STRINGS.putIfAbsent("res.clay", "Clay & Mud (Pits)");
        EN_STRINGS.putIfAbsent("res.coal", "Coal (Mines)");
        EN_STRINGS.putIfAbsent("res.livestock", "Livestock & Dung (Pastures)");
        EN_STRINGS.putIfAbsent("res.wood", "Wood (Deforestation)");
        EN_STRINGS.putIfAbsent("res.ores", "Ores (Surplus)");
        EN_STRINGS.putIfAbsent("setting.threshold", "Trigger Threshold");
        EN_STRINGS.putIfAbsent("setting.threshold.full", "At 100% (Full)");
        EN_STRINGS.putIfAbsent("setting.threshold.90", "At 90% Capacity");
        EN_STRINGS.putIfAbsent("setting.threshold.75", "At 75% Capacity");
        EN_STRINGS.putIfAbsent("setting.threshold.instant", "Instant (On Deposit)");
        EN_STRINGS.putIfAbsent("setting.retain", "Retained Buffer");
        EN_STRINGS.putIfAbsent("setting.retain.zero", "0% (Empty Completely)");
        EN_STRINGS.putIfAbsent("setting.retain.20", "Keep 20% Buffer");
        EN_STRINGS.putIfAbsent("setting.retain.50", "Keep 50% Buffer");
        EN_STRINGS.putIfAbsent("setting.mode", "Target Scope");
        EN_STRINGS.putIfAbsent("setting.mode.all", "All Warehouses of This Resource");
        EN_STRINGS.putIfAbsent("setting.mode.designated_only", "Designated Void Crates Only");
        EN_STRINGS.putIfAbsent("status.purged", "Purged: {0} crates ({1} items destroyed)!");
        EN_STRINGS.putIfAbsent("status.marked", "{0} crates marked as Black Hole!");
        EN_STRINGS.putIfAbsent("status.unmarked", "All crate designations cleared.");

        // DE
        DE_STRINGS.putIfAbsent("mod.name", "Syx Void Crates");
        DE_STRINGS.putIfAbsent("mod.desc", "Automatischer Muellschlucker fuer Ueberschuss-Gueter");
        DE_STRINGS.putIfAbsent("ui.title", "Void Crates - Schwarzes Loch");
        DE_STRINGS.putIfAbsent("ui.subtitle", "Automatischer Muellschlucker fuer Ueberschuss-Gueter");
        DE_STRINGS.putIfAbsent("ui.tab.rules", "Muellschlucker-Regeln");
        DE_STRINGS.putIfAbsent("ui.tab.crates", "Kisten-Verwaltung");
        DE_STRINGS.putIfAbsent("ui.stats", "Bisher vernichtet: {0} Gueter");
        DE_STRINGS.putIfAbsent("ui.status.active", "Automatisches Voiding: AKTIV");
        DE_STRINGS.putIfAbsent("ui.status.inactive", "Automatisches Voiding: AUS");
        DE_STRINGS.putIfAbsent("btn.toggle_active", "Master-Schalter");
        DE_STRINGS.putIfAbsent("btn.purge_now", "Jetzt volle Kisten leeren");
        DE_STRINGS.putIfAbsent("btn.clear_marks", "Alle Markierungen loeschen");
        DE_STRINGS.putIfAbsent("btn.mark_all_current", "Alle Kisten dieser Ressource markieren");
        DE_STRINGS.putIfAbsent("btn.language", "Sprache: DE");
        DE_STRINGS.putIfAbsent("btn.close", "Schliessen (ESC / F9)");
        DE_STRINGS.putIfAbsent("res.stone", "Stein (Minen-Nebenprodukt)");
        DE_STRINGS.putIfAbsent("res.clay", "Ton & Lehm (Gruben)");
        DE_STRINGS.putIfAbsent("res.coal", "Kohle (Bergwerk)");
        DE_STRINGS.putIfAbsent("res.livestock", "Vieh & Dung (Weiden)");
        DE_STRINGS.putIfAbsent("res.wood", "Holz (Rodungen)");
        DE_STRINGS.putIfAbsent("res.ores", "Erze (Ueberschuss)");
        DE_STRINGS.putIfAbsent("setting.threshold", "Ausloese-Schwelle");
        DE_STRINGS.putIfAbsent("setting.threshold.full", "Bei 100% (Voll)");
        DE_STRINGS.putIfAbsent("setting.threshold.90", "Bei 90% Kapazitaet");
        DE_STRINGS.putIfAbsent("setting.threshold.75", "Bei 75% Kapazitaet");
        DE_STRINGS.putIfAbsent("setting.threshold.instant", "Sofort (Jeder Eingang)");
        DE_STRINGS.putIfAbsent("setting.retain", "Rest-Puffer");
        DE_STRINGS.putIfAbsent("setting.retain.zero", "0% (Kiste ganz leeren)");
        DE_STRINGS.putIfAbsent("setting.retain.20", "20% Puffer behalten");
        DE_STRINGS.putIfAbsent("setting.retain.50", "50% Puffer behalten");
        DE_STRINGS.putIfAbsent("setting.mode", "Geltungsbereich");
        DE_STRINGS.putIfAbsent("setting.mode.all", "Alle Lagerhaeuser dieser Gueter");
        DE_STRINGS.putIfAbsent("setting.mode.designated_only", "Nur markierte Void-Kisten");
        DE_STRINGS.putIfAbsent("status.purged", "Geleert: {0} Kisten ({1} Einheiten vernichtet)!");
        DE_STRINGS.putIfAbsent("status.marked", "{0} Kisten als Schwarzes Loch markiert!");
        DE_STRINGS.putIfAbsent("status.unmarked", "Alle Kisten-Markierungen aufgehoben.");
    }

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(Language lang) {
        if (lang != null) {
            currentLanguage = lang;
        }
    }

    public static void toggleLanguage() {
        currentLanguage = (currentLanguage == Language.DE) ? Language.EN : Language.DE;
    }

    public static String get(String key) {
        Map<String, String> dict = (currentLanguage == Language.DE) ? DE_STRINGS : EN_STRINGS;
        String val = dict.get(key);
        if (val == null) {
            val = EN_STRINGS.get(key);
        }
        return (val != null) ? sanitizeAscii(val) : key;
    }

    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    public static String sanitizeAscii(String text) {
        if (text == null) return "";
        return text
            .replace("ä", "ae").replace("Ä", "Ae")
            .replace("ö", "oe").replace("Ö", "Oe")
            .replace("ü", "ue").replace("Ü", "Ue")
            .replace("ß", "ss");
    }
}
