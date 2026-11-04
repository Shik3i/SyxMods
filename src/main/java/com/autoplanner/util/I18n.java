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
        EN_STRINGS.put("ui.overlay.room", "{0} ({1} tiles)");
        EN_STRINGS.put("ui.overlay.no_room", "No active room draft");

        EN_STRINGS.put("ui.panel.title", "Syx Auto-Planner Settings");
        EN_STRINGS.put("ui.panel.subtitle", "Intelligent room builder assistant for Songs of Syx");
        EN_STRINGS.put("ui.panel.style", "Packing Strategy");
        EN_STRINGS.put("ui.panel.style.desc", "Spacing density between workstation clusters");
        EN_STRINGS.put("ui.panel.style.compact", "Compact (Dense)");
        EN_STRINGS.put("ui.panel.style.balanced", "Balanced (Aisles)");
        EN_STRINGS.put("ui.panel.target_eff", "Target Efficiency");
        EN_STRINGS.put("ui.panel.target_eff.desc", "Required auxiliary modules per workstation");
        EN_STRINGS.put("ui.panel.storage", "Internal Storage");
        EN_STRINGS.put("ui.panel.storage.desc", "Floor area reserved for internal storage boxes");
        EN_STRINGS.put("ui.panel.storage.none", "Disabled (0%)");
        EN_STRINGS.put("ui.panel.storage.val", "{0}% Storage");
        EN_STRINGS.put("ui.panel.language", "Interface Language");
        EN_STRINGS.put("ui.panel.language.desc", "Display language for buttons, dialogs and tooltips");
        
        EN_STRINGS.put("btn.strut", "Auto-Strut");
        EN_STRINGS.put("btn.furnish", "Auto-Furnish");
        EN_STRINGS.put("btn.quickfill", "Quick-Build");
        EN_STRINGS.put("btn.clear", "Clear");
        EN_STRINGS.put("btn.settings", "Config");
        EN_STRINGS.put("btn.style.compact", "Style: Compact");
        EN_STRINGS.put("btn.style.balanced", "Style: Balanced");
        EN_STRINGS.put("btn.target_eff", "{0}% Target");
        EN_STRINGS.put("btn.storage_ratio", "{0}% Storage");
        EN_STRINGS.put("btn.language", "Language: EN");
        EN_STRINGS.put("btn.reset", "Defaults");
        EN_STRINGS.put("btn.close", "Close");
        EN_STRINGS.put("btn.snap", "Dock");

        EN_STRINGS.put("tooltip.strut", "Punches minimal support pillars to eliminate 100% yellow ceiling tiles");
        EN_STRINGS.put("tooltip.furnish", "Fills room with maximum workstations at 100% target efficiency");
        EN_STRINGS.put("tooltip.quickfill", "Executes Auto-Strut and Auto-Furnish in one click");
        EN_STRINGS.put("tooltip.clear", "Removes all placed furniture in this room draft");
        EN_STRINGS.put("tooltip.settings", "Opens Auto-Planner configuration (styles, target efficiency)");
        EN_STRINGS.put("tooltip.snap", "Snaps toolbar centered directly under the Room Editor panel");

        EN_STRINGS.put("msg.strut_success", "Auto-Strut: Placed {0} pillar(s). 0 yellow tiles remaining!");
        EN_STRINGS.put("msg.strut_none_needed", "Auto-Strut: Ceiling is already 100% supported!");
        EN_STRINGS.put("msg.strut_failed", "Auto-Strut: Could not place pillars for {0} yellow tile(s).");
        EN_STRINGS.put("msg.furnish_success", "Auto-Furnish: Placed {0} workstation(s) & {1} module(s). Eff: {2}%.");
        EN_STRINGS.put("msg.furnish_virtual_door", "Auto-Furnish: Placed {0} workstation(s) & {1} module(s) ({2}% eff). No door set - reserved path to wall ({3}, {4})!");
        EN_STRINGS.put("msg.furnish_fail_doors", "Auto-Furnish: Please place at least one door first!");
        EN_STRINGS.put("msg.furnish_no_workstations", "Auto-Furnish: No workstation items found for this blueprint.");
        EN_STRINGS.put("msg.clear_success", "Cleared all furniture from current room draft.");
        EN_STRINGS.put("msg.draw_room_first", "Please draw the room area on the floor first!");

        EN_STRINGS.put("hud.planner", "AUTO");
        EN_STRINGS.put("hud.tooltip_title", "Auto-Planner  [F9]");
        EN_STRINGS.put("hud.tooltip_desc", "Click opens Auto-Planner options and settings (Hotkeys: F9 / ESC).");

        // --- German Translations ---
        DE_STRINGS.put("mod.name", "Syx Auto-Planner");
        DE_STRINGS.put("mod.desc", "Intelligenter Bau-Assistent: Automatische Stützpfeiler & Möblierung für Songs of Syx.");
        
        DE_STRINGS.put("ui.overlay.title", "Auto-Planer");
        DE_STRINGS.put("ui.overlay.room", "{0} ({1} Felder)");
        DE_STRINGS.put("ui.overlay.no_room", "Kein Raum im Entwurf");

        DE_STRINGS.put("ui.panel.title", "Syx Auto-Planer Optionen");
        DE_STRINGS.put("ui.panel.subtitle", "Intelligenter Bau-Assistent für Songs of Syx");
        DE_STRINGS.put("ui.panel.style", "Platzierungs-Stil");
        DE_STRINGS.put("ui.panel.style.desc", "Dichte und Gänge zwischen Arbeitsplatz-Gruppen");
        DE_STRINGS.put("ui.panel.style.compact", "Kompakt (Dicht)");
        DE_STRINGS.put("ui.panel.style.balanced", "Ausgewogen (Gänge)");
        DE_STRINGS.put("ui.panel.target_eff", "Ziel-Effizienz");
        DE_STRINGS.put("ui.panel.target_eff.desc", "Benötigte Raumeffizienz für Hilfsmodule (100% empfohlen)");
        DE_STRINGS.put("ui.panel.storage", "Internes Lager");
        DE_STRINGS.put("ui.panel.storage.desc", "Fläche für interne Lagerkisten und Material-Puffer");
        DE_STRINGS.put("ui.panel.storage.none", "Deaktiviert (0%)");
        DE_STRINGS.put("ui.panel.storage.val", "{0}% Lager");
        DE_STRINGS.put("ui.panel.language", "Benutzeroberfläche");
        DE_STRINGS.put("ui.panel.language.desc", "Sprache für Schaltflächen, Menüs und Hilfetexte");
        
        DE_STRINGS.put("btn.strut", "Auto-Stützen");
        DE_STRINGS.put("btn.furnish", "Auto-Möbel");
        DE_STRINGS.put("btn.quickfill", "Schnellbau");
        DE_STRINGS.put("btn.clear", "Leeren");
        DE_STRINGS.put("btn.settings", "Optionen");
        DE_STRINGS.put("btn.style.compact", "Stil: Kompakt");
        DE_STRINGS.put("btn.style.balanced", "Stil: Ausgewogen");
        DE_STRINGS.put("btn.target_eff", "{0}% Ziel");
        DE_STRINGS.put("btn.storage_ratio", "{0}% Lager");
        DE_STRINGS.put("btn.language", "Sprache: DE");
        DE_STRINGS.put("btn.reset", "Standard");
        DE_STRINGS.put("btn.close", "Schließen");
        DE_STRINGS.put("btn.snap", "Anheften");

        DE_STRINGS.put("tooltip.strut", "Setzt minimale Pfeiler, um 100% gelbe/teure Deckenbereiche zu entfernen");
        DE_STRINGS.put("tooltip.furnish", "Füllt den Raum mit maximalen Workstations bei 100% Ziel-Effizienz");
        DE_STRINGS.put("tooltip.quickfill", "Führt Auto-Stützen und Auto-Möblierung mit einem Klick aus");
        DE_STRINGS.put("tooltip.clear", "Entfernt alle platzierten Möbel aus diesem Entwurf");
        DE_STRINGS.put("tooltip.settings", "Öffnet die Auto-Planner Einstellungen (Stil, Ziel-Effizienz)");
        DE_STRINGS.put("tooltip.snap", "Dockt die Toolbar wieder zentriert direkt unter dem Raum-Editor an");

        DE_STRINGS.put("msg.strut_success", "Auto-Stützen: {0} Pfeiler gesetzt. 0 gelbe Bereiche übrig!");
        DE_STRINGS.put("msg.strut_none_needed", "Auto-Stützen: Die Decke ist bereits zu 100% gestützt!");
        DE_STRINGS.put("msg.strut_failed", "Auto-Stützen: Konnte keine Pfeiler für {0} gelbe Bereiche setzen.");
        DE_STRINGS.put("msg.furnish_success", "Auto-Möblieren: {0} Station(en) & {1} Modul(e) platziert. Eff: {2}%.");
        DE_STRINGS.put("msg.furnish_virtual_door", "Auto-Möblieren: {0} Station(en) & {1} Modul(e) ({2}% Eff.). Keine Tür gesetzt - Pfad zu Wand ({3}, {4}) reserviert!");
        DE_STRINGS.put("msg.furnish_fail_doors", "Auto-Möblieren: Bitte zuerst mindestens eine Tür setzen!");
        DE_STRINGS.put("msg.furnish_no_workstations", "Auto-Möblieren: Keine Arbeitsstationen für diesen Raumtyp.");
        DE_STRINGS.put("msg.clear_success", "Alle Einrichtungsgegenstände im Raum-Entwurf entfernt.");
        DE_STRINGS.put("msg.draw_room_first", "Bitte zeichne zuerst die Raumfläche auf dem Boden!");

        DE_STRINGS.put("hud.planner", "AUTO");
        DE_STRINGS.put("hud.tooltip_title", "Auto-Planner  [F9]");
        DE_STRINGS.put("hud.tooltip_desc", "Klick öffnet Auto-Strut & Möbel-Optionen (Hotkeys: F9 / ESC).");
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
