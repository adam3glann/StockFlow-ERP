package com.adam.warehouse.view;

import com.adam.warehouse.controller.PurchaseReturnController;
import com.adam.warehouse.model.PurchaseReturn;
import com.adam.warehouse.model.PurchaseReturnItem;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.view.dialog.PurchaseReturnDialog;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PurchaseReturnView extends BorderPane {

    private final PurchaseReturnController purchaseReturnController = new PurchaseReturnController();

    private final TableView<PurchaseReturn> returnTable = new TableView<>();
    private final Button newReturnButton = new Button(LanguageManager.get("button.new.purchase.return"));
    private final Button viewButton = new Button(LanguageManager.get("button.view"));
    private final Button refreshButton = new Button(LanguageManager.get("refresh"));

    public PurchaseReturnView() {
        setPadding(new Insets(15));
        createTop();
        createTable();
        createBottom();
        registerEvents();
        loadReturns();
    }

    private void createTop() {
        Label title = new Label("↩ " + LanguageManager.get("purchase.return.title"));
        title.getStyleClass().add("page-title");
        newReturnButton.getStyleClass().add("primary-button");

        HBox top = new HBox(10);
        top.setPadding(new Insets(10));
        HBox.setHgrow(title, Priority.ALWAYS);
        top.getChildren().addAll(title, newReturnButton);
        setTop(top);
    }

    private void createTable() {
        TableColumn<PurchaseReturn, Integer> idColumn = new TableColumn<>(LanguageManager.get("return.column.id"));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseReturnId"));

        TableColumn<PurchaseReturn, Integer> purchaseColumn = new TableColumn<>(LanguageManager.get("purchase.return.column.purchase"));
        purchaseColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseId"));

        TableColumn<PurchaseReturn, String> supplierColumn = new TableColumn<>(LanguageManager.get("purchase.supplier"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));

        TableColumn<PurchaseReturn, String> dateColumn = new TableColumn<>(LanguageManager.get("return.column.date"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));

        TableColumn<PurchaseReturn, Double> creditColumn = new TableColumn<>(LanguageManager.get("purchase.return.column.credit"));
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("totalCredit"));

        TableColumn<PurchaseReturn, String> employeeColumn = new TableColumn<>(LanguageManager.get("return.column.employee"));
        employeeColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));

        returnTable.getColumns().addAll(idColumn, purchaseColumn, supplierColumn, dateColumn, creditColumn, employeeColumn);
        returnTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        returnTable.setPrefHeight(500);
        setCenter(returnTable);
    }

    private void createBottom() {
        HBox bottom = new HBox(10);
        bottom.setPadding(new Insets(10));
        bottom.getChildren().addAll(viewButton, refreshButton);
        setBottom(bottom);
    }

    private void registerEvents() {
        newReturnButton.setOnAction(event -> {
            PurchaseReturnDialog dialog = new PurchaseReturnDialog();
            dialog.showAndWait();
            loadReturns();
        });

        refreshButton.setOnAction(event -> loadReturns());

        viewButton.disableProperty().bind(returnTable.getSelectionModel().selectedItemProperty().isNull());
        viewButton.setOnAction(event -> viewReturnDetails());
    }

    private void loadReturns() {
        returnTable.setItems(purchaseReturnController.getReturns());
    }

    private void viewReturnDetails() {
        PurchaseReturn selected = returnTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        PurchaseReturn purchaseReturn = purchaseReturnController.getReturnById(selected.getPurchaseReturnId());
        if (purchaseReturn == null) {
            return;
        }

        TableView<PurchaseReturnItem> itemsTable = new TableView<>();

        TableColumn<PurchaseReturnItem, String> productColumn = new TableColumn<>(LanguageManager.get("return.column.product"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<PurchaseReturnItem, Integer> qtyColumn = new TableColumn<>(LanguageManager.get("return.column.quantity"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<PurchaseReturnItem, Double> creditColumn = new TableColumn<>(LanguageManager.get("purchase.return.column.credit"));
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("creditAmount"));

        itemsTable.getColumns().addAll(productColumn, qtyColumn, creditColumn);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.getItems().addAll(purchaseReturn.getItems());
        itemsTable.setPrefHeight(240);
        itemsTable.setPrefWidth(420);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label header = new Label(
                LanguageManager.get("return.column.id") + ": " + purchaseReturn.getPurchaseReturnId()
                        + "   |   " + LanguageManager.get("purchase.return.column.purchase") + ": #" + purchaseReturn.getPurchaseId());

        Label totalLabel = new Label(String.format(LanguageManager.get("purchase.return.total.format"), purchaseReturn.getTotalCredit()));
        totalLabel.getStyleClass().add("page-title");

        content.getChildren().addAll(header, itemsTable, totalLabel);

        if (purchaseReturn.getReason() != null && !purchaseReturn.getReason().isBlank()) {
            content.getChildren().add(new Label(LanguageManager.get("return.reason") + ": " + purchaseReturn.getReason()));
        }

        Alert dialog = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
        dialog.setTitle(LanguageManager.get("purchase.return.view.details.title"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(460);
        dialog.showAndWait();
    }
}
