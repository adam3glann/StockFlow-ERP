package com.adam.warehouse.view;

import com.adam.warehouse.dao.EmployeeDAO;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.util.AlertUtil;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/** One-time, database-serialized setup for a fresh installation's first admin account. */
public final class InitialAdminSetupView extends GridPane {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final TextField firstName = new TextField();
    private final TextField lastName = new TextField();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirmation = new PasswordField();

    public InitialAdminSetupView(Stage stage) {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));
        setHgap(12);
        setVgap(12);
        add(new Label(LanguageManager.get("admin.setup.title")), 0, 0, 2, 1);
        add(new Label(LanguageManager.get("employee.first.name")), 0, 1);
        add(firstName, 1, 1);
        add(new Label(LanguageManager.get("employee.last.name")), 0, 2);
        add(lastName, 1, 2);
        add(new Label(LanguageManager.get("username")), 0, 3);
        add(username, 1, 3);
        add(new Label(LanguageManager.get("password")), 0, 4);
        add(password, 1, 4);
        add(new Label(LanguageManager.get("admin.setup.confirm.password")), 0, 5);
        add(confirmation, 1, 5);
        Label hint = new Label(LanguageManager.get("admin.setup.password.hint"));
        hint.setWrapText(true);
        add(hint, 0, 6, 2, 1);
        Button create = new Button(LanguageManager.get("admin.setup.create"));
        create.getStyleClass().add("primary-button");
        add(create, 1, 7);
        create.setOnAction(event -> createAdministrator(stage));
    }

    private void createAdministrator(Stage stage) {
        String selectedUsername = username.getText().trim();
        String selectedPassword = password.getText();
        if (firstName.getText().isBlank() || lastName.getText().isBlank()
                || selectedUsername.length() < 3 || selectedPassword.length() < 12) {
            showError(LanguageManager.get("admin.setup.validation"));
            return;
        }
        if (!selectedPassword.equals(confirmation.getText())) {
            showError(LanguageManager.get("admin.setup.password.mismatch"));
            return;
        }

        Employee admin = new Employee();
        admin.setFirstName(firstName.getText());
        admin.setLastName(lastName.getText());
        admin.setUsername(selectedUsername);
        admin.setPassword(selectedPassword);
        try {
            boolean created = employeeDAO.createInitialAdministrator(admin);
            if (!created) {
                showError(LanguageManager.get("admin.setup.already.exists"));
                showLogin(stage);
                return;
            }
            showLogin(stage);
        } catch (RuntimeException exception) {
            // Never show database exception details: drivers may include connection information.
            showError(LanguageManager.get("admin.setup.failed"));
        }
    }

    private void showLogin(Stage stage) {
        Scene scene = new Scene(new LoginView(stage), 500, 300);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.setTitle(LanguageManager.get("app.title"));
        stage.setWidth(500);
        stage.setHeight(300);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    private void showError(String message) {
        Alert alert = AlertUtil.create(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
