package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC persistence for suppliers. Database errors are propagated to the UI layer safely. */
public class SupplierDAO {

    private static final String INSERT = """
            INSERT INTO suppliers (name, phone, email, address, active)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE suppliers SET name = ?, phone = ?, email = ?, address = ?, active = ?
            WHERE supplier_id = ?
            """;
    private static final String DEACTIVATE = "UPDATE suppliers SET active = FALSE WHERE supplier_id = ?";
    private static final String GET_ALL = "SELECT * FROM suppliers WHERE active = TRUE ORDER BY name";
    private static final String SEARCH = """
            SELECT * FROM suppliers WHERE active = TRUE
            AND (LOWER(name) LIKE LOWER(?) OR phone LIKE ? OR LOWER(email) LIKE LOWER(?))
            ORDER BY name
            """;

    public void addSupplier(Supplier supplier) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            fill(statement, supplier);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to add supplier.", exception);
        }
    }

    public List<Supplier> getAllSuppliers() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to load suppliers.", exception);
        }
    }

    public void updateSupplier(Supplier supplier) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            fill(statement, supplier);
            statement.setInt(6, supplier.getSupplierId());
            ensureUpdated(statement.executeUpdate(), "Supplier not found.");
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to update supplier.", exception);
        }
    }

    public void deactivateSupplier(int supplierId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DEACTIVATE)) {
            statement.setInt(1, supplierId);
            ensureUpdated(statement.executeUpdate(), "Supplier not found.");
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to deactivate supplier.", exception);
        }
    }

    public List<Supplier> searchSuppliers(String keyword) {
        String search = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH)) {
            statement.setString(1, search);
            statement.setString(2, search);
            statement.setString(3, search);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to search suppliers.", exception);
        }
    }

    private void fill(PreparedStatement statement, Supplier supplier) throws SQLException {
        statement.setString(1, supplier.getName());
        statement.setString(2, supplier.getPhone());
        statement.setString(3, supplier.getEmail());
        statement.setString(4, supplier.getAddress());
        statement.setBoolean(5, supplier.isActive());
    }

    private List<Supplier> mapAll(ResultSet resultSet) throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        while (resultSet.next()) {
            Supplier supplier = new Supplier();
            supplier.setSupplierId(resultSet.getInt("supplier_id"));
            supplier.setName(resultSet.getString("name"));
            supplier.setPhone(resultSet.getString("phone"));
            supplier.setEmail(resultSet.getString("email"));
            supplier.setAddress(resultSet.getString("address"));
            supplier.setActive(resultSet.getBoolean("active"));
            suppliers.add(supplier);
        }
        return suppliers;
    }

    private void ensureUpdated(int count, String message) {
        if (count != 1) {
            throw new IllegalArgumentException(message);
        }
    }
}
