package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.BranchIdentity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class BranchSettingsDAO {
    public BranchIdentity load() {
        String sql = "SELECT branch_code, branch_name, branch_location, tax_rate FROM branch_settings WHERE singleton_id = 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) throw new SQLException("Branch settings row is missing.");
            return new BranchIdentity(result.getString("branch_code"), result.getString("branch_name"),
                    result.getString("branch_location"), result.getBigDecimal("tax_rate").doubleValue());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load branch identity.");
        }
    }

    public void save(BranchIdentity branch, int employeeId) {
        String sql = """
                UPDATE branch_settings
                SET branch_code = ?, branch_name = ?, branch_location = ?, tax_rate = ?,
                    updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE singleton_id = 1
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, branch.code());
            statement.setString(2, branch.name());
            statement.setString(3, branch.location());
            statement.setDouble(4, branch.taxRate());
            statement.setInt(5, employeeId);
            if (statement.executeUpdate() != 1) throw new SQLException("Branch settings row is missing.");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save branch identity.");
        }
    }
}
