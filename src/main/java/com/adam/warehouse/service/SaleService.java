package com.adam.warehouse.service;

import com.adam.warehouse.dao.SaleDAO;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.util.MoneyValidation;

import java.time.LocalDateTime;
import java.util.List;

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
        validateSale(sale);

        if (sale.getSaleDate() == null) {
            sale.setSaleDate(LocalDateTime.now());
        }

        sale.calculateTotalAmount();
        if (!MoneyValidation.isValid(sale.getTotalAmount())) {
            throw new IllegalArgumentException("Sale total is outside the supported amount range.");
        }

        saleDAO.saveSale(sale);
        activityLogService.logCurrentUser("SALE", "CREATE",
                "Recorded sale with " + sale.getTotalItems() + " item(s), total " + sale.getTotalAmount() + ".");

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

    }

}
