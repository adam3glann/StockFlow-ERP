package com.adam.warehouse.view;

import com.adam.warehouse.controller.LoginController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import com.adam.warehouse.util.LanguageManager;

import java.sql.SQLException;

public class LoginView extends GridPane {

    private final LoginController loginController;

    private final TextField usernameField;
    private final PasswordField passwordField;

    private final Button loginButton;

    public LoginView(Stage stage) {

        loginController = new LoginController();

        usernameField = new TextField();
        passwordField = new PasswordField();

        loginButton = new Button(LanguageManager.get("login"));

        createUI(stage);

    }

    private void createUI(Stage stage) {

        setAlignment(Pos.CENTER);

        setPadding(new Insets(30));

        setHgap(15);
        setVgap(15);

        usernameField.setPrefWidth(250);
        passwordField.setPrefWidth(250);

        loginButton.getStyleClass().add("primary-button");

        add(new Label(LanguageManager.get("username")), 0, 0);
        add(usernameField, 1, 0);

        add(new Label(LanguageManager.get("password")), 0, 1);
        add(passwordField, 1, 1);

        add(loginButton, 1, 2);

        loginButton.setOnAction(event -> login(stage));

    }

    private void login(Stage stage) {

        try {

            boolean success = loginController.login(
                    usernameField.getText(),
                    passwordField.getText()
            );

            if (!success) {

                Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);

                alert.setHeaderText(null);

                alert.setContentText(LanguageManager.get("login.failed"));

                alert.showAndWait();

                return;

            }

            Scene dashboardScene = new Scene(
                    new DashboardView(),
                    1300,
                    800
            );

            com.adam.warehouse.util.ThemeManager.applyTheme(dashboardScene);

            stage.setScene(dashboardScene);

            stage.setWidth(1300);
            stage.setHeight(800);

            stage.setResizable(true);

            stage.centerOnScreen();

        } catch (Exception ex) {

            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);

            alert.setHeaderText(LanguageManager.get("login.error.title"));

            alert.setContentText(loginFailureMessage(ex));

            alert.showAndWait();

        }

    }

    private String loginFailureMessage(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException) {
                return LanguageManager.get("error.database.unavailable");
            }
            current = current.getCause();
        }
        return LanguageManager.get("error.signin.generic");
    }

}
