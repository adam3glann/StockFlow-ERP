package com.adam.warehouse.view;

import com.adam.warehouse.controller.PurchaseController;
import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.view.dialog.PurchaseDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.format.DateTimeFormatter;
import javafx.stage.FileChooser;
import com.adam.warehouse.util.InvoiceExportUtil;
import com.adam.warehouse.util.SimplePdfInvoiceExporter;
import java.io.File;
import java.io.IOException;

public class PurchaseView extends BorderPane {

    private final PurchaseController purchaseController =
            new PurchaseController();

    private final TableView<Purchase> purchaseTable =
            new TableView<>();

    public PurchaseView() {

        setPadding(new Insets(15));

        createTop();

        createTable();

        loadPurchases();

    }

    private void createTop() {

        Label title = new Label(
                LanguageManager.get("purchase.title")
        );

        title.getStyleClass().add("page-title");

        Button newPurchaseButton = new Button(
                LanguageManager.get("button.new.purchase")
        );

        newPurchaseButton.getStyleClass().add(
                "primary-button"
        );

        newPurchaseButton.setOnAction(event -> {

            PurchaseDialog dialog =
                    new PurchaseDialog();

            dialog.showAndWait();

            loadPurchases();

        });

        Button refreshButton = new Button(
                LanguageManager.get("refresh")
        );

        Button exportButton = new Button(LanguageManager.get("button.export.invoice"));
        exportButton.disableProperty().bind(purchaseTable.getSelectionModel().selectedItemProperty().isNull());
        exportButton.setOnAction(event -> exportInvoice());

        Button exportPdfButton = new Button(LanguageManager.get("button.export.pdf"));
        exportPdfButton.disableProperty().bind(purchaseTable.getSelectionModel().selectedItemProperty().isNull());
        exportPdfButton.setOnAction(event -> exportPdfInvoice());

        refreshButton.setOnAction(event ->
                loadPurchases()
        );

        HBox top = new HBox(
                10,
                title,
                newPurchaseButton,

                exportButton,

                exportPdfButton,
                refreshButton
        );

        HBox.setHgrow(
                title,
                Priority.ALWAYS
        );

        top.setPadding(new Insets(10));

        setTop(top);

    }

    private void createTable() {

        TableColumn<Purchase, Integer> idColumn =
                new TableColumn<>(
                        LanguageManager.get("purchase.id")
                );

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("purchaseId")
        );

        TableColumn<Purchase, String> supplierColumn =
                new TableColumn<>(
                        LanguageManager.get("purchase.supplier")
                );

        supplierColumn.setCellValueFactory(
                new PropertyValueFactory<>("supplierName")
        );

        TableColumn<Purchase, String> dateColumn =
                new TableColumn<>(
                        LanguageManager.get("purchase.date")
                );

        dateColumn.setCellValueFactory(cell ->

                new SimpleStringProperty(

                        cell.getValue()
                                .getPurchaseDate()
                                .format(
                                        DateTimeFormatter.ofPattern(
                                                "dd MMM yyyy, HH:mm"
                                        )
                                )

                )

        );

        TableColumn<Purchase, Double> totalColumn =
                new TableColumn<>(
                        LanguageManager.get("purchase.total")
                );

        totalColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalAmount")
        );

        purchaseTable.getColumns().addAll(

                idColumn,

                supplierColumn,

                dateColumn,

                totalColumn

        );

        purchaseTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        purchaseTable.setPlaceholder(

                new Label(

                        LanguageManager.get("purchase.empty")

                )

        );

        setCenter(purchaseTable);

    }

    private void loadPurchases() {

        purchaseTable.setItems(

                purchaseController.getPurchases()

        );

    }

    private void exportInvoice() {
        try {
            Purchase purchase = purchaseController.getPurchaseById(purchaseTable.getSelectionModel().getSelectedItem().getPurchaseId());
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LanguageManager.get("purchase.export.title"));
            chooser.setInitialFileName("purchase-invoice-" + purchase.getPurchaseId() + ".csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("filter.csv.files"), "*.csv"));
            File destination = chooser.showSaveDialog(getScene().getWindow());
            if (destination == null) return;
            InvoiceExportUtil.purchase(destination.toPath(), purchase);
            notice(Alert.AlertType.INFORMATION, LanguageManager.get("notice.invoice.exported.title"), LanguageManager.get("notice.saved.to") + " " + destination.getAbsolutePath());
        } catch (IOException | RuntimeException exception) { notice(Alert.AlertType.ERROR, LanguageManager.get("notice.export.failed.title"), LanguageManager.get("notice.export.failed.purchase")); }
    }

    private void exportPdfInvoice() {
        try {
            Purchase purchase = purchaseController.getPurchaseById(purchaseTable.getSelectionModel().getSelectedItem().getPurchaseId());
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LanguageManager.get("purchase.export.pdf.title"));
            chooser.setInitialFileName("purchase-invoice-" + purchase.getPurchaseId() + ".pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("filter.pdf.files"), "*.pdf"));
            File destination = chooser.showSaveDialog(getScene().getWindow());
            if (destination == null) return;
            SimplePdfInvoiceExporter.purchaseInvoice(destination.toPath(), purchase);
            notice(Alert.AlertType.INFORMATION, LanguageManager.get("notice.pdf.exported.title"), LanguageManager.get("notice.saved.to") + " " + destination.getAbsolutePath());
        } catch (IOException | RuntimeException exception) { notice(Alert.AlertType.ERROR, LanguageManager.get("notice.export.failed.title"), LanguageManager.get("notice.export.failed.purchase.pdf")); }
    }

    private void notice(Alert.AlertType type, String heading, String content) { Alert alert = com.adam.warehouse.util.AlertUtil.create(type); alert.setHeaderText(heading); alert.setContentText(content); alert.showAndWait(); }

}
