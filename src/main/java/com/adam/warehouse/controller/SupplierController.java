package com.adam.warehouse.controller;

import com.adam.warehouse.model.Supplier;
import com.adam.warehouse.service.SupplierService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController() {

        this.supplierService = new SupplierService();

    }

    // ==========================
    // Get All Suppliers
    // ==========================

    public ObservableList<Supplier> getSuppliers() {

        return FXCollections.observableArrayList(
                supplierService.getAllSuppliers()
        );

    }

    // ==========================
    // Add Supplier
    // ==========================

    public void addSupplier(Supplier supplier) {

        supplierService.addSupplier(supplier);

    }

    // ==========================
    // Update Supplier
    // ==========================

    public void updateSupplier(Supplier supplier) {

        supplierService.updateSupplier(supplier);

    }

    // ==========================
    // Deactivate Supplier
    // ==========================

    public void deactivateSupplier(int supplierId) {

        supplierService.deactivateSupplier(supplierId);

    }

    // ==========================
    // Search Suppliers
    // ==========================

    public ObservableList<Supplier> searchSuppliers(String keyword) {

        return FXCollections.observableArrayList(
                supplierService.searchSuppliers(keyword)
        );

    }

}