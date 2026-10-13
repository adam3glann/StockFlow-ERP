package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.ReturnController;
import com.adam.warehouse.controller.SaleController;
import com.adam.warehouse.model.Return;
import com.adam.warehouse.model.ReturnItem;
import com.adam.warehouse.model.ReturnableItem;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.util.LanguageManager;
import javafx.beans.property.SimpleBooleanProperty;
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
 * Select a sale, choose which of its items (and how much of each) to return,
 * decide whether the returned quantity goes back into sellable stock, and
 * record the refund. Restocking and the refund total are handled atomically
 * by {@link com.adam.warehouse.dao.ReturnDAO}.
 */
public class ReturnDialog extends BaseDialog {

    private final SaleController saleController = new SaleController();
    private final ReturnController returnController = new ReturnController();

    private final ComboBox<Sale> saleComboBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final TableView<ReturnLineRow> itemsTable = new TableView<>();
    private final TextArea reasonArea = new TextArea();
    private final Label totalLabel = new Label(String.format(LanguageManager.get("return.total.format"), 0.0));

    public ReturnDialog() {
        super(LanguageManager.get("return.dialog.new"));
        createTable();
        createForm();
        loadSales("");
    }

    private void createTable() {

        TableColumn<ReturnLineRow, String> product = new TableColumn<>(LanguageManager.get("return.column.product"));
        product.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<ReturnLineRow, Integer> sold = new TableColumn<>(LanguageManager.get("return.column.sold"));
        sold.setCellValueFactory(new PropertyValueFactory<>("soldQuantity"));

        TableColumn<ReturnLineRow, Integer> remaining = new TableColumn<>(LanguageManager.get("return.column.remaining"));
        remaining.setCellValueFactory(new PropertyValueFactory<>("remainingQuantity"));

        TableColumn<ReturnLineRow, Double> price = new TableColumn<>(LanguageManager.get("return.column.unit.price"));
        price.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<ReturnLineRow, Integer> qty = new TableColumn<>(LanguageManager.get("return.column.quantity"));
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
                ReturnLineRow row = (ReturnLineRow) getTableRow().getItem();
                int max = Math.max(0, row.remainingQuantity());
                spinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, row.returnQuantityProperty().get()));
                currentListener = (observable, oldValue, newValue) ->
                        row.returnQuantityProperty().set(newValue == null ? 0 : newValue);
                spinner.valueProperty().addListener(currentListener);
                setGraphic(max <= 0 ? null : spinner);
            }
        });

        TableColumn<ReturnLineRow, Boolean> restock = new TableColumn<>(LanguageManager.get("return.column.restock"));
        restock.setCellValueFactory(cell -> cell.getValue().restockProperty());
        restock.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(Boolean value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ReturnLineRow row = (ReturnLineRow) getTableRow().getItem();
                checkBox.setSelected(row.restockProperty().get());
                checkBox.setOnAction(event -> row.restockProperty().set(checkBox.isSelected()));
                setGraphic(checkBox);
            }
        });

        itemsTable.getColumns().addAll(product, sold, remaining, price, qty, restock);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.setEditable(true);
        itemsTable.setPlaceholder(new Label(LanguageManager.get("return.select.sale.hint")));
        itemsTable.setPrefHeight(260);
    }

    private void createForm() {

        saleComboBox.setPrefWidth(360);
        saleComboBox.setPromptText(LanguageManager.get("return.select.sale"));
        saleComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Sale sale) {
                if (sale == null) return "";
                String customer = sale.getCustomerName() == null || sale.getCustomerName().isBlank()
                        ? LanguageManager.get("sale.customer.walkin") : sale.getCustomerName();
                return "#" + sale.getSaleId() + " — " + customer + " — "
                        + String.format(LanguageManager.get("return.sale.total.format"), sale.getTotalAmount());
            }

            @Override
            public Sale fromString(String string) {
                return null;
            }
        });
        saleComboBox.setOnAction(event -> loadReturnableItems());

        searchField.setPromptText(LanguageManager.get("return.search.sale.hint"));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadSales(newValue));

        reasonArea.setPromptText(LanguageManager.get("return.reason.hint"));
        reasonArea.setPrefRowCount(3);

        Button save = new Button(LanguageManager.get("button.complete.return"));
        Button cancel = new Button(LanguageManager.get("cancel"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveReturn());
        cancel.setOnAction(event -> close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);
        grid.add(new Label(LanguageManager.get("return.search.sale")), 0, 0);
        grid.add(searchField, 1, 0);
        grid.add(new Label(LanguageManager.get("return.select.sale")), 0, 1);
        grid.add(saleComboBox, 1, 1);
        grid.add(new Label(LanguageManager.get("return.items")), 0, 2);
        grid.add(itemsTable, 1, 2);
        grid.add(totalLabel, 1, 3);
        grid.add(new Label(LanguageManager.get("return.reason")), 0, 4);
        grid.add(reasonArea, 1, 4);
        grid.add(new HBox(10, save, cancel), 1, 5);
        root.setCenter(grid);
    }

    private void loadSales(String keyword) {
        Sale previouslySelected = saleComboBox.getValue();
        List<Sale> sales = keyword == null || keyword.isBlank()
                ? saleController.getSales()
                : saleController.searchSales(keyword);
        ObservableList<Sale> items = FXCollections.observableArrayList(sales);
        saleComboBox.setItems(items);
        if (previouslySelected != null && items.stream().anyMatch(s -> s.getSaleId() == previouslySelected.getSaleId())) {
            saleComboBox.setValue(previouslySelected);
        }
    }

    private void loadReturnableItems() {
        Sale selected = saleComboBox.getValue();
        itemsTable.getItems().clear();
        if (selected == null) {
            updateTotal();
            return;
        }
        try {
            List<ReturnableItem> returnable = returnController.getReturnableItems(selected.getSaleId());
            for (ReturnableItem item : returnable) {
                ReturnLineRow row = new ReturnLineRow(item);
                row.returnQuantityProperty().addListener((observable, oldValue, newValue) -> updateTotal());
                row.restockProperty().addListener((observable, oldValue, newValue) -> updateTotal());
                itemsTable.getItems().add(row);
            }
            updateTotal();
        } catch (RuntimeException exception) {
            show(Alert.AlertType.ERROR, LanguageManager.get("return.error.prefix") + " " + safeMessage(exception));
        }
    }

    private void updateTotal() {
        double total = itemsTable.getItems().stream()
                .mapToDouble(row -> row.returnQuantityProperty().get() * row.unitPrice())
                .sum();
        totalLabel.setText(String.format(LanguageManager.get("return.total.format"), total));
    }

    private void saveReturn() {

        Sale selected = saleComboBox.getValue();
        if (selected == null) {
            show(Alert.AlertType.WARNING, LanguageManager.get("return.select.sale.warning"));
            return;
        }

        List<ReturnLineRow> chosen = itemsTable.getItems().stream()
                .filter(row -> row.returnQuantityProperty().get() > 0)
                .toList();

        if (chosen.isEmpty()) {
            show(Alert.AlertType.WARNING, LanguageManager.get("return.items.required"));
            return;
        }

        try {
            Return returnRecord = new Return();
            returnRecord.setSaleId(selected.getSaleId());
            returnRecord.setReason(reasonArea.getText());

            for (ReturnLineRow row : chosen) {
                returnRecord.addItem(new ReturnItem(
                        row.saleItemId(),
                        row.productId(),
                        row.productName(),
                        row.returnQuantityProperty().get(),
                        row.unitPrice(),
                        row.restockProperty().get()
                ));
            }

            returnController.saveReturn(returnRecord);
            show(Alert.AlertType.INFORMATION, LanguageManager.get("return.completed"));
            close();
        } catch (RuntimeException exception) {
            show(Alert.AlertType.ERROR, LanguageManager.get("return.error.prefix") + " " + safeMessage(exception));
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

    /** Table row view-model: a returnable sale line plus the operator's chosen return quantity/restock flag. */
    public static class ReturnLineRow {

        private final ReturnableItem source;
        private final SimpleIntegerProperty returnQuantity = new SimpleIntegerProperty(0);
        private final SimpleBooleanProperty restock = new SimpleBooleanProperty(true);

        public ReturnLineRow(ReturnableItem source) {
            this.source = source;
        }

        public int saleItemId() {
            return source.saleItemId();
        }

        public int productId() {
            return source.productId();
        }

        public String getProductName() {
            return source.productName();
        }

        public int getSoldQuantity() {
            return source.soldQuantity();
        }

        public int getRemainingQuantity() {
            return remainingQuantity();
        }

        public int remainingQuantity() {
            return source.remainingQuantity();
        }

        public double getUnitPrice() {
            return source.unitPrice();
        }

        public double unitPrice() {
            return source.unitPrice();
        }

        public String productName() {
            return source.productName();
        }

        public SimpleIntegerProperty returnQuantityProperty() {
            return returnQuantity;
        }

        public SimpleBooleanProperty restockProperty() {
            return restock;
        }
    }
}
