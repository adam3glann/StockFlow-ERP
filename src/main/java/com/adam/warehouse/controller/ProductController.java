package com.adam.warehouse.controller;

import com.adam.warehouse.model.Product;
import com.adam.warehouse.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ProductController {

    private final ProductService productService;

    public ProductController() {
        this.productService = new ProductService();
    }

    // ==========================
    // Get All Products
    // ==========================

    public ObservableList<Product> getProducts() {
        return FXCollections.observableArrayList(
                productService.getAllProducts()
        );
    }

    // ==========================
    // Add Product
    // ==========================

    public void addProduct(Product product) {
        productService.addProduct(product);
    }

    // ==========================
    // Update Product
    // ==========================

    public void updateProduct(Product product) {
        productService.updateProduct(product);
    }
// ==========================
// Search Products
// ==========================

    public ObservableList<Product> searchProducts(String keyword) {

        return FXCollections.observableArrayList(
                productService.searchProducts(keyword)
        );

    }
    // ==========================
    // Deactivate Product
    // ==========================

    public void deactivateProduct(int productId) {
        productService.deactivateProduct(productId);
    }

    public void adjustStock(int productId, int quantityChange, String reason) {
        productService.adjustStock(productId, quantityChange, reason);
    }

    // ==========================
    // Search by Barcode
    // ==========================

    public Product getProductByBarcode(String barcode) {
        return productService.getProductByBarcode(barcode);
    }

    // ==========================
    // Barcode Helper
    // ==========================

    public boolean isBarcodeAvailable(String barcode) {
        return productService.isBarcodeAvailable(barcode);
    }

}
