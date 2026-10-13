package com.adam.warehouse.model;

/**
 * A line item of a sale annotated with how much of it is still eligible to be
 * returned (sold quantity minus whatever has already been returned against it).
 * Used to populate the "select items to return" table for a chosen sale.
 */
public record ReturnableItem(
        int saleItemId,
        int productId,
        String productName,
        int soldQuantity,
        int alreadyReturnedQuantity,
        double refundLimit,
        double alreadyRefundedAmount
) {
    public int remainingQuantity() {
        return Math.max(0, soldQuantity - alreadyReturnedQuantity);
    }

    public boolean isFullyReturned() {
        return remainingQuantity() <= 0;
    }

    /** Spreads the exact remaining refundable balance across the remaining quantity. */
    public double unitPrice() {
        int remaining = remainingQuantity();
        return remaining <= 0 ? 0 : Math.max(0, refundLimit - alreadyRefundedAmount) / remaining;
    }
}
