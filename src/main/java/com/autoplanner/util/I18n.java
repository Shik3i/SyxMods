package com.autoplanner.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance bilingual (EN/DE) localization utility for Syx Auto-Planner.
 */
public final class I18n {

    public enum Language {
        EN("English", "en"),
        DE("Deutsch", "de");

        public final String displayName;
        public final String code;

        Language(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }

        public static Language fromCode(String code) {
            if ("de".equalsIgnoreCase(code)) {
                return DE;
            }
            return EN;
        }
    }

    private static Language current = Language.EN;
    private static final Map<String, String> EN_STRINGS = new HashMap<>();
    private static final Map<String, String> DE_STRINGS = new HashMap<>();

    static {
        // --- English Translations ---
        EN_STRINGS.put("mod.name", "Syx Auto-Planner");
        EN_STRINGS.put("mod.desc", "Intelligent room builder assistant: Auto-Strut & Auto-Furnish for Songs of Syx.");
        
        EN_STRINGS.put("ui.overlay.title", "Auto-Planner");
        EN_STRINGS.put("ui.overlay.room", "Room: {0} ({1} tiles)");
        EN_STRINGS.put("ui.overlay.no_room", "No active room draft");
        
        EN_STRINGS.put("btn.strut", "Auto-Strut (Pillars)");
        EN_STRINGS.put("btn.furnish", "Auto-Furnish (Max)");
        EN_STRINGS.put("btn.quickfill", "Quick Build (All)");
        EN_STRINGS.put("btn.clear", "Clear Furniture");
        EN_STRINGS.put("btn.settings", "Config");
        EN_STRINGS.put("btn.style.compact", "Style: Compact");
        EN_STRINGS.put("btn.style.balanced", "Style: Balanced");
        EN_STRINGS.put("btn.target_eff", "Target Eff: {0}%");
        EN_STRINGS.put("btn.storage_ratio", "Storage: {0}%");
        EN_STRINGS.put("btn.language", "Language: EN");
        EN_STRINGS.put("btn.close", "Close");

        EN_STRINGS.put("msg.strut_success", "Auto-Strut: Placed {0} pillar(s). 0 yellow tiles remaining!");
        EN_STRINGS.put("msg.strut_none_needed", "Auto-Strut: Ceiling is already 100% supported!");
        EN_STRINGS.put("msg.furnish_success", "Auto-Furnish: Placed {0} workstation(s) & {1} module(s). Eff: {2}%.");
        EN_STRINGS.put("msg.furnish_fail_doors", "Auto-Furnish: Please place at least one door first!");
        EN_STRINGS.put("msg.furnish_no_workstations", "Auto-Furnish: No workstation items found for this blueprint.");
        EN_STRINGS.put("msg.clear_success", "Cleared all furniture from current room draft.");

        // --- German Translations ---
        DE_STRINGS.put("mod.name", "Syx Auto-Planner");
        DE_STRINGS.put("mod.desc", "Intelligenter Bau-Assistent: Automatische Stützpfeiler & Möblierung für Songs of Syx.");
        
        DE_STRINGS.put("ui.overlay.title", "Auto-Planer");
        DE_STRINGS.put("ui.overlay.room", "Raum: {0} ({1} Felder)");
        DE_STRINGS.put("ui.overlay.no_room", "Kein Raum im Entwurf");
        
        DE_STRINGS.put("btn.strut", "Auto-Stützen (Pfeiler)");
        DE_STRINGS.put("btn.furnish", "Auto-Möblieren (Max)");
        DE_STRINGS.put("btn.quickfill", "Schnellbau (Alles)");
        DE_STRINGS.put("btn.clear", "Möbel leeren");
        DE_STRINGS.put("btn.settings", "Optionen");
        DE_STRINGS.put("btn.style.compact", "Stil: Kompakt");
        DE_STRINGS.put("btn.style.balanced", "Stil: Ausgewogen");
        DE_STRINGS.put("btn.target_eff", "Ziel-Eff: {0}%");
        DE_STRINGS.put("btn.storage_ratio", "Lager: {0}%");
        DE_STRINGS.put("btn.language", "Sprache: DE");
        DE_STRINGS.put("btn.close", "Schließen");

        DE_STRINGS.put("msg.strut_success", "Auto-Stützen: {0} Pfeiler gesetzt. 0 gelbe Bereiche übrig!");
        DE_STRINGS.put("msg.strut_none_needed", "Auto-Stützen: Die Decke ist bereits zu 100% gestützt!");
        DE_STRINGS.put("msg.furnish_success", "Auto-Möblieren: {0} Station(en) & {1} Modul(e) platziert. Eff: {2}%.");
        DE_STRINGS.put("msg.furnish_fail_doors", "Auto-Möblieren: Bitte zuerst mindestens eine Tür setzen!");
        DE_STRINGS.put("msg.furnish_no_workstations", "Auto-Möblieren: Keine Arbeitsstationen für diesen Raumtyp.");
        DE_STRINGS.put("msg.clear_success", "Alle Einrichtungsgegenstände im Raum-Entwurf entfernt.");
    }

    private I18n() {}

    public static Language getLanguage() {
        return current;
    }

    public static void setLanguage(Language lang) {
        if (lang != null) {
            current = lang;
        }
    }

    public static void toggleLanguage() {
        current = (current == Language.EN) ? Language.DE : Language.EN;
    }

    public static String get(String key) {
        Map<String, String> dict = (current == Language.DE) ? DE_STRINGS : EN_STRINGS;
        String val = dict.get(key);
        if (val == null) {
            val = EN_STRINGS.get(key);
        }
        return val != null ? val : key;
    }

    public static String get(String key, Object... args) {
        String template = get(key);
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return MessageFormat.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
}
