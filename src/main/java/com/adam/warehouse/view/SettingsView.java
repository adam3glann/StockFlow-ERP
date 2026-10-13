package com.adam.warehouse.view;

import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.SettingsManager;
import com.adam.warehouse.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.util.Locale;

public class SettingsView extends BorderPane {

    private final ComboBox<String> languageComboBox;

    private final CheckBox darkModeCheckBox;

    private final Button saveButton;

    public SettingsView() {

        languageComboBox = new ComboBox<>();

        darkModeCheckBox = new CheckBox(LanguageManager.get("settings.dark.mode"));

        saveButton = new Button(
                LanguageManager.get("save")
        );

        setPadding(new Insets(20));

        createUI();

        loadSettings();

        registerEvents();

    }

    private void createUI() {

        Label title = new Label(
                LanguageManager.get("settings")
        );

        title.getStyleClass().add("page-title");

        setTop(title);

        GridPane grid = new GridPane();

        grid.setPadding(new Insets(20));

        grid.setHgap(15);

        grid.setVgap(20);

        languageComboBox.getItems().addAll(

                LanguageManager.get("english"),

                LanguageManager.get("arabic")

        );

        grid.add(

                new Label(
                        LanguageManager.get("language")
                ),

                0,

                0

        );

        grid.add(

                languageComboBox,

                1,

                0

        );

        grid.add(

                saveButton,

                1,

                1

        );

        grid.add(

                darkModeCheckBox,

                1,

                2

        );

        saveButton.getStyleClass().add(
                "primary-button"
        );

        setCenter(grid);

    }

    private void loadSettings() {

        darkModeCheckBox.setSelected(SettingsManager.isDarkMode());

        if (LanguageManager.getLocale().getLanguage().equals("ar")) {

            languageComboBox.setValue(
                    LanguageManager.get("arabic")
            );

        } else {

            languageComboBox.setValue(
                    LanguageManager.get("english")
            );

        }

    }

    private void registerEvents() {

        saveButton.setOnAction(e -> {

            Locale locale;

            if (languageComboBox.getValue().equals(
                    LanguageManager.get("arabic"))) {

                locale = Locale.forLanguageTag("ar");

            } else {

                locale = Locale.ENGLISH;

            }

            LanguageManager.setLanguage(locale);

            SettingsManager.saveLanguage(locale);

            SettingsManager.saveDarkMode(darkModeCheckBox.isSelected());

            if (getScene() != null) {
                ThemeManager.applyTheme(getScene());
            }

            Alert alert =
                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);

            alert.setHeaderText(null);

            alert.setContentText(
                    LanguageManager.get("settings.saved.restart")
            );

            alert.showAndWait();

        });

    }

}
