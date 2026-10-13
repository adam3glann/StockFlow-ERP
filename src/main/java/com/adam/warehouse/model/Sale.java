package com.adam.warehouse.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.adam.warehouse.enums.PaymentMethod;

public class Sale {

    private int saleId;

    private int employeeId;

    private Integer customerId;

    private String customerName;

    private String notes;

    private LocalDateTime saleDate;

    private double totalAmount;
    private double subtotalAmount;
    private double discountAmount;
    private double taxRate;
    private double taxAmount;
    private double amountPaid;
    private double amountTendered;
    private double changeDue;
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private String paymentReference;

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

    public double getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(double value) { subtotalAmount = value; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double value) { discountAmount = value; calculateTotalAmount(); }
    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double value) { taxRate = value; calculateTotalAmount(); }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double value) { taxAmount = value; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double value) { amountPaid = value; }
    public double getAmountTendered() { return amountTendered; }
    public void setAmountTendered(double value) { amountTendered = value; }
    public double getChangeDue() { return changeDue; }
    public void setChangeDue(double value) { changeDue = value; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod value) { paymentMethod = value; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String value) { paymentReference = value == null ? null : value.trim(); }
    public double getBalanceDue() { return Math.max(0, totalAmount - amountPaid); }
    public String getPaymentStatus() {
        if (getBalanceDue() <= 0.005) return "PAID";
        return amountPaid > 0 ? "PARTIAL" : "UNPAID";
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
        BigDecimal subtotal = items.stream()
                .map(item -> BigDecimal.valueOf(item.getSubtotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        subtotalAmount = subtotal.doubleValue();
        BigDecimal discount = BigDecimal.valueOf(Math.max(0, discountAmount)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        taxAmount = taxable.multiply(BigDecimal.valueOf(Math.max(0, taxRate)))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
        totalAmount = taxable.add(BigDecimal.valueOf(taxAmount)).setScale(2, RoundingMode.HALF_UP).doubleValue();
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
