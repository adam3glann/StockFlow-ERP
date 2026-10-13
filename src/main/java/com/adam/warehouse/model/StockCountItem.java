package com.adam.warehouse.model;

import java.time.LocalDateTime;

public class StockCountItem {
    private int countItemId;
    private int countId;
    private int productId;
    private String productName;
    private String barcode;
    private int recordedQuantity;
    private Long lastMovementId;
    private Integer countedQuantity;
    private Integer variance;
    private String reason;
    private String resolutionStatus;
    private String resolutionNotes;
    private String resolvedByName;
    private LocalDateTime resolvedAt;

    public int getCountItemId() { return countItemId; }
    public void setCountItemId(int countItemId) { this.countItemId = countItemId; }
    public int getCountId() { return countId; }
    public void setCountId(int countId) { this.countId = countId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public int getRecordedQuantity() { return recordedQuantity; }
    public void setRecordedQuantity(int recordedQuantity) { this.recordedQuantity = recordedQuantity; }
    public Long getLastMovementId() { return lastMovementId; }
    public void setLastMovementId(Long lastMovementId) { this.lastMovementId = lastMovementId; }
    public Integer getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(Integer countedQuantity) { this.countedQuantity = countedQuantity; }
    public Integer getVariance() { return variance; }
    public void setVariance(Integer variance) { this.variance = variance; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getResolutionStatus() { return resolutionStatus; }
    public void setResolutionStatus(String resolutionStatus) { this.resolutionStatus = resolutionStatus; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public String getResolvedByName() { return resolvedByName; }
    public void setResolvedByName(String resolvedByName) { this.resolvedByName = resolvedByName; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    @Override
    public String toString() {
        return productName + " (" + barcode + "): " + recordedQuantity + " → "
                + (countedQuantity == null ? "?" : countedQuantity)
                + (variance == null ? "" : " [" + (variance > 0 ? "+" : "") + variance + "]");
    }
}
