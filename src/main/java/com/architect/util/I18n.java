package com.architect.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance, allocation-minimal bilingual localization system for Syx Architect.
 * Supports on-the-fly toggling between English and German.
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
        // Built-in English dictionary
        EN_STRINGS.put("mod.name", "Syx Architect");
        EN_STRINGS.put("mod.desc", "Creative, Schönbau & Sandbox Suite for Songs of Syx.");
        EN_STRINGS.put("ui.title", "Syx Architect - Creative Suite");
        EN_STRINGS.put("ui.tab.boosts", "Boosts & Science");
        EN_STRINGS.put("ui.tab.logistics", "Logistics & Storage");
        EN_STRINGS.put("ui.tab.construction", "Construction & Decor");
        EN_STRINGS.put("ui.tab.security", "Security & Peace");
        EN_STRINGS.put("ui.tab.settings", "Settings");

        EN_STRINGS.put("cheat.knowledge", "Knowledge & Innovation");
        EN_STRINGS.put("cheat.knowledge.desc", "Massively accelerates technology and innovation generation.");
        EN_STRINGS.put("cheat.maintenance", "No Building Wear");
        EN_STRINGS.put("cheat.maintenance.desc", "Prevents building degradation and eliminates maintenance needs.");
        EN_STRINGS.put("cheat.spoilage", "Infinite Shelf Life");
        EN_STRINGS.put("cheat.spoilage.desc", "Stored goods, food, and rations will never spoil.");
        EN_STRINGS.put("cheat.loyalty", "Absolute Loyalty & Peace");
        EN_STRINGS.put("cheat.loyalty.desc", "Maximizes citizen happiness and eliminates rebellion risks.");
        EN_STRINGS.put("cheat.instant_build", "Instant Construction");
        EN_STRINGS.put("cheat.instant_build.desc", "Instantly completes planned rooms, walls, and structures without resources.");
        EN_STRINGS.put("cheat.infinite_crates", "Infinite Storage Crates");
        EN_STRINGS.put("cheat.infinite_crates.desc", "Multiplies crate and stockpile capacities across the settlement.");
        EN_STRINGS.put("cheat.raid_protection", "Raid Immunity");
        EN_STRINGS.put("cheat.raid_protection.desc", "Automatically cancels hostile raids and barbarian invasions.");
        EN_STRINGS.put("cheat.battle_autowin", "Battle Auto-Win");
        EN_STRINGS.put("cheat.battle_autowin.desc", "Ensures absolute morale victory in all tactical battles.");

        EN_STRINGS.put("tier.off", "OFF");
        EN_STRINGS.put("tier.normal", "2x");
        EN_STRINGS.put("tier.high", "5x");
        EN_STRINGS.put("tier.extreme", "10x");
        EN_STRINGS.put("tier.god", "100x");

        EN_STRINGS.put("btn.apply", "Apply");
        EN_STRINGS.put("btn.close", "Close");
        EN_STRINGS.put("btn.tutorial", "Tutorial & Guide");
        EN_STRINGS.put("btn.language", "Language: English");
        EN_STRINGS.put("status.active", "ACTIVE");
        EN_STRINGS.put("status.inactive", "INACTIVE");
        EN_STRINGS.put("status.instant_triggered", "Instant construction executed!");

        // Built-in German dictionary
        DE_STRINGS.put("mod.name", "Syx Architect");
        DE_STRINGS.put("mod.desc", "Creative-, Schönbau- & Sandbox-Suite für Songs of Syx.");
        DE_STRINGS.put("ui.title", "Syx Architect - Creative Suite");
        DE_STRINGS.put("ui.tab.boosts", "Boosts & Forschung");
        DE_STRINGS.put("ui.tab.logistics", "Logistik & Lager");
        DE_STRINGS.put("ui.tab.construction", "Bau & Schönbau");
        DE_STRINGS.put("ui.tab.security", "Sicherheit & Frieden");
        DE_STRINGS.put("ui.tab.settings", "Einstellungen");

        DE_STRINGS.put("cheat.knowledge", "Wissen & Innovation");
        DE_STRINGS.put("cheat.knowledge.desc", "Beschleunigt Technologie- und Innovationspunkte massiv.");
        DE_STRINGS.put("cheat.maintenance", "Kein Gebäude-Verschleiß");
        DE_STRINGS.put("cheat.maintenance.desc", "Verhindert Gebäude-Degradation und macht Hausmeister überflüssig.");
        DE_STRINGS.put("cheat.spoilage", "Kein Verderb (Unendliche Haltbarkeit)");
        DE_STRINGS.put("cheat.spoilage.desc", "Gelagerte Nahrung und Güter verderben niemals.");
        DE_STRINGS.put("cheat.loyalty", "Absolute Loyalität & Zufriedenheit");
        DE_STRINGS.put("cheat.loyalty.desc", "Maximiert Zufriedenheit und verhindert Aufstände vollständig.");
        DE_STRINGS.put("cheat.instant_build", "Sofortiges Bauen (Instant Build)");
        DE_STRINGS.put("cheat.instant_build.desc", "Stellt geplante Räume, Wände und Gebäude sofort ohne Materialaufwand fertig.");
        DE_STRINGS.put("cheat.infinite_crates", "Unendliche Lagerkisten");
        DE_STRINGS.put("cheat.infinite_crates.desc", "Vervielfacht die Kapazität aller Kisten und Halden.");
        DE_STRINGS.put("cheat.raid_protection", "Raid-Immunität");
        DE_STRINGS.put("cheat.raid_protection.desc", "Bricht feindliche Raids und Barbaren-Invasionen automatisch ab.");
        DE_STRINGS.put("cheat.battle_autowin", "Taktischer Auto-Win");
        DE_STRINGS.put("cheat.battle_autowin.desc", "Sichert bedingungslosen Moralsieg in allen Feldschlachten.");

        DE_STRINGS.put("tier.off", "AUS");
        DE_STRINGS.put("tier.normal", "2x");
        DE_STRINGS.put("tier.high", "5x");
        DE_STRINGS.put("tier.extreme", "10x");
        DE_STRINGS.put("tier.god", "100x");

        DE_STRINGS.put("btn.apply", "Anwenden");
        DE_STRINGS.put("btn.close", "Schließen");
        DE_STRINGS.put("btn.tutorial", "Tutorial & Handbuch");
        DE_STRINGS.put("btn.language", "Sprache: Deutsch");
        DE_STRINGS.put("status.active", "AKTIV");
        DE_STRINGS.put("status.inactive", "INAKTIV");
        DE_STRINGS.put("status.instant_triggered", "Sofortbau ausgeführt!");
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
