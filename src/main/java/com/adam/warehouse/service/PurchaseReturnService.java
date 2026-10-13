package com.adam.warehouse.service;

import com.adam.warehouse.dao.PurchaseReturnDAO;
import com.adam.warehouse.model.PurchaseReturn;
import com.adam.warehouse.model.PurchaseReturnItem;
import com.adam.warehouse.model.ReturnablePurchaseItem;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.util.MoneyValidation;

import java.time.LocalDateTime;
import java.util.List;

public class PurchaseReturnService {

    private final PurchaseReturnDAO purchaseReturnDAO;
    private final ActivityLogService activityLogService;

    public PurchaseReturnService() {
        purchaseReturnDAO = new PurchaseReturnDAO();
        activityLogService = new ActivityLogService();
    }

    public void saveReturn(PurchaseReturn purchaseReturn) {

        if (purchaseReturn == null) {
            throw new IllegalArgumentException("Purchase return cannot be null.");
        }

        PermissionManager.require(PermissionManager.canManagePurchaseReturns(), "process purchase returns");

        purchaseReturn.setEmployeeId(SessionManager.requireCurrentEmployee().getEmployeeId());

        validateReturn(purchaseReturn);

        if (purchaseReturn.getReturnDate() == null) {
            purchaseReturn.setReturnDate(LocalDateTime.now());
        }

        purchaseReturn.calculateTotalCredit();
        if (!MoneyValidation.isValid(purchaseReturn.getTotalCredit())) {
            throw new IllegalArgumentException("Supplier credit is outside the supported amount range.");
        }

        purchaseReturnDAO.saveReturn(purchaseReturn);

        activityLogService.logCurrentUser("PURCHASE_RETURN", "CREATE",
                "Returned " + purchaseReturn.getTotalItems() + " item(s) to supplier for Purchase #"
                        + purchaseReturn.getPurchaseId() + ", credit "
                        + String.format("%.2f", purchaseReturn.getTotalCredit()) + ".");
    }

    public List<PurchaseReturn> getAllReturns() {
        PermissionManager.require(PermissionManager.canManagePurchaseReturns(), "view purchase returns");
        return purchaseReturnDAO.getAllReturns();
    }

    public List<PurchaseReturn> getReturnsForPurchase(int purchaseId) {
        PermissionManager.require(PermissionManager.canManagePurchaseReturns(), "view purchase returns");
        return purchaseReturnDAO.getReturnsForPurchase(purchaseId);
    }

    public PurchaseReturn getReturnById(int purchaseReturnId) {
        PermissionManager.require(PermissionManager.canManagePurchaseReturns(), "view purchase returns");
        return purchaseReturnDAO.getReturnById(purchaseReturnId);
    }

    public List<ReturnablePurchaseItem> getReturnableItems(int purchaseId) {
        PermissionManager.require(PermissionManager.canManagePurchaseReturns(), "process purchase returns");
        if (purchaseId <= 0) {
            throw new IllegalArgumentException("Invalid purchase.");
        }
        return purchaseReturnDAO.getReturnableItems(purchaseId);
    }

    private void validateReturn(PurchaseReturn purchaseReturn) {

        if (purchaseReturn.getPurchaseId() <= 0) {
            throw new IllegalArgumentException("Please select a purchase to return items from.");
        }

        if (purchaseReturn.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Invalid employee.");
        }

        if (purchaseReturn.getItems() == null || purchaseReturn.getItems().isEmpty()) {
            throw new IllegalArgumentException("Select at least one item to return.");
        }

        for (PurchaseReturnItem item : purchaseReturn.getItems()) {

            if (item == null) {
                throw new IllegalArgumentException("Invalid return item.");
            }

            if (item.getProductId() <= 0) {
                throw new IllegalArgumentException("Invalid product.");
            }

            if (item.getPurchaseItemId() <= 0) {
                throw new IllegalArgumentException("Invalid purchase item.");
            }

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Return quantity must be greater than zero.");
            }

            if (!MoneyValidation.isValid(item.getUnitPrice()) || !MoneyValidation.isValid(item.getCreditAmount())) {
                throw new IllegalArgumentException("Invalid unit price.");
            }
        }
    }
}
