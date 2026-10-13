package com.adam.warehouse.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Return {

    private int returnId;

    private int saleId;

    private int employeeId;

    private String employeeName;

    /** Convenience for the UI; not persisted directly (sales.customer_id/name owns it). */
    private String customerName;

    private LocalDateTime returnDate;

    private double totalRefund;

    private String reason;

    private final List<ReturnItem> items;

    public Return() {
        this.items = new ArrayList<>();
        this.returnDate = LocalDateTime.now();
    }

    public int getReturnId() {
        return returnId;
    }

    public void setReturnId(int returnId) {
        this.returnId = returnId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName == null ? "" : customerName.trim();
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public double getTotalRefund() {
        return totalRefund;
    }

    public void setTotalRefund(double totalRefund) {
        this.totalRefund = totalRefund;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason == null ? "" : reason.trim();
    }

    public List<ReturnItem> getItems() {
        return items;
    }

    public void setItems(List<ReturnItem> returnItems) {
        items.clear();
        if (returnItems != null) {
            items.addAll(returnItems);
        }
        calculateTotalRefund();
    }

    public void addItem(ReturnItem item) {
        if (item == null) {
            return;
        }
        items.add(item);
        calculateTotalRefund();
    }

    public int getTotalItems() {
        return items.stream().mapToInt(ReturnItem::getQuantity).sum();
    }

    public void calculateTotalRefund() {
        totalRefund = items.stream().mapToDouble(ReturnItem::getRefundAmount).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public String toString() {
        return "Return #" + returnId
                + " | Sale #" + saleId
                + " | Items: " + getTotalItems()
                + " | Refund: " + String.format("%.2f", totalRefund);
    }
}
