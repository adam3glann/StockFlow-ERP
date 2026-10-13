package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.model.PurchaseItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    private final InventoryMovementDAO inventoryMovementDAO = new InventoryMovementDAO();

    private static final String INSERT_PURCHASE = """
            INSERT INTO purchases
            (supplier_id,
             employee_id,
             purchase_date,
             total_amount,
             notes)
            VALUES (?,?,?,?,?)
            """;

    private static final String INSERT_ITEM = """
            INSERT INTO purchase_items
            (purchase_id,
             product_id,
             quantity,
             purchase_price,
             subtotal)
            VALUES (?,?,?,?,?)
            """;

    private static final String INCREASE_STOCK = """
            UPDATE products
            SET quantity = quantity + ?
            WHERE product_id = ?
            RETURNING quantity
            """;

    private static final String GET_ALL = """
            SELECT
                p.*,
                s.name supplier_name
            FROM purchases p
            LEFT JOIN suppliers s
                ON p.supplier_id = s.supplier_id
            ORDER BY
                p.purchase_date DESC,
                p.purchase_id DESC
            """;

    private static final String GET_BY_ID = """
            SELECT
                p.*,
                s.name supplier_name
            FROM purchases p
            LEFT JOIN suppliers s
                ON p.supplier_id = s.supplier_id
            WHERE p.purchase_id = ?
            """;

    private static final String SEARCH = """
            SELECT
                p.*,
                s.name supplier_name
            FROM purchases p
            JOIN suppliers s
                ON p.supplier_id = s.supplier_id
            WHERE
                LOWER(s.name) LIKE LOWER(?)
                OR CAST(p.purchase_id AS TEXT) LIKE ?
            ORDER BY p.purchase_date DESC
            """;

    public void savePurchase(Purchase purchase) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int purchaseId;
                try (PreparedStatement statement = connection.prepareStatement(
                        INSERT_PURCHASE, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, purchase.getSupplierId());
                    statement.setInt(2, purchase.getEmployeeId());
                    statement.setTimestamp(3, Timestamp.valueOf(purchase.getPurchaseDate()));
                    statement.setDouble(4, purchase.getTotalAmount());
                    statement.setString(5, purchase.getNotes());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Failed to create purchase.");
                        purchaseId = keys.getInt(1);
                    }
                }

                for (PurchaseItem item : purchase.getItems()) {
                    savePurchaseItem(connection, purchaseId, item);
                    increaseStock(connection, purchase, purchaseId, item);
                }
                connection.commit();
                purchase.setPurchaseId(purchaseId);
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to save purchase.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to save purchase.", exception);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private void savePurchaseItem(
            Connection connection,
            int purchaseId,
            PurchaseItem item)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             INSERT_ITEM)) {

            statement.setInt(1,
                    purchaseId);

            statement.setInt(2,
                    item.getProductId());

            statement.setInt(3,
                    item.getQuantity());

            statement.setDouble(4,
                    item.getPurchasePrice());

            statement.setDouble(5,
                    item.getSubtotal());

            statement.executeUpdate();

        }

    }

    private void increaseStock(
            Connection connection,
            Purchase purchase,
            int purchaseId,
            PurchaseItem item)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             INCREASE_STOCK)) {

            statement.setInt(1,
                    item.getQuantity());

            statement.setInt(2,
                    item.getProductId());

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException("Product not found: " + item.getProductId());
                }

                int quantityAfter = rs.getInt("quantity");
                int quantityBefore = quantityAfter - item.getQuantity();

                InventoryMovement movement = new InventoryMovement(
                        item.getProductId(),
                        item.getProductName(),
                        "PURCHASE",
                        item.getQuantity(),
                        quantityBefore,
                        quantityAfter,
                        purchase.getEmployeeId(),
                        null,
                        "Purchase #" + purchaseId
                );

                inventoryMovementDAO.record(connection, movement);

            }

        }

    }
    public List<Purchase> getAllPurchases() {

        List<Purchase> purchases = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(GET_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                purchases.add(mapPurchase(rs));

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load purchases.", e);

        }

        return purchases;

    }

    public Purchase getPurchaseById(int purchaseId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(GET_BY_ID)) {

            statement.setInt(1, purchaseId);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                Purchase purchase = mapPurchase(rs);

                purchase.setItems(
                        getPurchaseItems(
                                connection,
                                purchaseId
                        )
                );

                purchase.calculateTotalAmount();

                return purchase;

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load purchase.", e);

        }

    }

    public List<Purchase> searchPurchases(String keyword) {

        List<Purchase> purchases = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SEARCH)) {

            String search = "%" + keyword + "%";

            statement.setString(1, search);
            statement.setString(2, search);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    purchases.add(
                            mapPurchase(rs)
                    );

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to search purchases.", e);

        }

        return purchases;

    }

    private List<PurchaseItem> getPurchaseItems(
            Connection connection,
            int purchaseId)
            throws SQLException {

        String sql = """
                SELECT
                    pi.*,
                    p.name
                FROM purchase_items pi
                JOIN products p
                    ON p.product_id = pi.product_id
                WHERE purchase_id = ?
                ORDER BY purchase_item_id
                """;

        List<PurchaseItem> items =
                new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, purchaseId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    PurchaseItem item =
                            new PurchaseItem();

                    item.setPurchaseItemId(
                            rs.getInt("purchase_item_id"));

                    item.setPurchaseId(
                            purchaseId);

                    item.setProductId(
                            rs.getInt("product_id"));

                    item.setProductName(
                            rs.getString("name"));

                    item.setQuantity(
                            rs.getInt("quantity"));

                    item.setPurchasePrice(
                            rs.getDouble("purchase_price"));

                    item.setSubtotal(
                            rs.getDouble("subtotal"));

                    items.add(item);

                }

            }

        }

        return items;

    }

    private Purchase mapPurchase(ResultSet rs)
            throws SQLException {

        Purchase purchase =
                new Purchase();

        purchase.setPurchaseId(
                rs.getInt("purchase_id"));

        purchase.setSupplierId(
                rs.getInt("supplier_id"));

        purchase.setSupplierName(
                rs.getString("supplier_name"));

        purchase.setEmployeeId(
                rs.getInt("employee_id"));

        purchase.setPurchaseDate(
                rs.getTimestamp("purchase_date")
                        .toLocalDateTime());

        purchase.setTotalAmount(
                rs.getDouble("total_amount"));

        purchase.setNotes(
                rs.getString("notes"));

        return purchase;

    }

}
