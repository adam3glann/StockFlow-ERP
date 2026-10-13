package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.CustomerController;
import com.adam.warehouse.controller.SaleController;
import com.adam.warehouse.model.Customer;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.LanguageManager;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;

/** Records a sale, optionally linked to a customer for sales history and reporting. */
public class SaleDialog extends BaseDialog {

    private final SaleController saleController = new SaleController();
    private final CustomerController customerController = new CustomerController();
    private final ComboBox<Customer> customerComboBox = new ComboBox<>();
    private final TableView<SaleItem> itemsTable = new TableView<>();
    private final TextArea notesArea = new TextArea();
    private final Label totalLabel = new Label(String.format(LanguageManager.get("sale.total.format"), 0.0));

    public SaleDialog() {
        super(LanguageManager.get("sale.dialog.new"));
        createTable();
        createForm();
    }

    private void createTable() {
        TableColumn<SaleItem, String> product = new TableColumn<>(LanguageManager.get("product"));
        product.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<SaleItem, Integer> quantity = new TableColumn<>(LanguageManager.get("quantity"));
        quantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<SaleItem, Double> price = new TableColumn<>(LanguageManager.get("price"));
        price.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        TableColumn<SaleItem, Double> subtotal = new TableColumn<>(LanguageManager.get("sale.subtotal"));
        subtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        itemsTable.getColumns().addAll(product, quantity, price, subtotal);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.setPlaceholder(new Label(LanguageManager.get("sale.empty.products")));
        itemsTable.setPrefHeight(250);
    }

    private void createForm() {
        customerComboBox.setItems(customerController.getCustomers());
        customerComboBox.setPromptText(LanguageManager.get("sale.customer.walkin"));
        customerComboBox.setPrefWidth(320);
        notesArea.setPromptText(LanguageManager.get("sale.notes.hint"));
        notesArea.setPrefRowCount(3);
        Button addItem = new Button(LanguageManager.get("button.add.item"));
        Button removeItem = new Button(LanguageManager.get("sale.item.remove.selected"));
        Button save = new Button(LanguageManager.get("button.complete.sale"));
        Button cancel = new Button(LanguageManager.get("cancel"));
        addItem.getStyleClass().add("primary-button");
        save.getStyleClass().add("primary-button");
        addItem.setOnAction(event -> addItem());
        removeItem.setOnAction(event -> removeSelectedItem());
        save.setOnAction(event -> saveSale());
        cancel.setOnAction(event -> close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);
        grid.add(new Label(LanguageManager.get("sale.customer")), 0, 0);
        grid.add(customerComboBox, 1, 0);
        grid.add(new Label(LanguageManager.get("sale.products")), 0, 1);
        grid.add(itemsTable, 1, 1);
        grid.add(new HBox(10, addItem, removeItem), 1, 2);
        grid.add(totalLabel, 1, 3);
        grid.add(new Label(LanguageManager.get("sale.notes")), 0, 4);
        grid.add(notesArea, 1, 4);
        grid.add(new HBox(10, save, cancel), 1, 5);
        root.setCenter(grid);
    }

    private void addItem() {
        SaleItemDialog dialog = new SaleItemDialog();
        dialog.showAndWait();
        if (dialog.isSaved()) {
            SaleItem newItem = dialog.getSaleItem();
            SaleItem existing = itemsTable.getItems().stream()
                    .filter(item -> item.getProductId() == newItem.getProductId())
                    .findFirst().orElse(null);
            if (existing == null) {
                itemsTable.getItems().add(newItem);
            } else {
                existing.setQuantity(existing.getQuantity() + newItem.getQuantity());
                existing.calculateSubtotal();
                itemsTable.refresh();
            }
            updateTotal();
        }
    }

    private void removeSelectedItem() {
        SaleItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            itemsTable.getItems().remove(selected);
            updateTotal();
        }
    }

    private void updateTotal() {
        double total = itemsTable.getItems().stream().mapToDouble(SaleItem::getSubtotal).sum();
        totalLabel.setText(String.format(LanguageManager.get("sale.total.format"), total));
    }

    private void saveSale() {
        if (itemsTable.getItems().isEmpty()) {
            show(Alert.AlertType.WARNING, LanguageManager.get("sale.items.required"));
            return;
        }
        try {
            Sale sale = new Sale();
            Customer customer = customerComboBox.getValue();
            if (customer != null) {
                sale.setCustomerId(customer.getCustomerId());
                sale.setCustomerName(customer.getFullName());
            }
            sale.setEmployeeId(SessionManager.requireCurrentEmployee().getEmployeeId());
            sale.setNotes(notesArea.getText());
            sale.setItems(FXCollections.observableArrayList(itemsTable.getItems()));
            saleController.saveSale(sale);
            show(Alert.AlertType.INFORMATION, LanguageManager.get("sale.completed"));
            close();
        } catch (RuntimeException exception) {
            show(Alert.AlertType.ERROR, LanguageManager.get("sale.error.prefix") + " " + safeMessage(exception));
        }
    }

    private void show(Alert.AlertType type, String message) {
        Alert alert = com.adam.warehouse.util.AlertUtil.create(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safeMessage(RuntimeException exception) {
        Throwable cause = exception.getCause();
        return cause == null || cause.getMessage() == null ? LanguageManager.get("try.again") : LanguageManager.message(cause.getMessage());
    }
}
