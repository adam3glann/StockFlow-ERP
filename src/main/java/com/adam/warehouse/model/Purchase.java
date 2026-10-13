package com.adam.warehouse.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Purchase {

    private int purchaseId;

    private int supplierId;

    private String supplierName;

    private int employeeId;

    private LocalDateTime purchaseDate;

    private double totalAmount;

    private String notes;

    private final List<PurchaseItem> items;

    public Purchase() {

        items = new ArrayList<>();

        purchaseDate = LocalDateTime.now();

    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {

        this.notes = notes == null ? "" : notes.trim();

    }

    public List<PurchaseItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItem> purchaseItems) {

        items.clear();

        if (purchaseItems != null) {

            items.addAll(purchaseItems);

        }

        calculateTotalAmount();

    }

    public void addItem(PurchaseItem item) {

        if (item == null) {
            return;
        }

        for (PurchaseItem existing : items) {

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

        items.removeIf(item ->
                item.getProductId() == productId);

        calculateTotalAmount();

    }

    public void clearItems() {

        items.clear();

        totalAmount = 0;

    }

    public int getTotalItems() {

        return items.stream()

                .mapToInt(PurchaseItem::getQuantity)

                .sum();

    }

    public void calculateTotalAmount() {

        totalAmount = items.stream()

                .mapToDouble(PurchaseItem::getSubtotal)

                .sum();

    }

    public boolean isEmpty() {

        return items.isEmpty();

    }

    @Override
    public String toString() {

        return "Purchase #" + purchaseId
                + " | Supplier: " + supplierName
                + " | Items: " + getTotalItems()
                + " | Total: "
                + String.format("%.2f", totalAmount);

    }

}
