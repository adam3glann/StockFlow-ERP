package com.adam.warehouse.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

        try {

            properties.setProperty(
                    LANGUAGE_KEY,
                    locale.getLanguage()
            );

            try (OutputStream output =
                         Files.newOutputStream(SETTINGS_FILE)) {

                properties.store(
                        output,
                        "StockFlow ERP Settings"
                );

            }

        } catch (IOException exception) {

            System.err.println("Unable to save local language settings.");

        }

    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void saveDarkMode(boolean enabled) {

        darkMode = enabled;

        try {

            properties.setProperty(
                    DARK_MODE_KEY,
                    Boolean.toString(enabled)
            );

            try (OutputStream output =
                         Files.newOutputStream(SETTINGS_FILE)) {

                properties.store(
                        output,
                        "StockFlow ERP Settings"
                );

            }

        } catch (IOException exception) {

            System.err.println("Unable to save local appearance settings.");

        }

    }

}
