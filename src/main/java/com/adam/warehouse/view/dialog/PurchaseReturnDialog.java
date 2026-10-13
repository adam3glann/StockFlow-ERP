package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.PurchaseController;
import com.adam.warehouse.controller.PurchaseReturnController;
import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.model.PurchaseReturn;
import com.adam.warehouse.model.PurchaseReturnItem;
import com.adam.warehouse.model.ReturnablePurchaseItem;
import com.adam.warehouse.util.LanguageManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Select a purchase, choose which of its items (and how much of each) to send
 * back to the supplier, and record the resulting credit. Stock is decreased and
 * the credit total is handled atomically by {@link com.adam.warehouse.dao.PurchaseReturnDAO}.
 * A line can never be sent back for more than currently remains in stock.
 */
public class PurchaseReturnDialog extends BaseDialog {

    private final PurchaseController purchaseController = new PurchaseController();
    private final PurchaseReturnController purchaseReturnController = new PurchaseReturnController();

    private final ComboBox<Purchase> purchaseComboBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final TableView<PurchaseReturnLineRow> itemsTable = new TableView<>();
    private final TextArea reasonArea = new TextArea();
    private final Label totalLabel = new Label(String.format(LanguageManager.get("purchase.return.total.format"), 0.0));

    public PurchaseReturnDialog() {
        super(LanguageManager.get("purchase.return.dialog.new"));
        createTable();
        createForm();
        loadPurchases("");
    }

    private void createTable() {

        TableColumn<PurchaseReturnLineRow, String> product = new TableColumn<>(LanguageManager.get("return.column.product"));
        product.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<PurchaseReturnLineRow, Integer> purchased = new TableColumn<>(LanguageManager.get("purchase.return.column.purchased"));
        purchased.setCellValueFactory(new PropertyValueFactory<>("purchasedQuantity"));

        TableColumn<PurchaseReturnLineRow, Integer> inStock = new TableColumn<>(LanguageManager.get("purchase.return.column.in.stock"));
        inStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));

        TableColumn<PurchaseReturnLineRow, Integer> maxReturnable = new TableColumn<>(LanguageManager.get("return.column.remaining"));
        maxReturnable.setCellValueFactory(new PropertyValueFactory<>("maxReturnable"));

        TableColumn<PurchaseReturnLineRow, Double> price = new TableColumn<>(LanguageManager.get("return.column.unit.price"));
        price.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<PurchaseReturnLineRow, Integer> qty = new TableColumn<>(LanguageManager.get("return.column.quantity"));
        qty.setCellValueFactory(cell -> cell.getValue().returnQuantityProperty().asObject());
        qty.setCellFactory(column -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>();
            private javafx.beans.value.ChangeListener<Integer> currentListener;

            {
                spinner.setEditable(true);
                spinner.setPrefWidth(90);
            }

            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (currentListener != null) {
                    spinner.valueProperty().removeListener(currentListener);
                    currentListener = null;
                }
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                PurchaseReturnLineRow row = (PurchaseReturnLineRow) getTableRow().getItem();
                int max = Math.max(0, row.getMaxReturnable());
                spinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, row.returnQuantityProperty().get()));
                currentListener = (observable, oldValue, newValue) ->
                        row.returnQuantityProperty().set(newValue == null ? 0 : newValue);
                spinner.valueProperty().addListener(currentListener);
                setGraphic(max <= 0 ? null : spinner);
            }
        });

        itemsTable.getColumns().addAll(product, purchased, inStock, maxReturnable, price, qty);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.setEditable(true);
        itemsTable.setPlaceholder(new Label(LanguageManager.get("purchase.return.select.purchase.hint")));
        itemsTable.setPrefHeight(260);
    }

    private void createForm() {

        purchaseComboBox.setPrefWidth(360);
        purchaseComboBox.setPromptText(LanguageManager.get("purchase.return.select.purchase"));
        purchaseComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Purchase purchase) {
                if (purchase == null) return "";
                return "#" + purchase.getPurchaseId() + " — " + purchase.getSupplierName() + " — "
                        + String.format(LanguageManager.get("return.sale.total.format"), purchase.getTotalAmount());
            }

            @Override
            public Purchase fromString(String string) {
                return null;
            }
        });
        purchaseComboBox.setOnAction(event -> loadReturnableItems());

        searchField.setPromptText(LanguageManager.get("purchase.return.search.purchase.hint"));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadPurchases(newValue));

        reasonArea.setPromptText(LanguageManager.get("return.reason.hint"));
        reasonArea.setPrefRowCount(3);

        Button save = new Button(LanguageManager.get("button.complete.purchase.return"));
        Button cancel = new Button(LanguageManager.get("cancel"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveReturn());
        cancel.setOnAction(event -> close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);
        grid.add(new Label(LanguageManager.get("purchase.return.search.purchase")), 0, 0);
        grid.add(searchField, 1, 0);
        grid.add(new Label(LanguageManager.get("purchase.return.select.purchase")), 0, 1);
        grid.add(purchaseComboBox, 1, 1);
        grid.add(new Label(LanguageManager.get("return.items")), 0, 2);
        grid.add(itemsTable, 1, 2);
        grid.add(totalLabel, 1, 3);
        grid.add(new Label(LanguageManager.get("return.reason")), 0, 4);
        grid.add(reasonArea, 1, 4);
        grid.add(new HBox(10, save, cancel), 1, 5);
        root.setCenter(grid);
    }

    private void loadPurchases(String keyword) {
        Purchase previouslySelected = purchaseComboBox.getValue();
        List<Purchase> purchases = keyword == null || keyword.isBlank()
                ? purchaseController.getPurchases()
                : purchaseController.searchPurchases(keyword);
        ObservableList<Purchase> items = FXCollections.observableArrayList(purchases);
        purchaseComboBox.setItems(items);
        if (previouslySelected != null
                && items.stream().anyMatch(p -> p.getPurchaseId() == previouslySelected.getPurchaseId())) {
            purchaseComboBox.setValue(previouslySelected);
        }
    }

    private void loadReturnableItems() {
        Purchase selected = purchaseComboBox.getValue();
        itemsTable.getItems().clear();
        if (selected == null) {
            updateTotal();
            return;
        }
        try {
            List<ReturnablePurchaseItem> returnable = purchaseReturnController.getReturnableItems(selected.getPurchaseId());
            for (ReturnablePurchaseItem item : returnable) {
                PurchaseReturnLineRow row = new PurchaseReturnLineRow(item);
                row.returnQuantityProperty().addListener((observable, oldValue, newValue) -> updateTotal());
                itemsTable.getItems().add(row);
            }
            updateTotal();
        } catch (RuntimeException exception) {
            show(Alert.AlertType.ERROR, LanguageManager.get("purchase.return.error.prefix") + " " + safeMessage(exception));
        }
    }

    private void updateTotal() {
        double total = itemsTable.getItems().stream()
                .mapToDouble(row -> row.returnQuantityProperty().get() * row.getUnitPrice())
                .sum();
        totalLabel.setText(String.format(LanguageManager.get("purchase.return.total.format"), total));
    }

    private void saveReturn() {

        Purchase selected = purchaseComboBox.getValue();
        if (selected == null) {
            show(Alert.AlertType.WARNING, LanguageManager.get("purchase.return.select.purchase.warning"));
            return;
        }

        List<PurchaseReturnLineRow> chosen = itemsTable.getItems().stream()
                .filter(row -> row.returnQuantityProperty().get() > 0)
                .toList();

        if (chosen.isEmpty()) {
            show(Alert.AlertType.WARNING, LanguageManager.get("return.items.required"));
            return;
        }

        try {
            PurchaseReturn purchaseReturn = new PurchaseReturn();
            purchaseReturn.setPurchaseId(selected.getPurchaseId());
            purchaseReturn.setReason(reasonArea.getText());

            for (PurchaseReturnLineRow row : chosen) {
                purchaseReturn.addItem(new PurchaseReturnItem(
                        row.getPurchaseItemId(),
                        row.getProductId(),
                        row.getProductName(),
                        row.returnQuantityProperty().get(),
                        row.getUnitPrice()
                ));
            }

            purchaseReturnController.saveReturn(purchaseReturn);
            show(Alert.AlertType.INFORMATION, LanguageManager.get("purchase.return.completed"));
            close();
        } catch (RuntimeException exception) {
            show(Alert.AlertType.ERROR, LanguageManager.get("purchase.return.error.prefix") + " " + safeMessage(exception));
        }
    }

    private void show(Alert.AlertType type, String message) {
        Alert alert = com.adam.warehouse.util.AlertUtil.create(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safeMessage(RuntimeException exception) {
        if (exception.getMessage() != null && exception instanceof IllegalArgumentException) {
            return LanguageManager.message(exception.getMessage());
        }
        Throwable cause = exception.getCause();
        return cause == null || cause.getMessage() == null ? LanguageManager.get("try.again") : LanguageManager.message(cause.getMessage());
    }

    /** Table row view-model: a returnable purchase line plus the operator's chosen return quantity. */
    public static class PurchaseReturnLineRow {

        private final ReturnablePurchaseItem source;
        private final SimpleIntegerProperty returnQuantity = new SimpleIntegerProperty(0);

        public PurchaseReturnLineRow(ReturnablePurchaseItem source) {
            this.source = source;
        }

        public int getPurchaseItemId() {
            return source.purchaseItemId();
        }

        public int getProductId() {
            return source.productId();
        }

        public String getProductName() {
            return source.productName();
        }

        public int getPurchasedQuantity() {
            return source.purchasedQuantity();
        }

        public int getCurrentStock() {
            return source.currentStock();
        }

        public int getMaxReturnable() {
            return source.maxReturnable();
        }

        public double getUnitPrice() {
            return source.unitPrice();
        }

        public SimpleIntegerProperty returnQuantityProperty() {
            return returnQuantity;
        }
    }
}
