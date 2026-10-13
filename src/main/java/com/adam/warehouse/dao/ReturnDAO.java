package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.Return;
import com.adam.warehouse.model.ReturnItem;
import com.adam.warehouse.model.ReturnableItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists customer returns: for every line item this both records the refund and,
 * when marked restocked, puts the quantity back into sellable stock (with a matching
 * RETURN inventory movement, mirroring how SaleDAO/PurchaseDAO record their own
 * movements). Runs as a single transaction so a partial failure never leaves stock
 * adjusted without the return being recorded, or vice versa.
 */
public class ReturnDAO {

    private final InventoryMovementDAO inventoryMovementDAO = new InventoryMovementDAO();

    private static final String INSERT_RETURN = """
            INSERT INTO returns
            (sale_id, employee_id, return_date, total_refund, reason)
            VALUES (?,?,?,?,?)
            """;

    private static final String INSERT_ITEM = """
            INSERT INTO return_items
            (return_id, sale_item_id, product_id, quantity, refund_amount, restocked)
            VALUES (?,?,?,?,?,?)
            """;

    // Locks the sale_item row so two concurrent returns against the same line item
    // can't both pass the "enough left to return" check.
    private static final String LOCK_SALE_ITEM = """
            SELECT si.quantity, si.product_id, si.selling_price, p.name AS product_name
            FROM sale_items si
            JOIN products p ON p.product_id = si.product_id
            WHERE si.sale_item_id = ? AND si.sale_id = ?
            FOR UPDATE OF si
            """;

    private static final String ALREADY_RETURNED = """
            SELECT COALESCE(SUM(quantity), 0)
            FROM return_items
            WHERE sale_item_id = ?
            """;

    private static final String RESTOCK = """
            UPDATE products
            SET quantity = quantity + ?
            WHERE product_id = ?
            RETURNING quantity
            """;

    private static final String GET_RETURNABLE_ITEMS = """
            SELECT si.sale_item_id, si.product_id, p.name AS product_name,
                   si.quantity AS sold_quantity, si.selling_price,
                   COALESCE(ri.returned_quantity, 0) AS returned_quantity
            FROM sale_items si
            JOIN products p ON p.product_id = si.product_id
            LEFT JOIN (
                SELECT sale_item_id, SUM(quantity) AS returned_quantity
                FROM return_items
                GROUP BY sale_item_id
            ) ri ON ri.sale_item_id = si.sale_item_id
            WHERE si.sale_id = ?
            ORDER BY si.sale_item_id
            """;

    private static final String GET_ALL = """
            SELECT r.*, e.first_name || ' ' || e.last_name AS employee_name,
                   c.full_name AS customer_name
            FROM returns r
            JOIN employees e ON e.employee_id = r.employee_id
            JOIN sales s ON s.sale_id = r.sale_id
            LEFT JOIN customers c ON c.customer_id = s.customer_id
            ORDER BY r.return_date DESC, r.return_id DESC
            """;

    private static final String GET_BY_ID = """
            SELECT r.*, e.first_name || ' ' || e.last_name AS employee_name,
                   c.full_name AS customer_name
            FROM returns r
            JOIN employees e ON e.employee_id = r.employee_id
            JOIN sales s ON s.sale_id = r.sale_id
            LEFT JOIN customers c ON c.customer_id = s.customer_id
            WHERE r.return_id = ?
            """;

    private static final String GET_FOR_SALE = """
            SELECT r.*, e.first_name || ' ' || e.last_name AS employee_name,
                   c.full_name AS customer_name
            FROM returns r
            JOIN employees e ON e.employee_id = r.employee_id
            JOIN sales s ON s.sale_id = r.sale_id
            LEFT JOIN customers c ON c.customer_id = s.customer_id
            WHERE r.sale_id = ?
            ORDER BY r.return_date DESC, r.return_id DESC
            """;

    private static final String GET_ITEMS_FOR_RETURN = """
            SELECT ri.*, p.name AS product_name
            FROM return_items ri
            JOIN products p ON p.product_id = ri.product_id
            WHERE ri.return_id = ?
            ORDER BY ri.return_item_id
            """;

    public void saveReturn(Return returnRecord) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int returnId;
                try (PreparedStatement statement = connection.prepareStatement(
                        INSERT_RETURN, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, returnRecord.getSaleId());
                    statement.setInt(2, returnRecord.getEmployeeId());
                    statement.setTimestamp(3, Timestamp.valueOf(returnRecord.getReturnDate()));
                    statement.setDouble(4, returnRecord.getTotalRefund());
                    statement.setString(5, returnRecord.getReason());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Unable to create return.");
                        returnId = keys.getInt(1);
                    }
                }
                for (ReturnItem item : returnRecord.getItems()) {
                    applyReturnItem(connection, returnRecord, returnId, item);
                }
                connection.commit();
                returnRecord.setReturnId(returnId);
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
                throw new RuntimeException("Unable to save return.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to save return.", exception);
        }
    }

    private void applyReturnItem(Connection connection, Return returnRecord, int returnId, ReturnItem item)
            throws SQLException {

        int soldQuantity;
        String productName;

        try (PreparedStatement lock = connection.prepareStatement(LOCK_SALE_ITEM)) {
            lock.setInt(1, item.getSaleItemId());
            lock.setInt(2, returnRecord.getSaleId());
            try (ResultSet rs = lock.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Sale item not found on this sale.");
                }
                soldQuantity = rs.getInt("quantity");
                productName = rs.getString("product_name");
                if (rs.getInt("product_id") != item.getProductId()) {
                    throw new IllegalArgumentException("Sale item does not match the selected product.");
                }
            }
        }

        int alreadyReturned;
        try (PreparedStatement statement = connection.prepareStatement(ALREADY_RETURNED)) {
            statement.setInt(1, item.getSaleItemId());
            try (ResultSet rs = statement.executeQuery()) {
                alreadyReturned = rs.next() ? rs.getInt(1) : 0;
            }
        }

        int remaining = soldQuantity - alreadyReturned;

        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Return quantity must be greater than zero for " + productName + ".");
        }

        if (item.getQuantity() > remaining) {
            throw new IllegalArgumentException(
                    "Cannot return " + item.getQuantity() + " of " + productName
                            + " — only " + remaining + " remaining eligible for return.");
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_ITEM)) {
            statement.setInt(1, returnId);
            statement.setInt(2, item.getSaleItemId());
            statement.setInt(3, item.getProductId());
            statement.setInt(4, item.getQuantity());
            statement.setDouble(5, item.getRefundAmount());
            statement.setBoolean(6, item.isRestocked());
            statement.executeUpdate();
        }

        if (item.isRestocked()) {
            try (PreparedStatement statement = connection.prepareStatement(RESTOCK)) {
                statement.setInt(1, item.getQuantity());
                statement.setInt(2, item.getProductId());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Unable to restock " + productName + ".");
                    }
                    int quantityAfter = rs.getInt("quantity");
                    int quantityBefore = quantityAfter - item.getQuantity();

                    InventoryMovement movement = new InventoryMovement(
                            item.getProductId(),
                            productName,
                            "RETURN",
                            item.getQuantity(),
                            quantityBefore,
                            quantityAfter,
                            returnRecord.getEmployeeId(),
                            null,
                            "Return #" + returnId + " (Sale #" + returnRecord.getSaleId() + ")"
                    );

                    inventoryMovementDAO.record(connection, movement);
                }
            }
        }
    }

    public List<ReturnableItem> getReturnableItems(int saleId) {

        List<ReturnableItem> items = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_RETURNABLE_ITEMS)) {

            statement.setInt(1, saleId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(new ReturnableItem(
                            rs.getInt("sale_item_id"),
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("sold_quantity"),
                            rs.getInt("returned_quantity"),
                            rs.getDouble("selling_price")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to load returnable items for this sale.", e);
        }

        return items;
    }

    public List<Return> getAllReturns() {
        return queryReturns(GET_ALL, -1);
    }

    public List<Return> getReturnsForSale(int saleId) {
        return queryReturns(GET_FOR_SALE, saleId);
    }

    public Return getReturnById(int returnId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_BY_ID)) {

            statement.setInt(1, returnId);

            Return returnRecord;
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                returnRecord = mapReturn(rs);
            }

            returnRecord.setItems(getItemsForReturn(connection, returnId));
            return returnRecord;

        } catch (SQLException e) {
            throw new RuntimeException("Unable to load return.", e);
        }
    }

    private List<Return> queryReturns(String sql, int parameter) {

        List<Return> returns = new ArrayList<>();

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
            throw new RuntimeException("Unable to load returns.", e);
        }

        return returns;
    }

    private List<ReturnItem> getItemsForReturn(Connection connection, int returnId) throws SQLException {

        List<ReturnItem> items = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(GET_ITEMS_FOR_RETURN)) {
            statement.setInt(1, returnId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ReturnItem item = new ReturnItem();
                    item.setReturnItemId(rs.getInt("return_item_id"));
                    item.setReturnId(rs.getInt("return_id"));
                    item.setSaleItemId(rs.getInt("sale_item_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setRefundAmount(rs.getDouble("refund_amount"));
                    item.setRestocked(rs.getBoolean("restocked"));
                    items.add(item);
                }
            }
        }

        return items;
    }

    private Return mapReturn(ResultSet rs) throws SQLException {

        Return returnRecord = new Return();

        returnRecord.setReturnId(rs.getInt("return_id"));
        returnRecord.setSaleId(rs.getInt("sale_id"));
        returnRecord.setEmployeeId(rs.getInt("employee_id"));
        returnRecord.setEmployeeName(rs.getString("employee_name"));
        returnRecord.setCustomerName(rs.getString("customer_name"));
        returnRecord.setReturnDate(rs.getTimestamp("return_date").toLocalDateTime());
        returnRecord.setTotalRefund(rs.getDouble("total_refund"));
        returnRecord.setReason(rs.getString("reason"));

        return returnRecord;
    }
}
