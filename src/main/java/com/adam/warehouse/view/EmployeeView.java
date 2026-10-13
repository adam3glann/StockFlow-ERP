package com.adam.warehouse.view;

import com.adam.warehouse.controller.EmployeeController;
import com.adam.warehouse.enums.DialogMode;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.view.dialog.EmployeeDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class EmployeeView extends BorderPane {

    private final EmployeeController employeeController;

    private final TableView<Employee> employeeTable;

    private final TextField searchField;

    private final Button addButton;
    private final Button editButton;
    private final Button deactivateButton;
    private final Button refreshButton;

    public EmployeeView() {

        employeeController = new EmployeeController();

        employeeTable = new TableView<>();

        searchField = new TextField();

        addButton = new Button(
                LanguageManager.get("button.add.employee")
        );

        editButton = new Button(
                LanguageManager.get("edit")
        );

        deactivateButton = new Button(
                LanguageManager.get("deactivate")
        );

        refreshButton = new Button(
                LanguageManager.get("refresh")
        );

        setPadding(new Insets(15));

        createTop();

        createTable();

        createBottom();

        registerEvents();

        loadEmployees();

    }

    private void createTop() {

        Label title = new Label(
                "👨 " + LanguageManager.get("employee.title")
        );

        title.getStyleClass().add("page-title");

        searchField.setPromptText(
                LanguageManager.get("employee.search")
        );

        HBox.setHgrow(
                searchField,
                Priority.ALWAYS
        );

        addButton.getStyleClass().add("primary-button");

        HBox top = new HBox(10);

        top.setPadding(new Insets(10));

        top.getChildren().addAll(

                title,

                searchField,

                addButton

        );

        setTop(top);

    }

    private void createBottom() {

        HBox bottom = new HBox(10);

        bottom.setPadding(new Insets(10));

        bottom.getChildren().addAll(

                editButton,

                deactivateButton,

                refreshButton

        );

        setBottom(bottom);

    }

    private void createTable() {

        TableColumn<Employee, String> nameColumn =
                new TableColumn<>(
                        LanguageManager.get("employee.name")
                );

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getFullName()
                )
        );

        TableColumn<Employee, String> usernameColumn =
                new TableColumn<>(
                        LanguageManager.get("employee.username")
                );

        usernameColumn.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );

        TableColumn<Employee, String> roleColumn =
                new TableColumn<>(
                        LanguageManager.get("employee.role")
                );

        roleColumn.setCellValueFactory(
                new PropertyValueFactory<>("role")
        );
        TableColumn<Employee, String> phoneColumn =
                new TableColumn<>(
                        LanguageManager.get("employee.phone")
                );

        phoneColumn.setCellValueFactory(
                new PropertyValueFactory<>("phone")
        );

        TableColumn<Employee, String> emailColumn =
                new TableColumn<>(
                        LanguageManager.get("employee.email")
                );

        emailColumn.setCellValueFactory(
                new PropertyValueFactory<>("email")
        );

        TableColumn<Employee, Boolean> statusColumn =
                new TableColumn<>(
                        LanguageManager.get("employee.status")
                );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );

        statusColumn.setCellFactory(column -> new TableCell<>() {

            @Override
            protected void updateItem(Boolean active, boolean empty) {

                super.updateItem(active, empty);

                if (empty || active == null) {

                    setText(null);
                    setGraphic(null);

                } else {

                    setAlignment(Pos.CENTER);

                    setText(
                            active
                                    ? "✅ " + LanguageManager.get("active")
                                    : "❌ " + LanguageManager.get("inactive")
                    );

                }

            }

        });

        TableColumn<Employee, String> notesColumn =
                new TableColumn<>(
                        LanguageManager.get("employee.notes")
                );

        notesColumn.setCellValueFactory(
                new PropertyValueFactory<>("notes")
        );

        notesColumn.setCellFactory(column -> new TableCell<>() {

            @Override
            protected void updateItem(String notes, boolean empty) {

                super.updateItem(notes, empty);

                setAlignment(Pos.CENTER);
                setGraphic(null);
                setTooltip(null);

                if (empty) {

                    setText(null);
                    return;

                }

                if (notes == null || notes.isBlank()) {

                    setText("—");

                } else {

                    setText("📝");

                    Tooltip tooltip = new Tooltip(notes);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);

                    setTooltip(tooltip);

                }

            }

        });

        employeeTable.getColumns().addAll(

                nameColumn,
                usernameColumn,
                roleColumn,
                phoneColumn,
                emailColumn,
                statusColumn,
                notesColumn

        );

        employeeTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        employeeTable.setPrefHeight(500);

        setCenter(employeeTable);

    }

    private void loadEmployees() {

        employeeTable.setItems(
                employeeController.getEmployees()
        );

        if (!employeeTable.getItems().isEmpty()) {
            employeeTable.getSelectionModel().selectFirst();
        }

    }

    private void registerEvents() {
        editButton.disableProperty().bind(
                employeeTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );

        deactivateButton.disableProperty().bind(
                employeeTable.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );

        employeeTable.setPlaceholder(
                new Label(
                        LanguageManager.get("employee.no.data")
                )
        );

        refreshButton.setOnAction(e ->
                loadEmployees()
        );

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue == null || newValue.isBlank()) {

                loadEmployees();

            } else {

                employeeTable.setItems(
                        employeeController.searchEmployees(newValue)
                );

                if (!employeeTable.getItems().isEmpty()) {
                    employeeTable.getSelectionModel().selectFirst();
                }

            }

        });

        addButton.setOnAction(e -> {

            EmployeeDialog dialog =
                    new EmployeeDialog(DialogMode.ADD);

            dialog.setOnEmployeeSaved(this::loadEmployees);

            dialog.showAndWait();

        });

        editButton.setOnAction(e -> {

            Employee selectedEmployee =
                    employeeTable.getSelectionModel().getSelectedItem();

            if (selectedEmployee == null) {

                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);

                alert.setHeaderText(null);

                alert.setContentText(
                        LanguageManager.get("alert.employee.select")
                );

                alert.showAndWait();

                return;

            }

            EmployeeDialog dialog =
                    new EmployeeDialog(DialogMode.EDIT);

            dialog.setEmployee(selectedEmployee);

            dialog.setOnEmployeeSaved(this::loadEmployees);

            dialog.showAndWait();

        });

        deactivateButton.setOnAction(e -> {

            Employee selectedEmployee =
                    employeeTable.getSelectionModel().getSelectedItem();

            if (selectedEmployee == null) {

                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);

                alert.setHeaderText(null);

                alert.setContentText(
                        LanguageManager.get("alert.employee.select")
                );

                alert.showAndWait();

                return;

            }

            if (SessionManager.getCurrentEmployee() != null &&
                    selectedEmployee.getEmployeeId() ==
                            SessionManager.getCurrentEmployee().getEmployeeId()) {

                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);

                alert.setHeaderText(null);

                alert.setContentText(
                        LanguageManager.get("alert.employee.self.deactivate")
                );

                alert.showAndWait();

                return;

            }

            Alert confirmation =
                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle(
                    LanguageManager.get("alert.employee.deactivate.title")
            );

            confirmation.setHeaderText(
                    LanguageManager.get("alert.employee.deactivate.header")
            );

            confirmation.setContentText(

                    String.format(

                            LanguageManager.get(
                                    "alert.employee.deactivate.confirm"
                            ),

                            selectedEmployee.getFullName()

                    )

            );

            confirmation.showAndWait().ifPresent(response -> {

                if (response == ButtonType.OK) {

                    employeeController.deactivateEmployee(
                            selectedEmployee.getEmployeeId()
                    );

                    loadEmployees();

                    Alert success =
                            com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);

                    success.setHeaderText(null);

                    success.setContentText(
                            LanguageManager.get(
                                    "alert.employee.deactivated"
                            )
                    );

                    success.showAndWait();

                }

            });

        });

        employeeTable.setRowFactory(tv -> {

            TableRow<Employee> row = new TableRow<>();

            row.setOnMouseClicked(event -> {

                if (event.getClickCount() == 2 &&
                        !row.isEmpty()) {

                    EmployeeDialog dialog =
                            new EmployeeDialog(DialogMode.EDIT);

                    dialog.setEmployee(row.getItem());

                    dialog.setOnEmployeeSaved(
                            this::loadEmployees
                    );

                    dialog.showAndWait();

                }

            });

            return row;

        });

    }

}

