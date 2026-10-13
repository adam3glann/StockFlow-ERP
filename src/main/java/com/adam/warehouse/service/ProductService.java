package com.adam.warehouse.service;

import com.adam.warehouse.dao.ProductDAO;
import com.adam.warehouse.model.Product;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.PermissionManager;
import com.adam.warehouse.util.MoneyValidation;

import java.util.List;
import java.util.Objects;

public class ProductService {

    private final ProductDAO productDAO;

    public ProductService() {
        productDAO = new ProductDAO();
    }

    public void addProduct(Product product) {
        PermissionManager.require(PermissionManager.canManageProducts(), "manage products");

        validateProduct(product);

        if (productDAO.barcodeExists(product.getBarcode())) {

            throw new IllegalArgumentException(
                    "Barcode already exists."
            );

        }

        productDAO.addProduct(product, SessionManager.requireCurrentEmployee().getEmployeeId());

    }

    public void updateProduct(Product product) {
        PermissionManager.require(PermissionManager.canManageProducts(), "manage products");

        validateProduct(product);

        if (product.getProductId() <= 0) {

            throw new IllegalArgumentException(
                    "Invalid product."
            );

        }

        Product current = productDAO.getProductById(product.getProductId());
        if (current == null) {
            throw new IllegalArgumentException("Product not found.");
        }
        product.setQuantity(current.getQuantity());

        if (productDAO.barcodeExists(
                product.getBarcode(),
                product.getProductId())) {

            throw new IllegalArgumentException(
                    "Barcode already belongs to another product."
            );

        }

        productDAO.updateProduct(product);

    }

    public void deactivateProduct(int productId) {
        PermissionManager.require(PermissionManager.canManageProducts(), "manage products");

        if (productId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid product ID."
            );

        }

        productDAO.deactivateProduct(productId);

    }

    public void adjustStock(int productId, int quantityChange, String reason) {
        PermissionManager.require(PermissionManager.canManageProducts(), "adjust stock");
        if (productId <= 0 || quantityChange == 0) {
            throw new IllegalArgumentException("Select a product and enter a non-zero quantity change.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required for a stock adjustment.");
        }
        productDAO.adjustStock(productId, quantityChange,
                SessionManager.requireCurrentEmployee().getEmployeeId(), reason.trim());
    }

    public Product getProductById(int productId) {
        PermissionManager.requireAuthenticated("view products");

        if (productId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid product ID."
            );

        }

        return productDAO.getProductById(productId);

    }

    public Product getProductByBarcode(String barcode) {
        PermissionManager.requireAuthenticated("view products");

        if (barcode == null || barcode.isBlank()) {

            throw new IllegalArgumentException(
                    "Barcode cannot be empty."
            );

        }

        return productDAO.getProductByBarcode(
                barcode.trim()
        );

    }

    public List<Product> getAllProducts() {
        PermissionManager.requireAuthenticated("view products");

        return productDAO.getAllProducts();

    }

    public List<Product> searchProducts(String keyword) {
        PermissionManager.requireAuthenticated("view products");

        if (keyword == null) {

            keyword = "";

        }

        return productDAO.searchProducts(
                keyword.trim()
        );

    }

    public boolean isBarcodeAvailable(String barcode) {
        PermissionManager.require(PermissionManager.canManageProducts(), "manage products");

        if (barcode == null || barcode.isBlank()) {

            return false;

        }

        return !productDAO.barcodeExists(
                barcode.trim()
        );

    }

    private void validateProduct(Product product) {

        Objects.requireNonNull(
                product,
                "Product cannot be null."
        );

        if (product.getName() != null) {

            product.setName(
                    product.getName().trim()
            );

        }

        if (product.getBarcode() != null) {

            product.setBarcode(
                    product.getBarcode().trim()
            );

        }

        if (product.getDescription() != null) {

            product.setDescription(
                    product.getDescription().trim()
            );

        }

        if (product.getName() == null ||
                product.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Product name cannot be empty."
            );

        }

        if (product.getBarcode() == null ||
                product.getBarcode().isBlank()) {

            throw new IllegalArgumentException(
                    "Barcode cannot be empty."
            );

        }

        if (product.getCategoryId() <= 0) {

            throw new IllegalArgumentException(
                    "Select a category."
            );

        }

        if (!MoneyValidation.isValidPositive(product.getPurchasePrice())) {

            throw new IllegalArgumentException(
                    "Purchase price must be greater than zero."
            );

        }

        if (!MoneyValidation.isValidPositive(product.getSellingPrice())) {

            throw new IllegalArgumentException(
                    "Selling price must be greater than zero."
            );

        }

        if (product.getSellingPrice() < product.getPurchasePrice()) {

            throw new IllegalArgumentException(
                    "Selling price must be greater than or equal to purchase price."
            );

        }

        if (product.getQuantity() < 0) {

            throw new IllegalArgumentException(
                    "Quantity cannot be negative."
            );

        }

        if (product.getMinimumStock() < 0) {

            throw new IllegalArgumentException(
                    "Minimum stock cannot be negative."
            );

        }

    }

}
