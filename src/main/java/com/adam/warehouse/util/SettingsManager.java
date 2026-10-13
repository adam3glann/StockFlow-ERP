package com.adam.warehouse.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;

public final class SettingsManager {

    private static final Path SETTINGS_FILE =
            Path.of("settings.properties");

    private static final String LANGUAGE_KEY =
            "language";

    private static final String DARK_MODE_KEY =
            "darkMode";

    private static boolean darkMode = false;

    private static final Properties properties =
            new Properties();

    private SettingsManager() {

    }

    public static void load() {

        try {

            if (Files.exists(SETTINGS_FILE)) {

                try (InputStream input =
                             Files.newInputStream(SETTINGS_FILE)) {

                    properties.load(input);

                }

            }

            String language = properties.getProperty(LANGUAGE_KEY, "en");
            if (!"ar".equalsIgnoreCase(language) && !"en".equalsIgnoreCase(language)) language = "en";

            LanguageManager.setLanguage(
                    java.util.Locale.forLanguageTag(language)
            );

            darkMode = Boolean.parseBoolean(
                    properties.getProperty(DARK_MODE_KEY, "false")
            );

        } catch (IOException exception) {

            System.err.println("Unable to load local appearance settings.");

        }

    }

    public static void saveLanguage(Locale locale) {
        save(locale, darkMode);
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void saveDarkMode(boolean enabled) {
        save(LanguageManager.getLocale(), enabled);
    }

    /** Saves both preferences together so the UI never reports a partial save. */
    public static synchronized boolean save(Locale locale, boolean enabled) {
        if (locale == null) return false;
        Properties updated = new Properties();
        updated.putAll(properties);
        updated.setProperty(LANGUAGE_KEY, locale.getLanguage());
        updated.setProperty(DARK_MODE_KEY, Boolean.toString(enabled));
        Path temporary = SETTINGS_FILE.resolveSibling(SETTINGS_FILE.getFileName() + ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                updated.store(output, "StockFlow ERP Settings");
            }
            try {
                Files.move(temporary, SETTINGS_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveUnavailable) {
                Files.move(temporary, SETTINGS_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            properties.clear();
            properties.putAll(updated);
            darkMode = enabled;
            return true;
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            System.err.println("Unable to save local application settings.");
            return false;
        }
    }

}
