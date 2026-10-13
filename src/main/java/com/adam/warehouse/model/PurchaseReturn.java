package com.adam.warehouse.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Stock sent back to a supplier (defective/wrong/excess goods received). */
public class PurchaseReturn {

    private int purchaseReturnId;

    private int purchaseId;

    private int employeeId;

    private String employeeName;

    private String supplierName;

    private LocalDateTime returnDate;

    private double totalCredit;

    private String reason;

    private final List<PurchaseReturnItem> items;

    public PurchaseReturn() {
        this.items = new ArrayList<>();
        this.returnDate = LocalDateTime.now();
    }

    public int getPurchaseReturnId() {
        return purchaseReturnId;
    }

    public void setPurchaseReturnId(int purchaseReturnId) {
        this.purchaseReturnId = purchaseReturnId;
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName == null ? "" : supplierName.trim();
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public double getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(double totalCredit) {
        this.totalCredit = totalCredit;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason == null ? "" : reason.trim();
    }

    public List<PurchaseReturnItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseReturnItem> purchaseReturnItems) {
        items.clear();
        if (purchaseReturnItems != null) {
            items.addAll(purchaseReturnItems);
        }
        calculateTotalCredit();
    }

    public void addItem(PurchaseReturnItem item) {
        if (item == null) {
            return;
        }
        items.add(item);
        calculateTotalCredit();
    }

    public int getTotalItems() {
        return items.stream().mapToInt(PurchaseReturnItem::getQuantity).sum();
    }

    public void calculateTotalCredit() {
        totalCredit = items.stream().mapToDouble(PurchaseReturnItem::getCreditAmount).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public String toString() {
        return "Purchase Return #" + purchaseReturnId
                + " | Purchase #" + purchaseId
                + " | Items: " + getTotalItems()
                + " | Credit: " + String.format("%.2f", totalCredit);
    }
}
