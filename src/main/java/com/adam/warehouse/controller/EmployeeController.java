package com.adam.warehouse.controller;

import com.adam.warehouse.model.Employee;
import com.adam.warehouse.service.EmployeeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class EmployeeController {

    private final EmployeeService employeeService = new EmployeeService();

    public ObservableList<Employee> getEmployees() {

        return FXCollections.observableArrayList(
                employeeService.getAllEmployees()
        );

    }

    public void addEmployee(Employee employee) {

        employeeService.addEmployee(employee);

    }

    public void updateEmployee(Employee employee) {

        employeeService.updateEmployee(employee);

    }

    public void updateOwnProfile(Employee employee) {
        employeeService.updateOwnProfile(employee);
    }

    public void deactivateEmployee(int employeeId) {

        employeeService.deactivateEmployee(employeeId);

    }

    public ObservableList<Employee> searchEmployees(String keyword) {

        return FXCollections.observableArrayList(
                employeeService.searchEmployees(keyword)
        );

    }

    public Employee login(String username,
                          String password) {

        return employeeService.login(
                username,
                password
        );

    }

}
