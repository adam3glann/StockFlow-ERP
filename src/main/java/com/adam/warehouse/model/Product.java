package com.adam.warehouse.model;

public class Product {

    // =========================
    // Identification
    // =========================

    private int productId;

    private String barcode;

    private String name;

    private String categoryName;

    // =========================
    // Details
    // =========================

    private String description;

    private int categoryId;

    // =========================
    // Pricing
    // =========================

    private double purchasePrice;

    private double sellingPrice;

    // =========================
    // Inventory
    // =========================

    private int quantity;

    private int minimumStock;

    // =========================
    // Status
    // =========================

    private boolean active;

    public Product() {
    }

    public Product(int productId,
                   String barcode,
                   String name,
                   String description,
                   int categoryId,
                   String categoryName,
                   double purchasePrice,
                   double sellingPrice,
                   int quantity,
                   int minimumStock,
                   boolean active) {

        this.productId = productId;
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.minimumStock = minimumStock;
        this.active = active;
    }

    // =========================
    // Getters & Setters
    // =========================

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode == null ? "" : barcode.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName == null ? "" : categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = Math.max(0, purchasePrice);
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = Math.max(0, sellingPrice);
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = Math.max(0, minimumStock);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // =========================
    // Business Methods
    // =========================

    public boolean isLowStock() {
        return quantity <= minimumStock;
    }

    public boolean isOutOfStock() {
        return quantity == 0;
    }

    public double getProfitPerUnit() {
        return sellingPrice - purchasePrice;
    }

    public double getProfitMargin() {

        if (purchasePrice == 0) {
            return 0;
        }

        return ((sellingPrice - purchasePrice) / purchasePrice) * 100.0;
    }

    public double getInventoryValue() {
        return purchasePrice * quantity;
    }

    public double getPotentialRevenue() {
        return sellingPrice * quantity;
    }

    public double getPotentialProfit() {
        return (sellingPrice - purchasePrice) * quantity;
    }

    public void increaseStock(int amount) {

        if (amount > 0) {
            quantity += amount;
        }

    }

    public void decreaseStock(int amount) {

        if (amount <= 0) {
            return;
        }

        quantity -= amount;

        if (quantity < 0) {
            quantity = 0;
        }

    }

    @Override
    public String toString() {

        if (barcode == null || barcode.isBlank()) {
            return name;
        }

        return name + " (" + barcode + ")";

    }

}