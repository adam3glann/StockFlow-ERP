package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.StockDiscrepancy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists inventory movement history. {@link #record} is designed to run inside an
 * existing transaction (the same connection used by a purchase or sale save), so a
 * failure here rolls back together with the stock change it documents.
 */
public class InventoryMovementDAO {

    private static final String INSERT = """
            INSERT INTO inventory_movements
            (product_id,
             employee_id,
             movement_type,
             quantity_changed,
             quantity_before,
             quantity_after,
             reference,
             notes,
             movement_date)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;

    private static final String SELECT_BASE = """
            SELECT
                m.*,
                p.name product_name,
                e.first_name || ' ' || e.last_name employee_name
            FROM inventory_movements m
            JOIN products p ON p.product_id = m.product_id
            JOIN employees e ON e.employee_id = m.employee_id
            """;

    private static final String SELECT_FOR_PRODUCT =
            SELECT_BASE + " WHERE m.product_id = ? ORDER BY m.movement_date DESC, m.movement_id DESC";

    private static final String SELECT_ALL =
            SELECT_BASE + " ORDER BY m.movement_date DESC, m.movement_id DESC LIMIT 500";

    private static final String SELECT_STOCK_AUDIT = """
            SELECT p.product_id, p.name AS product_name, p.barcode,
                   COALESCE(c.name, '') AS category_name, p.quantity AS current_quantity,
                   lm.movement_id, lm.quantity_after AS last_recorded_quantity,
                   lm.movement_type AS last_movement_type, lm.reference AS last_reference,
                   lm.movement_date AS last_movement_date
            FROM products p
            LEFT JOIN categories c ON c.category_id = p.category_id
            LEFT JOIN LATERAL (
                SELECT movement_id, quantity_after, movement_type, reference, movement_date
                FROM inventory_movements
                WHERE product_id = p.product_id
                ORDER BY movement_date DESC, movement_id DESC
                LIMIT 1
            ) lm ON TRUE
            WHERE p.active = TRUE
            ORDER BY CASE WHEN lm.movement_id IS NULL THEN 0
                          WHEN p.quantity <> lm.quantity_after THEN 1 ELSE 2 END,
                     p.name
            """;

    /**
     * Inserts a movement row using the caller's connection so it commits/rolls back
     * with the surrounding purchase or sale transaction.
     */
    public void record(Connection connection, InventoryMovement movement) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, movement.getProductId());
            statement.setInt(2, movement.getEmployeeId());
            statement.setString(3, movement.getMovementType());
            statement.setInt(4, movement.getQuantityChanged());
            statement.setInt(5, movement.getQuantityBefore());
            statement.setInt(6, movement.getQuantityAfter());
            statement.setString(7, movement.getReference());
            statement.setString(8, movement.getNotes());
            statement.setTimestamp(9, Timestamp.valueOf(movement.getMovementDate()));

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    movement.setMovementId(keys.getInt(1));
                }
            }
        }
    }

    public List<InventoryMovement> getForProduct(int productId) {

        List<InventoryMovement> movements = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_FOR_PRODUCT)) {

            statement.setInt(1, productId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapMovement(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to load inventory movement history.", e);
        }

        return movements;
    }

    public List<InventoryMovement> getAllMovements() {

        List<InventoryMovement> movements = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                movements.add(mapMovement(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to load inventory movement history.", e);
        }

        return movements;
    }

    /** Compares current stock with the latest recorded movement; this never changes stock. */
    public List<StockDiscrepancy> getStockAudit() {
        List<StockDiscrepancy> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_STOCK_AUDIT);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                StockDiscrepancy row = new StockDiscrepancy();
                row.setProductId(rs.getInt("product_id"));
                row.setProductName(rs.getString("product_name"));
                row.setBarcode(rs.getString("barcode"));
                row.setCategoryName(rs.getString("category_name"));
                row.setCurrentQuantity(rs.getInt("current_quantity"));
                row.setHasMovementHistory(rs.getObject("movement_id") != null);
                row.setLastRecordedQuantity(rs.getInt("last_recorded_quantity"));
                row.setLastMovementType(rs.getString("last_movement_type"));
                row.setLastReference(rs.getString("last_reference"));
                Timestamp movementDate = rs.getTimestamp("last_movement_date");
                row.setLastMovementDate(movementDate == null ? null : movementDate.toLocalDateTime());
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to audit stock against movement history.", e);
        }
        return rows;
    }

    private InventoryMovement mapMovement(ResultSet rs) throws SQLException {

        InventoryMovement movement = new InventoryMovement();

        movement.setMovementId(rs.getInt("movement_id"));
        movement.setProductId(rs.getInt("product_id"));
        movement.setProductName(rs.getString("product_name"));
        movement.setMovementType(rs.getString("movement_type"));
        movement.setQuantityChanged(rs.getInt("quantity_changed"));
        movement.setQuantityBefore(rs.getInt("quantity_before"));
        movement.setQuantityAfter(rs.getInt("quantity_after"));
        movement.setEmployeeId(rs.getInt("employee_id"));
        movement.setEmployeeName(rs.getString("employee_name"));
        movement.setReference(rs.getString("reference"));
        movement.setMovementDate(rs.getTimestamp("movement_date").toLocalDateTime());

        return movement;
    }
}
