package com.adam.warehouse.view;

import com.adam.warehouse.controller.SaleController;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.view.dialog.SaleDialog;
import com.adam.warehouse.view.dialog.PaymentDialog;
import javafx.geometry.Insets;
import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.adam.warehouse.util.InvoiceExportUtil;
import com.adam.warehouse.util.SimplePdfInvoiceExporter;
import java.io.File;
import java.io.IOException;

public class SaleView extends BorderPane {

    private final SaleController saleController;

    private final TableView<Sale> saleTable;

    private final TextField searchField;

    private final Button newSaleButton;
    private final Button viewButton;
    private final Button recordPaymentButton;
    private final Button refreshButton;
    private final Button exportButton = new Button(LanguageManager.get("button.export.invoice"));
    private final Button exportPdfButton = new Button(LanguageManager.get("button.export.pdf"));

    public SaleView() {

        saleController = new SaleController();

        saleTable = new TableView<>();

        searchField = new TextField();

        newSaleButton = new Button(
                LanguageManager.get("button.new.sale")
        );

        viewButton = new Button(
                LanguageManager.get("button.view")
        );
        recordPaymentButton = new Button(LanguageManager.get("sale.payment.record"));

        refreshButton = new Button(
                LanguageManager.get("refresh")
        );

        setPadding(new Insets(15));

        createTop();

        createTable();

        createBottom();

        registerEvents();

        loadSales();

    }

    private void createTop() {

        Label title = new Label(
                "🛒 " + LanguageManager.get("sale.title")
        );

        title.getStyleClass().add("page-title");

        searchField.setPromptText(
                LanguageManager.get("sale.search")
        );

        HBox.setHgrow(searchField, Priority.ALWAYS);

        newSaleButton.getStyleClass().add("primary-button");

        HBox top = new HBox(10);

        top.setPadding(new Insets(10));

        top.getChildren().addAll(

                title,

                searchField,

                newSaleButton

        );

        setTop(top);

    }

    private void createBottom() {

        HBox bottom = new HBox(10);

        bottom.setPadding(new Insets(10));

        bottom.getChildren().addAll(

                viewButton,

                recordPaymentButton,

                exportButton,

                exportPdfButton,

                refreshButton

        );

        setBottom(bottom);

    }

    private void loadSales() {

        saleTable.setItems(
                saleController.getSales()
        );

    }

    private void registerEvents() {

        newSaleButton.setOnAction(e -> {

            SaleDialog dialog =
                    new SaleDialog();

            dialog.showAndWait();

            loadSales();

        });

        refreshButton.setOnAction(e -> {
            searchField.clear();
            loadSales();
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String keyword = newValue == null ? "" : newValue.trim();
            if (keyword.isEmpty()) {
                loadSales();
            } else {
                saleTable.setItems(saleController.searchSales(keyword));
            }
        });

        exportButton.disableProperty().bind(saleTable.getSelectionModel().selectedItemProperty().isNull());
        exportButton.setOnAction(event -> exportInvoice());

        exportPdfButton.disableProperty().bind(saleTable.getSelectionModel().selectedItemProperty().isNull());
        exportPdfButton.setOnAction(event -> exportPdfInvoice());

        viewButton.disableProperty().bind(saleTable.getSelectionModel().selectedItemProperty().isNull());
        viewButton.setOnAction(event -> viewSaleDetails());
        recordPaymentButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Sale selected = saleTable.getSelectionModel().getSelectedItem();
            return selected == null || selected.getBalanceDue() <= 0.005;
        }, saleTable.getSelectionModel().selectedItemProperty()));
        recordPaymentButton.setOnAction(event -> recordPayment());

    }

    private void createTable() {

        TableColumn<Sale, Integer> idColumn =
                new TableColumn<>(
                        LanguageManager.get("sale.id")
                );

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("saleId")
        );

        TableColumn<Sale, String> dateColumn =
                new TableColumn<>(
                        LanguageManager.get("sale.date")
                );

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("saleDate")
        );

        TableColumn<Sale, String> customerColumn = new TableColumn<>(LanguageManager.get("sale.customer"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Sale, Double> totalColumn =
                new TableColumn<>(
                        LanguageManager.get("sale.total")
                );

        totalColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalAmount")
        );

        TableColumn<Sale, String> paymentStatusColumn = new TableColumn<>(LanguageManager.get("sale.payment.status"));
        paymentStatusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                LanguageManager.get("sale.payment.status." + cell.getValue().getPaymentStatus().toLowerCase(java.util.Locale.ROOT))));
        TableColumn<Sale, String> balanceColumn = new TableColumn<>(LanguageManager.get("sale.balance.due"));
        balanceColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                String.format(LanguageManager.get("sale.amount.format"), cell.getValue().getBalanceDue())));

        saleTable.getColumns().addAll(

                idColumn,

                dateColumn,

                customerColumn,

                totalColumn,

                paymentStatusColumn,

                balanceColumn

        );

        saleTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        saleTable.setPrefHeight(500);

        setCenter(saleTable);

    }

    private void recordPayment() {
        Sale selected = saleTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Sale sale = saleController.getSaleById(selected.getSaleId());
        if (sale == null || sale.getBalanceDue() <= 0.005) {
            loadSales();
            return;
        }
        PaymentDialog.show(sale.getBalanceDue(), getScene() == null ? null : getScene().getWindow()).ifPresent(payment -> {
            try {
                saleController.recordPayment(sale.getSaleId(), payment.amount(), payment.method(),
                        payment.tendered(), payment.reference());
                notice(Alert.AlertType.INFORMATION, LanguageManager.get("success"), LanguageManager.get("sale.payment.saved"));
                loadSales();
            } catch (RuntimeException exception) {
                notice(Alert.AlertType.ERROR, LanguageManager.get("error"),
                        LanguageManager.message(exception.getMessage()));
            }
        });
    }

    private void viewSaleDetails() {
        Sale selected = saleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            notice(Alert.AlertType.WARNING, LanguageManager.get("alert.sale.select"), "");
            return;
        }

        Sale sale = saleController.getSaleById(selected.getSaleId());
        if (sale == null) return;

        TableView<SaleItem> itemsTable = new TableView<>();

        TableColumn<SaleItem, String> productColumn = new TableColumn<>(LanguageManager.get("sale.product"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<SaleItem, Integer> qtyColumn = new TableColumn<>(LanguageManager.get("sale.quantity"));
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<SaleItem, Double> priceColumn = new TableColumn<>(LanguageManager.get("sale.selling.price"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));

        TableColumn<SaleItem, Double> subtotalColumn = new TableColumn<>(LanguageManager.get("sale.subtotal"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        itemsTable.getColumns().addAll(productColumn, qtyColumn, priceColumn, subtotalColumn);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.getItems().addAll(sale.getItems());
        itemsTable.setPrefHeight(260);
        itemsTable.setPrefWidth(480);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label header = new Label(
                LanguageManager.get("sale.id") + ": " + sale.getSaleId()
                        + "   |   " + LanguageManager.get("sale.customer") + ": "
                        + (sale.getCustomerName() == null || sale.getCustomerName().isBlank()
                                ? LanguageManager.get("sale.customer.walkin") : sale.getCustomerName())
        );

        Label dateLabel = new Label(LanguageManager.get("sale.date") + ": " + sale.getSaleDate());

        Label totalLabel = new Label(String.format(LanguageManager.get("sale.total.format"), sale.getTotalAmount()));
        totalLabel.getStyleClass().add("page-title");

        content.getChildren().addAll(header, dateLabel, itemsTable, totalLabel);

        if (sale.getNotes() != null && !sale.getNotes().isBlank()) {
            content.getChildren().add(new Label(LanguageManager.get("sale.notes") + ": " + sale.getNotes()));
        }

        Alert dialog = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
        dialog.setTitle(LanguageManager.get("sale.view.details.title"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.showAndWait();
    }

    private void exportInvoice() {
        try {
            Sale sale = saleController.getSaleById(saleTable.getSelectionModel().getSelectedItem().getSaleId());
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LanguageManager.get("sale.export.title"));
            chooser.setInitialFileName("sale-invoice-" + sale.getSaleId() + ".csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("filter.csv.files"), "*.csv"));
            File destination = chooser.showSaveDialog(getScene().getWindow());
            if (destination == null) return;
            InvoiceExportUtil.sale(destination.toPath(), sale);
            notice(Alert.AlertType.INFORMATION, LanguageManager.get("notice.invoice.exported.title"), LanguageManager.get("notice.saved.to") + " " + destination.getAbsolutePath());
        } catch (IOException | RuntimeException exception) { notice(Alert.AlertType.ERROR, LanguageManager.get("notice.export.failed.title"), LanguageManager.get("notice.export.failed.sale")); }
    }

    private void exportPdfInvoice() {
        try {
            Sale sale = saleController.getSaleById(saleTable.getSelectionModel().getSelectedItem().getSaleId());
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LanguageManager.get("sale.export.pdf.title"));
            chooser.setInitialFileName("sale-invoice-" + sale.getSaleId() + ".pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("filter.pdf.files"), "*.pdf"));
            File destination = chooser.showSaveDialog(getScene().getWindow());
            if (destination == null) return;
            SimplePdfInvoiceExporter.saleInvoice(destination.toPath(), sale);
            notice(Alert.AlertType.INFORMATION, LanguageManager.get("notice.pdf.exported.title"), LanguageManager.get("notice.saved.to") + " " + destination.getAbsolutePath());
        } catch (IOException | RuntimeException exception) { notice(Alert.AlertType.ERROR, LanguageManager.get("notice.export.failed.title"), LanguageManager.get("notice.export.failed.sale.pdf")); }
    }

    private void notice(Alert.AlertType type, String heading, String content) { Alert alert = com.adam.warehouse.util.AlertUtil.create(type); alert.setHeaderText(heading); alert.setContentText(content); alert.showAndWait(); }

}
