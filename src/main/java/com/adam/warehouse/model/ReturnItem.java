package com.adam.warehouse.model;

public class ReturnItem {

    private int returnItemId;

    private int returnId;

    private int saleItemId;

    private int productId;

    private String productName;

    private int quantity;

    /** Unit price refunded per item; normally the original sale's selling price. */
    private double unitPrice;

    private double refundAmount;

    /** Whether this quantity was put back into sellable stock. */
    private boolean restocked = true;

    public ReturnItem() {
    }

    public ReturnItem(int saleItemId, int productId, String productName,
                       int quantity, double unitPrice, boolean restocked) {
        this.saleItemId = saleItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.restocked = restocked;
        calculateRefundAmount();
    }

    public int getReturnItemId() {
        return returnItemId;
    }

    public void setReturnItemId(int returnItemId) {
        this.returnItemId = returnItemId;
    }

    public int getReturnId() {
        return returnId;
    }

    public void setReturnId(int returnId) {
        this.returnId = returnId;
    }

    public int getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(int saleItemId) {
        this.saleItemId = saleItemId;
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
        calculateRefundAmount();
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        calculateRefundAmount();
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public void calculateRefundAmount() {
        refundAmount = quantity * unitPrice;
    }

    public boolean isRestocked() {
        return restocked;
    }

    /** Alias for {@link #isRestocked()} so PropertyValueFactory("restocked") resolves reliably. */
    public boolean getRestocked() {
        return restocked;
    }

    public void setRestocked(boolean restocked) {
        this.restocked = restocked;
    }

    @Override
    public String toString() {
        return productName + " x" + quantity + " = " + String.format("%.2f", refundAmount)
                + (restocked ? " (restocked)" : " (not restocked)");
    }
}
