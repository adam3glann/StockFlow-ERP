package com.adam.warehouse.view;



import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.controller.ActivityLogController;

import com.adam.warehouse.controller.DashboardController;

import com.adam.warehouse.controller.PurchaseController;

import com.adam.warehouse.controller.SaleController;

import com.adam.warehouse.model.ActivityLog;

import com.adam.warehouse.model.LowStockProduct;

import com.adam.warehouse.model.Purchase;

import com.adam.warehouse.model.Sale;

import com.adam.warehouse.model.TopSellingProduct;

import javafx.geometry.Insets;

import javafx.geometry.Pos;

import javafx.scene.control.Alert;

import javafx.scene.control.Button;

import javafx.scene.control.Label;

import javafx.scene.control.ListView;

import javafx.scene.chart.BarChart;

import javafx.scene.chart.CategoryAxis;

import javafx.scene.chart.NumberAxis;

import javafx.scene.chart.XYChart;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.GridPane;

import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;

import javafx.scene.layout.VBox;



import java.text.NumberFormat;

import java.time.format.DateTimeFormatter;

import java.util.Comparator;

import java.util.Locale;



public class DashboardHomeView extends BorderPane {



    private final DashboardController dashboardController =

            new DashboardController();

    private final SaleController saleController = new SaleController();

    private final PurchaseController purchaseController = new PurchaseController();

    private final ActivityLogController activityLogController = new ActivityLogController();

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");



    private final Label salesValue = new Label();

    private final Label purchasesValue = new Label();

    private final Label profitValue = new Label();

    private final Label productsValue = new Label();

    private final Label customersValue = new Label();

    private final Label suppliersValue = new Label();

    private final Label lowStockValue = new Label();
    private final Label employeesValue = new Label();
    private final Label outOfStockValue = new Label();
    private final Label returnsTodayValue = new Label();



    private final ListView<String> lowStockList =

            new ListView<>();

    private final ListView<String> recentSalesList = new ListView<>();

    private final ListView<String> recentPurchasesList = new ListView<>();

    private final ListView<String> topProductsList = new ListView<>();

    private final ListView<String> recentActivityList = new ListView<>();

    private final CategoryAxis trendXAxis = new CategoryAxis();

    private final NumberAxis trendYAxis = new NumberAxis();

    private final BarChart<String, Number> trendChart = new BarChart<>(trendXAxis, trendYAxis);



    public DashboardHomeView() {



        setPadding(new Insets(24));



        createLayout();



        refreshDashboard();



    }



    private void createLayout() {



        Label title = new Label(

                LanguageManager.get("dashboard.title")

        );



        title.getStyleClass().add("page-title");



        Label subtitle = new Label(

                LanguageManager.get("dashboard.subtitle")

        );



        subtitle.getStyleClass().add("dashboard-subtitle");



        Button refreshButton = new Button(

                LanguageManager.get("button.refresh")

        );



        refreshButton.getStyleClass().add("primary-button");



        refreshButton.setOnAction(event ->

                refreshDashboard()

        );



        VBox heading = new VBox(

                4,

                title,

                subtitle

        );



        HBox top = new HBox(

                16,

                heading,

                refreshButton

        );



        top.setAlignment(Pos.CENTER_LEFT);



        HBox.setHgrow(

                heading,

                Priority.ALWAYS

        );



        setTop(top);



        GridPane financialMetrics = new GridPane();



        financialMetrics.setHgap(16);



        financialMetrics.setVgap(16);



        financialMetrics.setPadding(

                new Insets(24, 0, 16, 0)

        );



        financialMetrics.add(



                createMetricCard(

                        LanguageManager.get("dashboard.sales.today"),

                        salesValue,

                        "dashboard-card-accent"

                ),



                0,



                0



        );



        financialMetrics.add(



                createMetricCard(

                        LanguageManager.get("dashboard.purchases.today"),

                        purchasesValue,

                        "dashboard-card-warning"

                ),



                1,



                0



        );



        financialMetrics.add(



                createMetricCard(

                        LanguageManager.get("dashboard.profit.today"),

                        profitValue,

                        "dashboard-card-success"

                ),



                2,



                0



        );



        GridPane businessMetrics = new GridPane();



        businessMetrics.setHgap(16);



        businessMetrics.setVgap(16);



        businessMetrics.add(



                createMetricCard(

                        LanguageManager.get("dashboard.products"),

                        productsValue,

                        ""

                ),



                0,



                0



        );



        businessMetrics.add(



                createMetricCard(

                        LanguageManager.get("dashboard.customers"),

                        customersValue,

                        ""

                ),



                1,



                0



        );



        businessMetrics.add(



                createMetricCard(

                        LanguageManager.get("dashboard.suppliers"),

                        suppliersValue,

                        ""

                ),



                2,



                0



        );



        businessMetrics.add(



                createMetricCard(

                        LanguageManager.get("dashboard.low.stock"),

                        lowStockValue,

                        "dashboard-card-danger"

                ),



                3,



                0



        );

        businessMetrics.add(
                createMetricCard(LanguageManager.get("employees"), employeesValue, ""), 0, 1
        );
        businessMetrics.add(
                createMetricCard(LanguageManager.get("inventory.out.of.stock"), outOfStockValue, "dashboard-card-danger"), 1, 1
        );
        businessMetrics.add(
                createMetricCard(LanguageManager.get("dashboard.returns.today"), returnsTodayValue, "dashboard-card-warning"), 2, 1
        );



        configureMetricGrid(financialMetrics, 3);
        setVisibleManaged(financialMetrics.getChildren().get(0), PermissionManager.canViewReports());
        setVisibleManaged(financialMetrics.getChildren().get(1), PermissionManager.canManagePurchases());
        setVisibleManaged(financialMetrics.getChildren().get(2), PermissionManager.canViewReports());



        configureMetricGrid(businessMetrics, 4);
        setVisibleManaged(businessMetrics.getChildren().get(1), PermissionManager.canManageCustomers());
        setVisibleManaged(businessMetrics.getChildren().get(2), PermissionManager.canManageSuppliers());
        setVisibleManaged(businessMetrics.getChildren().get(3), PermissionManager.canManageProducts());
        setVisibleManaged(businessMetrics.getChildren().get(4), PermissionManager.canManageEmployees());
        setVisibleManaged(businessMetrics.getChildren().get(5), PermissionManager.canManageProducts());
        setVisibleManaged(businessMetrics.getChildren().get(6), PermissionManager.canViewReports());



        Label trendTitle = new Label(LanguageManager.get("dashboard.trend.title"));
        trendTitle.getStyleClass().add("section-title");
        trendXAxis.setLabel(LanguageManager.get("dashboard.trend.axis.day"));
        trendYAxis.setLabel(LanguageManager.get("dashboard.trend.axis.amount"));
        trendChart.setAnimated(false);
        trendChart.setLegendVisible(true);
        trendChart.setPrefHeight(280);



        Label lowStockTitle = new Label(



                LanguageManager.get("dashboard.low.stock.title")



        );



        lowStockTitle.getStyleClass().add(

                "section-title"

        );

        setVisibleManaged(lowStockTitle, PermissionManager.canManageProducts());
        setVisibleManaged(lowStockList, PermissionManager.canManageProducts());



        lowStockList.setPlaceholder(



                new Label(

                        LanguageManager.get(

                                "dashboard.low.stock.empty"

                        )

                )



        );



        lowStockList.setPrefHeight(190);

        recentSalesList.setPlaceholder(new Label(LanguageManager.get("dashboard.recent.sales.empty")));
        recentSalesList.setPrefHeight(170);

        recentPurchasesList.setPlaceholder(new Label(LanguageManager.get("dashboard.recent.purchases.empty")));
        recentPurchasesList.setPrefHeight(170);

        topProductsList.setPlaceholder(new Label(LanguageManager.get("dashboard.top.products.empty")));
        topProductsList.setPrefHeight(170);

        recentActivityList.setPlaceholder(new Label(LanguageManager.get("dashboard.recent.activity.empty")));
        recentActivityList.setPrefHeight(170);

        VBox recentSalesPanel = activityPanel("dashboard.recent.sales.title", recentSalesList);
        VBox recentPurchasesPanel = activityPanel("dashboard.recent.purchases.title", recentPurchasesList);
        VBox topProductsPanel = activityPanel("dashboard.top.products.title", topProductsList);
        VBox recentActivityPanel = activityPanel("dashboard.recent.activity.title", recentActivityList);
        setVisibleManaged(recentSalesPanel, PermissionManager.canMakeSales());
        setVisibleManaged(recentPurchasesPanel, PermissionManager.canManagePurchases());
        setVisibleManaged(topProductsPanel, PermissionManager.canViewReports());
        setVisibleManaged(recentActivityPanel, PermissionManager.canViewReports());
        setVisibleManaged(trendTitle, PermissionManager.canViewReports());
        setVisibleManaged(trendChart, PermissionManager.canViewReports());

        GridPane activityGrid = new GridPane();
        activityGrid.setHgap(16);
        activityGrid.setVgap(16);
        activityGrid.add(recentSalesPanel, 0, 0);
        activityGrid.add(recentPurchasesPanel, 1, 0);
        activityGrid.add(topProductsPanel, 0, 1);
        activityGrid.add(recentActivityPanel, 1, 1);
        configureMetricGrid(activityGrid, 2);



        VBox content = new VBox(



                16,



                financialMetrics,



                businessMetrics,



                trendTitle,

                trendChart,



                lowStockTitle,



                lowStockList,

                activityGrid



        );



        setCenter(content);



    }



    private VBox createMetricCard(

            String title,

            Label value,

            String extraStyleClass

    ) {



        Label titleLabel = new Label(title);



        titleLabel.getStyleClass().add(

                "metric-card-title"

        );



        value.getStyleClass().add(

                "metric-card-value"

        );



        VBox card = new VBox(

                8,

                titleLabel,

                value

        );



        card.getStyleClass().add(

                "dashboard-card"

        );



        if (!extraStyleClass.isBlank()) {



            card.getStyleClass().add(

                    extraStyleClass

            );



        }



        card.setMaxWidth(Double.MAX_VALUE);



        return card;



    }

    private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private VBox activityPanel(String titleKey, ListView<String> list) {
        Label title = new Label(LanguageManager.get(titleKey));
        title.getStyleClass().add("section-title");
        VBox panel = new VBox(8, title, list);
        panel.getStyleClass().add("dashboard-card");
        panel.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private void configureMetricGrid(GridPane grid, int columns) {



        for (int index = 0; index < columns; index++) {



            javafx.scene.layout.ColumnConstraints column =

                    new javafx.scene.layout.ColumnConstraints();



            column.setPercentWidth(100.0 / columns);



            column.setHgrow(Priority.ALWAYS);



            grid.getColumnConstraints().add(column);



        }



    }



    private void refreshDashboard() {



        try {



            if (PermissionManager.canViewReports()) salesValue.setText(

                    formatCurrency(

                            dashboardController.getTodaySalesTotal()

                    )

            );



            if (PermissionManager.canManagePurchases()) purchasesValue.setText(

                    formatCurrency(

                            dashboardController.getTodayPurchasesTotal()

                    )

            );



            if (PermissionManager.canViewReports()) profitValue.setText(

                    formatCurrency(

                            dashboardController.getEstimatedGrossProfitToday()

                    )

            );



            productsValue.setText(

                    String.valueOf(

                            dashboardController.getTotalProducts()

                    )

            );



            if (PermissionManager.canManageCustomers()) customersValue.setText(

                    String.valueOf(

                            dashboardController.getTotalCustomers()

                    )

            );



            if (PermissionManager.canManageSuppliers()) suppliersValue.setText(

                    String.valueOf(

                            dashboardController.getTotalSuppliers()

                    )

            );



            if (PermissionManager.canManageProducts()) lowStockValue.setText(

                    String.valueOf(

                            dashboardController.getLowStockCount()

                    )

            );

            if (PermissionManager.canManageEmployees()) employeesValue.setText(String.valueOf(dashboardController.getTotalEmployees()));
            if (PermissionManager.canManageProducts()) outOfStockValue.setText(String.valueOf(dashboardController.getOutOfStockCount()));
            if (PermissionManager.canViewReports()) returnsTodayValue.setText(formatCurrency(dashboardController.getTodayReturnsTotal()));



            if (PermissionManager.canManageProducts()) lowStockList.getItems().setAll(



                    dashboardController.getLowStockProducts()



                            .stream()



                            .map(this::formatLowStockProduct)



                            .toList()



            );

            if (PermissionManager.canMakeSales()) recentSalesList.getItems().setAll(
                    saleController.getSales().stream()
                            .sorted(Comparator.comparing(Sale::getSaleDate).reversed())
                            .limit(5)
                            .map(this::formatRecentSale)
                            .toList()
            );

            if (PermissionManager.canManagePurchases()) recentPurchasesList.getItems().setAll(
                    purchaseController.getPurchases().stream()
                            .sorted(Comparator.comparing(Purchase::getPurchaseDate).reversed())
                            .limit(5)
                            .map(this::formatRecentPurchase)
                            .toList()
            );

            if (PermissionManager.canViewReports()) topProductsList.getItems().setAll(
                    dashboardController.getTopSellingProducts(5).stream()
                            .map(this::formatTopSellingProduct)
                            .toList()
            );

            if (PermissionManager.canViewReports()) recentActivityList.getItems().setAll(
                    activityLogController.getLogs().stream()
                            .limit(5)
                            .map(this::formatRecentActivity)
                            .toList()
            );

            if (PermissionManager.canViewReports()) updateTrendChart();

        } catch (RuntimeException exception) {

            // Keep database exception details out of console output and user-facing logs.

            Alert alert =

                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);



            alert.setHeaderText(

                    LanguageManager.get(

                            "dashboard.unavailable"

                    )

            );



            alert.setContentText(

                    LanguageManager.get(

                            "dashboard.load.error"

                    )

            );



            alert.showAndWait();



        }



    }



    private String formatCurrency(double amount) {



        return NumberFormat

                .getNumberInstance(Locale.US)

                .format(amount)

                + " " + LanguageManager.get("currency.egp");



    }



    private String formatRecentSale(Sale sale) {

        String customer = sale.getCustomerName() == null || sale.getCustomerName().isBlank()
                ? LanguageManager.get("dashboard.walkin.customer")
                : sale.getCustomerName();

        String when = sale.getSaleDate() == null ? "" : DATE_TIME.format(sale.getSaleDate());

        return String.format(
                LanguageManager.get("dashboard.recent.sale.item"),
                customer,
                formatCurrency(sale.getTotalAmount()),
                when
        );

    }

    private String formatRecentPurchase(Purchase purchase) {

        String when = purchase.getPurchaseDate() == null ? "" : DATE_TIME.format(purchase.getPurchaseDate());

        return String.format(
                LanguageManager.get("dashboard.recent.purchase.item"),
                purchase.getSupplierName(),
                formatCurrency(purchase.getTotalAmount()),
                when
        );

    }

    private String formatTopSellingProduct(TopSellingProduct product) {

        return String.format(
                LanguageManager.get("dashboard.top.product.item"),
                product.name(),
                product.quantitySold(),
                formatCurrency(product.revenue())
        );

    }

    private String formatRecentActivity(ActivityLog log) {

        String when = log.getTimestamp() == null ? "" : DATE_TIME.format(log.getTimestamp());

        return String.format(
                LanguageManager.get("dashboard.recent.activity.item"),
                when,
                log.getEmployeeName(),
                LanguageManager.activityDescription(log.getDescription())
        );

    }

    private String formatLowStockProduct(

            LowStockProduct product

    ) {



        return String.format(



                LanguageManager.get(

                        "dashboard.low.stock.item"

                ),



                product.name(),



                product.quantity(),



                product.minimumStock()



        );



    }



    private void updateTrendChart() {

        java.util.List<com.adam.warehouse.model.DailyTotal> totals =
                dashboardController.getDailyTotals(7);

        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("MM-dd");

        XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
        salesSeries.setName(LanguageManager.get("dashboard.trend.sales.series"));

        XYChart.Series<String, Number> purchasesSeries = new XYChart.Series<>();
        purchasesSeries.setName(LanguageManager.get("dashboard.trend.purchases.series"));

        for (com.adam.warehouse.model.DailyTotal total : totals) {
            String label = dayFormat.format(total.date());
            salesSeries.getData().add(new XYChart.Data<>(label, total.sales()));
            purchasesSeries.getData().add(new XYChart.Data<>(label, total.purchases()));
        }

        trendChart.getData().setAll(salesSeries, purchasesSeries);

    }

}
