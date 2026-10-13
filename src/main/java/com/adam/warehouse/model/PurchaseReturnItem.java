package com.adam.warehouse.model;

public class PurchaseReturnItem {

    private int purchaseReturnItemId;

    private int purchaseReturnId;

    private int purchaseItemId;

    private int productId;

    private String productName;

    private int quantity;

    private double unitPrice;

    private double creditAmount;

    public PurchaseReturnItem() {
    }

    public PurchaseReturnItem(int purchaseItemId, int productId, String productName,
                               int quantity, double unitPrice) {
        this.purchaseItemId = purchaseItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateCreditAmount();
    }

    public int getPurchaseReturnItemId() {
        return purchaseReturnItemId;
    }

    public void setPurchaseReturnItemId(int purchaseReturnItemId) {
        this.purchaseReturnItemId = purchaseReturnItemId;
    }

    public int getPurchaseReturnId() {
        return purchaseReturnId;
    }

    public void setPurchaseReturnId(int purchaseReturnId) {
        this.purchaseReturnId = purchaseReturnId;
    }

    public int getPurchaseItemId() {
        return purchaseItemId;
    }

    public void setPurchaseItemId(int purchaseItemId) {
        this.purchaseItemId = purchaseItemId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
        calculateCreditAmount();
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        calculateCreditAmount();
    }

    public double getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(double creditAmount) {
        this.creditAmount = creditAmount;
    }

    public void calculateCreditAmount() {
        creditAmount = quantity * unitPrice;
    }

    @Override
    public String toString() {
        return productName + " x" + quantity + " = " + String.format("%.2f", creditAmount);
    }
}
