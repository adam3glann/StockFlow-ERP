package com.adam.warehouse.view;

import com.adam.warehouse.controller.ReturnController;
import com.adam.warehouse.model.Return;
import com.adam.warehouse.model.ReturnItem;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.view.dialog.ReturnDialog;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ReturnView extends BorderPane {

    private final ReturnController returnController = new ReturnController();

    private final TableView<Return> returnTable = new TableView<>();
    private final Button newReturnButton = new Button(LanguageManager.get("button.new.return"));
    private final Button viewButton = new Button(LanguageManager.get("button.view"));
    private final Button refreshButton = new Button(LanguageManager.get("refresh"));

    public ReturnView() {
        setPadding(new Insets(15));
        createTop();
        createTable();
        createBottom();
        registerEvents();
        loadReturns();
    }

    private void createTop() {
        Label title = new Label("↩ " + LanguageManager.get("return.title"));
        title.getStyleClass().add("page-title");
        newReturnButton.getStyleClass().add("primary-button");

        HBox top = new HBox(10);
        top.setPadding(new Insets(10));
        HBox.setHgrow(title, Priority.ALWAYS);
        top.getChildren().addAll(title, newReturnButton);
        setTop(top);
    }

    private void createTable() {
        TableColumn<Return, Integer> idColumn = new TableColumn<>(LanguageManager.get("return.column.id"));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("returnId"));

        TableColumn<Return, Integer> saleColumn = new TableColumn<>(LanguageManager.get("return.column.sale"));
        saleColumn.setCellValueFactory(new PropertyValueFactory<>("saleId"));

        TableColumn<Return, String> customerColumn = new TableColumn<>(LanguageManager.get("sale.customer"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Return, String> dateColumn = new TableColumn<>(LanguageManager.get("return.column.date"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));

        TableColumn<Return, Double> refundColumn = new TableColumn<>(LanguageManager.get("return.column.refund"));
        refundColumn.setCellValueFactory(new PropertyValueFactory<>("totalRefund"));

        TableColumn<Return, String> employeeColumn = new TableColumn<>(LanguageManager.get("return.column.employee"));
        employeeColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));

        returnTable.getColumns().addAll(idColumn, saleColumn, customerColumn, dateColumn, refundColumn, employeeColumn);
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
            ReturnDialog dialog = new ReturnDialog();
            dialog.showAndWait();
            loadReturns();
        });

        refreshButton.setOnAction(event -> loadReturns());

        viewButton.disableProperty().bind(returnTable.getSelectionModel().selectedItemProperty().isNull());
        viewButton.setOnAction(event -> viewReturnDetails());
    }

    private void loadReturns() {
        returnTable.setItems(returnController.getReturns());
    }

    private void viewReturnDetails() {
        Return selected = returnTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Return returnRecord = returnController.getReturnById(selected.getReturnId());
        if (returnRecord == null) {
            return;
        }

        TableView<ReturnItem> itemsTable = new TableView<>();

        TableColumn<ReturnItem, String> productColumn = new TableColumn<>(LanguageManager.get("return.column.product"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<ReturnItem, Integer> qtyColumn = new TableColumn<>(LanguageManager.get("return.column.quantity"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<ReturnItem, Double> refundColumn = new TableColumn<>(LanguageManager.get("return.column.refund"));
        refundColumn.setCellValueFactory(new PropertyValueFactory<>("refundAmount"));

        TableColumn<ReturnItem, Boolean> restockColumn = new TableColumn<>(LanguageManager.get("return.column.restock"));
        restockColumn.setCellValueFactory(new PropertyValueFactory<>("restocked"));

        itemsTable.getColumns().addAll(productColumn, qtyColumn, refundColumn, restockColumn);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.getItems().addAll(returnRecord.getItems());
        itemsTable.setPrefHeight(240);
        itemsTable.setPrefWidth(480);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label header = new Label(
                LanguageManager.get("return.column.id") + ": " + returnRecord.getReturnId()
                        + "   |   " + LanguageManager.get("return.column.sale") + ": #" + returnRecord.getSaleId());

        Label totalLabel = new Label(String.format(LanguageManager.get("return.total.format"), returnRecord.getTotalRefund()));
        totalLabel.getStyleClass().add("page-title");

        content.getChildren().addAll(header, itemsTable, totalLabel);

        if (returnRecord.getReason() != null && !returnRecord.getReason().isBlank()) {
            content.getChildren().add(new Label(LanguageManager.get("return.reason") + ": " + returnRecord.getReason()));
        }

        Alert dialog = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
        dialog.setTitle(LanguageManager.get("return.view.details.title"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.showAndWait();
    }
}
