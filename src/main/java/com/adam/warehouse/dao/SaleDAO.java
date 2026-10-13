package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    private final InventoryMovementDAO inventoryMovementDAO = new InventoryMovementDAO();

    private static final String INSERT_SALE = """
            INSERT INTO sales
            (customer_id,
             employee_id,
             sale_date,
             total_amount,
             notes)
            VALUES (?,?,?,?,?)
            """;

    private static final String INSERT_ITEM = """
            INSERT INTO sale_items
            (sale_id,
             product_id,
             quantity,
             selling_price,
             cost_price,
             subtotal)
            VALUES (?,?,?,?,?,?)
            """;

    private static final String DECREASE_STOCK = """
            UPDATE products
            SET quantity = quantity - ?
            WHERE product_id = ?
              AND active = true
              AND quantity >= ?
            RETURNING quantity
            """;

    private static final String CHECK_STOCK = """
            SELECT quantity
            FROM products
            WHERE product_id = ?
            """;

    private static final String GET_ALL = """
            SELECT s.*, c.full_name customer_name
            FROM sales s
            LEFT JOIN customers c ON c.customer_id = s.customer_id
            ORDER BY sale_date DESC,
                     sale_id DESC
            """;

    private static final String GET_BY_ID = """
            SELECT s.*, c.full_name customer_name
            FROM sales s
            LEFT JOIN customers c ON c.customer_id = s.customer_id
            WHERE s.sale_id = ?
            """;

    private static final String SEARCH = """
            SELECT s.*, c.full_name customer_name
            FROM sales s
            LEFT JOIN customers c ON c.customer_id = s.customer_id
            WHERE CAST(s.sale_id AS TEXT) LIKE ?
               OR LOWER(COALESCE(c.full_name, '')) LIKE LOWER(?)
            ORDER BY s.sale_date DESC
            """;

    public void saveSale(Sale sale) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int saleId;
                try (PreparedStatement statement = connection.prepareStatement(
                        INSERT_SALE, Statement.RETURN_GENERATED_KEYS)) {
                    if (sale.getCustomerId() == null || sale.getCustomerId() <= 0) {
                        statement.setNull(1, Types.INTEGER);
                    } else {
                        statement.setInt(1, sale.getCustomerId());
                    }
                    statement.setInt(2, sale.getEmployeeId());
                    statement.setTimestamp(3, Timestamp.valueOf(sale.getSaleDate()));
                    statement.setDouble(4, sale.getTotalAmount());
                    statement.setString(5, sale.getNotes());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Unable to create sale.");
                        saleId = keys.getInt(1);
                    }
                }

                for (SaleItem item : sale.getItems()) {
                    checkStock(connection, item);
                    saveSaleItem(connection, saleId, item);
                    decreaseStock(connection, sale, saleId, item);
                }
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to save sale.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to save sale.", exception);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private void checkStock(
            Connection connection,
            SaleItem item)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             CHECK_STOCK)) {

            statement.setInt(
                    1,
                    item.getProductId()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (!rs.next()) {

                    throw new SQLException(
                            "Product not found."
                    );

                }

                int stock =
                        rs.getInt("quantity");

                if (stock < item.getQuantity()) {

                    throw new IllegalArgumentException(
                            "Not enough stock for "
                                    + item.getProductName()
                    );

                }

            }

        }

    }

    private void saveSaleItem(
            Connection connection,
            int saleId,
            SaleItem item)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             INSERT_ITEM)) {

            statement.setInt(1, saleId);
            statement.setInt(2, item.getProductId());
            statement.setInt(3, item.getQuantity());
            statement.setDouble(4, item.getSellingPrice());
            statement.setDouble(5, item.getCostPrice());
            statement.setDouble(6, item.getSubtotal());

            statement.executeUpdate();

        }

    }

    private void decreaseStock(
            Connection connection,
            Sale sale,
            int saleId,
            SaleItem item)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             DECREASE_STOCK)) {

            statement.setInt(
                    1,
                    item.getQuantity()
            );

            statement.setInt(2, item.getProductId());
            statement.setInt(3, item.getQuantity());

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "Insufficient stock or unavailable product: " + item.getProductName());
                }

                int quantityAfter = rs.getInt("quantity");
                int quantityBefore = quantityAfter + item.getQuantity();

                InventoryMovement movement = new InventoryMovement(
                        item.getProductId(),
                        item.getProductName(),
                        "SALE",
                        -item.getQuantity(),
                        quantityBefore,
                        quantityAfter,
                        sale.getEmployeeId(),
                        null,
                        "Sale #" + saleId
                );

                inventoryMovementDAO.record(connection, movement);

            }

        }

    }
    public List<Sale> getAllSales() {

        List<Sale> sales = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(GET_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                sales.add(mapSale(rs));

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load sales.", e);

        }

        return sales;

    }

    public Sale getSaleById(int saleId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(GET_BY_ID)) {

            statement.setInt(1, saleId);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                Sale sale = mapSale(rs);

                sale.setItems(
                        getSaleItems(
                                connection,
                                saleId
                        )
                );

                sale.calculateTotalAmount();

                return sale;

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load sale.", e);

        }

    }

    public List<Sale> searchSales(String keyword) {

        List<Sale> sales = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SEARCH)) {

            String search = "%" + keyword + "%";

            statement.setString(1, search);
            statement.setString(2, search);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    sales.add(mapSale(rs));

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to search sales.", e);

        }

        return sales;

    }

    private List<SaleItem> getSaleItems(
            Connection connection,
            int saleId)
            throws SQLException {

        String sql = """
                SELECT
                    si.*,
                    p.name
                FROM sale_items si
                JOIN products p
                    ON p.product_id = si.product_id
                WHERE sale_id = ?
                ORDER BY sale_item_id
                """;

        List<SaleItem> items = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, saleId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    SaleItem item = new SaleItem();

                    item.setSaleItemId(
                            rs.getInt("sale_item_id"));

                    item.setSaleId(
                            saleId);

                    item.setProductId(
                            rs.getInt("product_id"));

                    item.setProductName(
                            rs.getString("name"));

                    item.setQuantity(
                            rs.getInt("quantity"));

                    item.setSellingPrice(
                            rs.getDouble("selling_price"));

                    item.setCostPrice(
                            rs.getDouble("cost_price"));

                    item.setSubtotal(
                            rs.getDouble("subtotal"));

                    items.add(item);

                }

            }

        }

        return items;

    }

    private Sale mapSale(ResultSet rs)
            throws SQLException {

        Sale sale = new Sale();

        sale.setSaleId(
                rs.getInt("sale_id"));

        sale.setEmployeeId(
                rs.getInt("employee_id"));

        Object customerId = rs.getObject("customer_id");
        sale.setCustomerId(customerId == null ? null : ((Number) customerId).intValue());
        sale.setCustomerName(rs.getString("customer_name"));
        sale.setNotes(rs.getString("notes"));

        sale.setSaleDate(
                rs.getTimestamp("sale_date")
                        .toLocalDateTime());

        sale.setTotalAmount(
                rs.getDouble("total_amount"));

        return sale;

    }

}
