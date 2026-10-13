package com.adam.warehouse.view;



import com.adam.warehouse.controller.CustomerController;

import com.adam.warehouse.enums.DialogMode;

import com.adam.warehouse.model.Customer;

import com.adam.warehouse.util.LanguageManager;

import com.adam.warehouse.view.dialog.CustomerDialog;

import javafx.geometry.Insets;

import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;



public class CustomerView extends BorderPane {



    private final CustomerController customerController;



    private final TableView<Customer> customerTable;



    private final TextField searchField;



    private final Button addButton;

    private final Button editButton;

    private final Button deactivateButton;

    private final Button refreshButton;



    public CustomerView() {



        customerController = new CustomerController();



        customerTable = new TableView<>();



        searchField = new TextField();



        addButton = new Button(

                LanguageManager.get("button.add.customer")

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



        loadCustomers();



    }



    private void loadCustomers() {



        customerTable.setItems(

                customerController.getCustomers()

        );



    }



    private void createTop() {



        Label title = new Label(

                "👥 " + LanguageManager.get("customer.title")

        );



        title.getStyleClass().add("page-title");



        searchField.setPromptText(

                LanguageManager.get("customer.search")

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



        TableColumn<Customer, String> nameColumn =
                new TableColumn<>(
                        LanguageManager.get("customer.name")
                );

        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getFullName()
                )
        );



        TableColumn<Customer, String> phoneColumn =

                new TableColumn<>(

                        LanguageManager.get("customer.phone")

                );



        phoneColumn.setCellValueFactory(

                new PropertyValueFactory<>("phone")

        );



        TableColumn<Customer, String> emailColumn =

                new TableColumn<>(

                        LanguageManager.get("customer.email")

                );



        emailColumn.setCellValueFactory(

                new PropertyValueFactory<>("email")

        );



        TableColumn<Customer, String> addressColumn =

                new TableColumn<>(

                        LanguageManager.get("customer.address")

                );



        addressColumn.setCellValueFactory(

                new PropertyValueFactory<>("address")

        );



        TableColumn<Customer, Boolean> statusColumn =

                new TableColumn<>(

                        LanguageManager.get("customer.status")

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



                } else {



                    setText(active

                            ? "✅ " + LanguageManager.get("active")

                            : "❌ " + LanguageManager.get("inactive"));



                }



            }



        });



        customerTable.getColumns().addAll(



                nameColumn,

                phoneColumn,

                emailColumn,

                addressColumn,

                statusColumn



        );



        customerTable.setColumnResizePolicy(

                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN

        );



        customerTable.setPrefHeight(500);



        setCenter(customerTable);



    }



    private void registerEvents() {



        refreshButton.setOnAction(e ->

                loadCustomers()

        );



        searchField.textProperty().addListener((observable, oldValue, newValue) -> {



            if (newValue == null || newValue.isBlank()) {



                loadCustomers();



            } else {



                customerTable.setItems(

                        customerController.searchCustomers(newValue)

                );



            }



        });



        addButton.setOnAction(e -> {



            CustomerDialog dialog =

                    new CustomerDialog(DialogMode.ADD);



            dialog.setOnCustomerSaved(this::loadCustomers);



            dialog.showAndWait();



        });



        editButton.setOnAction(e -> {



            Customer selectedCustomer =

                    customerTable.getSelectionModel().getSelectedItem();



            if (selectedCustomer == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.customer.select")

                );



                alert.showAndWait();



                return;



            }



            CustomerDialog dialog =

                    new CustomerDialog(DialogMode.EDIT);



            dialog.setCustomer(selectedCustomer);



            dialog.setOnCustomerSaved(this::loadCustomers);



            dialog.showAndWait();



        });



        deactivateButton.setOnAction(e -> {



            Customer selectedCustomer =

                    customerTable.getSelectionModel().getSelectedItem();



            if (selectedCustomer == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.customer.select")

                );



                alert.showAndWait();



                return;



            }



            Alert confirmation =

                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);



            confirmation.setTitle(

                    LanguageManager.get("alert.customer.deactivate.title")

            );



            confirmation.setHeaderText(

                    LanguageManager.get("alert.customer.deactivate.header")

            );



            confirmation.setContentText(



                    String.format(



                            LanguageManager.get(

                                    "alert.customer.deactivate.confirm"

                            ),



                            selectedCustomer.getFullName()



                    )



            );



            confirmation.showAndWait().ifPresent(response -> {



                if (response == ButtonType.OK) {



                    customerController.deactivateCustomer(

                            selectedCustomer.getCustomerId()

                    );



                    loadCustomers();



                    Alert success =

                            com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);



                    success.setHeaderText(null);



                    success.setContentText(

                            LanguageManager.get(

                                    "alert.customer.deactivated"

                            )

                    );



                    success.showAndWait();



                }



            });



        });



        customerTable.setRowFactory(tv -> {



            TableRow<Customer> row = new TableRow<>();



            row.setOnMouseClicked(event -> {



                if (event.getClickCount() == 2 && !row.isEmpty()) {



                    CustomerDialog dialog =

                            new CustomerDialog(DialogMode.EDIT);



                    dialog.setCustomer(row.getItem());



                    dialog.setOnCustomerSaved(this::loadCustomers);



                    dialog.showAndWait();



                }



            });



            return row;



        });



    }



}