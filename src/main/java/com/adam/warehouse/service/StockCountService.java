package com.adam.warehouse.service;

import com.adam.warehouse.dao.StockCountDAO;
import com.adam.warehouse.model.StockCountItem;
import com.adam.warehouse.model.StockCountSession;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Business rules for physical stock counts and explicit variance resolution. */
public class StockCountService {
    private final StockCountDAO stockCountDAO = new StockCountDAO();
    private final ActivityLogService activityLogService = new ActivityLogService();

    public StockCountSession startCount() {
        PermissionManager.require(PermissionManager.canManageProducts(), "start a physical stock count");
        StockCountSession session = stockCountDAO.startCount(SessionManager.requireCurrentEmployee().getEmployeeId());
        activityLogService.logCurrentUser("INVENTORY", "COUNT_START", "Started physical stock count #" + session.getCountId() + ".");
        return session;
    }

    public List<StockCountSession> getOpenSessions() {
        PermissionManager.require(PermissionManager.canManageProducts(), "view physical stock counts");
        return stockCountDAO.getOpenSessions();
    }

    public List<StockCountSession> getClosedSessions() {
        PermissionManager.require(PermissionManager.canManageProducts(), "view physical stock counts");
        return stockCountDAO.getClosedSessions();
    }

    public void cancelCount(int countId) {
        PermissionManager.require(PermissionManager.canManageProducts(), "discard a physical stock count");
        if (countId <= 0) throw new IllegalArgumentException("Invalid count session.");
        int employeeId = SessionManager.requireCurrentEmployee().getEmployeeId();
        stockCountDAO.cancelCount(countId, employeeId);
        activityLogService.logCurrentUser("INVENTORY", "COUNT_CANCEL", "Discarded physical stock count #" + countId + ".");
    }

    public List<StockCountItem> getCountItems(int countId) {
        PermissionManager.require(PermissionManager.canManageProducts(), "view physical stock counts");
        if (countId <= 0) throw new IllegalArgumentException("Invalid count session.");
        return stockCountDAO.getCountItems(countId);
    }

    public List<StockCountItem> getOpenVariances() {
        PermissionManager.require(PermissionManager.canManageProducts(), "investigate stock variances");
        return stockCountDAO.getOpenVariances();
    }

    public void completeCount(int countId, List<StockCountItem> items) {
        PermissionManager.require(PermissionManager.canManageProducts(), "complete a physical stock count");
        if (countId <= 0 || items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A count session must contain product counts.");
        }
        Set<Integer> itemIds = new HashSet<>();
        for (StockCountItem item : items) {
            if (item == null || item.getCountItemId() <= 0 || item.getCountId() != countId
                    || item.getCountedQuantity() == null || item.getCountedQuantity() < 0
                    || !itemIds.add(item.getCountItemId())) {
                throw new IllegalArgumentException("Enter a valid non-negative count for every product.");
            }
            int variance = item.getCountedQuantity() - item.getRecordedQuantity();
            if (variance != 0 && (item.getReason() == null || item.getReason().isBlank())) {
                throw new IllegalArgumentException("Enter a reason for every count variance.");
            }
        }
        int employeeId = SessionManager.requireCurrentEmployee().getEmployeeId();
        stockCountDAO.completeCount(countId, employeeId, items);
        long variances = items.stream().filter(item -> item.getCountedQuantity() != item.getRecordedQuantity()).count();
        activityLogService.logCurrentUser("INVENTORY", "COUNT_COMPLETE",
                "Completed physical count #" + countId + " with " + variances + " variance(s).");
    }

    public void resolveVariance(int countItemId, String resolutionNotes) {
        PermissionManager.require(PermissionManager.canManageProducts(), "resolve a stock variance");
        if (countItemId <= 0) throw new IllegalArgumentException("Invalid count variance.");
        if (resolutionNotes == null || resolutionNotes.isBlank()) {
            throw new IllegalArgumentException("Enter an investigation result before applying a stock adjustment.");
        }
        int employeeId = SessionManager.requireCurrentEmployee().getEmployeeId();
        stockCountDAO.resolveVariance(countItemId, employeeId, resolutionNotes.trim());
        activityLogService.logCurrentUser("INVENTORY", "COUNT_RESOLVE",
                "Resolved physical stock count variance item #" + countItemId + ".");
    }
}
