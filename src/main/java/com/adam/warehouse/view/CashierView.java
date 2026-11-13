package com.adam.warehouse.view;

import com.adam.warehouse.controller.CustomerController;
import com.adam.warehouse.controller.ProductController;
import com.adam.warehouse.controller.SaleController;
import com.adam.warehouse.enums.PaymentMethod;
import com.adam.warehouse.model.Customer;
import com.adam.warehouse.model.Product;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.util.BranchIdentityManager;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.SimplePdfInvoiceExporter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** Focused point-of-sale workspace for cashier accounts. */
public final class CashierView extends BorderPane {
    private final ProductController productController = new ProductController();
    private final CustomerController customerController = new CustomerController();
    private final SaleController saleController = new SaleController();
    private final TextField barcodeField = new TextField();
    private final TextField searchField = new TextField();
    private final TextField discountField = new TextField("0.00");
    private final TextField tenderedField = new TextField("0.00");
    private final TextField referenceField = new TextField();
    private final TableView<Product> productsTable = new TableView<>();
    private final TableView<SaleItem> cartTable = new TableView<>();
    private final ComboBox<Customer> customerBox = new ComboBox<>();
    private final ComboBox<PaymentMethod> paymentMethodBox = new ComboBox<>();
    private final Label subtotalValue = new Label();
    private final Label taxValue = new Label();
    private final Label totalValue = new Label();
    private final Label changeValue = new Label();
    private final Label balanceValue = new Label();
    private final Button checkoutButton = new Button();
    private final Button receiptButton = new Button();
    private Sale lastSale;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(250));

    public CashierView() {
        setPadding(new Insets(18));
        createTables();
        createLayout();
        wireEvents();
        updateTotals();
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) Platform.runLater(barcodeField::requestFocus);
        });
        BranchIdentityManager.identityProperty().addListener((observable, oldValue, newValue) -> updateTotals());
    }

    private void createTables() {
        TableColumn<Product, String> productName = new TableColumn<>(LanguageManager.get("product.name"));
        productName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Product, String> productPrice = new TableColumn<>(LanguageManager.get("price"));
        productPrice.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(money(cell.getValue().getSellingPrice())));
        TableColumn<Product, Integer> productStock = new TableColumn<>(LanguageManager.get("inventory.on.hand"));
        productStock.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        productsTable.getColumns().setAll(productName, productPrice, productStock);
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        productsTable.setPlaceholder(new Label(LanguageManager.get("cashier.products.hint")));

        TableColumn<SaleItem, String> itemName = new TableColumn<>(LanguageManager.get("product.name"));
        itemName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<SaleItem, Integer> itemQuantity = new TableColumn<>(LanguageManager.get("quantity"));
        itemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<SaleItem, String> itemPrice = new TableColumn<>(LanguageManager.get("price"));
        itemPrice.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(money(cell.getValue().getSellingPrice())));
        TableColumn<SaleItem, String> itemSubtotal = new TableColumn<>(LanguageManager.get("sale.subtotal"));
        itemSubtotal.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(money(cell.getValue().getSubtotal())));
        cartTable.getColumns().setAll(itemName, itemQuantity, itemPrice, itemSubtotal);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cartTable.setPlaceholder(new Label(LanguageManager.get("cashier.cart.empty")));
    }

    private void createLayout() {
        Label title = new Label(LanguageManager.get("cashier.screen.title"));
        title.getStyleClass().add("page-title");
        Label branch = new Label();
        branch.textProperty().bind(BranchIdentityManager.displayProperty());
        branch.getStyleClass().add("branch-label");
        VBox heading = new VBox(4, title, branch);

        barcodeField.setPromptText(LanguageManager.get("barcode.scan.hint"));
        barcodeField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        barcodeField.setPrefColumnCount(18);
        searchField.setPromptText(LanguageManager.get("cashier.search.products"));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button searchButton = new Button(LanguageManager.get("cashier.search"));
        Button scanButton = new Button(LanguageManager.get("cashier.add.barcode"));
        scanButton.getStyleClass().add("primary-button");
        HBox searchRow = new HBox(8, barcodeField, scanButton, searchField, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        customerBox.setItems(customerController.getCustomers());
        customerBox.setPromptText(LanguageManager.get("sale.customer.walkin"));
        customerBox.setMaxWidth(Double.MAX_VALUE);
        customerBox.setConverter(new StringConverter<>() {
            @Override public String toString(Customer customer) {
                return customer == null ? "" : customer.getFullName();
            }
            @Override public Customer fromString(String value) { return null; }
        });
        discountField.setPrefColumnCount(8);
        tenderedField.setPrefColumnCount(10);
        referenceField.setPromptText(LanguageManager.get("sale.payment.reference.prompt"));
        paymentMethodBox.getItems().setAll(PaymentMethod.CASH, PaymentMethod.CARD,
                PaymentMethod.BANK_TRANSFER, PaymentMethod.OTHER);
        paymentMethodBox.setValue(PaymentMethod.CASH);
        paymentMethodBox.setConverter(new StringConverter<>() {
            @Override public String toString(PaymentMethod method) { return method == null ? "" : method.toString(); }
            @Override public PaymentMethod fromString(String value) { return null; }
        });

        Button addButton = new Button(LanguageManager.get("cashier.add.selected"));
        Button decreaseButton = new Button(LanguageManager.get("cashier.quantity.decrease"));
        Button increaseButton = new Button(LanguageManager.get("cashier.quantity.increase"));
        Button removeButton = new Button(LanguageManager.get("cashier.remove.selected"));
        Button clearButton = new Button(LanguageManager.get("cashier.clear.cart"));
        checkoutButton.setText(LanguageManager.get("cashier.checkout"));
        checkoutButton.getStyleClass().add("primary-button");
        receiptButton.setText(LanguageManager.get("cashier.receipt.export"));
        receiptButton.setDisable(true);

        HBox catalogButtons = new HBox(8, addButton);
        HBox cartButtons = new HBox(8, decreaseButton, increaseButton, removeButton, clearButton);
        HBox totals = new HBox(24,
                metric(LanguageManager.get("sale.subtotal"), subtotalValue),
                metric(LanguageManager.get("sale.tax"), taxValue),
                metric(LanguageManager.get("sale.total.label"), totalValue),
                metric(LanguageManager.get("sale.change.due"), changeValue),
                metric(LanguageManager.get("sale.balance.due"), balanceValue));
        totals.setAlignment(Pos.CENTER_LEFT);
        GridPane payment = new GridPane();
        payment.setHgap(10);
        payment.setVgap(8);
        payment.add(new Label(LanguageManager.get("sale.customer")), 0, 0);
        payment.add(customerBox, 1, 0);
        payment.add(new Label(LanguageManager.get("sale.discount")), 2, 0);
        payment.add(discountField, 3, 0);
        payment.add(new Label(LanguageManager.get("sale.payment.method")), 4, 0);
        payment.add(paymentMethodBox, 5, 0);
        payment.add(new Label(LanguageManager.get("sale.amount.tendered")), 0, 1);
        payment.add(tenderedField, 1, 1);
        payment.add(new Label(LanguageManager.get("sale.payment.reference")), 2, 1);
        payment.add(referenceField, 3, 1, 3, 1);
        GridPane.setHgrow(customerBox, Priority.ALWAYS);
        GridPane.setHgrow(referenceField, Priority.ALWAYS);

        VBox catalog = new VBox(10,
                new Label(LanguageManager.get("cashier.products.title")), searchRow, productsTable, catalogButtons);
        catalog.getStyleClass().add("dashboard-card");
        catalog.setPadding(new Insets(14));
        VBox.setVgrow(productsTable, Priority.ALWAYS);
        VBox basket = new VBox(10,
                new Label(LanguageManager.get("cashier.cart.title")), cartTable, cartButtons);
        basket.getStyleClass().add("dashboard-card");
        basket.setPadding(new Insets(14));
        VBox.setVgrow(cartTable, Priority.ALWAYS);
        HBox.setHgrow(catalog, Priority.ALWAYS);
        HBox.setHgrow(basket, Priority.ALWAYS);
        catalog.setPrefWidth(420);
        basket.setPrefWidth(650);
        HBox workArea = new HBox(14, catalog, basket);
        VBox.setVgrow(workArea, Priority.ALWAYS);

        HBox actions = new HBox(10, checkoutButton, receiptButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox footer = new VBox(12, totals, payment, actions);
        footer.setPadding(new Insets(14, 2, 0, 2));
        setTop(heading);
        setCenter(workArea);
        setBottom(footer);

        scanButton.setOnAction(event -> addBarcode());
        searchButton.setOnAction(event -> searchProducts());
        addButton.setOnAction(event -> addSelectedProduct());
        productsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) addSelectedProduct();
        });
        decreaseButton.setOnAction(event -> changeSelectedQuantity(-1));
        increaseButton.setOnAction(event -> changeSelectedQuantity(1));
        removeButton.setOnAction(event -> removeSelectedItem());
        clearButton.setOnAction(event -> clearCart());
    }

    private VBox metric(String caption, Label value) {
        Label label = new Label(caption);
        label.getStyleClass().add("metric-card-title");
        value.getStyleClass().add("metric-card-value");
        return new VBox(4, label, value);
    }

    private void wireEvents() {
        barcodeField.setOnAction(event -> addBarcode());
        searchField.setOnAction(event -> searchProducts());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchDelay.stop();
            searchDelay.setOnFinished(event -> searchProducts());
            searchDelay.playFromStart();
        });
        discountField.textProperty().addListener((observable, oldValue, newValue) -> updateTotals());
        tenderedField.textProperty().addListener((observable, oldValue, newValue) -> updateTotals());
        paymentMethodBox.setOnAction(event -> updateTotals());
        checkoutButton.setOnAction(event -> checkout());
        receiptButton.setOnAction(event -> exportReceipt());
    }

    private void searchProducts() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (keyword.length() < 2) {
            productsTable.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            productsTable.setItems(productController.searchProducts(keyword));
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void addBarcode() {
        String barcode = barcodeField.getText() == null ? "" : barcodeField.getText().trim();
        if (barcode.isEmpty()) return;
        try {
            Product product = productController.getProductByBarcode(barcode);
            if (product == null) {
                show(Alert.AlertType.WARNING, LanguageManager.get("cashier.barcode.not.found"));
                barcodeField.selectAll();
                return;
            }
            addProduct(product);
            barcodeField.clear();
            barcodeField.requestFocus();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void addSelectedProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            show(Alert.AlertType.WARNING, LanguageManager.get("cashier.product.select"));
            return;
        }
        addProduct(selected);
    }

    private void addProduct(Product product) {
        SaleItem existing = cartTable.getItems().stream()
                .filter(item -> item.getProductId() == product.getProductId()).findFirst().orElse(null);
        int nextQuantity = existing == null ? 1 : existing.getQuantity() + 1;
        if (product.getQuantity() < nextQuantity) {
            show(Alert.AlertType.WARNING, LanguageManager.get("cashier.stock.insufficient"));
            return;
        }
        if (existing == null) {
            cartTable.getItems().add(new SaleItem(product.getProductId(), product.getName(), 1,
                    product.getSellingPrice(), product.getPurchasePrice()));
        } else {
            existing.setQuantity(nextQuantity);
            cartTable.refresh();
        }
        updateTotals();
    }

    private void changeSelectedQuantity(int delta) {
        SaleItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item == null) return;
        if (delta < 0 && item.getQuantity() == 1) {
            cartTable.getItems().remove(item);
        } else {
            if (delta > 0) {
                try {
                    Product current = productController.getProductById(item.getProductId());
                    if (current == null || current.getQuantity() < item.getQuantity() + 1) {
                        show(Alert.AlertType.WARNING, LanguageManager.get("cashier.stock.insufficient"));
                        return;
                    }
                } catch (RuntimeException exception) {
                    showError(exception);
                    return;
                }
            }
            item.setQuantity(item.getQuantity() + delta);
            cartTable.refresh();
        }
        updateTotals();
    }

    private void removeSelectedItem() {
        SaleItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) cartTable.getItems().remove(selected);
        updateTotals();
    }

    private void clearCart() {
        if (cartTable.getItems().isEmpty()) return;
        Alert confirm = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION,
                LanguageManager.get("cashier.clear.confirm"), ButtonType.CANCEL, ButtonType.OK);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            cartTable.getItems().clear();
            discountField.setText("0.00");
            tenderedField.setText("0.00");
            updateTotals();
        }
    }

    private Sale previewSale() {
        Sale sale = new Sale();
        sale.setItems(cartTable.getItems());
        sale.setDiscountAmount(parseOrZero(discountField.getText()));
        sale.setTaxRate(BranchIdentityManager.get().taxRate());
        sale.calculateTotalAmount();
        return sale;
    }

    private void updateTotals() {
        Sale sale = previewSale();
        double tendered = parseOrZero(tenderedField.getText());
        double paid = Math.min(sale.getTotalAmount(), tendered);
        boolean cash = paymentMethodBox.getValue() == PaymentMethod.CASH;
        subtotalValue.setText(money(sale.getSubtotalAmount()));
        taxValue.setText(String.format(LanguageManager.get("sale.tax.format"), sale.getTaxRate(), sale.getTaxAmount()));
        totalValue.setText(money(sale.getTotalAmount()));
        changeValue.setText(money(cash ? Math.max(0, tendered - sale.getTotalAmount()) : 0));
        balanceValue.setText(money(Math.max(0, sale.getTotalAmount() - paid)));
        checkoutButton.setDisable(cartTable.getItems().isEmpty());
    }

    private void checkout() {
        try {
            if (cartTable.getItems().isEmpty()) throw new IllegalArgumentException("cashier.cart.empty");
            Sale sale = previewSale();
            double discount = Double.parseDouble(discountField.getText().trim().replace(',', '.'));
            double tendered = Double.parseDouble(tenderedField.getText().trim().replace(',', '.'));
            if (!Double.isFinite(discount) || discount < 0 || discount > sale.getSubtotalAmount()) {
                throw new IllegalArgumentException("sale.discount.invalid");
            }
            if (!Double.isFinite(tendered) || tendered <= 0) throw new IllegalArgumentException("sale.payment.invalid");
            double paid = Math.min(sale.getTotalAmount(), tendered);
            if (paid <= 0) throw new IllegalArgumentException("sale.payment.invalid");
            if (paid + 0.005 < sale.getTotalAmount() && customerBox.getValue() == null) {
                throw new IllegalArgumentException("sale.partial.payment.customer.required");
            }
            PaymentMethod method = paymentMethodBox.getValue();
            if (method == null || (method != PaymentMethod.CASH && tendered > sale.getTotalAmount() + 0.005)) {
                throw new IllegalArgumentException("sale.payment.no.change.for.non.cash");
            }

            Customer customer = customerBox.getValue();
            if (customer != null) {
                sale.setCustomerId(customer.getCustomerId());
                sale.setCustomerName(customer.getFullName());
            }
            sale.setEmployeeId(com.adam.warehouse.session.SessionManager.requireCurrentEmployee().getEmployeeId());
            sale.setPaymentMethod(method);
            sale.setAmountTendered(tendered);
            sale.setAmountPaid(paid);
            sale.setChangeDue(method == PaymentMethod.CASH ? Math.max(0, tendered - sale.getTotalAmount()) : 0);
            sale.setPaymentReference(referenceField.getText());
            saleController.saveSale(sale);
            lastSale = sale;
            receiptButton.setDisable(lastSale == null);
            cartTable.getItems().clear();
            customerBox.setValue(null);
            discountField.setText("0.00");
            tenderedField.setText("0.00");
            referenceField.clear();
            updateTotals();
            barcodeField.requestFocus();
            show(Alert.AlertType.INFORMATION, String.format(LanguageManager.get("cashier.sale.saved"),
                    sale.getSaleId(), money(sale.getTotalAmount())));
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void exportReceipt() {
        if (lastSale == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LanguageManager.get("cashier.receipt.export"));
        chooser.setInitialFileName("sale-receipt-" + lastSale.getSaleId() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                LanguageManager.get("filter.pdf.files"), "*.pdf"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        try {
            SimplePdfInvoiceExporter.saleInvoice(file.toPath(), lastSale);
            show(Alert.AlertType.INFORMATION, LanguageManager.get("notice.saved.to") + " " + file.getAbsolutePath());
        } catch (IOException | RuntimeException exception) {
            show(Alert.AlertType.ERROR, LanguageManager.get("notice.export.failed.sale.pdf"));
        }
    }

    private double parseOrZero(String value) {
        try { return value == null || value.isBlank() ? 0 : Double.parseDouble(value.trim().replace(',', '.')); }
        catch (NumberFormatException exception) { return 0; }
    }

    private String money(double value) {
        return String.format(Locale.US, "%.2f %s", value, LanguageManager.get("currency.egp"));
    }

    private void showError(RuntimeException exception) {
        String raw = exception.getMessage();
        String message = raw == null ? LanguageManager.get("cashier.operation.failed") : LanguageManager.get(raw);
        if (message.startsWith("!")) message = LanguageManager.message(raw);
        show(Alert.AlertType.ERROR, message);
    }

    private void show(Alert.AlertType type, String message) {
        Alert alert = com.adam.warehouse.util.AlertUtil.create(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
