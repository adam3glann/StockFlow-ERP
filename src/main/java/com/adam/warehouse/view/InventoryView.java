package com.adam.warehouse.view;

import com.adam.warehouse.controller.ProductController;
import com.adam.warehouse.controller.StockCountController;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.Product;
import com.adam.warehouse.model.StockDiscrepancy;
import com.adam.warehouse.model.StockCountItem;
import com.adam.warehouse.model.StockCountSession;
import com.adam.warehouse.service.InventoryMovementService;
import com.adam.warehouse.util.CsvExportUtil;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.ThemeManager;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.text.NumberFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Live inventory monitor with stock health and valuation. */
public class InventoryView extends BorderPane {
    private final ProductController products = new ProductController();
    private final InventoryMovementService movementService = new InventoryMovementService();
    private final StockCountController stockCountController = new StockCountController();
    private final TableView<Product> table = new TableView<>();
    private final TextField search = new TextField();
    private final Label totalProducts = new Label();
    private final Label inventoryValue = new Label();
    private final Label lowStock = new Label();
    private final Label outOfStock = new Label();

    public InventoryView() {
        setPadding(new Insets(24));
        createLayout();
        refresh();
    }

    private void createLayout() {
        Label title = new Label(LanguageManager.get("inventory.title"));
        title.getStyleClass().add("page-title");
        search.setPromptText(LanguageManager.get("inventory.search"));
        HBox.setHgrow(search, Priority.ALWAYS);
        Button refresh = new Button(LanguageManager.get("button.refresh"));
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(event -> refresh());
        Button history = new Button(LanguageManager.get("inventory.movement.button"));
        history.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        history.setOnAction(event -> showMovementHistory());
        Button stockAudit = new Button(LanguageManager.get("inventory.audit.button"));
        stockAudit.setOnAction(event -> showStockAudit());
        Button adjust = new Button(LanguageManager.get("inventory.adjust.button"));
        adjust.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        adjust.setOnAction(event -> showStockAdjustment());
        Button physicalCount = new Button(LanguageManager.get("inventory.count.button"));
        physicalCount.setOnAction(event -> chooseCountSession());
        Button reviewCounts = new Button(LanguageManager.get("inventory.count.review"));
        reviewCounts.setOnAction(event -> reviewCountVariances());
        Button countHistory = new Button(LanguageManager.get("inventory.count.history"));
        countHistory.setOnAction(event -> showCountHistory());
        setTop(new VBox(16,
                new HBox(12, title, search, refresh, history),
                new HBox(12, stockAudit, adjust, physicalCount, reviewCounts, countHistory),
                cards()));
        createTable();
        search.textProperty().addListener((observable, oldValue, value) -> filter(value));
    }

    private HBox cards() {
        HBox cards = new HBox(12,
                card(LanguageManager.get("inventory.active.products"), totalProducts, ""),
                card(LanguageManager.get("inventory.value"), inventoryValue, "dashboard-card-accent"),
                card(LanguageManager.get("inventory.low.stock"), lowStock, "dashboard-card-warning"),
                card(LanguageManager.get("inventory.out.of.stock"), outOfStock, "dashboard-card-danger"));
        return cards;
    }

    private VBox card(String caption, Label value, String style) {
        Label label = new Label(caption);
        label.getStyleClass().add("metric-card-title");
        value.getStyleClass().add("metric-card-value");
        VBox card = new VBox(6, label, value);
        card.getStyleClass().add("dashboard-card");
        if (!style.isBlank()) card.getStyleClass().add(style);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void createTable() {
        TableColumn<Product, String> name = textColumn(LanguageManager.get("product"), Product::getName);
        TableColumn<Product, String> barcode = textColumn(LanguageManager.get("barcode"), Product::getBarcode);
        TableColumn<Product, String> category = textColumn(LanguageManager.get("category"), Product::getCategoryName);
        TableColumn<Product, String> quantity = textColumn(LanguageManager.get("inventory.on.hand"), product -> String.valueOf(product.getQuantity()));
        TableColumn<Product, String> minimum = textColumn(LanguageManager.get("inventory.minimum"), product -> String.valueOf(product.getMinimumStock()));
        TableColumn<Product, String> value = textColumn(LanguageManager.get("inventory.column.value"), product -> money(product.getInventoryValue()));
        TableColumn<Product, String> status = textColumn(LanguageManager.get("inventory.stock.status"), this::status);
        String outLabel = LanguageManager.get("inventory.out.of.stock");
        String lowLabel = LanguageManager.get("inventory.low.stock");
        status.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("stock-ok", "stock-low", "stock-out");
                if (!empty) getStyleClass().add(item.equals(outLabel) ? "stock-out" : item.equals(lowLabel) ? "stock-low" : "stock-ok");
            }
        });
        table.getColumns().addAll(name, barcode, category, quantity, minimum, value, status);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(LanguageManager.get("inventory.empty")));
        setCenter(table);
    }

    private <T> TableColumn<T, String> textColumn(String title, java.util.function.Function<T, String> mapper) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(mapper.apply(cell.getValue())));
        return column;
    }

    private void refresh() {
        table.setItems(products.getProducts());
        updateMetrics();
        filter(search.getText());
    }

    private void filter(String query) {
        if (table.getItems() == null) return;
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        FilteredList<Product> filtered = new FilteredList<>(products.getProducts(), product -> normalized.isEmpty()
                || product.getName().toLowerCase(Locale.ROOT).contains(normalized)
                || product.getBarcode().toLowerCase(Locale.ROOT).contains(normalized)
                || product.getCategoryName().toLowerCase(Locale.ROOT).contains(normalized));
        table.setItems(filtered);
    }

    private void updateMetrics() {
        var items = products.getProducts();
        totalProducts.setText(String.valueOf(items.size()));
        inventoryValue.setText(money(items.stream().mapToDouble(Product::getInventoryValue).sum()));
        lowStock.setText(String.valueOf(items.stream().filter(Product::isLowStock).count()));
        outOfStock.setText(String.valueOf(items.stream().filter(Product::isOutOfStock).count()));
    }

    private String status(Product product) {
        return product.isOutOfStock() ? LanguageManager.get("inventory.out.of.stock")
                : product.isLowStock() ? LanguageManager.get("inventory.low.stock")
                : LanguageManager.get("inventory.in.stock");
    }

    private String money(double amount) { return NumberFormat.getNumberInstance(Locale.US).format(amount) + " " + LanguageManager.get("currency.egp"); }

    private void showMovementHistory() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        List<InventoryMovement> movements;
        try {
            movements = movementService.getForProduct(selected.getProductId());
        } catch (RuntimeException exception) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
            alert.setHeaderText(LanguageManager.get("inventory.movement.history"));
            alert.setContentText(com.adam.warehouse.util.LanguageManager.message(exception.getMessage()));
            alert.showAndWait();
            return;
        }

        TableView<InventoryMovement> historyTable = new TableView<>();
        historyTable.getColumns().addAll(
                textColumn(LanguageManager.get("sale.date"), m -> String.valueOf(m.getMovementDate())),
                textColumn(LanguageManager.get("inventory.movement.type"), m -> LanguageManager.code("movement.type", m.getMovementType())),
                textColumn(LanguageManager.get("inventory.movement.quantity.changed"), m -> String.valueOf(m.getQuantityChanged())),
                textColumn(LanguageManager.get("inventory.movement.before"), m -> String.valueOf(m.getQuantityBefore())),
                textColumn(LanguageManager.get("inventory.movement.after"), m -> String.valueOf(m.getQuantityAfter())),
                textColumn(LanguageManager.get("inventory.movement.reference"), InventoryMovement::getReference),
                textColumn(LanguageManager.get("employees"), InventoryMovement::getEmployeeName)
        );
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.getItems().addAll(movements);
        historyTable.setPrefSize(700, 320);
        historyTable.setPlaceholder(new Label(LanguageManager.get("inventory.movement.empty")));

        VBox content = new VBox(10, new Label(selected.getName()), historyTable);
        content.setPadding(new Insets(10));

        Alert dialog = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
        dialog.setTitle(LanguageManager.get("inventory.movement.history"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(740);
        dialog.showAndWait();
    }

    private void showStockAudit() {
        List<StockDiscrepancy> rows;
        try {
            rows = movementService.getStockAudit();
        } catch (RuntimeException exception) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
            alert.setHeaderText(LanguageManager.get("inventory.audit.title"));
            alert.setContentText(LanguageManager.message(exception.getMessage()));
            alert.showAndWait();
            return;
        }

        var source = FXCollections.observableArrayList(rows);
        FilteredList<StockDiscrepancy> filtered = new FilteredList<>(source, row -> true);
        TextField query = new TextField();
        query.setPromptText(LanguageManager.get("inventory.audit.search"));
        ChoiceBox<String> status = new ChoiceBox<>(FXCollections.observableArrayList(
                LanguageManager.get("inventory.audit.filter.all"),
                LanguageManager.get("inventory.audit.status.mismatch"),
                LanguageManager.get("inventory.audit.status.no.history"),
                LanguageManager.get("inventory.audit.status.consistent")));
        status.getSelectionModel().selectFirst();

        TableView<StockDiscrepancy> auditTable = new TableView<>(filtered);
        auditTable.getColumns().addAll(
                textColumn(LanguageManager.get("product"), StockDiscrepancy::getProductName),
                textColumn(LanguageManager.get("barcode"), StockDiscrepancy::getBarcode),
                textColumn(LanguageManager.get("category"), StockDiscrepancy::getCategoryName),
                textColumn(LanguageManager.get("inventory.audit.status"), row -> auditStatus(row)),
                textColumn(LanguageManager.get("inventory.audit.last.quantity"), row -> row.isHasMovementHistory() ? String.valueOf(row.getLastRecordedQuantity()) : "—"),
                textColumn(LanguageManager.get("inventory.on.hand"), row -> String.valueOf(row.getCurrentQuantity())),
                textColumn(LanguageManager.get("inventory.audit.difference"), row -> row.isHasMovementHistory() ? String.valueOf(row.getDifference()) : "—"),
                textColumn(LanguageManager.get("inventory.movement.type"), row -> row.getLastMovementType() == null ? "—" : LanguageManager.code("movement.type", row.getLastMovementType())),
                textColumn(LanguageManager.get("inventory.movement.reference"), row -> row.getLastReference() == null ? "—" : row.getLastReference()),
                textColumn(LanguageManager.get("sale.date"), row -> row.getLastMovementDate() == null ? "—" : String.valueOf(row.getLastMovementDate())));
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        auditTable.setPlaceholder(new Label(LanguageManager.get("inventory.audit.empty")));
        auditTable.setPrefSize(1050, 420);

        Runnable applyFilter = () -> {
            String needle = query.getText() == null ? "" : query.getText().trim().toLowerCase(Locale.ROOT);
            String selectedStatus = status.getValue();
            filtered.setPredicate(row -> {
                boolean textMatches = needle.isBlank()
                        || row.getProductName().toLowerCase(Locale.ROOT).contains(needle)
                        || row.getBarcode().toLowerCase(Locale.ROOT).contains(needle)
                        || row.getCategoryName().toLowerCase(Locale.ROOT).contains(needle);
                boolean statusMatches = LanguageManager.get("inventory.audit.filter.all").equals(selectedStatus)
                        || auditStatus(row).equals(selectedStatus);
                return textMatches && statusMatches;
            });
        };
        query.textProperty().addListener((observable, oldValue, newValue) -> applyFilter.run());
        status.valueProperty().addListener((observable, oldValue, newValue) -> applyFilter.run());

        Button export = new Button(LanguageManager.get("report.export.csv"));
        export.setOnAction(event -> exportStockAudit(filtered));
        Label explanation = new Label(LanguageManager.get("inventory.audit.explanation"));
        explanation.setWrapText(true);
        HBox filters = new HBox(10, query, status, export);
        HBox.setHgrow(query, Priority.ALWAYS);
        VBox content = new VBox(10, explanation, filters, auditTable);
        content.setPadding(new Insets(10));

        Alert dialog = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
        dialog.setTitle(LanguageManager.get("inventory.audit.title"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(1080);
        dialog.showAndWait();
    }

    private void showStockAdjustment() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextField change = new TextField();
        change.setPromptText(LanguageManager.get("inventory.adjust.quantity.prompt"));
        TextField reason = new TextField();
        reason.setPromptText(LanguageManager.get("inventory.adjust.reason.prompt"));
        Label direction = new Label(LanguageManager.get("inventory.adjust.explanation"));
        direction.setWrapText(true);
        VBox form = new VBox(10, new Label(selected.getName()), direction, change, reason);
        form.setPadding(new Insets(12));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("inventory.adjust.title"));
        dialog.getDialogPane().sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) ThemeManager.applyTheme(newScene);
        });
        ButtonType save = new ButtonType(LanguageManager.get("save"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.showAndWait().filter(save::equals).ifPresent(result -> {
            final int quantityChange;
            try {
                quantityChange = Integer.parseInt(change.getText().trim());
                if (quantityChange == 0) throw new NumberFormatException();
                if (reason.getText() == null || reason.getText().isBlank()) {
                    throw new IllegalArgumentException(LanguageManager.get("inventory.adjust.reason.required"));
                }
            } catch (NumberFormatException exception) {
                showAdjustmentError(LanguageManager.get("inventory.adjust.quantity.invalid"));
                return;
            } catch (IllegalArgumentException exception) {
                showAdjustmentError(exception.getMessage());
                return;
            }

            Alert confirmation = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);
            confirmation.setHeaderText(LanguageManager.get("inventory.adjust.confirm.title"));
            confirmation.setContentText(String.format(LanguageManager.get("inventory.adjust.confirm.message"), selected.getName(), quantityChange));
            if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;

            try {
                products.adjustStock(selected.getProductId(), quantityChange, reason.getText().trim());
                refresh();
            } catch (RuntimeException exception) {
                showAdjustmentError(LanguageManager.message(exception.getMessage()));
            }
        });
    }

    private void showAdjustmentError(String message) {
        Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
        alert.setHeaderText(LanguageManager.get("inventory.adjust.title"));
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String auditStatus(StockDiscrepancy row) {
        return LanguageManager.get("inventory.audit.status." + row.getStatus().toLowerCase(Locale.ROOT));
    }

    private void exportStockAudit(List<StockDiscrepancy> rows) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LanguageManager.get("inventory.audit.export.title"));
        chooser.setInitialFileName("stockflow-stock-audit.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("filter.csv.files"), "*.csv"));
        var file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        try {
            List<List<String>> csv = new ArrayList<>();
            csv.add(List.of(LanguageManager.get("product"), LanguageManager.get("barcode"), LanguageManager.get("category"),
                    LanguageManager.get("inventory.audit.status"), LanguageManager.get("inventory.audit.last.quantity"),
                    LanguageManager.get("inventory.on.hand"), LanguageManager.get("inventory.audit.difference"),
                    LanguageManager.get("inventory.movement.type"), LanguageManager.get("inventory.movement.reference"),
                    LanguageManager.get("sale.date")));
            for (StockDiscrepancy row : rows) {
                csv.add(List.of(row.getProductName(), row.getBarcode(), row.getCategoryName(), auditStatus(row),
                        row.isHasMovementHistory() ? String.valueOf(row.getLastRecordedQuantity()) : "",
                        String.valueOf(row.getCurrentQuantity()), row.isHasMovementHistory() ? String.valueOf(row.getDifference()) : "",
                        row.getLastMovementType() == null ? "" : LanguageManager.code("movement.type", row.getLastMovementType()),
                        row.getLastReference() == null ? "" : row.getLastReference(),
                        row.getLastMovementDate() == null ? "" : String.valueOf(row.getLastMovementDate())));
            }
            CsvExportUtil.write(file.toPath(), csv);
        } catch (IOException | RuntimeException exception) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
            alert.setHeaderText(LanguageManager.get("inventory.audit.title"));
            alert.setContentText(LanguageManager.get("inventory.audit.export.error"));
            alert.showAndWait();
        }
    }

    private void chooseCountSession() {
        List<StockCountSession> openSessions;
        try {
            openSessions = stockCountController.getOpenSessions();
        } catch (RuntimeException exception) {
            showAdjustmentError(LanguageManager.message(exception.getMessage()));
            return;
        }
        ChoiceBox<StockCountSession> choices = new ChoiceBox<>(FXCollections.observableArrayList(openSessions));
        if (!openSessions.isEmpty()) choices.getSelectionModel().selectFirst();
        VBox content = new VBox(10,
                new Label(LanguageManager.get("inventory.count.resume.prompt")), choices);
        content.setPadding(new Insets(10));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("inventory.count.title"));
        dialog.getDialogPane().sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) ThemeManager.applyTheme(newScene);
        });
        ButtonType resume = new ButtonType(LanguageManager.get("inventory.count.resume"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType start = new ButtonType(LanguageManager.get("inventory.count.start"), javafx.scene.control.ButtonBar.ButtonData.OTHER);
        ButtonType discard = new ButtonType(LanguageManager.get("inventory.count.discard"), javafx.scene.control.ButtonBar.ButtonData.NO);
        dialog.getDialogPane().getButtonTypes().addAll(resume, start, discard, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);
        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() == ButtonType.CANCEL) return;
        try {
            if (choice.get().equals(discard)) {
                StockCountSession selected = choices.getValue();
                if (selected == null) return;
                Alert confirmation = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);
                confirmation.setHeaderText(LanguageManager.get("inventory.count.discard.confirm.title"));
                confirmation.setContentText(LanguageManager.get("inventory.count.discard.confirm.message"));
                if (confirmation.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
                    stockCountController.cancelCount(selected.getCountId());
                }
                return;
            }
            StockCountSession session;
            if (choice.get().equals(start)) {
                session = stockCountController.startCount();
            } else {
                session = choices.getValue();
                if (session == null) return;
            }
            new com.adam.warehouse.view.dialog.StockCountDialog(session,
                    stockCountController.getCountItems(session.getCountId())).show();
        } catch (RuntimeException exception) {
            showAdjustmentError(LanguageManager.message(exception.getMessage()));
        }
    }

    private void reviewCountVariances() {
        List<StockCountItem> variances;
        try {
            variances = stockCountController.getOpenVariances();
        } catch (RuntimeException exception) {
            showAdjustmentError(LanguageManager.message(exception.getMessage()));
            return;
        }
        if (variances.isEmpty()) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
            alert.setHeaderText(LanguageManager.get("inventory.count.review"));
            alert.setContentText(LanguageManager.get("inventory.count.no.variances"));
            alert.showAndWait();
            return;
        }
        TableView<StockCountItem> varianceTable = new TableView<>(FXCollections.observableArrayList(variances));
        varianceTable.getColumns().addAll(
                textColumn(LanguageManager.get("product"), StockCountItem::getProductName),
                textColumn(LanguageManager.get("barcode"), StockCountItem::getBarcode),
                textColumn(LanguageManager.get("inventory.count.recorded"), item -> String.valueOf(item.getRecordedQuantity())),
                textColumn(LanguageManager.get("inventory.count.actual"), item -> String.valueOf(item.getCountedQuantity())),
                textColumn(LanguageManager.get("inventory.count.variance"), item -> String.valueOf(item.getVariance())),
                textColumn(LanguageManager.get("inventory.count.reason"), item -> item.getReason() == null ? "" : item.getReason()));
        varianceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        varianceTable.setPrefSize(850, 360);
        Button resolve = new Button(LanguageManager.get("inventory.count.resolve"));
        resolve.disableProperty().bind(varianceTable.getSelectionModel().selectedItemProperty().isNull());
        resolve.setOnAction(event -> resolveVariance(varianceTable.getSelectionModel().getSelectedItem(), varianceTable));
        VBox content = new VBox(10, varianceTable, resolve);
        content.setPadding(new Insets(10));
        Alert dialog = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
        dialog.setTitle(LanguageManager.get("inventory.count.review"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(880);
        dialog.showAndWait();
    }

    private void resolveVariance(StockCountItem item, TableView<StockCountItem> table) {
        if (item == null) return;
        TextArea notes = new TextArea();
        notes.setPromptText(LanguageManager.get("inventory.count.resolution.prompt"));
        notes.setPrefRowCount(4);
        Dialog<ButtonType> notesDialog = new Dialog<>();
        notesDialog.setTitle(LanguageManager.get("inventory.count.resolve"));
        notesDialog.getDialogPane().sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) ThemeManager.applyTheme(newScene);
        });
        ButtonType save = new ButtonType(LanguageManager.get("inventory.count.resolve"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        notesDialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        notesDialog.getDialogPane().setContent(new VBox(8,
                new Label(LanguageManager.get("inventory.count.resolve.explanation")), notes));
        if (notesDialog.showAndWait().filter(save::equals).isEmpty()) return;
        if (notes.getText() == null || notes.getText().isBlank()) {
            showAdjustmentError(LanguageManager.get("inventory.count.resolution.required"));
            return;
        }
        Alert confirmation = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);
        confirmation.setHeaderText(LanguageManager.get("inventory.count.resolve.confirm.title"));
        confirmation.setContentText(String.format(LanguageManager.get("inventory.count.resolve.confirm.message"),
                item.getProductName(), item.getRecordedQuantity(), item.getCountedQuantity()));
        if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;
        try {
            stockCountController.resolveVariance(item.getCountItemId(), notes.getText());
            table.getItems().remove(item);
            refresh();
        } catch (RuntimeException exception) {
            showAdjustmentError(LanguageManager.message(exception.getMessage()));
        }
    }

    private void showCountHistory() {
        List<StockCountSession> completed;
        try {
            completed = stockCountController.getClosedSessions();
        } catch (RuntimeException exception) {
            showAdjustmentError(LanguageManager.message(exception.getMessage()));
            return;
        }
        if (completed.isEmpty()) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
            alert.setHeaderText(LanguageManager.get("inventory.count.history"));
            alert.setContentText(LanguageManager.get("inventory.count.history.empty"));
            alert.showAndWait();
            return;
        }
        ChoiceDialog<StockCountSession> chooser = new ChoiceDialog<>(completed.getFirst(), completed);
        chooser.setTitle(LanguageManager.get("inventory.count.history"));
        chooser.setHeaderText(LanguageManager.get("inventory.count.history.select"));
        chooser.getDialogPane().sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) ThemeManager.applyTheme(newScene);
        });
        chooser.showAndWait().ifPresent(session -> {
            try {
                List<StockCountItem> items = stockCountController.getCountItems(session.getCountId());
                TableView<StockCountItem> historyTable = new TableView<>(FXCollections.observableArrayList(items));
                historyTable.getColumns().addAll(
                        textColumn(LanguageManager.get("product"), StockCountItem::getProductName),
                        textColumn(LanguageManager.get("barcode"), StockCountItem::getBarcode),
                        textColumn(LanguageManager.get("inventory.count.recorded"), item -> String.valueOf(item.getRecordedQuantity())),
                        textColumn(LanguageManager.get("inventory.count.actual"), item -> item.getCountedQuantity() == null ? "—" : String.valueOf(item.getCountedQuantity())),
                        textColumn(LanguageManager.get("inventory.count.variance"), item -> item.getVariance() == null ? "—" : String.valueOf(item.getVariance())),
                        textColumn(LanguageManager.get("inventory.count.status"), item -> LanguageManager.get("inventory.count.status." + item.getResolutionStatus().toLowerCase(Locale.ROOT))),
                        textColumn(LanguageManager.get("inventory.count.reason"), item -> item.getReason() == null ? "" : item.getReason()),
                        textColumn(LanguageManager.get("inventory.count.resolution"), item -> item.getResolutionNotes() == null ? "" : item.getResolutionNotes()),
                        textColumn(LanguageManager.get("employees"), StockCountItem::getResolvedByName),
                        textColumn(LanguageManager.get("inventory.count.resolved.at"), item -> item.getResolvedAt() == null ? "—" : String.valueOf(item.getResolvedAt())));
                historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
                historyTable.setPrefSize(1100, 450);
                Alert historyDialog = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
                historyDialog.setHeaderText(String.format(LanguageManager.get("inventory.count.session.title"), session.getCountId()));
                Label sessionDetails = new Label(String.format(LanguageManager.get("inventory.count.history.details"),
                        session.getStartedByName(), session.getStartedAt(), session.getCompletedByName(), session.getCompletedAt()));
                sessionDetails.setWrapText(true);
                historyDialog.getDialogPane().setContent(new VBox(10, sessionDetails, historyTable));
                historyDialog.getDialogPane().setPrefWidth(1140);
                historyDialog.showAndWait();
            } catch (RuntimeException exception) {
                showAdjustmentError(LanguageManager.message(exception.getMessage()));
            }
        });
    }
}
