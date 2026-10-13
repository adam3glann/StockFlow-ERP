package com.adam.warehouse.view;



import com.adam.warehouse.controller.ProductController;

import com.adam.warehouse.enums.DialogMode;

import com.adam.warehouse.model.Product;

import com.adam.warehouse.util.LanguageManager;

import com.adam.warehouse.view.dialog.BarcodeDialog;

import com.adam.warehouse.view.dialog.ProductDialog;

import javafx.geometry.Insets;

import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;



public class ProductView extends BorderPane {



    private final ProductController productController;



    private final TableView<Product> productTable;



    private final TextField searchField;



    private final Button addButton;

    private final Button editButton;

    private final Button deactivateButton;

    private final Button barcodeButton;

    private final Button refreshButton;



    public ProductView() {



        productController = new ProductController();



        productTable = new TableView<>();



        searchField = new TextField();



        addButton = new Button(

                LanguageManager.get("button.add.product")

        );



        editButton = new Button(

                LanguageManager.get("edit")

        );



        deactivateButton = new Button(

                LanguageManager.get("deactivate")

        );



        barcodeButton = new Button(

                LanguageManager.get("button.barcode")

        );



        refreshButton = new Button(

                LanguageManager.get("refresh")

        );



        setPadding(new Insets(15));



        createTop();



        createTable();



        createBottom();



        registerEvents();



        loadProducts();



    }



    private void loadProducts() {



        productTable.setItems(

                productController.getProducts()

        );



    }



    private void createTop() {



        Label title = new Label(

                "📦 " + LanguageManager.get("product.title")

        );



        title.getStyleClass().add("page-title");



        searchField.setPromptText(

                LanguageManager.get("product.search")

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



                barcodeButton,



                refreshButton



        );



        setBottom(bottom);



    }



    private void createTable() {



        TableColumn<Product, String> barcodeColumn =

                new TableColumn<>(

                        LanguageManager.get("product.barcode")

                );



        barcodeColumn.setCellValueFactory(

                new PropertyValueFactory<>("barcode")

        );



        TableColumn<Product, String> nameColumn =

                new TableColumn<>(

                        LanguageManager.get("product.name")

                );



        nameColumn.setCellValueFactory(

                new PropertyValueFactory<>("name")

        );



        TableColumn<Product, Double> purchaseColumn =

                new TableColumn<>(

                        LanguageManager.get("product.purchase")

                );



        purchaseColumn.setCellValueFactory(

                new PropertyValueFactory<>("purchasePrice")

        );



        TableColumn<Product, Double> sellingColumn =

                new TableColumn<>(

                        LanguageManager.get("product.selling")

                );



        sellingColumn.setCellValueFactory(

                new PropertyValueFactory<>("sellingPrice")

        );



        TableColumn<Product, Integer> quantityColumn =

                new TableColumn<>(

                        LanguageManager.get("product.quantity")

                );



        quantityColumn.setCellValueFactory(

                new PropertyValueFactory<>("quantity")

        );



        TableColumn<Product, Integer> minimumColumn =

                new TableColumn<>(

                        LanguageManager.get("product.minimum")

                );



        minimumColumn.setCellValueFactory(

                new PropertyValueFactory<>("minimumStock")

        );



        TableColumn<Product, Boolean> statusColumn =

                new TableColumn<>(

                        LanguageManager.get("product.status")

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



        productTable.getColumns().addAll(



                barcodeColumn,

                nameColumn,

                purchaseColumn,

                sellingColumn,

                quantityColumn,

                minimumColumn,

                statusColumn



        );



        productTable.setColumnResizePolicy(

                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN

        );



        productTable.setPrefHeight(500);



        setCenter(productTable);



    }



    private void registerEvents() {



        refreshButton.setOnAction(e ->

                loadProducts()

        );



        searchField.textProperty().addListener((observable, oldValue, newValue) -> {



            if (newValue == null || newValue.isBlank()) {



                loadProducts();



            } else {



                productTable.setItems(

                        productController.searchProducts(newValue)

                );



            }



        });



        addButton.setOnAction(e -> {



            ProductDialog dialog =

                    new ProductDialog(DialogMode.ADD);



            dialog.setOnProductSaved(this::loadProducts);



            dialog.showAndWait();



        });



        editButton.setOnAction(e -> {



            Product selectedProduct =

                    productTable.getSelectionModel().getSelectedItem();



            if (selectedProduct == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.product.select")

                );



                alert.showAndWait();



                return;



            }



            ProductDialog dialog =

                    new ProductDialog(DialogMode.EDIT);



            dialog.setProduct(selectedProduct);



            dialog.setOnProductSaved(this::loadProducts);



            dialog.showAndWait();



        });



        deactivateButton.setOnAction(e -> {



            Product selectedProduct =

                    productTable.getSelectionModel().getSelectedItem();



            if (selectedProduct == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.product.select")

                );



                alert.showAndWait();



                return;



            }



            Alert confirmation =

                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);



            confirmation.setTitle(

                    LanguageManager.get("alert.product.deactivate.title")

            );



            confirmation.setHeaderText(

                    LanguageManager.get("alert.product.deactivate.header")

            );



            confirmation.setContentText(



                    String.format(



                            LanguageManager.get(

                                    "alert.product.deactivate.confirm"

                            ),



                            selectedProduct.getName()



                    )



            );



            confirmation.showAndWait().ifPresent(response -> {



                if (response == ButtonType.OK) {



                    productController.deactivateProduct(

                            selectedProduct.getProductId()

                    );



                    loadProducts();



                    Alert success =

                            com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);



                    success.setHeaderText(null);



                    success.setContentText(

                            LanguageManager.get(

                                    "alert.product.deactivated"

                            )

                    );



                    success.showAndWait();



                }



            });



        });



        barcodeButton.setOnAction(e -> {



            Product selectedProduct =

                    productTable.getSelectionModel().getSelectedItem();



            if (selectedProduct == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.product.select")

                );



                alert.showAndWait();



                return;



            }



            BarcodeDialog dialog =

                    new BarcodeDialog(selectedProduct);



            dialog.showAndWait();



        });



        productTable.setRowFactory(tv -> {



            TableRow<Product> row = new TableRow<>();



            row.setOnMouseClicked(event -> {



                if (event.getClickCount() == 2 && !row.isEmpty()) {



                    ProductDialog dialog =

                            new ProductDialog(DialogMode.EDIT);



                    dialog.setProduct(row.getItem());



                    dialog.setOnProductSaved(this::loadProducts);



                    dialog.showAndWait();



                }



            });



            return row;



        });



    }



}