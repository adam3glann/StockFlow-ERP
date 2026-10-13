package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.PurchaseReturn;
import com.adam.warehouse.model.PurchaseReturnItem;
import com.adam.warehouse.model.ReturnablePurchaseItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists purchase returns: goods sent back to a supplier. Reduces stock (the
 * mirror image of ReturnDAO, which increases it) and records a matching
 * PURCHASE_RETURN inventory movement. The stock decrease is done in the same
 * atomic UPDATE ... WHERE quantity >= ? pattern SaleDAO uses, so this can never
 * push a product's quantity negative even under concurrent activity.
 */
public class PurchaseReturnDAO {

    private final InventoryMovementDAO inventoryMovementDAO = new InventoryMovementDAO();

    private static final String INSERT_RETURN = """
            INSERT INTO purchase_returns
            (purchase_id, employee_id, return_date, total_credit, reason)
            VALUES (?,?,?,?,?)
            """;

    private static final String INSERT_ITEM = """
            INSERT INTO purchase_return_items
            (purchase_return_id, purchase_item_id, product_id, quantity, credit_amount)
            VALUES (?,?,?,?,?)
            """;

    private static final String LOCK_PURCHASE_ITEM = """
            SELECT pi.quantity, pi.product_id, pi.purchase_price, p.name AS product_name
            FROM purchase_items pi
            JOIN products p ON p.product_id = pi.product_id
            WHERE pi.purchase_item_id = ? AND pi.purchase_id = ?
            FOR UPDATE OF pi
            """;

    private static final String ALREADY_RETURNED = """
            SELECT COALESCE(SUM(quantity), 0)
            FROM purchase_return_items
            WHERE purchase_item_id = ?
            """;

    private static final String DECREASE_STOCK = """
            UPDATE products
            SET quantity = quantity - ?
            WHERE product_id = ?
            AND quantity >= ?
            RETURNING quantity
            """;

    private static final String GET_RETURNABLE_ITEMS = """
            SELECT pi.purchase_item_id, pi.product_id, p.name AS product_name,
                   pi.quantity AS purchased_quantity, pi.purchase_price, p.quantity AS current_stock,
                   COALESCE(pri.returned_quantity, 0) AS returned_quantity
            FROM purchase_items pi
            JOIN products p ON p.product_id = pi.product_id
            LEFT JOIN (
                SELECT purchase_item_id, SUM(quantity) AS returned_quantity
                FROM purchase_return_items
                GROUP BY purchase_item_id
            ) pri ON pri.purchase_item_id = pi.purchase_item_id
            WHERE pi.purchase_id = ?
            ORDER BY pi.purchase_item_id
            """;

    private static final String GET_ALL = """
            SELECT pr.*, e.first_name || ' ' || e.last_name AS employee_name,
                   s.name AS supplier_name
            FROM purchase_returns pr
            JOIN employees e ON e.employee_id = pr.employee_id
            JOIN purchases p ON p.purchase_id = pr.purchase_id
            LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id
            ORDER BY pr.return_date DESC, pr.purchase_return_id DESC
            """;

    private static final String GET_BY_ID = """
            SELECT pr.*, e.first_name || ' ' || e.last_name AS employee_name,
                   s.name AS supplier_name
            FROM purchase_returns pr
            JOIN employees e ON e.employee_id = pr.employee_id
            JOIN purchases p ON p.purchase_id = pr.purchase_id
            LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id
            WHERE pr.purchase_return_id = ?
            """;

    private static final String GET_FOR_PURCHASE = """
            SELECT pr.*, e.first_name || ' ' || e.last_name AS employee_name,
                   s.name AS supplier_name
            FROM purchase_returns pr
            JOIN employees e ON e.employee_id = pr.employee_id
            JOIN purchases p ON p.purchase_id = pr.purchase_id
            LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id
            WHERE pr.purchase_id = ?
            ORDER BY pr.return_date DESC, pr.purchase_return_id DESC
            """;

    private static final String GET_ITEMS_FOR_RETURN = """
            SELECT pri.*, p.name AS product_name
            FROM purchase_return_items pri
            JOIN products p ON p.product_id = pri.product_id
            WHERE pri.purchase_return_id = ?
            ORDER BY pri.purchase_return_item_id
            """;

    public void saveReturn(PurchaseReturn purchaseReturn) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int purchaseReturnId;
                try (PreparedStatement statement = connection.prepareStatement(
                        INSERT_RETURN, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, purchaseReturn.getPurchaseId());
                    statement.setInt(2, purchaseReturn.getEmployeeId());
                    statement.setTimestamp(3, Timestamp.valueOf(purchaseReturn.getReturnDate()));
                    statement.setDouble(4, purchaseReturn.getTotalCredit());
                    statement.setString(5, purchaseReturn.getReason());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Unable to create purchase return.");
                        purchaseReturnId = keys.getInt(1);
                    }
                }
                for (PurchaseReturnItem item : purchaseReturn.getItems()) {
                    applyReturnItem(connection, purchaseReturn, purchaseReturnId, item);
                }
                connection.commit();
                purchaseReturn.setPurchaseReturnId(purchaseReturnId);
            } catch (Exception exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                if (exception instanceof IllegalArgumentException illegalArgumentException) {
                    throw illegalArgumentException;
                }
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to save purchase return.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to save purchase return.", exception);
        }
    }

    private void applyReturnItem(Connection connection, PurchaseReturn purchaseReturn,
                                  int purchaseReturnId, PurchaseReturnItem item) throws SQLException {

        int purchasedQuantity;
        String productName;

        try (PreparedStatement lock = connection.prepareStatement(LOCK_PURCHASE_ITEM)) {
            lock.setInt(1, item.getPurchaseItemId());
            lock.setInt(2, purchaseReturn.getPurchaseId());
            try (ResultSet rs = lock.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Purchase item not found on this purchase.");
                }
                purchasedQuantity = rs.getInt("quantity");
                productName = rs.getString("product_name");
                if (rs.getInt("product_id") != item.getProductId()) {
                    throw new IllegalArgumentException("Purchase item does not match the selected product.");
                }
            }
        }

        int alreadyReturned;
        try (PreparedStatement statement = connection.prepareStatement(ALREADY_RETURNED)) {
            statement.setInt(1, item.getPurchaseItemId());
            try (ResultSet rs = statement.executeQuery()) {
                alreadyReturned = rs.next() ? rs.getInt(1) : 0;
            }
        }

        int remaining = purchasedQuantity - alreadyReturned;

        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Return quantity must be greater than zero for " + productName + ".");
        }

        if (item.getQuantity() > remaining) {
            throw new IllegalArgumentException(
                    "Cannot return " + item.getQuantity() + " of " + productName
                            + " — only " + remaining + " remaining eligible for return.");
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_ITEM)) {
            statement.setInt(1, purchaseReturnId);
            statement.setInt(2, item.getPurchaseItemId());
            statement.setInt(3, item.getProductId());
            statement.setInt(4, item.getQuantity());
            statement.setDouble(5, item.getCreditAmount());
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(DECREASE_STOCK)) {
            statement.setInt(1, item.getQuantity());
            statement.setInt(2, item.getProductId());
            statement.setInt(3, item.getQuantity());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "Not enough " + productName + " in stock to send back to the supplier.");
                }
                int quantityAfter = rs.getInt("quantity");
                int quantityBefore = quantityAfter + item.getQuantity();

                InventoryMovement movement = new InventoryMovement(
                        item.getProductId(),
                        productName,
                        "PURCHASE_RETURN",
                        -item.getQuantity(),
                        quantityBefore,
                        quantityAfter,
                        purchaseReturn.getEmployeeId(),
                        null,
                        "Purchase Return #" + purchaseReturnId + " (Purchase #" + purchaseReturn.getPurchaseId() + ")"
                );

                inventoryMovementDAO.record(connection, movement);
            }
        }
    }

    public List<ReturnablePurchaseItem> getReturnableItems(int purchaseId) {

        List<ReturnablePurchaseItem> items = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_RETURNABLE_ITEMS)) {

            statement.setInt(1, purchaseId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(new ReturnablePurchaseItem(
                            rs.getInt("purchase_item_id"),
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("purchased_quantity"),
                            rs.getInt("returned_quantity"),
                            rs.getInt("current_stock"),
                            rs.getDouble("purchase_price")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to load returnable items for this purchase.", e);
        }

        return items;
    }

    public List<PurchaseReturn> getAllReturns() {
        return queryReturns(GET_ALL, -1);
    }

    public List<PurchaseReturn> getReturnsForPurchase(int purchaseId) {
        return queryReturns(GET_FOR_PURCHASE, purchaseId);
    }

    public PurchaseReturn getReturnById(int purchaseReturnId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_BY_ID)) {

            statement.setInt(1, purchaseReturnId);

            PurchaseReturn purchaseReturn;
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                purchaseReturn = mapReturn(rs);
            }

            purchaseReturn.setItems(getItemsForReturn(connection, purchaseReturnId));
            return purchaseReturn;

        } catch (SQLException e) {
            throw new RuntimeException("Unable to load purchase return.", e);
        }
    }

    private List<PurchaseReturn> queryReturns(String sql, int parameter) {

        List<PurchaseReturn> returns = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter >= 0) {
                statement.setInt(1, parameter);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapReturn(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to load purchase returns.", e);
        }

        return returns;
    }

    private List<PurchaseReturnItem> getItemsForReturn(Connection connection, int purchaseReturnId) throws SQLException {

        List<PurchaseReturnItem> items = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(GET_ITEMS_FOR_RETURN)) {
            statement.setInt(1, purchaseReturnId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PurchaseReturnItem item = new PurchaseReturnItem();
                    item.setPurchaseReturnItemId(rs.getInt("purchase_return_item_id"));
                    item.setPurchaseReturnId(rs.getInt("purchase_return_id"));
                    item.setPurchaseItemId(rs.getInt("purchase_item_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setCreditAmount(rs.getDouble("credit_amount"));
                    items.add(item);
                }
            }
        }

        return items;
    }

    private PurchaseReturn mapReturn(ResultSet rs) throws SQLException {

        PurchaseReturn purchaseReturn = new PurchaseReturn();

        purchaseReturn.setPurchaseReturnId(rs.getInt("purchase_return_id"));
        purchaseReturn.setPurchaseId(rs.getInt("purchase_id"));
        purchaseReturn.setEmployeeId(rs.getInt("employee_id"));
        purchaseReturn.setEmployeeName(rs.getString("employee_name"));
        purchaseReturn.setSupplierName(rs.getString("supplier_name"));
        purchaseReturn.setReturnDate(rs.getTimestamp("return_date").toLocalDateTime());
        purchaseReturn.setTotalCredit(rs.getDouble("total_credit"));
        purchaseReturn.setReason(rs.getString("reason"));

        return purchaseReturn;
    }
}
