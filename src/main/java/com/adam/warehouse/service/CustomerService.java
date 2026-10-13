package com.adam.warehouse.service;

import com.adam.warehouse.dao.CustomerDAO;
import com.adam.warehouse.model.Customer;
import com.adam.warehouse.util.PermissionManager;

import java.util.List;
import java.util.Objects;

public class CustomerService {

    private final CustomerDAO customerDAO;

    public CustomerService() {
        customerDAO = new CustomerDAO();
    }

    public void addCustomer(Customer customer) {
        PermissionManager.require(PermissionManager.canManageCustomers(), "manage customers");

        validateCustomer(customer);

        customerDAO.addCustomer(customer);

    }

    public void updateCustomer(Customer customer) {
        PermissionManager.require(PermissionManager.canManageCustomers(), "manage customers");

        validateCustomer(customer);

        if (customer.getCustomerId() <= 0) {
            throw new IllegalArgumentException("Invalid customer ID.");
        }

        customerDAO.updateCustomer(customer);

    }

    public void deactivateCustomer(int customerId) {
        PermissionManager.require(PermissionManager.canManageCustomers(), "manage customers");

        if (customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID.");
        }

        customerDAO.deactivateCustomer(customerId);

    }

    public List<Customer> getAllCustomers() {
        PermissionManager.require(PermissionManager.canManageCustomers(), "view customers");

        return customerDAO.getAllCustomers();

    }

    public Customer getCustomerById(int customerId) {
        PermissionManager.require(PermissionManager.canManageCustomers(), "view customers");

        if (customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID.");
        }

        return customerDAO.getCustomerById(customerId);

    }

    public List<Customer> searchCustomers(String keyword) {
        PermissionManager.require(PermissionManager.canManageCustomers(), "view customers");

        if (keyword == null) {
            keyword = "";
        }

        return customerDAO.searchCustomers(keyword.trim());

    }

    private void validateCustomer(Customer customer) {

        Objects.requireNonNull(customer,
                "Customer cannot be null.");

        if (customer.getFullName() != null) {
            customer.setFullName(customer.getFullName().trim());
        }

        if (customer.getPhone() != null) {
            customer.setPhone(customer.getPhone().trim());
        }

        if (customer.getEmail() != null) {
            customer.setEmail(customer.getEmail().trim());
        }

        if (customer.getAddress() != null) {
            customer.setAddress(customer.getAddress().trim());
        }

        if (customer.getFullName() == null ||
                customer.getFullName().isBlank()) {

            throw new IllegalArgumentException(
                    "Customer name cannot be empty."
            );

        }

        if (customer.getPhone() == null ||
                customer.getPhone().isBlank()) {

            throw new IllegalArgumentException(
                    "Customer phone cannot be empty."
            );

        }

        if (!customer.getEmail().isBlank()) {

            if (!customer.getEmail().matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

                throw new IllegalArgumentException(
                        "Invalid email address."
                );

            }

        }

    }

}
