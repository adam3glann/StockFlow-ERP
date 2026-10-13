package com.adam.warehouse.model;

/**
 * A line item of a purchase annotated with how much of it is still eligible to be
 * sent back to the supplier (purchased quantity minus whatever has already been
 * returned against it).
 */
public record ReturnablePurchaseItem(
        int purchaseItemId,
        int productId,
        String productName,
        int purchasedQuantity,
        int alreadyReturnedQuantity,
        int currentStock,
        double unitPrice
) {
    public int remainingQuantity() {
        return Math.max(0, purchasedQuantity - alreadyReturnedQuantity);
    }

    /** Can't send back to the supplier more than is actually sitting in stock right now. */
    public int maxReturnable() {
        return Math.max(0, Math.min(remainingQuantity(), currentStock));
    }
}
