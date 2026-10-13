package com.adam.warehouse.view;



import com.adam.warehouse.controller.SupplierController;

import com.adam.warehouse.enums.DialogMode;

import com.adam.warehouse.model.Supplier;

import com.adam.warehouse.util.LanguageManager;

import com.adam.warehouse.view.dialog.SupplierDialog;

import javafx.geometry.Insets;

import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;



public class SupplierView extends BorderPane {



    private final SupplierController supplierController;



    private final TableView<Supplier> supplierTable;



    private final TextField searchField;



    private final Button addButton;

    private final Button editButton;

    private final Button deactivateButton;

    private final Button refreshButton;



    public SupplierView() {



        supplierController = new SupplierController();



        supplierTable = new TableView<>();



        searchField = new TextField();



        addButton = new Button(

                LanguageManager.get("button.add.supplier")

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



        loadSuppliers();



    }



    private void loadSuppliers() {



        supplierTable.setItems(

                supplierController.getSuppliers()

        );



    }



    private void createTop() {



        Label title = new Label(

                "🚚 " + LanguageManager.get("supplier.title")

        );



        title.getStyleClass().add("page-title");



        searchField.setPromptText(

                LanguageManager.get("supplier.search")

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



        TableColumn<Supplier, String> nameColumn =

                new TableColumn<>(

                        LanguageManager.get("supplier.name")

                );



        nameColumn.setCellValueFactory(

                new PropertyValueFactory<>("name")

        );



        TableColumn<Supplier, String> phoneColumn =

                new TableColumn<>(

                        LanguageManager.get("supplier.phone")

                );



        phoneColumn.setCellValueFactory(

                new PropertyValueFactory<>("phone")

        );



        TableColumn<Supplier, String> emailColumn =

                new TableColumn<>(

                        LanguageManager.get("supplier.email")

                );



        emailColumn.setCellValueFactory(

                new PropertyValueFactory<>("email")

        );



        TableColumn<Supplier, String> addressColumn =

                new TableColumn<>(

                        LanguageManager.get("supplier.address")

                );



        addressColumn.setCellValueFactory(

                new PropertyValueFactory<>("address")

        );



        TableColumn<Supplier, Boolean> statusColumn =

                new TableColumn<>(

                        LanguageManager.get("supplier.status")

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



                    if (active) {



                        setText(

                                "✅ " + LanguageManager.get("active")

                        );



                    } else {



                        setText(

                                "❌ " + LanguageManager.get("inactive")

                        );



                    }



                }



            }



        });



        supplierTable.getColumns().addAll(



                nameColumn,

                phoneColumn,

                emailColumn,

                addressColumn,

                statusColumn



        );



        supplierTable.setColumnResizePolicy(

                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN

        );



        supplierTable.setPrefHeight(500);



        setCenter(supplierTable);



    }



    private void registerEvents() {



        addButton.setOnAction(e -> {



            SupplierDialog dialog =

                    new SupplierDialog(DialogMode.ADD);



            dialog.setOnSupplierSaved(this::loadSuppliers);



            dialog.showAndWait();



        });



        refreshButton.setOnAction(e ->

                loadSuppliers()

        );



        searchField.textProperty().addListener((observable, oldValue, newValue) -> {



            if (newValue == null || newValue.isBlank()) {



                loadSuppliers();



            } else {



                supplierTable.setItems(

                        supplierController.searchSuppliers(newValue)

                );



            }



        });



        editButton.setOnAction(e -> {



            Supplier selectedSupplier =

                    supplierTable.getSelectionModel().getSelectedItem();



            if (selectedSupplier == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.supplier.select")

                );



                alert.showAndWait();



                return;



            }



            SupplierDialog dialog =

                    new SupplierDialog(DialogMode.EDIT);



            dialog.setSupplier(selectedSupplier);



            dialog.setOnSupplierSaved(this::loadSuppliers);



            dialog.showAndWait();



        });



        deactivateButton.setOnAction(e -> {



            Supplier selectedSupplier =

                    supplierTable.getSelectionModel().getSelectedItem();



            if (selectedSupplier == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.supplier.select")

                );



                alert.showAndWait();



                return;



            }



            Alert confirmation =

                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);



            confirmation.setTitle(

                    LanguageManager.get("alert.supplier.deactivate.title")

            );



            confirmation.setHeaderText(

                    LanguageManager.get("alert.supplier.deactivate.header")

            );



            confirmation.setContentText(



                    String.format(



                            LanguageManager.get(

                                    "alert.supplier.deactivate.confirm"

                            ),



                            selectedSupplier.getName()



                    )



            );



            confirmation.showAndWait().ifPresent(response -> {



                if (response == ButtonType.OK) {



                    supplierController.deactivateSupplier(

                            selectedSupplier.getSupplierId()

                    );



                    loadSuppliers();



                    Alert success =

                            com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);



                    success.setHeaderText(null);



                    success.setContentText(

                            LanguageManager.get(

                                    "alert.supplier.deactivated"

                            )

                    );



                    success.showAndWait();



                }



            });



        });



        supplierTable.setRowFactory(tv -> {



            TableRow<Supplier> row = new TableRow<>();



            row.setOnMouseClicked(event -> {



                if (event.getClickCount() == 2 && !row.isEmpty()) {



                    SupplierDialog dialog =

                            new SupplierDialog(DialogMode.EDIT);



                    dialog.setSupplier(row.getItem());



                    dialog.setOnSupplierSaved(this::loadSuppliers);



                    dialog.showAndWait();



                }



            });



            return row;



        });



    }



}