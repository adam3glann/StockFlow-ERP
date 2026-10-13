package com.adam.warehouse.controller;

import com.adam.warehouse.model.Customer;
import com.adam.warehouse.service.CustomerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CustomerController {

    private final CustomerService customerService;

    public CustomerController() {

        customerService = new CustomerService();

    }

    // ==========================
    // Get All Customers
    // ==========================

    public ObservableList<Customer> getCustomers() {

        return FXCollections.observableArrayList(
                customerService.getAllCustomers()
        );

    }

    // ==========================
    // Add Customer
    // ==========================

    public void addCustomer(Customer customer) {

        customerService.addCustomer(customer);

    }

    // ==========================
    // Update Customer
    // ==========================

    public void updateCustomer(Customer customer) {

        customerService.updateCustomer(customer);

    }

    // ==========================
    // Deactivate Customer
    // ==========================

    public void deactivateCustomer(int customerId) {

        customerService.deactivateCustomer(customerId);

    }

    // ==========================
    // Search Customers
    // ==========================

    public ObservableList<Customer> searchCustomers(String keyword) {

        return FXCollections.observableArrayList(
                customerService.searchCustomers(keyword)
        );

    }

}