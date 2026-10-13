package com.adam.warehouse.service;

import com.adam.warehouse.dao.CategoryDAO;
import com.adam.warehouse.model.Category;
import com.adam.warehouse.util.PermissionManager;

import java.util.List;
import java.util.Objects;

public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    // ==========================
    // Add Category
    // ==========================
    public void addCategory(Category category) {
        PermissionManager.require(PermissionManager.canManageCategories(), "manage categories");

        validateCategory(category);

        categoryDAO.addCategory(category);
    }

    // ==========================
    // Get All Categories
    // ==========================
    public List<Category> getAllCategories() {
        PermissionManager.require(PermissionManager.canManageCategories(), "view categories");
        return categoryDAO.getAllCategories();
    }

    // ==========================
    // Get Category By ID
    // ==========================
    public Category getCategoryById(int categoryId) {
        PermissionManager.require(PermissionManager.canManageCategories(), "view categories");

        if (categoryId <= 0) {
            throw new IllegalArgumentException("Invalid category ID.");
        }

        return categoryDAO.getCategoryById(categoryId);
    }

    // ==========================
    // Update Category
    // ==========================
    public void updateCategory(Category category) {
        PermissionManager.require(PermissionManager.canManageCategories(), "manage categories");

        validateCategory(category);

        if (category.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Invalid category ID.");
        }

        categoryDAO.updateCategory(category);
    }

    // ==========================
    // Delete Category
    // ==========================
    public void deleteCategory(int categoryId) {
        PermissionManager.require(PermissionManager.canManageCategories(), "manage categories");

        if (categoryId <= 0) {
            throw new IllegalArgumentException("Invalid category ID.");
        }

        categoryDAO.deactivateCategory(categoryId);
    }

    // ==========================
    // Helper Methods
    // ==========================
    public boolean categoryExists(int categoryId) {
        return getCategoryById(categoryId) != null;
    }

    // ==========================
    // Validation
    // ==========================
    private void validateCategory(Category category) {

        Objects.requireNonNull(category, "Category cannot be null.");

        // Remove leading/trailing spaces
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }

        if (category.getDescription() != null) {
            category.setDescription(category.getDescription().trim());
        }

        // Validate category name
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
    }
}
