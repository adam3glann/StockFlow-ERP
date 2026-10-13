package com.adam.warehouse.service;

import com.adam.warehouse.dao.ActivityLogDAO;
import com.adam.warehouse.model.ActivityLog;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;
import java.util.List;

/** Best-effort audit trail. Operational commits are never reported as failed solely because logging fails. */
public class ActivityLogService {

    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public void log(Employee employee, String module, String action, String description) {
        if (employee == null || employee.getEmployeeId() <= 0) {
            return;
        }
        try {
            activityLogDAO.save(new ActivityLog(
                    employee.getEmployeeId(), employee.getFullName(), module, action, description));
        } catch (RuntimeException ignored) {
            // Audit storage is non-blocking; the primary operation already completed safely.
        }
    }

    public void logCurrentUser(String module, String action, String description) {
        log(SessionManager.getCurrentEmployee(), module, action, description);
    }

    public List<ActivityLog> getAllLogs() {
        PermissionManager.require(PermissionManager.canViewReports(), "view activity history");
        return activityLogDAO.getAllLogs();
    }

    public List<ActivityLog> searchLogs(String keyword) {
        PermissionManager.require(PermissionManager.canViewReports(), "view activity history");
        return activityLogDAO.searchLogs(keyword == null ? "" : keyword.trim());
    }
}
