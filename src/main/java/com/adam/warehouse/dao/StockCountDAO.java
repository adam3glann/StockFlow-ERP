package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.StockCountItem;
import com.adam.warehouse.model.StockCountSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Persists physical counts and applies confirmed variances atomically with their movement. */
public class StockCountDAO {

    private final InventoryMovementDAO movementDAO = new InventoryMovementDAO();

    public StockCountSession startCount(int employeeId) {
        String insertSession = "INSERT INTO inventory_count_sessions(started_by) VALUES (?) RETURNING count_id, started_at";
        String snapshot = """
                INSERT INTO inventory_count_items(count_id, product_id, recorded_quantity, last_movement_id)
                SELECT ?, p.product_id, p.quantity,
                       (SELECT MAX(m.movement_id) FROM inventory_movements m WHERE m.product_id = p.product_id)
                FROM products p WHERE p.active = TRUE
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StockCountSession session = new StockCountSession();
                try (PreparedStatement statement = connection.prepareStatement(insertSession)) {
                    statement.setInt(1, employeeId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Unable to start physical count.");
                        session.setCountId(rs.getInt("count_id"));
                        session.setStartedAt(rs.getTimestamp("started_at").toLocalDateTime());
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(snapshot)) {
                    statement.setInt(1, session.getCountId());
                    statement.executeUpdate();
                }
                connection.commit();
                session.setStartedBy(employeeId);
                session.setStatus("IN_PROGRESS");
                return session;
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to start physical count.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to start physical count.", exception);
        }
    }

    public List<StockCountSession> getOpenSessions() {
        String sql = """
                SELECT s.*, e.first_name || ' ' || e.last_name AS employee_name,
                       '' AS completed_by_name,
                       COUNT(i.count_item_id) AS item_count,
                       0 AS open_variance_count
                FROM inventory_count_sessions s
                JOIN employees e ON e.employee_id = s.started_by
                LEFT JOIN inventory_count_items i ON i.count_id = s.count_id
                WHERE s.status = 'IN_PROGRESS'
                GROUP BY s.count_id, e.first_name, e.last_name
                ORDER BY s.started_at DESC, s.count_id DESC
                """;
        return getSessions(sql);
    }

    public List<StockCountSession> getClosedSessions() {
        String sql = """
                SELECT s.*, e.first_name || ' ' || e.last_name AS employee_name,
                       COALESCE(ce.first_name || ' ' || ce.last_name, '') AS completed_by_name,
                       COUNT(i.count_item_id) AS item_count,
                       COUNT(i.count_item_id) FILTER (WHERE i.resolution_status = 'OPEN') AS open_variance_count
                FROM inventory_count_sessions s
                JOIN employees e ON e.employee_id = s.started_by
                LEFT JOIN employees ce ON ce.employee_id = s.completed_by
                LEFT JOIN inventory_count_items i ON i.count_id = s.count_id
                WHERE s.status <> 'IN_PROGRESS'
                GROUP BY s.count_id, e.first_name, e.last_name, ce.first_name, ce.last_name
                ORDER BY s.started_at DESC, s.count_id DESC
                """;
        return getSessions(sql);
    }

    private List<StockCountSession> getSessions(String sql) {
        List<StockCountSession> sessions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                StockCountSession session = new StockCountSession();
                session.setCountId(rs.getInt("count_id"));
                session.setStartedBy(rs.getInt("started_by"));
                session.setStartedByName(rs.getString("employee_name"));
                session.setCompletedBy(rs.getInt("completed_by"));
                session.setCompletedByName(rs.getString("completed_by_name"));
                session.setStartedAt(rs.getTimestamp("started_at").toLocalDateTime());
                Timestamp completed = rs.getTimestamp("completed_at");
                session.setCompletedAt(completed == null ? null : completed.toLocalDateTime());
                session.setStatus(rs.getString("status"));
                session.setItemCount(rs.getInt("item_count"));
                try {
                    session.setOpenVarianceCount(rs.getInt("open_variance_count"));
                } catch (SQLException ignored) {
                    session.setOpenVarianceCount(0);
                }
                sessions.add(session);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to load physical count sessions.", exception);
        }
        return sessions;
    }

    public List<StockCountItem> getCountItems(int countId) {
        String sql = """
                SELECT i.*, p.name AS product_name, p.barcode,
                       COALESCE(e.first_name || ' ' || e.last_name, '') AS resolved_by_name
                FROM inventory_count_items i
                JOIN products p ON p.product_id = i.product_id
                LEFT JOIN employees e ON e.employee_id = i.resolved_by
                WHERE i.count_id = ?
                ORDER BY p.name, i.count_item_id
                """;
        return readItems(sql, countId);
    }

    public List<StockCountItem> getOpenVariances() {
        String sql = """
                SELECT i.*, p.name AS product_name, p.barcode,
                       COALESCE(e.first_name || ' ' || e.last_name, '') AS resolved_by_name
                FROM inventory_count_items i
                JOIN inventory_count_sessions s ON s.count_id = i.count_id
                JOIN products p ON p.product_id = i.product_id
                LEFT JOIN employees e ON e.employee_id = i.resolved_by
                WHERE s.status = 'COMPLETED' AND i.resolution_status = 'OPEN'
                ORDER BY s.completed_at, p.name
                """;
        return readItems(sql, null);
    }

    private List<StockCountItem> readItems(String sql, Integer countId) {
        List<StockCountItem> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (countId != null) statement.setInt(1, countId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    StockCountItem item = new StockCountItem();
                    item.setCountItemId(rs.getInt("count_item_id"));
                    item.setCountId(rs.getInt("count_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setBarcode(rs.getString("barcode"));
                    item.setRecordedQuantity(rs.getInt("recorded_quantity"));
                    long movementId = rs.getLong("last_movement_id");
                    item.setLastMovementId(rs.wasNull() ? null : movementId);
                    int counted = rs.getInt("counted_quantity");
                    item.setCountedQuantity(rs.wasNull() ? null : counted);
                    int variance = rs.getInt("variance");
                    item.setVariance(rs.wasNull() ? null : variance);
                    item.setReason(rs.getString("count_reason"));
                    item.setResolutionStatus(rs.getString("resolution_status"));
                    item.setResolutionNotes(rs.getString("resolution_notes"));
                    item.setResolvedByName(rs.getString("resolved_by_name"));
                    Timestamp resolvedAt = rs.getTimestamp("resolved_at");
                    item.setResolvedAt(resolvedAt == null ? null : resolvedAt.toLocalDateTime());
                    items.add(item);
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to load physical count items.", exception);
        }
        return items;
    }

    public void completeCount(int countId, int employeeId, List<StockCountItem> items) {
        String lockSession = "SELECT status FROM inventory_count_sessions WHERE count_id = ? FOR UPDATE";
        String updateItem = """
                UPDATE inventory_count_items SET counted_quantity = ?, variance = ?, count_reason = ?,
                    resolution_status = ?
                WHERE count_item_id = ? AND count_id = ? AND counted_quantity IS NULL
                """;
        String finishSession = "UPDATE inventory_count_sessions SET status = 'COMPLETED', completed_by = ?, completed_at = CURRENT_TIMESTAMP WHERE count_id = ? AND status = 'IN_PROGRESS'";
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement(lockSession)) {
                    lock.setInt(1, countId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next() || !"IN_PROGRESS".equals(rs.getString("status"))) {
                            throw new IllegalArgumentException("This physical count is no longer open.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM inventory_count_items WHERE count_id = ?")) {
                    statement.setInt(1, countId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != items.size()) {
                            throw new IllegalArgumentException("Enter a counted quantity for every product before completing the count.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT EXISTS (
                            SELECT 1 FROM products p
                            WHERE p.active = TRUE
                              AND NOT EXISTS (
                                  SELECT 1 FROM inventory_count_items i
                                  WHERE i.count_id = ? AND i.product_id = p.product_id
                              )
                        )
                        """)) {
                    statement.setInt(1, countId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next() && rs.getBoolean(1)) {
                            throw new IllegalArgumentException("A product was added after this count started. Discard this count and start again.");
                        }
                    }
                }
                for (StockCountItem item : items) {
                    int variance = item.getCountedQuantity() - item.getRecordedQuantity();
                    try (PreparedStatement statement = connection.prepareStatement(updateItem)) {
                        statement.setInt(1, item.getCountedQuantity());
                        statement.setInt(2, variance);
                        statement.setString(3, item.getReason());
                        statement.setString(4, variance == 0 ? "MATCHED" : "OPEN");
                        statement.setInt(5, item.getCountItemId());
                        statement.setInt(6, countId);
                        if (statement.executeUpdate() != 1) {
                            throw new IllegalArgumentException("A count row was changed or does not belong to this session.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(finishSession)) {
                    statement.setInt(1, employeeId);
                    statement.setInt(2, countId);
                    if (statement.executeUpdate() != 1) throw new IllegalArgumentException("This physical count is no longer open.");
                }
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to complete physical count.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to complete physical count.", exception);
        }
    }

    public void cancelCount(int countId, int employeeId) {
        String sql = "UPDATE inventory_count_sessions SET status = 'CANCELLED', completed_by = ?, completed_at = CURRENT_TIMESTAMP WHERE count_id = ? AND status = 'IN_PROGRESS'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setInt(2, countId);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("This physical count is no longer open.");
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to discard physical count.", exception);
        }
    }

    public void resolveVariance(int countItemId, int employeeId, String resolutionNotes) {
        String lockVariance = """
                SELECT i.*, s.started_at, s.status AS session_status, p.name AS product_name,
                       p.quantity AS current_quantity, p.active
                FROM inventory_count_items i
                JOIN inventory_count_sessions s ON s.count_id = i.count_id
                JOIN products p ON p.product_id = i.product_id
                WHERE i.count_item_id = ?
                FOR UPDATE OF i, s, p
                """;
        String hasLaterMovement = "SELECT EXISTS (SELECT 1 FROM inventory_movements WHERE product_id = ? AND movement_id > COALESCE(?, 0))";
        String applyQuantity = "UPDATE products SET quantity = ? WHERE product_id = ? AND active = TRUE AND quantity = ? RETURNING quantity";
        String finishVariance = """
                UPDATE inventory_count_items
                SET resolution_status = 'RESOLVED', resolution_notes = ?, resolved_by = ?, resolved_at = CURRENT_TIMESTAMP,
                    applied_quantity_before = ?, applied_quantity_after = ?
                WHERE count_item_id = ? AND resolution_status = 'OPEN'
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StockCountItem item = new StockCountItem();
                int currentQuantity;
                boolean active;
                String sessionStatus;
                try (PreparedStatement statement = connection.prepareStatement(lockVariance)) {
                    statement.setInt(1, countItemId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Count variance not found.");
                        item.setCountId(rs.getInt("count_id"));
                        item.setProductId(rs.getInt("product_id"));
                        item.setProductName(rs.getString("product_name"));
                        item.setRecordedQuantity(rs.getInt("recorded_quantity"));
                        item.setCountedQuantity(rs.getObject("counted_quantity") == null ? null : rs.getInt("counted_quantity"));
                        item.setLastMovementId((Long) rs.getObject("last_movement_id"));
                        item.setReason(rs.getString("count_reason"));
                        if (!"OPEN".equals(rs.getString("resolution_status"))) {
                            throw new IllegalArgumentException("This count variance has already been resolved.");
                        }
                        sessionStatus = rs.getString("session_status");
                        currentQuantity = rs.getInt("current_quantity");
                        active = rs.getBoolean("active");
                    }
                }
                if (!"COMPLETED".equals(sessionStatus)) throw new IllegalArgumentException("Complete the physical count before resolving variances.");
                if (!active) throw new IllegalArgumentException("The product is inactive and cannot be adjusted.");
                if (item.getCountedQuantity() == null) throw new IllegalArgumentException("The counted quantity is missing.");
                if (currentQuantity != item.getRecordedQuantity()) {
                    throw new IllegalArgumentException("Stock changed since this count began. Recount this product before resolving the variance.");
                }
                try (PreparedStatement statement = connection.prepareStatement(hasLaterMovement)) {
                    statement.setInt(1, item.getProductId());
                    if (item.getLastMovementId() == null) statement.setNull(2, Types.BIGINT);
                    else statement.setLong(2, item.getLastMovementId());
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next() && rs.getBoolean(1)) {
                            throw new IllegalArgumentException("Stock moved after this count began. Recount this product before resolving the variance.");
                        }
                    }
                }
                int countedQuantity = item.getCountedQuantity();
                int after;
                try (PreparedStatement statement = connection.prepareStatement(applyQuantity)) {
                    statement.setInt(1, countedQuantity);
                    statement.setInt(2, item.getProductId());
                    statement.setInt(3, currentQuantity);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Stock changed while resolving the count. Please retry.");
                        after = rs.getInt(1);
                    }
                }
                InventoryMovement movement = new InventoryMovement(item.getProductId(), item.getProductName(), "ADJUSTMENT",
                        after - currentQuantity, currentQuantity, after, employeeId, null,
                        "Physical count #" + item.getCountId());
                movement.setNotes("Count reason: " + item.getReason() + " | Resolution: " + resolutionNotes.trim());
                movementDAO.record(connection, movement);
                try (PreparedStatement statement = connection.prepareStatement(finishVariance)) {
                    statement.setString(1, resolutionNotes.trim());
                    statement.setInt(2, employeeId);
                    statement.setInt(3, currentQuantity);
                    statement.setInt(4, after);
                    statement.setInt(5, countItemId);
                    if (statement.executeUpdate() != 1) throw new IllegalArgumentException("This count variance was already resolved.");
                }
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to resolve physical count variance.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to resolve physical count variance.", exception);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
