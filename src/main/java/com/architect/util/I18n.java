package com.architect.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * High-performance, allocation-minimal bilingual localization system for Syx Architect.
 * Supports on-the-fly toggling between English and German.
 * Strictly enforces UTF-8 reading and automatic ASCII sanitization to guarantee 0% Font encoding issues in Snake2D.
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
        // Built-in English dictionary (Clean ASCII)
        EN_STRINGS.put("mod.name", "Syx Architect");
        EN_STRINGS.put("mod.desc", "Creative, Schoenbau & Sandbox Suite for Songs of Syx.");
        EN_STRINGS.put("ui.title", "Syx Architect - Creative Suite");
        EN_STRINGS.put("ui.presets", "Quick Presets:");
        EN_STRINGS.put("preset.god", "God Mode (Model Building)");
        EN_STRINGS.put("preset.living", "Living City (Free Mats + Work)");
        EN_STRINGS.put("preset.vanilla", "Reset All to Vanilla");

        EN_STRINGS.put("ui.tab.city", "[City & Economy]");
        EN_STRINGS.put("ui.tab.world", "[World & Population]");
        EN_STRINGS.put("ui.tab.boosts", "Boosts & Science");
        EN_STRINGS.put("ui.tab.logistics", "Logistics & Storage");
        EN_STRINGS.put("ui.tab.construction", "Construction & Decor");
        EN_STRINGS.put("ui.tab.security", "Security & Peace");
        EN_STRINGS.put("ui.tab.settings", "Settings");

        EN_STRINGS.put("cheat.knowledge", "Knowledge & Innovation");
        EN_STRINGS.put("cheat.knowledge.desc", "Locks research against forgetting or grants instant omniscience.");
        EN_STRINGS.put("cheat.maintenance", "Building Maintenance");
        EN_STRINGS.put("cheat.maintenance.desc", "Decouples janitor repair work from material costs.");
        EN_STRINGS.put("cheat.spoilage", "Infinite Shelf Life");
        EN_STRINGS.put("cheat.spoilage.desc", "Stored goods, food, and rations will never spoil.");
        EN_STRINGS.put("cheat.loyalty", "Absolute Loyalty & Peace");
        EN_STRINGS.put("cheat.loyalty.desc", "Maximizes citizen happiness and eliminates rebellion risks.");
        EN_STRINGS.put("cheat.construction", "Construction Mode");
        EN_STRINGS.put("cheat.construction.desc", "Free building materials with active workers or instant build.");
        EN_STRINGS.put("cheat.logistics", "Warehouse Auto-Refill");
        EN_STRINGS.put("cheat.logistics.desc", "Keeps stockpiles and raw materials supplied automatically.");
        EN_STRINGS.put("cheat.infinite_denari", "Infinite Treasury (Min. 1M Denari)");
        EN_STRINGS.put("cheat.infinite_denari.desc", "Keeps city treasury automatically stocked with at least 1,000,000 Denari.");
        EN_STRINGS.put("cheat.raid_protection", "Raid Immunity");
        EN_STRINGS.put("cheat.raid_protection.desc", "Automatically cancels hostile raids and barbarian invasions.");
        EN_STRINGS.put("cheat.battle_autowin", "Combat Victory");
        EN_STRINGS.put("cheat.battle_autowin.desc", "Ensures absolute victory in all tactical battles and sieges.");

        EN_STRINGS.put("tier.maintenance.vanilla", "Vanilla");
        EN_STRINGS.put("tier.maintenance.janitors_free", "Janitors Work (Free Mats)");
        EN_STRINGS.put("tier.maintenance.no_decay", "Zero Decay (No Wear)");

        EN_STRINGS.put("tier.construction.vanilla", "Vanilla");
        EN_STRINGS.put("tier.construction.free_work", "Free Materials (Keep Work)");
        EN_STRINGS.put("tier.construction.fast_free", "Fast & Free (10x Speed)");
        EN_STRINGS.put("tier.construction.instant", "Instant Build");

        EN_STRINGS.put("tier.knowledge.vanilla", "Vanilla");
        EN_STRINGS.put("tier.knowledge.no_forgetting", "No Forgetting (High-Watermark)");
        EN_STRINGS.put("tier.knowledge.omniscience", "Omniscience (+1M Tech)");

        EN_STRINGS.put("tier.logistics.vanilla", "Vanilla (Off)");
        EN_STRINGS.put("tier.logistics.raw_only", "Raw Materials (~80%)");
        EN_STRINGS.put("tier.logistics.all_crates", "All Assigned Crates (~80%)");

        EN_STRINGS.put("btn.instant_build_now", "[>>] Instant Build Now");
        EN_STRINGS.put("btn.fill_crates_100", "[++] Fill All Crates to 100%");
        EN_STRINGS.put("btn.add_denari_1m", "[$$] +1,000,000 Denari");
        EN_STRINGS.put("btn.win_combat_now", "[XX] Win Combat Now");
        EN_STRINGS.put("btn.apply", "Apply");
        EN_STRINGS.put("btn.close", "Close");
        EN_STRINGS.put("btn.tutorial", "Tutorial & Guide");
        EN_STRINGS.put("btn.language", "Language: English");
        EN_STRINGS.put("status.active", "ACTIVE");
        EN_STRINGS.put("status.inactive", "INACTIVE");
        EN_STRINGS.put("status.instant_triggered", "Instant construction executed!");
        EN_STRINGS.put("status.crates_filled", "All warehouse crates topped up to 100%!");
        EN_STRINGS.put("status.combat_won", "Victory achieved on all active combat fronts!");

        // World & Population Strings
        EN_STRINGS.put("world.population.title", "Population Manager (Instant Living City)");
        EN_STRINGS.put("world.population.species", "Species:");
        EN_STRINGS.put("world.btn.spawn10", "+10 Citizens");
        EN_STRINGS.put("world.btn.spawn50", "+50 Citizens");
        EN_STRINGS.put("world.btn.spawn100", "+100 Citizens");
        EN_STRINGS.put("world.btn.spawn_all_25", "[+] +25 Citizens (All Races)");
        EN_STRINGS.put("world.env.title", "Atmosphere, Longevity & Environment");
        EN_STRINGS.put("world.toggle.aging", "Disable Aging & Old Age Death");
        EN_STRINGS.put("world.toggle.weather", "Freeze Clear Daylight & Weather");
        EN_STRINGS.put("world.toggle.fertility", "Maximum Soil Moisture & Fertility (100%)");
        EN_STRINGS.put("world.btn.fertility_now", "[>>] Set Map Soil Moisture to 100% Now");

        // Built-in German dictionary (Clean ASCII)
        DE_STRINGS.put("mod.name", "Syx Architect");
        DE_STRINGS.put("mod.desc", "Creative-, Schoenbau- & Sandbox-Suite fuer Songs of Syx.");
        DE_STRINGS.put("ui.title", "Syx Architect - Creative Suite");
        DE_STRINGS.put("ui.presets", "Schnellstart-Presets:");
        DE_STRINGS.put("preset.god", "God Mode (Modellbau)");
        DE_STRINGS.put("preset.living", "Lebendige Stadt (Gratis Material)");
        DE_STRINGS.put("preset.vanilla", "Alles auf Vanilla zuruecksetzen");

        DE_STRINGS.put("ui.tab.city", "[Stadt & Wirtschaft]");
        DE_STRINGS.put("ui.tab.world", "[Welt & Bevoelkerung]");
        DE_STRINGS.put("ui.tab.boosts", "Boosts & Forschung");
        DE_STRINGS.put("ui.tab.logistics", "Logistik & Lager");
        DE_STRINGS.put("ui.tab.construction", "Bau & Schoenbau");
        DE_STRINGS.put("ui.tab.security", "Sicherheit & Frieden");
        DE_STRINGS.put("ui.tab.settings", "Einstellungen");

        DE_STRINGS.put("cheat.knowledge", "Wissen & Innovation");
        DE_STRINGS.put("cheat.knowledge.desc", "Sichert erforschte Technologien und beschleunigt Innovation.");
        DE_STRINGS.put("cheat.maintenance", "Gebaeude-Wartung");
        DE_STRINGS.put("cheat.maintenance.desc", "Entkoppelt Hausmeister-Arbeit von Reparatur-Materialkosten.");
        DE_STRINGS.put("cheat.spoilage", "Kein Verderb (Ewige Haltbarkeit)");
        DE_STRINGS.put("cheat.spoilage.desc", "Gelagerte Nahrung und Gueter verderben niemals.");
        DE_STRINGS.put("cheat.loyalty", "Absolute Loyalitaet & Zufriedenheit");
        DE_STRINGS.put("cheat.loyalty.desc", "Maximiert Zufriedenheit und verhindert Aufstaende vollstaendig.");
        DE_STRINGS.put("cheat.construction", "Baustellen-Modus");
        DE_STRINGS.put("cheat.construction.desc", "Gratis-Baumaterialien mit echten Bauarbeitern oder Sofortbau.");
        DE_STRINGS.put("cheat.logistics", "Lagerhaus Auto-Befuellung");
        DE_STRINGS.put("cheat.logistics.desc", "Haelt Lagerkisten und Rohstoffe automatisch versorgt.");
        DE_STRINGS.put("cheat.infinite_denari", "Unendliche Schatzkammer (Min. 1 Mio. Denari)");
        DE_STRINGS.put("cheat.infinite_denari.desc", "Haelt die staedtische Schatzkammer automatisch auf mindestens 1.000.000 Denari.");
        DE_STRINGS.put("cheat.raid_protection", "Raid-Immunitaet");
        DE_STRINGS.put("cheat.raid_protection.desc", "Bricht feindliche Raids und Barbaren-Invasionen automatisch ab.");
        DE_STRINGS.put("cheat.battle_autowin", "Schlachten-Sieg");
        DE_STRINGS.put("cheat.battle_autowin.desc", "Sichert bedingungslosen Moralsieg in allen Feldschlachten und Belagerungen.");

        DE_STRINGS.put("tier.maintenance.vanilla", "Vanilla");
        DE_STRINGS.put("tier.maintenance.janitors_free", "Hausmeister aktiv (Gratis Material)");
        DE_STRINGS.put("tier.maintenance.no_decay", "Kein Verschleiss (Dauerhaft 100%)");

        DE_STRINGS.put("tier.construction.vanilla", "Vanilla");
        DE_STRINGS.put("tier.construction.free_work", "Gratis Material (Arbeit bleibt)");
        DE_STRINGS.put("tier.construction.fast_free", "Schnell & Gratis (10x Tempo)");
        DE_STRINGS.put("tier.construction.instant", "Sofortbau (Instant Build)");

        DE_STRINGS.put("tier.knowledge.vanilla", "Vanilla");
        DE_STRINGS.put("tier.knowledge.no_forgetting", "Nie wieder vergessen (High-Watermark)");
        DE_STRINGS.put("tier.knowledge.omniscience", "Allwissenheit (+1 Mio. Punkte)");

        DE_STRINGS.put("tier.logistics.vanilla", "Vanilla (Aus)");
        DE_STRINGS.put("tier.logistics.raw_only", "Nur Basis-Rohstoffe (~80%)");
        DE_STRINGS.put("tier.logistics.all_crates", "Alle zugewiesenen Kisten (~80%)");

        DE_STRINGS.put("btn.instant_build_now", "[>>] Sofortbau jetzt ausfuehren");
        DE_STRINGS.put("btn.fill_crates_100", "[++] Alle Kisten auf 100%");
        DE_STRINGS.put("btn.add_denari_1m", "[$$] +1.000.000 Denari");
        DE_STRINGS.put("btn.win_combat_now", "[XX] Sofort-Sieg im Kampf");
        DE_STRINGS.put("btn.apply", "Anwenden");
        DE_STRINGS.put("btn.close", "Schliessen");
        DE_STRINGS.put("btn.tutorial", "Tutorial & Handbuch");
        DE_STRINGS.put("btn.language", "Sprache: Deutsch");
        DE_STRINGS.put("status.active", "AKTIV");
        DE_STRINGS.put("status.inactive", "INAKTIV");
        DE_STRINGS.put("status.instant_triggered", "Sofortbau ausgefuehrt!");
        DE_STRINGS.put("status.crates_filled", "Alle Lagerkisten auf 100% aufgefuellt!");
        DE_STRINGS.put("status.combat_won", "Sieg auf allen aktiven Schlachtfeldern errungen!");

        // World & Population Strings
        DE_STRINGS.put("world.population.title", "Bevoelkerungs-Manager (Sofort-Gewusel)");
        DE_STRINGS.put("world.population.species", "Spezies:");
        DE_STRINGS.put("world.btn.spawn10", "+10 Buerger");
        DE_STRINGS.put("world.btn.spawn50", "+50 Buerger");
        DE_STRINGS.put("world.btn.spawn100", "+100 Buerger");
        DE_STRINGS.put("world.btn.spawn_all_25", "[+] +25 Buerger (Alle Rassen)");
        DE_STRINGS.put("world.env.title", "Atmosphere, Langlebigkeit & Umwelt");
        DE_STRINGS.put("world.toggle.aging", "Alterung & Alterstod deaktivieren");
        DE_STRINGS.put("world.toggle.weather", "Schoenes Bauwetter & Tageslicht fixieren");
        DE_STRINGS.put("world.toggle.fertility", "Maximale Bodenfeuchte & Fruchtbarkeit (100%)");
        DE_STRINGS.put("world.btn.fertility_now", "[>>] Bodenfeuchte sofort auf 100% setzen");

        // Load ClassLoader resources if packaged in JAR
        initFromJar();
    }

    private I18n() {}

    /**
     * Sanitizes strings to pure printable ASCII characters (32..126, \n, \t) to avoid
     * font mapping failures in Snake2D's sprite text engine.
     */
    public static String sanitizeAscii(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case 'ä': sb.append("ae"); break;
                case 'ö': sb.append("oe"); break;
                case 'ü': sb.append("ue"); break;
                case 'Ä': sb.append("Ae"); break;
                case 'Ö': sb.append("Oe"); break;
                case 'Ü': sb.append("Ue"); break;
                case 'ß': sb.append("ss"); break;
                case 'é': case 'è': case 'ê': sb.append('e'); break;
                case 'á': case 'à': case 'â': sb.append('a'); break;
                default:
                    if (c >= 32 && c <= 126) {
                        sb.append(c);
                    } else if (c == '\n' || c == '\t' || c == '\r') {
                        sb.append(c);
                    }
                    // Drop emojis and unmapped Unicode bytes
                    break;
            }
        }
        return sb.toString();
    }

    public static void initFromJar() {
        loadResourceSafely("strings_en.properties", Language.EN);
        loadResourceSafely("strings_de.properties", Language.DE);
    }

    private static void loadResourceSafely(String name, Language lang) {
        String[] candidatePaths = {
            "/" + name,
            "/assets/text/" + name,
            name,
            "assets/text/" + name
        };
        for (String path : candidatePaths) {
            try {
                InputStream in = I18n.class.getResourceAsStream(path);
                if (in == null && I18n.class.getClassLoader() != null) {
                    in = I18n.class.getClassLoader().getResourceAsStream(path);
                }
                if (in != null) {
                    loadProperties(in, lang);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
    }

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

    public static Map<String, String> getDictionary(Language lang) {
        return (lang == Language.DE) ? DE_STRINGS : EN_STRINGS;
    }

    /**
     * Loads localization properties using strict UTF-8 BufferedReader loading and ASCII sanitization.
     */
    public static void loadProperties(InputStream in, Language lang) {
        if (in == null || lang == null) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            Properties props = new Properties();
            props.load(reader);
            Map<String, String> target = (lang == Language.DE) ? DE_STRINGS : EN_STRINGS;
            for (String key : props.stringPropertyNames()) {
                String val = props.getProperty(key);
                if (val != null) {
                    target.put(key, sanitizeAscii(val));
                }
            }
        } catch (Exception e) {
            System.err.println("[SyxArchitect] Failed to load UTF-8 properties for " + lang + ": " + e.getMessage());
        }
    }

    public static String get(String key) {
        Map<String, String> dict = (current == Language.DE) ? DE_STRINGS : EN_STRINGS;
        String val = dict.get(key);
        if (val == null) {
            val = EN_STRINGS.get(key);
        }
        return val != null ? sanitizeAscii(val) : key;
    }

    public static String get(String key, Object... args) {
        String template = get(key);
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return sanitizeAscii(MessageFormat.format(template, args));
        } catch (Exception e) {
            return template;
        }
    }
}
