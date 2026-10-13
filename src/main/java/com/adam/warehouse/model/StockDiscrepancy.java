package com.adam.warehouse.model;

import java.time.LocalDateTime;

/**
 * Flags a product whose current on-hand quantity does not match the ending quantity of its
 * most recent recorded {@link InventoryMovement}. Every stock change made through the
 * application (purchases, sales, adjustments, returns) writes a movement in the same
 * transaction, so a mismatch means the stock was changed outside that path -- e.g. a direct
 * database edit, a restored backup, or a bug -- or that the product has never had a movement
 * recorded despite carrying stock.
 */
public class StockDiscrepancy {

    private int productId;
    private String productName;
    private String barcode;
    private String categoryName;
    private int currentQuantity;
    private boolean hasMovementHistory;
    private int lastRecordedQuantity;
    private LocalDateTime lastMovementDate;
    private String lastMovementType;
    private String lastReference;

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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public boolean isHasMovementHistory() {
        return hasMovementHistory;
    }

    public void setHasMovementHistory(boolean hasMovementHistory) {
        this.hasMovementHistory = hasMovementHistory;
    }

    public int getLastRecordedQuantity() {
        return lastRecordedQuantity;
    }

    public void setLastRecordedQuantity(int lastRecordedQuantity) {
        this.lastRecordedQuantity = lastRecordedQuantity;
    }

    public LocalDateTime getLastMovementDate() {
        return lastMovementDate;
    }

    public void setLastMovementDate(LocalDateTime lastMovementDate) {
        this.lastMovementDate = lastMovementDate;
    }

    public String getLastMovementType() { return lastMovementType; }

    public void setLastMovementType(String lastMovementType) { this.lastMovementType = lastMovementType; }

    public String getLastReference() { return lastReference; }

    public void setLastReference(String lastReference) { this.lastReference = lastReference; }

    public String getStatus() {
        if (!hasMovementHistory) return "NO_HISTORY";
        return getDifference() == 0 ? "CONSISTENT" : "MISMATCH";
    }

    /** Positive: more stock on hand than the movement history accounts for. */
    public int getDifference() {
        return currentQuantity - lastRecordedQuantity;
    }
}
