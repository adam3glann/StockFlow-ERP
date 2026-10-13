package com.adam.warehouse.service;

import com.adam.warehouse.dao.EmployeeDAO;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.session.SessionManager;

import java.util.List;
import java.util.Objects;

public class EmployeeService {

    private final EmployeeDAO employeeDAO;

    public EmployeeService() {
        employeeDAO = new EmployeeDAO();
    }

    public void addEmployee(Employee employee) {
        PermissionManager.require(PermissionManager.canManageEmployees(), "manage employees");

        validateEmployee(employee);

        employeeDAO.addEmployee(employee);

    }

    public void updateEmployee(Employee employee) {
        PermissionManager.require(PermissionManager.canManageEmployees(), "manage employees");

        validateEmployee(employee);

        if (employee.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Invalid employee ID.");
        }

        Employee signedIn = SessionManager.requireCurrentEmployee();
        Employee existing = employeeDAO.getEmployeeById(employee.getEmployeeId());
        if (existing == null) throw new IllegalArgumentException("Employee not found.");
        if (employee.getEmployeeId() == signedIn.getEmployeeId()
                && (!employee.isActive() || employee.getRole() != signedIn.getRole())) {
            throw new IllegalArgumentException("You cannot deactivate or change your own role.");
        }
        employeeDAO.updateEmployee(employee);

    }

    public void deactivateEmployee(int employeeId) {
        PermissionManager.require(PermissionManager.canManageEmployees(), "manage employees");

        if (employeeId <= 0) {
            throw new IllegalArgumentException("Invalid employee ID.");
        }

        Employee current = SessionManager.requireCurrentEmployee();
        if (employeeId == current.getEmployeeId()) {
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        }
        Employee target = employeeDAO.getEmployeeById(employeeId);
        if (target == null) {
            throw new IllegalArgumentException("Employee not found.");
        }
        employeeDAO.deactivateEmployee(employeeId);

    }

    public List<Employee> getAllEmployees() {
        PermissionManager.require(PermissionManager.canManageEmployees(), "view employees");

        return employeeDAO.getAllEmployees();

    }

    public List<Employee> searchEmployees(String keyword) {
        PermissionManager.require(PermissionManager.canManageEmployees(), "view employees");

        if (keyword == null) {
            keyword = "";
        }

        return employeeDAO.searchEmployees(keyword.trim());

    }

    public Employee login(String username, String password) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        Employee employee =
                employeeDAO.login(
                        username.trim(),
                        password
                );

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Invalid username or password."
            );
        }

        return employee;

    }

    public void updateOwnProfile(Employee employee) {
        Employee current = SessionManager.requireCurrentEmployee();
        if (employee == null || employee.getEmployeeId() != current.getEmployeeId()) {
            throw new SecurityException("You can update only your own profile.");
        }
        validateEmployee(employee);
        employeeDAO.updateOwnProfile(employee);
    }

    private void validateEmployee(Employee employee) {

        Objects.requireNonNull(
                employee,
                "Employee cannot be null."
        );

        if (employee.getFirstName() != null) {
            employee.setFirstName(employee.getFirstName().trim());
        }

        if (employee.getLastName() != null) {
            employee.setLastName(employee.getLastName().trim());
        }

        if (employee.getUsername() != null) {
            employee.setUsername(employee.getUsername().trim());
        }

        if (employee.getEmail() != null) {
            employee.setEmail(employee.getEmail().trim());
        }

        if (employee.getPhone() != null) {
            employee.setPhone(employee.getPhone().trim());
        }

        // Passwords are secrets, not display text: preserve every character
        // rather than silently changing the credential during validation.

        if (employee.getNotes() != null) {
            employee.setNotes(employee.getNotes().trim());
        }

        if (employee.getEmail() != null &&
                employee.getEmail().isBlank()) {
            employee.setEmail(null);
        }

        if (employee.getNotes() != null &&
                employee.getNotes().isBlank()) {
            employee.setNotes(null);
        }

        if (employee.getFirstName() == null ||
                employee.getFirstName().isBlank()) {

            throw new IllegalArgumentException(
                    "First name cannot be empty."
            );

        }

        if (employee.getLastName() == null ||
                employee.getLastName().isBlank()) {

            throw new IllegalArgumentException(
                    "Last name cannot be empty."
            );

        }

        if (employee.getUsername() == null ||
                employee.getUsername().isBlank()) {

            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );

        }

        if (employee.getRole() == null) {

            throw new IllegalArgumentException(
                    "Employee role is required."
            );

        }

        if (employee.getPhone() == null ||
                employee.getPhone().isBlank()) {

            throw new IllegalArgumentException(
                    "Phone number cannot be empty."
            );

        }

        if (employee.getPassword() != null &&
                !employee.getPassword().isBlank() &&
                employee.getPassword().length() < 6) {

            throw new IllegalArgumentException(
                    "Password must contain at least 6 characters."
            );

        }

    }

}
