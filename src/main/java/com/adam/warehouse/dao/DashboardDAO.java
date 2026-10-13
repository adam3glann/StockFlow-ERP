package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.LowStockProduct;
import com.adam.warehouse.model.TopSellingProduct;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    public int getTotalProducts() {
        return getCount("""
                SELECT COUNT(*)
                FROM products
                WHERE active = TRUE
                """);
    }

    public int getProductsInStock() {
        return getCount("""
                SELECT COUNT(*)
                FROM products
                WHERE active = TRUE
                AND quantity > 0
                """);
    }

    public int getLowStockCount() {
        return getCount("""
                SELECT COUNT(*)
                FROM products
                WHERE active = TRUE
                AND quantity <= minimum_stock
                """);
    }

    public int getTotalCustomers() {
        return getCount("""
                SELECT COUNT(*)
                FROM customers
                WHERE active = TRUE
                """);
    }

    public int getTotalSuppliers() {
        return getCount("""
                SELECT COUNT(*)
                FROM suppliers
                WHERE active = TRUE
                """);
    }

    public int getTotalEmployees() {
        return getCount("""
                SELECT COUNT(*) FROM employees WHERE active = TRUE
                """);
    }

    public int getOutOfStockCount() {
        return getCount("""
                SELECT COUNT(*) FROM products WHERE active = TRUE AND quantity = 0
                """);
    }

    public double getTodaySalesTotal() {
        return getAmount("""
                SELECT COALESCE(SUM(total_amount),0)
                FROM sales
                WHERE sale_date >= CURRENT_DATE
                AND sale_date < CURRENT_DATE + INTERVAL '1 day'
                """);
    }

    public double getTodayPurchasesTotal() {
        return getAmount("""
                SELECT COALESCE(SUM(total_amount),0)
                FROM purchases
                WHERE purchase_date >= CURRENT_DATE
                AND purchase_date < CURRENT_DATE + INTERVAL '1 day'
                """);
    }

    public double getTotalRevenue() {
        return getAmount("""
                SELECT COALESCE(SUM(total_amount),0)
                FROM sales
                """);
    }

    public double getInventoryValue() {
        return getAmount("""
                SELECT COALESCE(
                    SUM(quantity * purchase_price),
                    0
                )
                FROM products
                WHERE active = TRUE
                """);
    }

    public double getTotalProfit() {

        return getAmount("""
                SELECT COALESCE(
                    SUM(
                        (si.selling_price - si.cost_price)
                        * si.quantity
                    ),
                    0
                )
                FROM sale_items si
                """);

    }

    public double getTodayProfit() {

        return getAmount("""
                SELECT COALESCE(
                    SUM(
                        (si.selling_price - si.cost_price)
                        * si.quantity
                    ),
                    0
                )
                FROM sale_items si
                JOIN sales s
                ON s.sale_id = si.sale_id
                WHERE s.sale_date >= CURRENT_DATE
                AND s.sale_date < CURRENT_DATE + INTERVAL '1 day'
                """);

    }

    public double getTodayReturnsTotal() {
        return getAmount("""
                SELECT COALESCE(SUM(total_refund),0)
                FROM returns
                WHERE return_date >= CURRENT_DATE
                AND return_date < CURRENT_DATE + INTERVAL '1 day'
                """);
    }

    public double getTotalReturnsAmount() {
        return getAmount("""
                SELECT COALESCE(SUM(total_refund),0)
                FROM returns
                """);
    }

    public List<LowStockProduct> getLowStockProducts() {

        String sql = """
                SELECT
                    name,
                    quantity,
                    minimum_stock
                FROM products
                WHERE active = TRUE
                AND quantity <= minimum_stock
                ORDER BY quantity,name
                LIMIT 10
                """;

        List<LowStockProduct> products =
                new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                products.add(

                        new LowStockProduct(

                                rs.getString("name"),

                                rs.getInt("quantity"),

                                rs.getInt("minimum_stock")

                        )

                );

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load dashboard.",
                    e
            );

        }

        return products;

    }

    public List<TopSellingProduct> getTopSellingProducts(int limit) {

        String sql = """
                SELECT
                    p.name,
                    SUM(si.quantity) AS quantity_sold,
                    SUM(si.quantity * si.selling_price) AS revenue
                FROM sale_items si
                JOIN products p
                ON p.product_id = si.product_id
                GROUP BY p.name
                ORDER BY quantity_sold DESC
                LIMIT ?
                """;

        List<TopSellingProduct> products = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    products.add(new TopSellingProduct(
                            rs.getString("name"),
                            rs.getInt("quantity_sold"),
                            rs.getDouble("revenue")
                    ));

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load top selling products.",
                    e
            );

        }

        return products;

    }

    public java.util.List<com.adam.warehouse.model.DailyTotal> getDailyTotals(int days) {

        List<com.adam.warehouse.model.DailyTotal> totals = new ArrayList<>();

        String sql = """
                SELECT
                    d.day::date AS day,
                    COALESCE(s.total, 0) AS sales_total,
                    COALESCE(p.total, 0) AS purchases_total
                FROM generate_series(
                        CURRENT_DATE - (? - 1) * INTERVAL '1 day',
                        CURRENT_DATE,
                        INTERVAL '1 day'
                     ) AS d(day)
                LEFT JOIN (
                    SELECT sale_date::date AS day, SUM(total_amount) AS total
                    FROM sales
                    GROUP BY sale_date::date
                ) s ON s.day = d.day::date
                LEFT JOIN (
                    SELECT purchase_date::date AS day, SUM(total_amount) AS total
                    FROM purchases
                    GROUP BY purchase_date::date
                ) p ON p.day = d.day::date
                ORDER BY d.day
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, days);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    totals.add(new com.adam.warehouse.model.DailyTotal(
                            rs.getDate("day").toLocalDate(),
                            rs.getDouble("sales_total"),
                            rs.getDouble("purchases_total")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return totals;
    }

    private int getCount(String sql) {

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs =
                     statement.executeQuery()) {

            if (rs.next()) {

                return rs.getInt(1);

            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

        return 0;

    }

    private double getAmount(String sql) {

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs =
                     statement.executeQuery()) {

            if (rs.next()) {

                return rs.getDouble(1);

            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

        return 0;

    }

}
