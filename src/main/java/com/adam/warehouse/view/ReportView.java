package com.adam.warehouse.view;

import com.adam.warehouse.controller.ReportController;
import com.adam.warehouse.model.ReportSummary;
import com.adam.warehouse.model.TopSellingProduct;
import com.adam.warehouse.util.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.adam.warehouse.util.CsvExportUtil;
import com.adam.warehouse.util.SimplePdfInvoiceExporter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.text.NumberFormat;
import java.util.Locale;

/** Live operational report summary. Detailed printable reports can build on this data layer. */
public class ReportView extends BorderPane {

    private final ReportController reportController = new ReportController();
    private final ComboBox<String> periodSelector = new ComboBox<>();
    private final DatePicker startDate = new DatePicker();
    private final DatePicker endDate = new DatePicker();
    private final Label title = new Label();
    private final Label sales = new Label();
    private final Label purchases = new Label();
    private final Label profit = new Label();
    private final Label returns = new Label();
    private final Label grossProfitLessRefunds = new Label();
    private final Label discounts = new Label();
    private final Label tax = new Label();
    private final Label collected = new Label();
    private final Label balanceDue = new Label();
    private final Label cashCollected = new Label();
    private final Label cardCollected = new Label();
    private final Label transferCollected = new Label();
    private final Label inventory = new Label();
    private final Label transactions = new Label();
    private final Label productsSold = new Label();
    private final Label averageSale = new Label();
    private final Label margin = new Label();
    private final Label lowStock = new Label();
    private final TableView<TopSellingProduct> topProductsTable = new TableView<>();

    private String today() { return LanguageManager.get("report.period.today"); }
    private String allTime() { return LanguageManager.get("report.period.all.time"); }
    private String customRange() { return LanguageManager.get("report.period.custom"); }

    public ReportView() {
        setPadding(new Insets(24));
        createLayout();
        loadReport();
    }

    private void createLayout() {
        title.getStyleClass().add("page-title");
        periodSelector.getItems().addAll(today(), allTime(), customRange());
        periodSelector.getSelectionModel().selectFirst();
        Button refresh = new Button(LanguageManager.get("button.refresh"));
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(event -> loadReport());
        Button export = new Button(LanguageManager.get("report.export.csv"));
        export.setOnAction(event -> exportReport());
        Button exportPdf = new Button(LanguageManager.get("button.export.pdf"));
        exportPdf.setOnAction(event -> exportReportPdf());
        startDate.setPromptText(LanguageManager.get("report.start.date"));
        endDate.setPromptText(LanguageManager.get("report.end.date"));
        startDate.setVisible(false); startDate.setManaged(false);
        endDate.setVisible(false); endDate.setManaged(false);
        periodSelector.setOnAction(event -> {
            boolean custom = customRange().equals(periodSelector.getValue());
            startDate.setVisible(custom); startDate.setManaged(custom);
            endDate.setVisible(custom); endDate.setManaged(custom);
            if (!custom) loadReport();
        });

        HBox header = new HBox(12, title, periodSelector, startDate, endDate, refresh, export, exportPdf);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        setTop(header);

        GridPane metrics = new GridPane();
        metrics.setHgap(16);
        metrics.setVgap(16);
        metrics.setPadding(new Insets(24, 0, 0, 0));
        for (int index = 0; index < 3; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 3);
            column.setHgrow(Priority.ALWAYS);
            metrics.getColumnConstraints().add(column);
        }
        addMetric(metrics, LanguageManager.get("report.metric.sales"), sales, 0, 0);
        addMetric(metrics, LanguageManager.get("report.metric.purchases"), purchases, 1, 0);
        addMetric(metrics, LanguageManager.get("report.metric.gross.profit"), profit, 2, 0);
        addMetric(metrics, LanguageManager.get("report.metric.inventory.value"), inventory, 0, 1);
        addMetric(metrics, LanguageManager.get("report.metric.transactions"), transactions, 1, 1);
        addMetric(metrics, LanguageManager.get("report.metric.products.sold"), productsSold, 2, 1);
        addMetric(metrics, LanguageManager.get("report.metric.average.sale"), averageSale, 0, 2);
        addMetric(metrics, LanguageManager.get("report.metric.profit.margin"), margin, 1, 2);
        addMetric(metrics, LanguageManager.get("report.metric.low.stock"), lowStock, 2, 2);
        addMetric(metrics, LanguageManager.get("report.metric.returns"), returns, 0, 3);
        addMetric(metrics, LanguageManager.get("report.metric.gross.profit.after.refunds"), grossProfitLessRefunds, 1, 3);
        addMetric(metrics, LanguageManager.get("report.metric.discounts"), discounts, 2, 3);
        addMetric(metrics, LanguageManager.get("report.metric.tax"), tax, 0, 4);
        addMetric(metrics, LanguageManager.get("report.metric.collected"), collected, 1, 4);
        addMetric(metrics, LanguageManager.get("report.metric.balance.due"), balanceDue, 2, 4);
        addMetric(metrics, LanguageManager.get("report.payment.cash"), cashCollected, 0, 5);
        addMetric(metrics, LanguageManager.get("report.payment.card"), cardCollected, 1, 5);
        addMetric(metrics, LanguageManager.get("report.payment.transfer"), transferCollected, 2, 5);

        Label topProductsTitle = new Label(LanguageManager.get("report.top.products"));
        topProductsTitle.getStyleClass().add("section-title");
        createTopProductsTable();

        VBox content = new VBox(16, metrics, topProductsTitle, topProductsTable);
        content.setPadding(new Insets(0, 0, 0, 0));
        setCenter(content);
    }

    private void createTopProductsTable() {
        TableColumn<TopSellingProduct, String> product = new TableColumn<>(LanguageManager.get("report.top.products.column.product"));
        product.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
        TableColumn<TopSellingProduct, String> quantity = new TableColumn<>(LanguageManager.get("report.top.products.column.quantity"));
        quantity.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().quantitySold())));
        TableColumn<TopSellingProduct, String> revenue = new TableColumn<>(LanguageManager.get("report.top.products.column.revenue"));
        revenue.setCellValueFactory(cell -> new SimpleStringProperty(currency(cell.getValue().revenue())));
        topProductsTable.getColumns().addAll(product, quantity, revenue);
        topProductsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        topProductsTable.setPlaceholder(new Label(LanguageManager.get("report.top.products.empty")));
        topProductsTable.setPrefHeight(240);
    }

    private void addMetric(GridPane grid, String caption, Label value, int column, int row) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("metric-card-title");
        value.getStyleClass().add("metric-card-value");
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(8, captionLabel, value);
        card.getStyleClass().add("dashboard-card");
        card.setMaxWidth(Double.MAX_VALUE);
        grid.add(card, column, row);
    }

    private void loadReport() {
        try {
            boolean isToday = today().equals(periodSelector.getValue());
            boolean custom = customRange().equals(periodSelector.getValue());
            ReportSummary summary = custom ? reportController.getDateRangeSummary(startDate.getValue(), endDate.getValue())
                    : isToday ? reportController.getTodaySummary() : reportController.getSummary();
            title.setText(summary.getTitle());
            sales.setText(currency(summary.getTotalSales()));
            purchases.setText(currency(summary.getTotalPurchases()));
            profit.setText(currency(summary.getTotalProfit()));
            returns.setText(currency(summary.getTotalReturns()));
            grossProfitLessRefunds.setText(currency(summary.getGrossProfitLessRefunds()));
            discounts.setText(currency(summary.getTotalDiscounts()));
            tax.setText(currency(summary.getTotalTax()));
            collected.setText(currency(summary.getTotalCollected()));
            balanceDue.setText(currency(summary.getOutstandingBalance()));
            cashCollected.setText(currency(summary.getCashCollected()));
            cardCollected.setText(currency(summary.getCardCollected()));
            transferCollected.setText(currency(summary.getTransferCollected()));
            inventory.setText(isToday ? "—" : currency(summary.getInventoryValue()));
            transactions.setText(String.valueOf(summary.getTotalTransactions()));
            productsSold.setText(String.valueOf(summary.getTotalProductsSold()));
            averageSale.setText(currency(summary.getAverageSaleValue()));
            margin.setText(String.format(Locale.US, "%.1f%%", summary.getProfitMargin()));
            lowStock.setText(isToday ? "—" : String.valueOf(summary.getLowStockProducts()));

            LocalDate rangeStart = custom ? startDate.getValue() : isToday ? LocalDate.now() : null;
            LocalDate rangeEnd = custom ? endDate.getValue() : isToday ? LocalDate.now() : null;
            if (!custom || (rangeStart != null && rangeEnd != null && !rangeEnd.isBefore(rangeStart))) {
                topProductsTable.setItems(javafx.collections.FXCollections.observableArrayList(
                        reportController.getTopSellingProducts(rangeStart, rangeEnd, 10)));
            }
        } catch (RuntimeException exception) {
            // Keep database exception details out of console output and user-facing logs.
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
            alert.setHeaderText(LanguageManager.get("report.load.error.title"));
            alert.setContentText(LanguageManager.get("report.load.error.message"));
            alert.showAndWait();
        }
    }

    private String currency(double amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " " + LanguageManager.get("currency.egp");
    }

    private void exportReport() {
        try {
            boolean isToday = today().equals(periodSelector.getValue());
            boolean custom = customRange().equals(periodSelector.getValue());
            ReportSummary summary = custom ? reportController.getDateRangeSummary(startDate.getValue(), endDate.getValue())
                    : isToday ? reportController.getTodaySummary() : reportController.getSummary();
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LanguageManager.get("report.export.dialog.title"));
            chooser.setInitialFileName("stockflow-" + (custom ? "custom" : isToday ? "today" : "all-time") + "-report.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("filter.csv.files"), "*.csv"));
            File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
            if (file == null) return;
            CsvExportUtil.write(file.toPath(), List.of(
                    List.of(LanguageManager.get("report.pdf.report.title"), summary.getTitle()),
                    List.of(LanguageManager.get("report.generated.label"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now())),
                    List.of(LanguageManager.get("report.column.metric"), LanguageManager.get("inventory.column.value")),
                    List.of(LanguageManager.get("report.metric.sales"), currency(summary.getTotalSales())),
                    List.of(LanguageManager.get("report.metric.purchases"), currency(summary.getTotalPurchases())),
                    List.of(LanguageManager.get("report.metric.gross.profit"), currency(summary.getTotalProfit())),
                    List.of(LanguageManager.get("report.metric.returns"), currency(summary.getTotalReturns())),
                    List.of(LanguageManager.get("report.metric.gross.profit.after.refunds"), currency(summary.getGrossProfitLessRefunds())),
                    List.of(LanguageManager.get("report.metric.discounts"), currency(summary.getTotalDiscounts())),
                    List.of(LanguageManager.get("report.metric.tax"), currency(summary.getTotalTax())),
                    List.of(LanguageManager.get("report.metric.collected"), currency(summary.getTotalCollected())),
                    List.of(LanguageManager.get("report.metric.balance.due"), currency(summary.getOutstandingBalance())),
                    List.of(LanguageManager.get("report.payment.cash"), currency(summary.getCashCollected())),
                    List.of(LanguageManager.get("report.payment.card"), currency(summary.getCardCollected())),
                    List.of(LanguageManager.get("report.payment.transfer"), currency(summary.getTransferCollected())),
                    List.of(LanguageManager.get("report.metric.inventory.value"), currency(summary.getInventoryValue())),
                    List.of(LanguageManager.get("report.metric.transactions"), String.valueOf(summary.getTotalTransactions())),
                    List.of(LanguageManager.get("report.metric.products.sold"), String.valueOf(summary.getTotalProductsSold())),
                    List.of(LanguageManager.get("report.metric.average.sale"), currency(summary.getAverageSaleValue())),
                    List.of(LanguageManager.get("report.metric.profit.margin"), String.format(Locale.US, "%.1f%%", summary.getProfitMargin())),
                    List.of(LanguageManager.get("report.metric.low.stock"), String.valueOf(summary.getLowStockProducts()))));
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
            alert.setHeaderText(LanguageManager.get("report.export.success.title"));
            alert.setContentText(String.format(LanguageManager.get("report.export.success.message"), file.getAbsolutePath()));
            alert.showAndWait();
        } catch (IOException | RuntimeException exception) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
            alert.setHeaderText(LanguageManager.get("report.export.error.title"));
            alert.setContentText(LanguageManager.get("report.export.error.message"));
            alert.showAndWait();
        }
    }

    private void exportReportPdf() {
        try {
            boolean isToday = today().equals(periodSelector.getValue());
            boolean custom = customRange().equals(periodSelector.getValue());
            ReportSummary summary = custom ? reportController.getDateRangeSummary(startDate.getValue(), endDate.getValue())
                    : isToday ? reportController.getTodaySummary() : reportController.getSummary();
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LanguageManager.get("report.export.dialog.title"));
            chooser.setInitialFileName("stockflow-" + (custom ? "custom" : isToday ? "today" : "all-time") + "-report.pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("filter.pdf.files"), "*.pdf"));
            File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
            if (file == null) return;
            SimplePdfInvoiceExporter.reportSummary(file.toPath(), summary, topProductsTable.getItems());
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);
            alert.setHeaderText(LanguageManager.get("report.export.success.title"));
            alert.setContentText(String.format(LanguageManager.get("report.export.success.message"), file.getAbsolutePath()));
            alert.showAndWait();
        } catch (IOException | RuntimeException exception) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
            alert.setHeaderText(LanguageManager.get("report.export.error.title"));
            alert.setContentText(LanguageManager.get("report.export.error.message"));
            alert.showAndWait();
        }
    }
}
