package com.adam.warehouse.util;

import com.adam.warehouse.enums.UserRole;
import com.adam.warehouse.session.SessionManager;

public final class PermissionManager {

    private PermissionManager() {
    }

    private static UserRole role() {

        if (!SessionManager.isLoggedIn()) {
            return null;
        }

        return SessionManager.getCurrentEmployee().getRole();

    }

    public static boolean canManageProducts() {

        return role() == UserRole.ADMIN
                || role() == UserRole.MANAGER
                || role() == UserRole.STOREKEEPER;

    }

    public static boolean canManageEmployees() {

        return role() == UserRole.ADMIN;

    }

    public static boolean canManagePurchases() {

        return role() == UserRole.ADMIN
                || role() == UserRole.MANAGER
                || role() == UserRole.STOREKEEPER;

    }

    public static boolean canMakeSales() {
        return role() == UserRole.ADMIN
                || role() == UserRole.MANAGER
                || role() == UserRole.CASHIER;

    }

    public static boolean canViewReports() {

        return role() == UserRole.ADMIN
                || role() == UserRole.MANAGER;

    }

    public static boolean canOpenSettings() {

        return role() == UserRole.ADMIN;

    }

    public static boolean canManageCategories() {
        return canManageProducts();
    }

    public static boolean canManageSuppliers() {
        return canManagePurchases();
    }

    public static boolean canManageCustomers() {
        return role() == UserRole.ADMIN
                || role() == UserRole.MANAGER
                || role() == UserRole.CASHIER;
    }

    public static boolean canManageReturns() {
        return canMakeSales();
    }

    public static boolean canManagePurchaseReturns() {
        return canManagePurchases();
    }

    public static void require(boolean allowed, String action) {
        SessionManager.requireCurrentEmployee();
        if (!allowed) {
            throw new SecurityException(String.format(LanguageManager.get("permission.denied"),
                    LanguageManager.code("permission.action", action)));
        }
    }

    /** Guards services that may be used by any authenticated role. */
    public static void requireAuthenticated(String action) {
        SessionManager.requireCurrentEmployee();
    }

}
