package com.adam.warehouse.util;

import com.adam.warehouse.enums.UserRole;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.session.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PermissionManagerTest {

    @AfterEach
    void clearSession() {
        SessionManager.logout();
    }

    @Test
    void rolePermissionsMatchDocumentedAccess() {
        loginAs(UserRole.ADMIN);
        assertTrue(PermissionManager.canManageEmployees());
        assertTrue(PermissionManager.canViewReports());
        assertTrue(PermissionManager.canManageProducts());
        assertTrue(PermissionManager.canMakeSales());

        loginAs(UserRole.MANAGER);
        assertFalse(PermissionManager.canManageEmployees());
        assertTrue(PermissionManager.canViewReports());
        assertTrue(PermissionManager.canManagePurchases());
        assertTrue(PermissionManager.canMakeSales());

        loginAs(UserRole.STOREKEEPER);
        assertFalse(PermissionManager.canViewReports());
        assertTrue(PermissionManager.canManagePurchases());
        assertFalse(PermissionManager.canMakeSales());

        loginAs(UserRole.CASHIER);
        assertFalse(PermissionManager.canViewReports());
        assertFalse(PermissionManager.canManagePurchases());
        assertTrue(PermissionManager.canMakeSales());
        assertTrue(PermissionManager.canManageCustomers());
    }

    @Test
    void unauthenticatedAndInactiveSessionsAreRejected() {
        assertThrows(SecurityException.class,
                () -> PermissionManager.requireAuthenticated("view inventory"));

        Employee inactive = new Employee();
        inactive.setActive(false);
        inactive.setRole(UserRole.ADMIN);
        assertThrows(IllegalArgumentException.class, () -> SessionManager.login(inactive));
    }

    @Test
    void newlyChosenAdministratorPasswordsUseTheApplicationHashFormat() {
        String selectedPassword = "a user chosen unique password";
        String hashedPassword = PasswordHasher.hash(selectedPassword);
        assertTrue(PasswordHasher.isHashed(hashedPassword));
        assertTrue(PasswordHasher.matches(selectedPassword, hashedPassword));
        assertFalse(PasswordHasher.matches("wrong-password", hashedPassword));
        assertDoesNotThrow(() -> PasswordHasher.hash("another unique password"));
    }

    private void loginAs(UserRole role) {
        SessionManager.logout();
        Employee employee = new Employee();
        employee.setActive(true);
        employee.setRole(role);
        SessionManager.login(employee);
    }
}
