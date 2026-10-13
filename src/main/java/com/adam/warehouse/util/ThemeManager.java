package com.adam.warehouse.util;

import javafx.scene.Scene;
import javafx.geometry.NodeOrientation;

/**
 * Applies the app's stylesheet(s) to a Scene, adding the dark theme override on top
 * of the base stylesheet when dark mode is enabled. Centralized here so every window
 * (main app, login, and every dialog) themes consistently instead of each hardcoding
 * its own getResource("/styles/application.css") call.
 */
public final class ThemeManager {

    private static final String BASE_STYLESHEET = "/styles/application.css";
    private static final String DARK_STYLESHEET = "/styles/dark-theme.css";

    private ThemeManager() {
    }

    public static boolean isDarkMode() {
        return SettingsManager.isDarkMode();
    }

    public static void applyTheme(Scene scene) {

        if (scene == null) {
            return;
        }

        scene.getRoot().setNodeOrientation(LanguageManager.getLocale().getLanguage().equals("ar")
                ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        scene.getStylesheets().clear();

        scene.getStylesheets().add(
                ThemeManager.class.getResource(BASE_STYLESHEET).toExternalForm()
        );

        if (isDarkMode()) {
            scene.getStylesheets().add(
                    ThemeManager.class.getResource(DARK_STYLESHEET).toExternalForm()
            );
        }
    }
}
