package com.adam.warehouse.session;

import com.adam.warehouse.dao.EmployeeDAO;
import com.adam.warehouse.enums.UserRole;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.util.LanguageManager;

public class SessionManager {

    private static Employee currentEmployee;

    private SessionManager() {
    }

    public static void login(Employee employee) {
        if (employee == null || !employee.isActive()) {
            throw new IllegalArgumentException("Only an active employee can start a session.");
        }
        currentEmployee = employee;

    }

    public static void logout() {

        currentEmployee = null;

    }

    public static Employee getCurrentEmployee() {

        return currentEmployee;

    }

    public static boolean isLoggedIn() {

        return currentEmployee != null;

    }

    /** Returns the authenticated employee or fails before protected work begins. */
    public static Employee requireCurrentEmployee() {
        if (!isLoggedIn() || !currentEmployee.isActive()) {
            logout();
            throw new SecurityException(LanguageManager.get("permission.login.required"));
        }
        Employee latest = new EmployeeDAO().getEmployeeById(currentEmployee.getEmployeeId());
        if (latest == null || !latest.isActive()) {
            logout();
            throw new SecurityException(LanguageManager.get("permission.login.required"));
        }
        UserRole previousRole = currentEmployee.getRole();
        if (latest.getRole() != previousRole) {
            logout();
            throw new SecurityException(LanguageManager.get("session.permissions.changed"));
        }
        currentEmployee = latest;
        return currentEmployee;
    }

}
