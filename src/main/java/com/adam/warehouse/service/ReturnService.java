package com.adam.warehouse.service;

import com.adam.warehouse.dao.ReturnDAO;
import com.adam.warehouse.model.Return;
import com.adam.warehouse.model.ReturnItem;
import com.adam.warehouse.model.ReturnableItem;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.util.MoneyValidation;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public class ReturnService {

    private final ReturnDAO returnDAO;
    private final ActivityLogService activityLogService;

    public ReturnService() {
        returnDAO = new ReturnDAO();
        activityLogService = new ActivityLogService();
    }

    public void saveReturn(Return returnRecord) {

        if (returnRecord == null) {
            throw new IllegalArgumentException("Return cannot be null.");
        }

        PermissionManager.require(PermissionManager.canManageReturns(), "process returns");
        requireSaleAccess(returnRecord.getSaleId());

        // Never trust an employee ID submitted by a UI or external caller.
        returnRecord.setEmployeeId(SessionManager.requireCurrentEmployee().getEmployeeId());

        validateReturn(returnRecord);

        if (returnRecord.getReturnDate() == null) {
            returnRecord.setReturnDate(LocalDateTime.now());
        }

        returnRecord.calculateTotalRefund();
        if (!MoneyValidation.isValid(returnRecord.getTotalRefund())) {
            throw new IllegalArgumentException("Refund total is outside the supported amount range.");
        }

        returnDAO.saveReturn(returnRecord);

        activityLogService.logCurrentUser("RETURN", "CREATE",
                "Processed return for Sale #" + returnRecord.getSaleId()
                        + " with " + returnRecord.getTotalItems() + " item(s), refund "
                        + String.format("%.2f", returnRecord.getTotalRefund()) + ".");
    }

    public List<Return> getAllReturns() {
        PermissionManager.require(PermissionManager.canManageReturns(), "view returns");
        var employee = SessionManager.requireCurrentEmployee();
        Integer saleOwnerFilter = employee.getRole() == UserRole.CASHIER ? employee.getEmployeeId() : null;
        return returnDAO.getAllReturns(saleOwnerFilter);
    }

    public List<Return> getReturnsForSale(int saleId) {
        PermissionManager.require(PermissionManager.canManageReturns(), "view returns");
        requireSaleAccess(saleId);
        return returnDAO.getReturnsForSale(saleId);
    }

    public Return getReturnById(int returnId) {
        PermissionManager.require(PermissionManager.canManageReturns(), "view returns");
        Return record = returnDAO.getReturnById(returnId);
        if (record != null) requireSaleAccess(record.getSaleId());
        return record;
    }

    public List<ReturnableItem> getReturnableItems(int saleId) {
        PermissionManager.require(PermissionManager.canManageReturns(), "process returns");
        if (saleId <= 0) {
            throw new IllegalArgumentException("Invalid sale.");
        }
        requireSaleAccess(saleId);
        return returnDAO.getReturnableItems(saleId);
    }

    private Sale requireSaleAccess(int saleId) {
        Sale sale = new SaleService().getSaleById(saleId);
        if (sale == null) throw new IllegalArgumentException("Please select a sale to return items from.");
        return sale;
    }

    private void validateReturn(Return returnRecord) {

        if (returnRecord.getSaleId() <= 0) {
            throw new IllegalArgumentException("Please select a sale to return items from.");
        }

        if (returnRecord.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Invalid employee.");
        }

        if (returnRecord.getItems() == null || returnRecord.getItems().isEmpty()) {
            throw new IllegalArgumentException("Select at least one item to return.");
        }

        for (ReturnItem item : returnRecord.getItems()) {

            if (item == null) {
                throw new IllegalArgumentException("Invalid return item.");
            }

            if (item.getProductId() <= 0) {
                throw new IllegalArgumentException("Invalid product.");
            }

            if (item.getSaleItemId() <= 0) {
                throw new IllegalArgumentException("Invalid sale item.");
            }

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Return quantity must be greater than zero.");
            }

            if (!MoneyValidation.isValid(item.getUnitPrice()) || !MoneyValidation.isValid(item.getRefundAmount())) {
                throw new IllegalArgumentException("Invalid unit price.");
            }
        }
    }
}
