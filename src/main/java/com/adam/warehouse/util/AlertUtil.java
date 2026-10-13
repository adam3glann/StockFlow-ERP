package com.adam.warehouse.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Creates alerts that inherit the active application theme and text direction. */
public final class AlertUtil {
    private AlertUtil() { }

    public static Alert create(Alert.AlertType type) {
        return configure(new Alert(type));
    }

    public static Alert create(Alert.AlertType type, String contentText) {
        Alert alert = new Alert(type);
        alert.setContentText(contentText);
        return configure(alert);
    }

    public static Alert create(Alert.AlertType type, String contentText, ButtonType... buttons) {
        return configure(new Alert(type, contentText, buttons));
    }

    private static Alert configure(Alert alert) {
        alert.getDialogPane().sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) ThemeManager.applyTheme(newScene);
        });
        return alert;
    }
}
