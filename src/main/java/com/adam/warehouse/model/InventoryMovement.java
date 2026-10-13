package com.adam.warehouse.model;

import java.time.LocalDateTime;

public class InventoryMovement {

    private int movementId;

    private int productId;

    private String productName;

    /**
     * PURCHASE
     * SALE
     * ADJUSTMENT
     * RETURN
     */
    private String movementType;

    /**
     * Positive = Stock Added
     * Negative = Stock Removed
     */
    private int quantityChanged;

    private int quantityBefore;

    private int quantityAfter;

    private int employeeId;

    private String employeeName;

    /**
     * Example:
     * Purchase #15
     * Sale #210
     * Manual Adjustment
     */
    private String reference;

    private String notes;

    private LocalDateTime movementDate;

    public InventoryMovement() {
        movementDate = LocalDateTime.now();
    }

    public InventoryMovement(int productId,
                             String productName,
                             String movementType,
                             int quantityChanged,
                             int quantityBefore,
                             int quantityAfter,
                             int employeeId,
                             String employeeName,
                             String reference) {

        this.productId = productId;
        this.productName = productName;
        this.movementType = movementType;
        this.quantityChanged = quantityChanged;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.reference = reference;
        this.movementDate = LocalDateTime.now();
    }

    public int getMovementId() {
        return movementId;
    }

    public void setMovementId(int movementId) {
        this.movementId = movementId;
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
        this.productName = productName == null ? "" : productName.trim();
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType == null
                ? ""
                : movementType.trim().toUpperCase();
    }

    public int getQuantityChanged() {
        return quantityChanged;
    }

    public void setQuantityChanged(int quantityChanged) {
        this.quantityChanged = quantityChanged;
    }

    public int getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(int quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(int quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName == null ? "" : employeeName.trim();
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference == null ? "" : reference.trim();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    public LocalDateTime getMovementDate() {
        return movementDate;
    }

    public void setMovementDate(LocalDateTime movementDate) {
        this.movementDate = movementDate;
    }

    public boolean isPurchase() {
        return "PURCHASE".equalsIgnoreCase(movementType);
    }

    public boolean isSale() {
        return "SALE".equalsIgnoreCase(movementType);
    }

    public boolean isAdjustment() {
        return "ADJUSTMENT".equalsIgnoreCase(movementType);
    }

    public boolean isReturn() {
        return "RETURN".equalsIgnoreCase(movementType);
    }

    @Override
    public String toString() {

        return productName
                + " | "
                + movementType
                + " | "
                + quantityChanged
                + " | "
                + movementDate;

    }

}