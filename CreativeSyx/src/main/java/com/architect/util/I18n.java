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
        EN_STRINGS.put("ui.presets", "Quick Presets");
        EN_STRINGS.put("ui.actions", "Instant Actions");
        EN_STRINGS.put("preset.god", "God Mode (Instant)");
        EN_STRINGS.put("preset.god.desc", "Instant construction, zero decay, omniscience, full crates, and infinite treasury.");
        EN_STRINGS.put("preset.living", "Living City (Free Mats)");
        EN_STRINGS.put("preset.living.desc", "Free building materials with real worker bustling and active maintenance.");
        EN_STRINGS.put("preset.vanilla", "Reset to Vanilla");
        EN_STRINGS.put("preset.vanilla.desc", "Resets all cheat tiers and boosts back to default game rules.");

        EN_STRINGS.put("ui.tab.city", "City & Economy");
        EN_STRINGS.put("ui.tab.world", "World & Population");
        EN_STRINGS.put("ui.tab.boosts", "Science");
        EN_STRINGS.put("ui.tab.logistics", "Logistics");
        EN_STRINGS.put("ui.tab.construction", "Build & Maintain");
        EN_STRINGS.put("ui.tab.security", "Security");
        EN_STRINGS.put("ui.tab.settings", "Settings");

        EN_STRINGS.put("cheat.knowledge", "Knowledge");
        EN_STRINGS.put("cheat.knowledge.desc", "Locks research against forgetting or grants 1,000,000 innovation points.");
        EN_STRINGS.put("cheat.maintenance", "Maintenance");
        EN_STRINGS.put("cheat.maintenance.desc", "Decouples janitor repair work from material costs or freezes wear at 100%.");
        EN_STRINGS.put("cheat.spoilage", "Shelf Life");
        EN_STRINGS.put("cheat.spoilage.desc", "Stored goods and food rations never spoil in warehouses.");
        EN_STRINGS.put("cheat.loyalty", "Loyalty");
        EN_STRINGS.put("cheat.loyalty.desc", "Maximizes citizen happiness and eliminates rebellion and riot risks.");
        EN_STRINGS.put("cheat.construction", "Construction");
        EN_STRINGS.put("cheat.construction.desc", "Free building materials with workers, 10x accelerated speed, or instant build.");
        EN_STRINGS.put("cheat.logistics", "Storage Refill");
        EN_STRINGS.put("cheat.logistics.desc", "Keeps stockpiles and raw materials supplied automatically to ~80%.");
        EN_STRINGS.put("cheat.infinite_denari", "Treasury");
        EN_STRINGS.put("cheat.infinite_denari.desc", "Keeps city treasury automatically stocked with at least 1,000,000 Denari.");
        EN_STRINGS.put("cheat.raid_protection", "Raid Immunity");
        EN_STRINGS.put("cheat.raid_protection.desc", "Automatically cancels hostile raids and barbarian incursions.");
        EN_STRINGS.put("cheat.battle_autowin", "Combat");
        EN_STRINGS.put("cheat.battle_autowin.desc", "Ensures unconditional morale victory in all tactical battles and sieges.");
        EN_STRINGS.put("cheat.instant_jobs", "Instant Jobs");
        EN_STRINGS.put("cheat.instant_jobs.desc", "Jobs like tree felling, road demolition, wall disassembly, and rock clearing complete instantly.");

        EN_STRINGS.put("tier.maintenance.vanilla", "Vanilla");
        EN_STRINGS.put("tier.maintenance.janitors_free", "Free Janitors");
        EN_STRINGS.put("tier.maintenance.no_decay", "Zero Decay (100%)");

        EN_STRINGS.put("tier.construction.vanilla", "Vanilla");
        EN_STRINGS.put("tier.construction.free_work", "Free Materials");
        EN_STRINGS.put("tier.construction.fast_free", "10x Speed & Free");
        EN_STRINGS.put("tier.construction.instant", "Instant Build");

        EN_STRINGS.put("tier.knowledge.vanilla", "Vanilla");
        EN_STRINGS.put("tier.knowledge.no_forgetting", "No Forgetting");
        EN_STRINGS.put("tier.knowledge.omniscience", "Omniscience (+1M)");

        EN_STRINGS.put("tier.logistics.vanilla", "Off (Vanilla)");
        EN_STRINGS.put("tier.logistics.raw_only", "Raw Materials (~80%)");
        EN_STRINGS.put("tier.logistics.all_crates", "All Crates (~80%)");

        EN_STRINGS.put("btn.instant_build_now", "Instant Build Now");
        EN_STRINGS.put("btn.fill_crates_100", "Fill Crates to 100%");
        EN_STRINGS.put("btn.add_denari_1m", "+1M Denari");
        EN_STRINGS.put("btn.win_combat_now", "Win Combat Now");
        EN_STRINGS.put("btn.apply", "Apply");
        EN_STRINGS.put("btn.close", "Close (ESC / F10)");
        EN_STRINGS.put("btn.tutorial", "Manual");
        EN_STRINGS.put("btn.language", "Language: EN");
        EN_STRINGS.put("status.active", "ACTIVE");
        EN_STRINGS.put("status.inactive", "OFF");
        EN_STRINGS.put("status.instant_triggered", "Instant construction executed!");
        EN_STRINGS.put("status.crates_filled", "All warehouse crates topped up to 100%!");
        EN_STRINGS.put("status.combat_won", "Victory achieved on all active combat fronts!");

        // World & Population Strings
        EN_STRINGS.put("world.population.title", "Population Manager");
        EN_STRINGS.put("world.population.species", "Species:");
        EN_STRINGS.put("world.btn.spawn10", "+10 Citizens");
        EN_STRINGS.put("world.btn.spawn50", "+50 Citizens");
        EN_STRINGS.put("world.btn.spawn100", "+100 Citizens");
        EN_STRINGS.put("world.btn.spawn_all_25", "+25 All Races");
        EN_STRINGS.put("world.env.title", "Atmosphere & Nature");
        EN_STRINGS.put("world.toggle.aging", "Disable Aging");
        EN_STRINGS.put("world.toggle.weather", "Clear Weather");
        EN_STRINGS.put("world.toggle.fertility", "100% Soil Moisture");
        EN_STRINGS.put("world.btn.fertility_now", "Set Moisture to 100%");

        // Built-in German dictionary (Clean ASCII)
        DE_STRINGS.put("mod.name", "Syx Architect");
        DE_STRINGS.put("mod.desc", "Creative-, Schoenbau- & Sandbox-Suite fuer Songs of Syx.");
        DE_STRINGS.put("ui.title", "Syx Architect - Creative Suite");
        DE_STRINGS.put("ui.presets", "Schnellstart-Profile");
        DE_STRINGS.put("ui.actions", "Sofort-Aktionen");
        DE_STRINGS.put("preset.god", "God Mode (Sofortbau)");
        DE_STRINGS.put("preset.god.desc", "Sofortbau, kein Verschleiss, Allwissenheit, volle Lager und unendliche Schatzkammer.");
        DE_STRINGS.put("preset.living", "Lebendige Stadt (Gratis)");
        DE_STRINGS.put("preset.living.desc", "Gratis Baumaterialien, aber echte Arbeiter fuer lebendiges Stadtgewusel.");
        DE_STRINGS.put("preset.vanilla", "Alles auf Vanilla");
        DE_STRINGS.put("preset.vanilla.desc", "Setzt alle Cheats und Boosts vollstaendig auf Vanilla-Werte zurueck.");

        DE_STRINGS.put("ui.tab.city", "Stadt & Wirtschaft");
        DE_STRINGS.put("ui.tab.world", "Welt & Bevoelkerung");
        DE_STRINGS.put("ui.tab.boosts", "Wissenschaft");
        DE_STRINGS.put("ui.tab.logistics", "Logistik");
        DE_STRINGS.put("ui.tab.construction", "Bauen & Wartung");
        DE_STRINGS.put("ui.tab.security", "Sicherheit");
        DE_STRINGS.put("ui.tab.settings", "Optionen");

        DE_STRINGS.put("cheat.knowledge", "Wissen");
        DE_STRINGS.put("cheat.knowledge.desc", "Sichert erforschte Technologien gegen Vergessen oder gewaehrt 1.000.000 Innovationspunkte.");
        DE_STRINGS.put("cheat.maintenance", "Wartung");
        DE_STRINGS.put("cheat.maintenance.desc", "Entkoppelt Hausmeister-Arbeit von Materialkosten oder friert Verschleiss auf 100% ein.");
        DE_STRINGS.put("cheat.spoilage", "Haltbarkeit");
        DE_STRINGS.put("cheat.spoilage.desc", "Gelagerte Nahrung und Gueter in Lagerhaeusern verderben niemals.");
        DE_STRINGS.put("cheat.loyalty", "Loyalitaet");
        DE_STRINGS.put("cheat.loyalty.desc", "Maximiert Zufriedenheit und verhindert Rebellionen und Aufstaende.");
        DE_STRINGS.put("cheat.construction", "Baustellen");
        DE_STRINGS.put("cheat.construction.desc", "Gratis Baumaterialien mit echten Arbeitern, 10x Tempo oder Sofortbau.");
        DE_STRINGS.put("cheat.logistics", "Lager-Refill");
        DE_STRINGS.put("cheat.logistics.desc", "Haelt Lagerkisten und Rohstoffe automatisch auf ca. 80% Fuellstand.");
        DE_STRINGS.put("cheat.infinite_denari", "Schatzkammer");
        DE_STRINGS.put("cheat.infinite_denari.desc", "Haelt die staedtische Schatzkammer automatisch auf mindestens 1.000.000 Denari.");
        DE_STRINGS.put("cheat.raid_protection", "Raid-Schutz");
        DE_STRINGS.put("cheat.raid_protection.desc", "Bricht feindliche Raids und Barbaren-Invasionen automatisch ab.");
        DE_STRINGS.put("cheat.battle_autowin", "Schlachten");
        DE_STRINGS.put("cheat.battle_autowin.desc", "Sichert bedingungslosen Moralsieg in allen Feldschlachten und Belagerungen.");
        DE_STRINGS.put("cheat.instant_jobs", "Sofort-Auftraege");
        DE_STRINGS.put("cheat.instant_jobs.desc", "Baeume faellen, Strassen abreissen, Mauern zerlegen und Felsen werden sofort erledigt.");

        DE_STRINGS.put("tier.maintenance.vanilla", "Vanilla");
        DE_STRINGS.put("tier.maintenance.janitors_free", "Gratis Hausmeister");
        DE_STRINGS.put("tier.maintenance.no_decay", "Kein Verschleiss (100%)");

        DE_STRINGS.put("tier.construction.vanilla", "Vanilla");
        DE_STRINGS.put("tier.construction.free_work", "Gratis Material");
        DE_STRINGS.put("tier.construction.fast_free", "10x Tempo & Gratis");
        DE_STRINGS.put("tier.construction.instant", "Sofortbau (Instant)");

        DE_STRINGS.put("tier.knowledge.vanilla", "Vanilla");
        DE_STRINGS.put("tier.knowledge.no_forgetting", "Nie vergessen");
        DE_STRINGS.put("tier.knowledge.omniscience", "Allwissenheit (+1M)");

        DE_STRINGS.put("tier.logistics.vanilla", "Aus (Vanilla)");
        DE_STRINGS.put("tier.logistics.raw_only", "Rohstoffe (~80%)");
        DE_STRINGS.put("tier.logistics.all_crates", "Alle Kisten (~80%)");

        DE_STRINGS.put("btn.instant_build_now", "Sofortbau jetzt");
        DE_STRINGS.put("btn.fill_crates_100", "Kisten auf 100%");
        DE_STRINGS.put("btn.add_denari_1m", "+1 Mio. Denari");
        DE_STRINGS.put("btn.win_combat_now", "Sofort-Sieg");
        DE_STRINGS.put("btn.apply", "Anwenden");
        DE_STRINGS.put("btn.close", "Schliessen (ESC / F10)");
        DE_STRINGS.put("btn.tutorial", "Handbuch");
        DE_STRINGS.put("btn.language", "Sprache: DE");
        DE_STRINGS.put("status.active", "AKTIV");
        DE_STRINGS.put("status.inactive", "AUS");
        DE_STRINGS.put("status.instant_triggered", "Sofortbau ausgefuehrt!");
        DE_STRINGS.put("status.crates_filled", "Alle Lagerkisten auf 100% aufgefuellt!");
        DE_STRINGS.put("status.combat_won", "Sieg auf allen aktiven Schlachtfeldern errungen!");

        // World & Population Strings
        DE_STRINGS.put("world.population.title", "Bevoelkerungs-Manager");
        DE_STRINGS.put("world.population.species", "Spezies:");
        DE_STRINGS.put("world.btn.spawn10", "+10 Buerger");
        DE_STRINGS.put("world.btn.spawn50", "+50 Buerger");
        DE_STRINGS.put("world.btn.spawn100", "+100 Buerger");
        DE_STRINGS.put("world.btn.spawn_all_25", "+25 Alle Rassen");
        DE_STRINGS.put("world.env.title", "Atmosphaere & Natur");
        DE_STRINGS.put("world.toggle.aging", "Alterung stoppen");
        DE_STRINGS.put("world.toggle.weather", "Schoenes Wetter");
        DE_STRINGS.put("world.toggle.fertility", "100% Bodenfeuchte");
        DE_STRINGS.put("world.btn.fertility_now", "Feuchte auf 100%");

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
