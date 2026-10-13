package com.adam.warehouse.service;

import com.adam.warehouse.dao.SaleDAO;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.enums.PaymentMethod;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.util.MoneyValidation;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SaleService {

    private final SaleDAO saleDAO;
    private final ActivityLogService activityLogService;

    public SaleService() {
        saleDAO = new SaleDAO();
        activityLogService = new ActivityLogService();
    }

    public void saveSale(Sale sale) {
        if (sale == null) {
            throw new IllegalArgumentException("Sale cannot be null.");
        }
        PermissionManager.require(PermissionManager.canMakeSales(), "record sales");
        // Never trust an employee ID submitted by a UI or external caller.
        sale.setEmployeeId(SessionManager.requireCurrentEmployee().getEmployeeId());
        sale.calculateTotalAmount();
        validateSale(sale);

        if (sale.getSaleDate() == null) {
            sale.setSaleDate(LocalDateTime.now());
        }

        if (!MoneyValidation.isValid(sale.getTotalAmount())) {
            throw new IllegalArgumentException("Sale total is outside the supported amount range.");
        }

        allocateRefundLimits(sale);

        saleDAO.saveSale(sale);
        activityLogService.logCurrentUser("SALE", "CREATE",
                "Recorded sale with " + sale.getTotalItems() + " item(s), total " + sale.getTotalAmount() + ".");

    }

    private void allocateRefundLimits(Sale sale) {
        BigDecimal subtotal = BigDecimal.valueOf(sale.getSubtotalAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountRemaining = BigDecimal.valueOf(sale.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRemaining = BigDecimal.valueOf(sale.getTaxAmount()).setScale(2, RoundingMode.HALF_UP);
        for (int index = 0; index < sale.getItems().size(); index++) {
            SaleItem item = sale.getItems().get(index);
            BigDecimal line = BigDecimal.valueOf(item.getSubtotal()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineDiscount = index == sale.getItems().size() - 1 || subtotal.signum() == 0
                    ? discountRemaining
                    : line.multiply(BigDecimal.valueOf(sale.getDiscountAmount()))
                            .divide(subtotal, 2, RoundingMode.HALF_UP).min(discountRemaining);
            lineDiscount = lineDiscount.min(line);
            discountRemaining = discountRemaining.subtract(lineDiscount);
            BigDecimal taxableLine = line.subtract(lineDiscount).max(BigDecimal.ZERO);
            BigDecimal lineTax = index == sale.getItems().size() - 1
                    ? taxRemaining
                    : taxableLine.multiply(BigDecimal.valueOf(sale.getTaxRate()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).min(taxRemaining);
            taxRemaining = taxRemaining.subtract(lineTax);
            item.setRefundLimit(taxableLine.add(lineTax).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
    }

    public void recordPayment(int saleId, double amount, PaymentMethod method, double tendered, String reference) {
        PermissionManager.require(PermissionManager.canMakeSales(), "record sale payments");
        if (saleId <= 0 || method == null || method == PaymentMethod.LEGACY
                || !MoneyValidation.isValidPositive(amount) || !MoneyValidation.isValid(tendered)
                || !MoneyValidation.hasAtMostTwoDecimalPlaces(amount)
                || !MoneyValidation.hasAtMostTwoDecimalPlaces(tendered)
                || tendered + 0.005 < amount || (method != PaymentMethod.CASH && Math.abs(tendered - amount) > 0.005)
                || (reference != null && reference.trim().length() > 120)) {
            throw new IllegalArgumentException("Payment details are invalid.");
        }
        int employeeId = SessionManager.requireCurrentEmployee().getEmployeeId();
        saleDAO.recordPayment(saleId, employeeId, method, amount, tendered,
                method == PaymentMethod.CASH ? Math.max(0, tendered - amount) : 0,
                reference == null || reference.isBlank() ? null : reference.trim());
        activityLogService.logCurrentUser("SALE", "PAYMENT",
                "Recorded payment " + amount + " for Sale #" + saleId + " by " + method.name() + ".");
    }

    public List<Sale> getAllSales() {
        PermissionManager.require(PermissionManager.canMakeSales(), "view sales");
        return saleDAO.getAllSales();
    }

    public Sale getSaleById(int saleId) {
        PermissionManager.require(PermissionManager.canMakeSales(), "view sales");
        return saleDAO.getSaleById(saleId);
    }

    public List<Sale> searchSales(String keyword) {
        PermissionManager.require(PermissionManager.canMakeSales(), "view sales");

        if (keyword == null) {
            keyword = "";
        }

        return saleDAO.searchSales(keyword.trim());

    }

    public double getTotalRevenue() {

        return getAllSales()

                .stream()

                .mapToDouble(Sale::getTotalAmount)

                .sum();

    }

    public int getTotalSalesCount() {

        return getAllSales().size();

    }

    private void validateSale(Sale sale) {

        if (sale == null) {

            throw new IllegalArgumentException(
                    "Sale cannot be null."
            );

        }

        if (sale.getEmployeeId() <= 0) {

            throw new IllegalArgumentException(
                    "Invalid employee."
            );

        }

        if (sale.getItems() == null ||
                sale.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "Sale must contain at least one item."
            );

        }

        for (SaleItem item : sale.getItems()) {

            if (item == null) {

                throw new IllegalArgumentException(
                        "Invalid sale item."
                );

            }

            if (item.getProductId() <= 0) {

                throw new IllegalArgumentException(
                        "Invalid product."
                );

            }

            if (item.getQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Invalid quantity."
                );

            }

            if (!MoneyValidation.isValidPositive(item.getSellingPrice())
                    || !MoneyValidation.isValid(item.getSubtotal())) {

                throw new IllegalArgumentException(
                        "Invalid selling price."
                );

            }

            if (!MoneyValidation.isValid(item.getCostPrice())) {

                throw new IllegalArgumentException(
                        "Invalid cost price."
                );

            }

        }

        if (!MoneyValidation.isValid(sale.getSubtotalAmount())
                || !MoneyValidation.isValid(sale.getDiscountAmount())
                || !MoneyValidation.hasAtMostTwoDecimalPlaces(sale.getDiscountAmount())
                || sale.getDiscountAmount() > sale.getSubtotalAmount() + 0.005) {
            throw new IllegalArgumentException("Sale discount is invalid.");
        }
        if (!Double.isFinite(sale.getTaxRate()) || sale.getTaxRate() < 0 || sale.getTaxRate() > 100
                || !MoneyValidation.hasAtMostTwoDecimalPlaces(sale.getTaxRate())) {
            throw new IllegalArgumentException("Sale tax rate is invalid.");
        }
        if (sale.getPaymentMethod() == null || sale.getPaymentMethod() == PaymentMethod.LEGACY
                || !MoneyValidation.isValid(sale.getAmountPaid())
                || !MoneyValidation.isValid(sale.getAmountTendered())
                || !MoneyValidation.isValid(sale.getChangeDue())
                || !MoneyValidation.hasAtMostTwoDecimalPlaces(sale.getAmountPaid())
                || !MoneyValidation.hasAtMostTwoDecimalPlaces(sale.getAmountTendered())
                || sale.getAmountPaid() > sale.getTotalAmount() + 0.005
                || sale.getAmountTendered() + 0.005 < sale.getAmountPaid()
                || Math.abs(sale.getAmountTendered() - sale.getAmountPaid() - sale.getChangeDue()) > 0.005
                || (sale.getPaymentMethod() != PaymentMethod.CASH && sale.getChangeDue() > 0.005)
                || (sale.getPaymentReference() != null && sale.getPaymentReference().length() > 120)) {
            throw new IllegalArgumentException("Sale payment details are invalid.");
        }

    }

}
