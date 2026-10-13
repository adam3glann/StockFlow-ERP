package com.adam.warehouse.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sale {

    private int saleId;

    private int employeeId;

    private Integer customerId;

    private String customerName;

    private String notes;

    private LocalDateTime saleDate;

    private double totalAmount;

    private final List<SaleItem> items;

    public Sale() {
        this.items = new ArrayList<>();
        this.saleDate = LocalDateTime.now();
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

    public Integer getCustomerId() { return customerId; }

    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }

    public void setCustomerName(String customerName) {
        this.customerName = customerName == null ? "" : customerName.trim();
    }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes == null ? "" : notes.trim(); }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> saleItems) {

        items.clear();

        if (saleItems != null) {
            items.addAll(saleItems);
        }

        calculateTotalAmount();
    }

    public void addItem(SaleItem item) {

        if (item == null) {
            return;
        }

        for (SaleItem existing : items) {

            if (existing.getProductId() == item.getProductId()) {

                existing.setQuantity(
                        existing.getQuantity() + item.getQuantity()
                );

                existing.calculateSubtotal();

                calculateTotalAmount();

                return;
            }

        }

        item.calculateSubtotal();

        items.add(item);

        calculateTotalAmount();
    }

    public void removeItem(int productId) {

        items.removeIf(i -> i.getProductId() == productId);

        calculateTotalAmount();
    }

    public void clearItems() {

        items.clear();

        totalAmount = 0;
    }

    public int getTotalItems() {

        return items.stream()
                .mapToInt(SaleItem::getQuantity)
                .sum();
    }

    public void calculateTotalAmount() {

        totalAmount = items.stream()
                .mapToDouble(SaleItem::getSubtotal)
                .sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public String toString() {

        return "Sale #" + saleId +
                " | Items: " + getTotalItems() +
                " | Total: " + String.format("%.2f", totalAmount);

    }

}
