package com.adam.warehouse.model;

public class PurchaseItem {

    private int purchaseItemId;

    private int purchaseId;

    private int productId;

    private String productName;

    private int quantity;

    private double purchasePrice;

    private double subtotal;

    public PurchaseItem() {
    }

    public PurchaseItem(int productId,
                        String productName,
                        int quantity,
                        double purchasePrice) {

        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;

        calculateSubtotal();
    }

    public int getPurchaseItemId() {
        return purchaseItemId;
    }

    public void setPurchaseItemId(int purchaseItemId) {
        this.purchaseItemId = purchaseItemId;
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
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

        calculateSubtotal();
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {

        this.purchasePrice = purchasePrice;

        calculateSubtotal();
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public void calculateSubtotal() {

        subtotal = quantity * purchasePrice;

    }

    public void increaseQuantity(int amount) {

        if (amount <= 0) {
            return;
        }

        quantity += amount;

        calculateSubtotal();
    }

    public void decreaseQuantity(int amount) {

        if (amount <= 0) {
            return;
        }

        quantity -= amount;

        if (quantity < 0) {
            quantity = 0;
        }

        calculateSubtotal();
    }

    @Override
    public String toString() {

        return productName
                + " x"
                + quantity
                + " = "
                + String.format("%.2f", subtotal);

    }

}