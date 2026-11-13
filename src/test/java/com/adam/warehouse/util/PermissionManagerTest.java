package com.adam.warehouse.util;

import com.adam.warehouse.enums.UserRole;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.service.SaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionManagerTest {

    @AfterEach
    void clearSession() {
        SessionManager.logout();
    }

    @Test
    void rolePermissionsMatchDocumentedAccess() {
        loginAs(UserRole.ADMIN);
        assertTrue(PermissionManager.canManageEmployees());
        assertTrue(PermissionManager.canManageCustomers());
        assertTrue(PermissionManager.canViewCustomers());
        assertTrue(PermissionManager.canViewReports());
        assertTrue(PermissionManager.canManageProducts());
        assertTrue(PermissionManager.canMakeSales());

        loginAs(UserRole.MANAGER);
        assertFalse(PermissionManager.canManageEmployees());
        assertTrue(PermissionManager.canViewReports());
        assertTrue(PermissionManager.canManagePurchases());
        assertTrue(PermissionManager.canMakeSales());
        assertTrue(PermissionManager.canManageCustomers());

        loginAs(UserRole.STOREKEEPER);
        assertFalse(PermissionManager.canViewReports());
        assertTrue(PermissionManager.canManagePurchases());
        assertFalse(PermissionManager.canMakeSales());

        loginAs(UserRole.CASHIER);
        assertFalse(PermissionManager.canViewReports());
        assertFalse(PermissionManager.canManagePurchases());
        assertTrue(PermissionManager.canMakeSales());
        assertFalse(PermissionManager.canManageCustomers());
        assertTrue(PermissionManager.canViewCustomers());
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
    void partialSalesRequireARegisteredCustomerAtTheServiceBoundary() {
        Employee cashier = new Employee();
        cashier.setEmployeeId(17);
        cashier.setActive(true);
        cashier.setRole(UserRole.CASHIER);
        SessionManager.login(cashier);

        Sale sale = new Sale();
        sale.addItem(new SaleItem(5, "Item", 1, 10.00, 5.00));
        sale.setAmountPaid(2.00);
        sale.setAmountTendered(2.00);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new SaleService().saveSale(sale));
        assertEquals("sale.partial.payment.customer.required", error.getMessage());
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
