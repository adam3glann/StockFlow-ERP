package com.adam.warehouse.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public final class SettingsManager {

    private static final Path SETTINGS_FILE =
            Path.of("settings.properties");

    private static final String LANGUAGE_KEY =
            "language";

    private static final String DARK_MODE_KEY =
            "darkMode";

    private static boolean darkMode = false;
    private static String branchName = "";
    private static String branchLocation = "";
    private static final ReadOnlyStringWrapper branchDisplay = new ReadOnlyStringWrapper();

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
            branchName = properties.getProperty("branchName", "").trim();
            branchLocation = properties.getProperty("branchLocation", "").trim();
            updateBranchDisplay();

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

    public static String getBranchName() { return branchName; }

    public static String getBranchLocation() { return branchLocation; }

    public static ReadOnlyStringProperty branchDisplayProperty() { return branchDisplay.getReadOnlyProperty(); }

    public static void refreshBranchDisplay() { updateBranchDisplay(); }

    /** Saves both preferences together so the UI never reports a partial save. */
    public static synchronized boolean save(Locale locale, boolean enabled) {
        return save(locale, enabled, branchName, branchLocation);
    }

    public static synchronized boolean save(Locale locale, boolean enabled, String newBranchName, String newBranchLocation) {
        if (locale == null) return false;
        String normalizedName = newBranchName == null ? "" : newBranchName.trim();
        String normalizedLocation = newBranchLocation == null ? "" : newBranchLocation.trim();
        Properties updated = new Properties();
        updated.putAll(properties);
        updated.setProperty(LANGUAGE_KEY, locale.getLanguage());
        updated.setProperty(DARK_MODE_KEY, Boolean.toString(enabled));
        updated.setProperty("branchName", normalizedName);
        updated.setProperty("branchLocation", normalizedLocation);
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
            branchName = normalizedName;
            branchLocation = normalizedLocation;
            updateBranchDisplay();
            return true;
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            System.err.println("Unable to save local application settings.");
            return false;
        }
    }

    private static void updateBranchDisplay() {
        String displayName = branchName.isBlank() ? LanguageManager.get("branch.default.name") : branchName;
        String displayLocation = branchLocation;
        branchDisplay.set(displayLocation.isBlank()
                ? displayName
                : String.format(LanguageManager.get("branch.header.format"), displayName, displayLocation));
    }

}
