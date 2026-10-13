package com.adam.warehouse.controller;

import com.adam.warehouse.model.Category;
import com.adam.warehouse.service.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController() {
        this.categoryService = new CategoryService();
    }

    // ==========================
    // Get All Categories
    // ==========================

    public ObservableList<Category> getCategories() {
        return FXCollections.observableArrayList(
                categoryService.getAllCategories()
        );
    }

    // ==========================
    // Add Category
    // ==========================

    public void addCategory(Category category) {
        categoryService.addCategory(category);
    }

    // ==========================
    // Update Category
    // ==========================

    public void updateCategory(Category category) {
        categoryService.updateCategory(category);
    }

    // ==========================
    // Deactivate Category
    // ==========================

    public void deactivateCategory(int categoryId) {
        categoryService.deleteCategory(categoryId);
    }

    // ==========================
    // Get Category By ID
    // ==========================

    public Category getCategoryById(int categoryId) {
        return categoryService.getCategoryById(categoryId);
    }

    // ==========================
    // Helper
    // ==========================

    public boolean categoryExists(int categoryId) {
        return categoryService.categoryExists(categoryId);
    }

}