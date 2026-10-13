package com.adam.warehouse.view;

import com.adam.warehouse.service.ApplicationResetService;
import com.adam.warehouse.util.AlertUtil;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.util.SettingsManager;
import com.adam.warehouse.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Optional;

public class SettingsView extends BorderPane {
    private final ComboBox<String> languageComboBox = new ComboBox<>();
    private final TextField branchNameField = new TextField();
    private final TextField branchLocationField = new TextField();
    private final CheckBox darkModeCheckBox = new CheckBox(LanguageManager.get("settings.dark.mode"));
    private final Button saveButton = new Button(LanguageManager.get("save"));
    private final Button resetButton = new Button(LanguageManager.get("settings.reset.button"));

    public SettingsView() {
        setPadding(new Insets(24));
        createUI();
        loadSettings();
        saveButton.setOnAction(event -> saveSettings());
        resetButton.setOnAction(event -> confirmAndReset());
    }

    private void createUI() {
        Label title = new Label(LanguageManager.get("settings"));
        title.getStyleClass().add("page-title");

        languageComboBox.getItems().setAll(LanguageManager.get("english"), LanguageManager.get("arabic"));
        languageComboBox.setMaxWidth(Double.MAX_VALUE);
        darkModeCheckBox.setWrapText(true);

        branchNameField.setPromptText(LanguageManager.get("branch.name.prompt"));
        branchLocationField.setPromptText(LanguageManager.get("branch.location.prompt"));

        VBox branchInfo = new VBox(14,
                sectionTitle("branch.settings.title"),
                labeledControl(LanguageManager.get("branch.name"), branchNameField),
                labeledControl(LanguageManager.get("branch.location"), branchLocationField));
        branchInfo.getStyleClass().add("settings-card");

        VBox appearance = new VBox(14,
                sectionTitle("settings.appearance"),
                labeledControl(LanguageManager.get("language"), languageComboBox),
                darkModeCheckBox);
        appearance.getStyleClass().add("settings-card");
        saveButton.getStyleClass().add("primary-button");
        HBox saveRow = new HBox(saveButton);
        saveRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label resetDescription = new Label(LanguageManager.get("settings.reset.description"));
        resetDescription.setWrapText(true);
        Label kept = new Label(LanguageManager.get("settings.reset.kept"));
        kept.setWrapText(true);
        Label cleared = new Label(LanguageManager.get("settings.reset.cleared"));
        cleared.setWrapText(true);
        resetButton.getStyleClass().add("danger-button");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        VBox reset = new VBox(12, sectionTitle("settings.reset.title"), resetDescription, kept, cleared, resetButton);
        reset.getStyleClass().addAll("settings-card", "settings-danger-card");

        VBox content = new VBox(18, title, branchInfo, appearance, saveRow, reset);
        content.setMaxWidth(720);
        VBox.setVgrow(content, Priority.NEVER);
        setCenter(content);
    }

    private Label sectionTitle(String key) {
        Label label = new Label(LanguageManager.get(key));
        label.getStyleClass().add("settings-section-title");
        return label;
    }

    private HBox labeledControl(String labelText, Control control) {
        Label label = new Label(labelText);
        HBox row = new HBox(18, label, control);
        row.setFillHeight(true);
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }

    private void loadSettings() {
        darkModeCheckBox.setSelected(SettingsManager.isDarkMode());
        String configuredName = SettingsManager.getBranchName();
        branchNameField.setText(configuredName.isBlank()
                ? LanguageManager.get("branch.default.name") : configuredName);
        branchLocationField.setText(SettingsManager.getBranchLocation());
        languageComboBox.setValue("ar".equals(LanguageManager.getLocale().getLanguage())
                ? LanguageManager.get("arabic") : LanguageManager.get("english"));
    }

    private void saveSettings() {
        if (languageComboBox.getValue() == null) {
            showMessage(Alert.AlertType.WARNING, "settings.error.title", "settings.language.required");
            return;
        }
        Locale locale = languageComboBox.getValue().equals(LanguageManager.get("arabic"))
                ? Locale.forLanguageTag("ar") : Locale.ENGLISH;
        if (branchNameField.getText() == null || branchNameField.getText().isBlank()) {
            showMessage(Alert.AlertType.WARNING, "settings.error.title", "branch.name.required");
            return;
        }
        if (!SettingsManager.save(locale, darkModeCheckBox.isSelected(),
                branchNameField.getText(), branchLocationField.getText())) {
            showMessage(Alert.AlertType.ERROR, "error", "settings.save.failed");
            return;
        }
        LanguageManager.setLanguage(locale);
        SettingsManager.refreshBranchDisplay();
        if (getScene() != null) ThemeManager.applyTheme(getScene());
        showMessage(Alert.AlertType.INFORMATION, null, "settings.saved.restart");
    }

    private void confirmAndReset() {
        if (!PermissionManager.canOpenSettings()) {
            showMessage(Alert.AlertType.ERROR, "error", "settings.reset.unauthorized");
            return;
        }
        Alert warning = AlertUtil.create(Alert.AlertType.WARNING,
                LanguageManager.get("settings.reset.confirm.message"), ButtonType.CANCEL, ButtonType.OK);
        warning.setTitle(LanguageManager.get("settings.reset.confirm.title"));
        warning.setHeaderText(LanguageManager.get("settings.reset.confirm.title"));
        Button okButton = (Button) warning.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(LanguageManager.get("settings.reset.continue"));
        if (warning.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        TextInputDialog typedConfirmation = new TextInputDialog();
        typedConfirmation.setTitle(LanguageManager.get("settings.reset.confirm.title"));
        typedConfirmation.setHeaderText(LanguageManager.get("settings.reset.type.header"));
        typedConfirmation.setContentText(LanguageManager.get("settings.reset.type.prompt"));
        typedConfirmation.getDialogPane().sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) ThemeManager.applyTheme(newScene);
        });
        Optional<String> response = typedConfirmation.showAndWait();
        if (response.isEmpty()) return;
        if (!response.get().trim().equals(LanguageManager.get("settings.reset.confirm.word"))) {
            showMessage(Alert.AlertType.WARNING, "settings.reset.confirm.title", "settings.reset.confirm.mismatch");
            return;
        }

        resetButton.setDisable(true);
        try {
            new ApplicationResetService().resetOperationalData();
            showMessage(Alert.AlertType.INFORMATION, "success", "settings.reset.success");
        } catch (SecurityException exception) {
            showMessage(Alert.AlertType.ERROR, "error", "settings.reset.unauthorized");
        } catch (RuntimeException exception) {
            showMessage(Alert.AlertType.ERROR, "settings.reset.failed.title", "settings.reset.failed");
        } finally {
            resetButton.setDisable(false);
        }
    }

    private void showMessage(Alert.AlertType type, String titleKey, String contentKey) {
        Alert alert = AlertUtil.create(type, LanguageManager.get(contentKey));
        if (titleKey != null) alert.setTitle(LanguageManager.get(titleKey));
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
