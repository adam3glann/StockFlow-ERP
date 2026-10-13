package com.adam.warehouse.model;

public class ReportSummary {

    private String title;

    private double totalSales;

    private double totalProfit;

    private double totalPurchases;

    private double inventoryValue;

    private int totalProductsSold;

    private int totalTransactions;

    private int totalCustomers;

    private int totalSuppliers;

    private int totalProducts;

    private int lowStockProducts;

    private double totalReturns;
    private double totalTax;
    private double totalDiscounts;
    private double totalCollected;
    private double outstandingBalance;
    private double cashCollected;
    private double cardCollected;
    private double transferCollected;
    private double otherCollected;

    public ReportSummary() {
    }

    public double getTotalReturns() {
        return totalReturns;
    }

    public void setTotalReturns(double totalReturns) {
        this.totalReturns = totalReturns;
    }

    public double getTotalTax() { return totalTax; }
    public void setTotalTax(double value) { totalTax = value; }
    public double getTotalDiscounts() { return totalDiscounts; }
    public void setTotalDiscounts(double value) { totalDiscounts = value; }
    public double getTotalCollected() { return totalCollected; }
    public void setTotalCollected(double value) { totalCollected = value; }
    public double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(double value) { outstandingBalance = value; }
    public double getCashCollected() { return cashCollected; }
    public void setCashCollected(double value) { cashCollected = value; }
    public double getCardCollected() { return cardCollected; }
    public void setCardCollected(double value) { cardCollected = value; }
    public double getTransferCollected() { return transferCollected; }
    public void setTransferCollected(double value) { transferCollected = value; }
    public double getOtherCollected() { return otherCollected; }
    public void setOtherCollected(double value) { otherCollected = value; }

    /** Gross profit minus refunds issued in the same period; operating expenses are not included. */
    public double getGrossProfitLessRefunds() {
        return totalProfit - totalReturns;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title.trim();
    }

    public double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(double totalProfit) {
        this.totalProfit = totalProfit;
    }

    public double getTotalPurchases() {
        return totalPurchases;
    }

    public void setTotalPurchases(double totalPurchases) {
        this.totalPurchases = totalPurchases;
    }

    public double getInventoryValue() {
        return inventoryValue;
    }

    public void setInventoryValue(double inventoryValue) {
        this.inventoryValue = inventoryValue;
    }

    public int getTotalProductsSold() {
        return totalProductsSold;
    }

    public void setTotalProductsSold(int totalProductsSold) {
        this.totalProductsSold = totalProductsSold;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public int getTotalSuppliers() {
        return totalSuppliers;
    }

    public void setTotalSuppliers(int totalSuppliers) {
        this.totalSuppliers = totalSuppliers;
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(int totalProducts) {
        this.totalProducts = totalProducts;
    }

    public int getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(int lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public double getAverageSaleValue() {

        if (totalTransactions == 0) {
            return 0;
        }

        return totalSales / totalTransactions;
    }

    public double getProfitMargin() {

        if (totalSales == 0) {
            return 0;
        }

        return (totalProfit / totalSales) * 100.0;
    }

    @Override
    public String toString() {

        return """
                ========= %s =========
                Total Sales        : %.2f
                Total Profit       : %.2f
                Total Purchases    : %.2f
                Inventory Value    : %.2f
                Products Sold      : %d
                Transactions       : %d
                Customers          : %d
                Suppliers          : %d
                Products           : %d
                Low Stock Products : %d
                Average Sale       : %.2f
                Profit Margin      : %.2f%%
                """.formatted(
                title,
                totalSales,
                totalProfit,
                totalPurchases,
                inventoryValue,
                totalProductsSold,
                totalTransactions,
                totalCustomers,
                totalSuppliers,
                totalProducts,
                lowStockProducts,
                getAverageSaleValue(),
                getProfitMargin()
        );
    }

}
