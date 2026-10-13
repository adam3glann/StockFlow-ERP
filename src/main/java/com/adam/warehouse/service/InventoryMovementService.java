package com.adam.warehouse.service;

import com.adam.warehouse.dao.InventoryMovementDAO;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.StockDiscrepancy;
import com.adam.warehouse.util.PermissionManager;

import java.util.List;

/** Read-only access to inventory movement history. Movements themselves are written
 *  transactionally by PurchaseDAO/SaleDAO as part of their own stock updates. */
public class InventoryMovementService {

    private final InventoryMovementDAO inventoryMovementDAO = new InventoryMovementDAO();

    public List<InventoryMovement> getForProduct(int productId) {
        PermissionManager.require(PermissionManager.canManageProducts(), "view inventory movement history");
        return inventoryMovementDAO.getForProduct(productId);
    }

    public List<InventoryMovement> getAllMovements() {
        PermissionManager.require(PermissionManager.canManageProducts(), "view inventory movement history");
        return inventoryMovementDAO.getAllMovements();
    }

    public List<StockDiscrepancy> getStockAudit() {
        PermissionManager.require(PermissionManager.canManageProducts(), "audit inventory");
        return inventoryMovementDAO.getStockAudit();
    }
}
