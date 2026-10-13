package com.adam.warehouse.controller;

import com.adam.warehouse.model.Employee;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.service.ActivityLogService;

public class LoginController {

    private final EmployeeController employeeController;
    private final ActivityLogService activityLogService;

    public LoginController() {

        employeeController = new EmployeeController();
        activityLogService = new ActivityLogService();

    }

    public boolean login(String username,
                         String password) {
        Employee employee;
        try {
            employee = employeeController.login(username, password);
        } catch (IllegalArgumentException exception) {
            // Validation and credential failures are deliberately indistinguishable.
            return false;
        }

        if (employee == null) {

            return false;

        }

        SessionManager.login(employee);
        activityLogService.log(employee, "AUTH", "LOGIN", "Signed in to StockFlow ERP.");

        return true;

    }

    public void logout() {
        activityLogService.logCurrentUser("AUTH", "LOGOUT", "Signed out of StockFlow ERP.");
        SessionManager.logout();

    }

}
