package com.adam.warehouse.controller;

import com.adam.warehouse.model.DailyTotal;
import com.adam.warehouse.model.LowStockProduct;
import com.adam.warehouse.model.TopSellingProduct;
import com.adam.warehouse.service.DashboardService;

import java.util.List;

public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController() {
        dashboardService = new DashboardService();
    }

    public int getTotalProducts() {
        return dashboardService.getTotalProducts();
    }

    public int getProductsInStock() {
        return dashboardService.getProductsInStock();
    }

    public int getLowStockCount() {
        return dashboardService.getLowStockCount();
    }

    public int getTotalCustomers() {
        return dashboardService.getTotalCustomers();
    }

    public int getTotalSuppliers() {
        return dashboardService.getTotalSuppliers();
    }

    public int getTotalEmployees() { return dashboardService.getTotalEmployees(); }

    public int getOutOfStockCount() { return dashboardService.getOutOfStockCount(); }

    public double getTodaySalesTotal() {
        return dashboardService.getTodaySalesTotal();
    }

    public double getTodayPurchasesTotal() {
        return dashboardService.getTodayPurchasesTotal();
    }

    public double getTodayProfit() {
        return dashboardService.getTodayProfit();
    }

    public double getTotalRevenue() {
        return dashboardService.getTotalRevenue();
    }

    public double getTotalProfit() {
        return dashboardService.getTotalProfit();
    }

    public double getInventoryValue() {
        return dashboardService.getInventoryValue();
    }

    public double getTodayReturnsTotal() {
        return dashboardService.getTodayReturnsTotal();
    }

    public double getTotalReturnsAmount() {
        return dashboardService.getTotalReturnsAmount();
    }

    public List<LowStockProduct> getLowStockProducts() {
        return dashboardService.getLowStockProducts();
    }
    public double getEstimatedGrossProfitToday() {
        return dashboardService.getTodayProfit();
    }

    public List<TopSellingProduct> getTopSellingProducts(int limit) {
        return dashboardService.getTopSellingProducts(limit);
    }

    public List<DailyTotal> getDailyTotals(int days) {
        return dashboardService.getDailyTotals(days);
    }

}
