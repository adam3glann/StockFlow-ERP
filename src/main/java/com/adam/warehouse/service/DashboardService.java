package com.adam.warehouse.service;

import com.adam.warehouse.dao.DashboardDAO;
import com.adam.warehouse.model.DailyTotal;
import com.adam.warehouse.model.LowStockProduct;
import com.adam.warehouse.model.TopSellingProduct;

import java.util.List;
import com.adam.warehouse.util.PermissionManager;

public class DashboardService {

    private final DashboardDAO dashboardDAO;

    public DashboardService() {
        dashboardDAO = new DashboardDAO();
    }

    public int getTotalProducts() {
        PermissionManager.requireAuthenticated("view dashboard");
        return dashboardDAO.getTotalProducts();
    }

    public int getProductsInStock() {
        PermissionManager.require(PermissionManager.canManageProducts(), "view inventory metrics");
        return dashboardDAO.getProductsInStock();
    }

    public int getLowStockCount() {
        PermissionManager.require(PermissionManager.canManageProducts(), "view inventory metrics");
        return dashboardDAO.getLowStockCount();
    }

    public int getTotalCustomers() {
        PermissionManager.require(PermissionManager.canManageCustomers(), "view customer metrics");
        return dashboardDAO.getTotalCustomers();
    }

    public int getTotalSuppliers() {
        PermissionManager.require(PermissionManager.canManageSuppliers(), "view supplier metrics");
        return dashboardDAO.getTotalSuppliers();
    }

    public int getTotalEmployees() { PermissionManager.require(PermissionManager.canManageEmployees(), "view employee metrics"); return dashboardDAO.getTotalEmployees(); }

    public int getOutOfStockCount() { PermissionManager.require(PermissionManager.canManageProducts(), "view inventory metrics"); return dashboardDAO.getOutOfStockCount(); }

    public double getTodaySalesTotal() {
        PermissionManager.require(PermissionManager.canViewReports(), "view sales metrics");
        return dashboardDAO.getTodaySalesTotal();
    }

    public double getTodayPurchasesTotal() {
        PermissionManager.require(PermissionManager.canViewReports(), "view purchase metrics");
        return dashboardDAO.getTodayPurchasesTotal();
    }

    public double getTodayProfit() {
        PermissionManager.require(PermissionManager.canViewReports(), "view profit metrics");
        return dashboardDAO.getTodayProfit();
    }

    public double getTotalRevenue() {
        PermissionManager.require(PermissionManager.canViewReports(), "view revenue metrics");
        return dashboardDAO.getTotalRevenue();
    }

    public double getTotalProfit() {
        PermissionManager.require(PermissionManager.canViewReports(), "view profit metrics");
        return dashboardDAO.getTotalProfit();
    }

    public double getInventoryValue() {
        PermissionManager.require(PermissionManager.canManageProducts(), "view inventory metrics");
        return dashboardDAO.getInventoryValue();
    }

    public double getTodayReturnsTotal() {
        PermissionManager.require(PermissionManager.canViewReports(), "view return metrics");
        return dashboardDAO.getTodayReturnsTotal();
    }

    public double getTotalReturnsAmount() {
        PermissionManager.require(PermissionManager.canViewReports(), "view return metrics");
        return dashboardDAO.getTotalReturnsAmount();
    }

    public List<LowStockProduct> getLowStockProducts() {
        PermissionManager.require(PermissionManager.canManageProducts(), "view inventory metrics");
        return dashboardDAO.getLowStockProducts();
    }

    public List<TopSellingProduct> getTopSellingProducts(int limit) {
        PermissionManager.require(PermissionManager.canViewReports(), "view sales metrics");
        return dashboardDAO.getTopSellingProducts(limit);
    }

    public boolean hasLowStockProducts() {
        return getLowStockCount() > 0;
    }

    public boolean hasSalesToday() {
        return getTodaySalesTotal() > 0;
    }

    public boolean hasPurchasesToday() {
        return getTodayPurchasesTotal() > 0;
    }

    public List<DailyTotal> getDailyTotals(int days) {
        PermissionManager.require(PermissionManager.canViewReports(), "view sales metrics");
        return dashboardDAO.getDailyTotals(days);
    }

}
