package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/** Atomically clears operational records while preserving catalog and inventory history. */
public final class ApplicationResetDAO {
    private static final List<String> RESET_STATEMENTS = List.of(
            "DELETE FROM purchase_return_items",
            "DELETE FROM purchase_returns",
            "DELETE FROM purchase_items",
            "DELETE FROM purchases",
            "DELETE FROM return_items",
            "DELETE FROM returns",
            "DELETE FROM sale_items",
            "DELETE FROM sales",
            "DELETE FROM customers",
            "DELETE FROM activity_logs"
    );

    public void resetOperationalData() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                lock.setLong(1, 4815162344L);
                lock.executeQuery().close();
            }
            try (Statement statement = connection.createStatement()) {
                for (String sql : RESET_STATEMENTS) {
                    statement.executeUpdate(sql);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            // Do not expose SQL, connection properties, or driver messages to the UI.
            throw new IllegalStateException("Application data reset failed.");
        }
    }
}
