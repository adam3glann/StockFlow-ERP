package com.adam.warehouse.service;

import com.adam.warehouse.dao.SupplierDAO;
import com.adam.warehouse.model.Supplier;
import com.adam.warehouse.util.PermissionManager;

import java.util.List;
import java.util.Objects;

public class SupplierService {

    private final SupplierDAO supplierDAO;

    public SupplierService() {
        supplierDAO = new SupplierDAO();
    }

    public void addSupplier(Supplier supplier) {
        PermissionManager.require(PermissionManager.canManageSuppliers(), "manage suppliers");

        validateSupplier(supplier);

        supplierDAO.addSupplier(supplier);

    }

    public void updateSupplier(Supplier supplier) {
        PermissionManager.require(PermissionManager.canManageSuppliers(), "manage suppliers");

        validateSupplier(supplier);

        if (supplier.getSupplierId() <= 0) {

            throw new IllegalArgumentException(
                    "Invalid supplier ID."
            );

        }

        supplierDAO.updateSupplier(supplier);

    }

    public void deactivateSupplier(int supplierId) {
        PermissionManager.require(PermissionManager.canManageSuppliers(), "manage suppliers");

        if (supplierId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid supplier ID."
            );

        }

        supplierDAO.deactivateSupplier(supplierId);

    }

    public List<Supplier> getAllSuppliers() {
        PermissionManager.require(PermissionManager.canManageSuppliers(), "view suppliers");

        return supplierDAO.getAllSuppliers();

    }

    public List<Supplier> searchSuppliers(String keyword) {
        PermissionManager.require(PermissionManager.canManageSuppliers(), "view suppliers");

        if (keyword == null) {

            keyword = "";

        }

        return supplierDAO.searchSuppliers(
                keyword.trim()
        );

    }

    private void validateSupplier(Supplier supplier) {

        Objects.requireNonNull(
                supplier,
                "Supplier cannot be null."
        );

        if (supplier.getName() != null) {

            supplier.setName(
                    supplier.getName().trim()
            );

        }

        if (supplier.getPhone() != null) {

            supplier.setPhone(
                    supplier.getPhone().trim()
            );

        }

        if (supplier.getEmail() != null) {

            supplier.setEmail(
                    supplier.getEmail().trim()
            );

        }

        if (supplier.getAddress() != null) {

            supplier.setAddress(
                    supplier.getAddress().trim()
            );

        }

        if (supplier.getName() == null ||
                supplier.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Supplier name cannot be empty."
            );

        }

        if (supplier.getPhone() == null ||
                supplier.getPhone().isBlank()) {

            throw new IllegalArgumentException(
                    "Supplier phone cannot be empty."
            );

        }

        if (supplier.getEmail() != null &&
                !supplier.getEmail().isBlank() &&
                !supplier.getEmail().matches(
                        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            throw new IllegalArgumentException(
                    "Invalid email address."
            );

        }



    }

}
