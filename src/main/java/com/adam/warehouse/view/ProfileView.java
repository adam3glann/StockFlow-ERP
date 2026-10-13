package com.adam.warehouse.view;

import com.adam.warehouse.controller.EmployeeController;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

/** Self-service profile and secure password-change screen for the current employee. */
public class ProfileView extends BorderPane {
    private final EmployeeController controller = new EmployeeController();
    private final TextField firstName = new TextField();
    private final TextField lastName = new TextField();
    private final TextField username = new TextField();
    private final TextField role = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final PasswordField newPassword = new PasswordField();

    public ProfileView() {
        setPadding(new Insets(24));
        Employee employee = SessionManager.requireCurrentEmployee();
        setTop(title());
        setCenter(form(employee));
    }

    private Label title() {
        Label title = new Label(LanguageManager.get("profile.title"));
        title.getStyleClass().add("page-title");
        return title;
    }

    private GridPane form(Employee employee) {
        firstName.setText(employee.getFirstName());
        lastName.setText(employee.getLastName());
        username.setText(employee.getUsername());
        role.setText(employee.getRoleName());
        phone.setText(employee.getPhone());
        email.setText(employee.getEmail());
        username.setEditable(false);
        role.setEditable(false);
        newPassword.setPromptText(LanguageManager.get("profile.password.hint"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(24, 0, 0, 0));
        add(grid, LanguageManager.get("employee.first.name"), firstName, 0);
        add(grid, LanguageManager.get("employee.last.name"), lastName, 1);
        add(grid, LanguageManager.get("username"), username, 2);
        add(grid, LanguageManager.get("employee.role"), role, 3);
        add(grid, LanguageManager.get("phone"), phone, 4);
        add(grid, LanguageManager.get("email"), email, 5);
        add(grid, LanguageManager.get("profile.new.password"), newPassword, 6);
        Button save = new Button(LanguageManager.get("profile.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> save(employee));
        grid.add(save, 1, 7);
        return grid;
    }

    private void add(GridPane grid, String label, TextField field, int row) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    private void save(Employee employee) {
        try {
            employee.setFirstName(firstName.getText());
            employee.setLastName(lastName.getText());
            employee.setPhone(phone.getText());
            employee.setEmail(email.getText());
            employee.setPassword(newPassword.getText());
            controller.updateOwnProfile(employee);
            newPassword.clear();
            message(Alert.AlertType.INFORMATION, LanguageManager.get("profile.saved.title"), LanguageManager.get("profile.saved.message"));
        } catch (RuntimeException exception) {
            message(Alert.AlertType.ERROR, LanguageManager.get("profile.error.title"), com.adam.warehouse.util.LanguageManager.message(exception.getMessage()));
        }
    }

    private void message(Alert.AlertType type, String title, String content) {
        Alert alert = com.adam.warehouse.util.AlertUtil.create(type);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
