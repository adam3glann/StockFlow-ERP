package com.adam.warehouse.model;

public class SaleItem {

    private int saleItemId;

    private int saleId;

    private int productId;

    private String productName;

    private int quantity;

    private double sellingPrice;

    /** Product's cost price at the moment of sale. Never recompute historical
     *  profit from the product's current cost — always use this stored value. */
    private double costPrice;

    private double subtotal;
    private double refundLimit;

    public SaleItem() {
    }

    public SaleItem(int productId,
                    String productName,
                    int quantity,
                    double sellingPrice) {

        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;

        calculateSubtotal();
    }

    public SaleItem(int productId,
                    String productName,
                    int quantity,
                    double sellingPrice,
                    double costPrice) {

        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;

        calculateSubtotal();
    }

    public int getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(int saleItemId) {
        this.saleItemId = saleItemId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
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

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {

        this.sellingPrice = sellingPrice;

        calculateSubtotal();
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    /** Profit for this line item, using the cost price stored at sale time. */
    public double getProfit() {
        return (sellingPrice - costPrice) * quantity;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getRefundLimit() { return refundLimit; }
    public void setRefundLimit(double refundLimit) { this.refundLimit = refundLimit; }

    public void calculateSubtotal() {

        subtotal = quantity * sellingPrice;

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

        return productName +
                " x" + quantity +
                " = " +
                String.format("%.2f", subtotal);

    }

}
