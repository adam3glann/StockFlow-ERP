package com.adam.warehouse.service;

import com.adam.warehouse.dao.PurchaseDAO;
import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.model.PurchaseItem;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.util.MoneyValidation;

import java.time.LocalDateTime;
import java.util.List;

public class PurchaseService {

    private final PurchaseDAO purchaseDAO;
    private final ActivityLogService activityLogService;

    public PurchaseService() {
        purchaseDAO = new PurchaseDAO();
        activityLogService = new ActivityLogService();
    }

    public void savePurchase(Purchase purchase) {
        if (purchase == null) {
            throw new IllegalArgumentException("Purchase cannot be null.");
        }
        PermissionManager.require(PermissionManager.canManagePurchases(), "record purchases");
        // The authenticated operator owns the transaction, never a caller-provided ID.
        purchase.setEmployeeId(SessionManager.requireCurrentEmployee().getEmployeeId());
        validatePurchase(purchase);

        if (purchase.getPurchaseDate() == null) {
            purchase.setPurchaseDate(LocalDateTime.now());
        }

        purchase.calculateTotalAmount();
        if (!MoneyValidation.isValid(purchase.getTotalAmount())) {
            throw new IllegalArgumentException("Purchase total is outside the supported amount range.");
        }

        purchaseDAO.savePurchase(purchase);
        activityLogService.logCurrentUser("PURCHASE", "CREATE",
                "Recorded purchase with " + purchase.getItems().size() + " item(s), total " + purchase.getTotalAmount() + ".");

    }

    public List<Purchase> getAllPurchases() {
        PermissionManager.require(PermissionManager.canManagePurchases(), "view purchases");
        return purchaseDAO.getAllPurchases();
    }

    public Purchase getPurchaseById(int purchaseId) {
        PermissionManager.require(PermissionManager.canManagePurchases(), "view purchases");

        if (purchaseId <= 0) {
            throw new IllegalArgumentException("Invalid purchase ID.");
        }

        return purchaseDAO.getPurchaseById(purchaseId);

    }

    public List<Purchase> searchPurchases(String keyword) {
        PermissionManager.require(PermissionManager.canManagePurchases(), "view purchases");

        if (keyword == null) {
            keyword = "";
        }

        return purchaseDAO.searchPurchases(keyword.trim());

    }

    private void validatePurchase(Purchase purchase) {

        if (purchase == null) {
            throw new IllegalArgumentException("Purchase cannot be null.");
        }

        if (purchase.getSupplierId() <= 0) {
            throw new IllegalArgumentException("Invalid supplier.");
        }

        if (purchase.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Invalid employee.");
        }

        if (purchase.getItems() == null || purchase.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Purchase must contain at least one item."
            );
        }

        for (PurchaseItem item : purchase.getItems()) {

            if (item == null) {
                throw new IllegalArgumentException(
                        "Purchase contains an invalid item."
                );
            }

            if (item.getProductId() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid product."
                );
            }

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero."
                );
            }

            if (!MoneyValidation.isValidPositive(item.getPurchasePrice())
                    || !MoneyValidation.isValid(item.getSubtotal())) {
                throw new IllegalArgumentException(
                        "Purchase price must be greater than zero."
                );
            }

        }

    }

}
