package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.CustomerController;
import com.adam.warehouse.controller.SaleController;
import com.adam.warehouse.model.Customer;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.enums.PaymentMethod;
import com.adam.warehouse.util.BranchIdentityManager;
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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.util.StringConverter;

/** Records a sale, optionally linked to a customer for sales history and reporting. */
public class SaleDialog extends BaseDialog {

    private final SaleController saleController = new SaleController();
    private final CustomerController customerController = new CustomerController();
    private final ComboBox<Customer> customerComboBox = new ComboBox<>();
    private final TableView<SaleItem> itemsTable = new TableView<>();
    private final TextArea notesArea = new TextArea();
    private final TextField discountField = new TextField("0.00");
    private final TextField amountTenderedField = new TextField("0.00");
    private final TextField paymentReferenceField = new TextField();
    private final ComboBox<PaymentMethod> paymentMethodBox = new ComboBox<>();
    private final Label subtotalLabel = new Label();
    private final Label taxLabel = new Label();
    private final Label totalLabel = new Label();
    private final Label changeLabel = new Label();
    private final Label balanceLabel = new Label();
    private boolean paymentAmountEdited;
    private boolean updatingPaymentField;
    private double currentTotal;

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
        discountField.setText("0.00");
        amountTenderedField.setText("0.00");
        paymentReferenceField.setPromptText(LanguageManager.get("sale.payment.reference.prompt"));
        paymentMethodBox.getItems().setAll(PaymentMethod.CASH, PaymentMethod.CARD,
                PaymentMethod.BANK_TRANSFER, PaymentMethod.OTHER);
        paymentMethodBox.setValue(PaymentMethod.CASH);
        paymentMethodBox.setConverter(new StringConverter<>() {
            @Override public String toString(PaymentMethod value) { return value == null ? "" : value.toString(); }
            @Override public PaymentMethod fromString(String value) { return null; }
        });
        discountField.textProperty().addListener((observable, oldValue, newValue) -> updateTotal());
        amountTenderedField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingPaymentField) paymentAmountEdited = true;
            updatePaymentSummary();
        });
        paymentMethodBox.setOnAction(event -> updatePaymentSummary());
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
        grid.add(new Label(LanguageManager.get("sale.subtotal")), 0, 3);
        grid.add(subtotalLabel, 1, 3);
        grid.add(new Label(LanguageManager.get("sale.discount")), 0, 4);
        grid.add(discountField, 1, 4);
        grid.add(new Label(LanguageManager.get("sale.tax")), 0, 5);
        grid.add(taxLabel, 1, 5);
        grid.add(new Label(LanguageManager.get("sale.total.label")), 0, 6);
        grid.add(totalLabel, 1, 6);
        grid.add(new Label(LanguageManager.get("sale.payment.method")), 0, 7);
        grid.add(paymentMethodBox, 1, 7);
        grid.add(new Label(LanguageManager.get("sale.amount.tendered")), 0, 8);
        grid.add(amountTenderedField, 1, 8);
        grid.add(new Label(LanguageManager.get("sale.change.due")), 0, 9);
        grid.add(changeLabel, 1, 9);
        grid.add(new Label(LanguageManager.get("sale.balance.due")), 0, 10);
        grid.add(balanceLabel, 1, 10);
        grid.add(new Label(LanguageManager.get("sale.payment.reference")), 0, 11);
        grid.add(paymentReferenceField, 1, 11);
        grid.add(new Label(LanguageManager.get("sale.notes")), 0, 12);
        grid.add(notesArea, 1, 12);
        grid.add(new HBox(10, save, cancel), 1, 13);
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
        Sale preview = new Sale();
        preview.setItems(itemsTable.getItems());
        preview.setDiscountAmount(parseMoney(discountField.getText(), 0));
        preview.setTaxRate(BranchIdentityManager.get().taxRate());
        preview.calculateTotalAmount();
        currentTotal = preview.getTotalAmount();
        subtotalLabel.setText(String.format(LanguageManager.get("sale.amount.format"), preview.getSubtotalAmount()));
        taxLabel.setText(String.format(LanguageManager.get("sale.tax.format"), preview.getTaxRate(), preview.getTaxAmount()));
        totalLabel.setText(String.format(LanguageManager.get("sale.total.format"), preview.getTotalAmount()));
        if (!paymentAmountEdited) {
            updatingPaymentField = true;
            amountTenderedField.setText(String.format(java.util.Locale.ROOT, "%.2f", preview.getTotalAmount()));
            updatingPaymentField = false;
        }
        updatePaymentSummary();
    }

    private void updatePaymentSummary() {
        double total = currentTotal;
        double tendered = parseMoney(amountTenderedField.getText(), 0);
        boolean cash = paymentMethodBox.getValue() == PaymentMethod.CASH;
        double applied = Math.min(total, tendered);
        double change = cash ? Math.max(0, tendered - total) : 0;
        changeLabel.setText(String.format(LanguageManager.get("sale.amount.format"), change));
        balanceLabel.setText(String.format(LanguageManager.get("sale.amount.format"), Math.max(0, total - applied)));
    }

    private double parseMoney(String text, double fallback) {
        if (text == null || text.isBlank()) return fallback;
        try { return Double.parseDouble(text.trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return fallback; }
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
            double discount = Double.parseDouble(discountField.getText().trim().replace(',', '.'));
            if (!Double.isFinite(discount) || discount < 0 || discount > sale.getSubtotalAmount()) {
                show(Alert.AlertType.WARNING, LanguageManager.get("sale.discount.invalid"));
                return;
            }
            sale.setDiscountAmount(discount);
            sale.setTaxRate(BranchIdentityManager.get().taxRate());
            sale.calculateTotalAmount();
            double tendered = Double.parseDouble(amountTenderedField.getText().trim().replace(',', '.'));
            if (!Double.isFinite(tendered) || tendered < 0) {
                show(Alert.AlertType.WARNING, LanguageManager.get("sale.payment.invalid"));
                return;
            }
            double applied = Math.min(sale.getTotalAmount(), tendered);
            if (paymentMethodBox.getValue() != PaymentMethod.CASH && tendered > sale.getTotalAmount() + 0.005) {
                show(Alert.AlertType.WARNING, LanguageManager.get("sale.payment.no.change.for.non.cash"));
                return;
            }
            sale.setPaymentMethod(paymentMethodBox.getValue());
            sale.setAmountTendered(tendered);
            sale.setAmountPaid(applied);
            sale.setChangeDue(paymentMethodBox.getValue() == PaymentMethod.CASH ? Math.max(0, tendered - sale.getTotalAmount()) : 0);
            sale.setPaymentReference(paymentReferenceField.getText());
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
