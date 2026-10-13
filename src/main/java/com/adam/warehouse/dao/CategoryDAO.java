package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    // =====================================================
    // SQL Queries
    // =====================================================

    private static final String INSERT_CATEGORY = """
        INSERT INTO categories(name, description)
        VALUES (?, ?)
        """;

    private static final String UPDATE_CATEGORY = """
            UPDATE categories
            SET name = ?,
                description = ?
            WHERE category_id = ?
            """;

    private static final String DEACTIVATE_CATEGORY = """
            UPDATE categories
            SET active = FALSE
            WHERE category_id = ?
            """;

    private static final String GET_ALL_CATEGORIES = """
            SELECT *
            FROM categories
            WHERE active = TRUE
            ORDER BY name
            """;

    private static final String GET_CATEGORY_BY_ID = """
            SELECT *
            FROM categories
            WHERE category_id = ?
            AND active = TRUE
            """;

    private static final String CATEGORY_EXISTS = """
            SELECT COUNT(*)
            FROM categories
            WHERE LOWER(name) = LOWER(?)
            AND active = TRUE
            """;

    // =====================================================
    // Add Category
    // =====================================================

    public void addCategory(Category category) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_CATEGORY)) {

            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add category.", e);
        }
    }

    // =====================================================
    // Update Category
    // =====================================================

    public void updateCategory(Category category) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_CATEGORY)) {

            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setInt(3, category.getCategoryId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update category.", e);
        }
    }

    // =====================================================
    // Soft Delete Category
    // =====================================================

    public void deactivateCategory(int categoryId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DEACTIVATE_CATEGORY)) {

            statement.setInt(1, categoryId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate category.", e);
        }
    }

    // =====================================================
    // Get All Categories
    // =====================================================

    public List<Category> getAllCategories() {

        List<Category> categories = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_ALL_CATEGORIES);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve categories.", e);
        }

        return categories;
    }

    // =====================================================
    // Get Category By ID
    // =====================================================

    public Category getCategoryById(int categoryId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_CATEGORY_BY_ID)) {

            statement.setInt(1, categoryId);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve category.", e);
        }

        return null;
    }

    // =====================================================
    // Category Exists
    // =====================================================

    public boolean categoryExists(String name) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(CATEGORY_EXISTS)) {

            statement.setString(1, name);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check category.", e);
        }

        return false;
    }

    // =====================================================
    // ResultSet Mapper
    // =====================================================

    private static Category mapResultSetToCategory(ResultSet rs) throws SQLException {

        Category category = new Category();

        category.setCategoryId(rs.getInt("category_id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));

        return category;
    }

}