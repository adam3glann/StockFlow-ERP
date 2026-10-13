package com.adam.warehouse.service;

import com.adam.warehouse.dao.BranchSettingsDAO;
import com.adam.warehouse.model.BranchIdentity;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.BranchIdentityManager;
import com.adam.warehouse.util.PermissionManager;

public final class BranchSettingsService {
    private final BranchSettingsDAO dao = new BranchSettingsDAO();

    public void save(String code, String name, String location, double taxRate) {
        PermissionManager.require(PermissionManager.canOpenSettings(), "manage branch information");
        String cleanCode = code == null ? "" : code.trim().toUpperCase(java.util.Locale.ROOT);
        String cleanName = name == null ? "" : name.trim();
        String cleanLocation = location == null ? "" : location.trim();
        if (!cleanCode.matches("[A-Z0-9_-]{2,24}")) {
            throw new IllegalArgumentException("branch.code.invalid");
        }
        if (cleanName.isBlank() || cleanName.length() > 180) {
            throw new IllegalArgumentException("branch.name.required");
        }
        if (cleanLocation.length() > 300) {
            throw new IllegalArgumentException("branch.location.too.long");
        }
        if (!Double.isFinite(taxRate) || taxRate < 0 || taxRate > 100
                || java.math.BigDecimal.valueOf(taxRate).stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("branch.tax.rate.invalid");
        }
        BranchIdentity branch = new BranchIdentity(cleanCode, cleanName, cleanLocation, taxRate);
        int employeeId = SessionManager.requireCurrentEmployee().getEmployeeId();
        try {
            dao.save(branch, employeeId);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("branch.save.failed");
        }
        BranchIdentityManager.set(branch);
        new ActivityLogService().logCurrentUser("Settings", "Branch identity updated",
                "Branch " + cleanCode + " information was updated.");
    }
}
