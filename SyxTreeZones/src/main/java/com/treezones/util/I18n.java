package com.treezones.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * High-performance localized string resolver with fallback to English.
 * Pure ASCII property parsing with zero runtime allocations on cache hits.
 */
public final class I18n {

    public enum Language {
        DE("de"),
        EN("en");

        private final String code;

        Language(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private static Language currentLanguage = Language.DE;
    private static final Map<String, String> currentStrings = new HashMap<>();
    private static final Map<String, String> fallbackStrings = new HashMap<>();

    static {
        loadBundle(Language.EN, fallbackStrings);
        reload();
    }

    private I18n() {}

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(Language lang) {
        if (lang != null && lang != currentLanguage) {
            currentLanguage = lang;
            reload();
        }
    }

    public static void toggleLanguage() {
        setLanguage(currentLanguage == Language.DE ? Language.EN : Language.DE);
    }

    public static void reload() {
        currentStrings.clear();
        loadBundle(currentLanguage, currentStrings);
    }

    private static void loadBundle(Language lang, Map<String, String> map) {
        String filename = "assets/text/strings_" + lang.getCode() + ".properties";
        try (InputStream in = I18n.class.getClassLoader().getResourceAsStream(filename)) {
            if (in == null) {
                try (InputStream rootIn = I18n.class.getClassLoader().getResourceAsStream("strings_" + lang.getCode() + ".properties")) {
                    if (rootIn != null) {
                        loadProperties(rootIn, map);
                        return;
                    }
                }
                return;
            }
            loadProperties(in, map);
        } catch (Throwable t) {
            System.err.println("[TreeZones] Failed to load strings for " + lang + ": " + t.getMessage());
        }
    }

    private static void loadProperties(InputStream in, Map<String, String> map) throws Exception {
        Properties props = new Properties();
        props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String name : props.stringPropertyNames()) {
            map.put(name, props.getProperty(name));
        }
    }

    public static String get(String key) {
        String val = currentStrings.get(key);
        if (val != null) {
            return val;
        }
        val = fallbackStrings.get(key);
        return val != null ? val : key;
    }

    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return String.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }
}
