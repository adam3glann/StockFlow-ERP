package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.ReportSummary;
import com.adam.warehouse.model.TopSellingProduct;
import com.adam.warehouse.util.LanguageManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public ReportSummary getSummary() {

        ReportSummary summary = new ReportSummary();

        summary.setTitle(LanguageManager.get("report.summary.title.all"));

        summary.setTotalSales(getDouble("""
                SELECT COALESCE(SUM(total_amount),0)
                FROM sales
                """));

        summary.setTotalPurchases(getDouble("""
                SELECT COALESCE(SUM(total_amount),0)
                FROM purchases
                """));

        summary.setInventoryValue(getDouble("""
                SELECT COALESCE(
                    SUM(quantity * purchase_price),
                    0
                )
                FROM products
                WHERE active = TRUE
                """));

        summary.setTotalProfit(getDouble("""
                SELECT COALESCE(
                    SUM(
                        (si.selling_price - si.cost_price)
                        * si.quantity
                    ),
                    0
                )
                FROM sale_items si
                """));

        summary.setTotalProductsSold(getInt("""
                SELECT COALESCE(
                    SUM(quantity),
                    0
                )
                FROM sale_items
                """));

        summary.setTotalTransactions(getInt("""
                SELECT COUNT(*)
                FROM sales
                """));

        summary.setTotalProducts(getInt("""
                SELECT COUNT(*)
                FROM products
                WHERE active = TRUE
                """));

        summary.setTotalCustomers(getInt("""
                SELECT COUNT(*)
                FROM customers
                WHERE active = TRUE
                """));

        summary.setTotalSuppliers(getInt("""
                SELECT COUNT(*)
                FROM suppliers
                WHERE active = TRUE
                """));

        summary.setLowStockProducts(getInt("""
                SELECT COUNT(*)
                FROM products
                WHERE active = TRUE
                AND quantity <= minimum_stock
                """));

        summary.setTotalReturns(getDouble("""
                SELECT COALESCE(SUM(total_refund),0)
                FROM returns
                """));

        return summary;

    }

    public ReportSummary getTodaySummary() {

        ReportSummary summary = new ReportSummary();

        summary.setTitle(LanguageManager.get("report.summary.title.today"));

        summary.setTotalSales(getDouble("""
                SELECT COALESCE(SUM(total_amount),0)
                FROM sales
                WHERE sale_date >= CURRENT_DATE
                AND sale_date < CURRENT_DATE + INTERVAL '1 day'
                """));

        summary.setTotalPurchases(getDouble("""
                SELECT COALESCE(SUM(total_amount),0)
                FROM purchases
                WHERE purchase_date >= CURRENT_DATE
                AND purchase_date < CURRENT_DATE + INTERVAL '1 day'
                """));

        summary.setTotalProfit(getDouble("""
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
                """));

        summary.setTotalProductsSold(getInt("""
                SELECT COALESCE(
                    SUM(si.quantity),
                    0
                )
                FROM sale_items si
                JOIN sales s
                ON s.sale_id = si.sale_id
                WHERE s.sale_date >= CURRENT_DATE
                AND s.sale_date < CURRENT_DATE + INTERVAL '1 day'
                """));

        summary.setTotalTransactions(getInt("""
                SELECT COUNT(*)
                FROM sales
                WHERE sale_date >= CURRENT_DATE
                AND sale_date < CURRENT_DATE + INTERVAL '1 day'
                """));

        summary.setTotalReturns(getDouble("""
                SELECT COALESCE(SUM(total_refund),0)
                FROM returns
                WHERE return_date >= CURRENT_DATE
                AND return_date < CURRENT_DATE + INTERVAL '1 day'
                """));

        return summary;

    }

    public ReportSummary getDateRangeSummary(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) throw new IllegalArgumentException("Choose a valid date range.");
        LocalDate exclusiveEnd = end.plusDays(1);
        ReportSummary summary = new ReportSummary();
        summary.setTitle(String.format(LanguageManager.get("report.summary.title.range"), start, end));
        summary.setTotalSales(getDouble("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE sale_date >= ? AND sale_date < ?", start, exclusiveEnd));
        summary.setTotalPurchases(getDouble("SELECT COALESCE(SUM(total_amount),0) FROM purchases WHERE purchase_date >= ? AND purchase_date < ?", start, exclusiveEnd));
        summary.setTotalProfit(getDouble("""
                SELECT COALESCE(SUM((si.selling_price - si.cost_price) * si.quantity),0)
                FROM sale_items si JOIN sales s ON s.sale_id=si.sale_id
                WHERE s.sale_date >= ? AND s.sale_date < ?""", start, exclusiveEnd));
        summary.setTotalProductsSold(getInt("""
                SELECT COALESCE(SUM(si.quantity),0) FROM sale_items si JOIN sales s ON s.sale_id=si.sale_id
                WHERE s.sale_date >= ? AND s.sale_date < ?""", start, exclusiveEnd));
        summary.setTotalTransactions(getInt("SELECT COUNT(*) FROM sales WHERE sale_date >= ? AND sale_date < ?", start, exclusiveEnd));
        summary.setTotalReturns(getDouble(
                "SELECT COALESCE(SUM(total_refund),0) FROM returns WHERE return_date >= ? AND return_date < ?",
                start, exclusiveEnd));
        return summary;
    }

    public List<TopSellingProduct> getTopSellingProducts(LocalDate start, LocalDate end, int limit) {

        boolean filtered = start != null && end != null;

        String sql = filtered ? """
                SELECT p.name, SUM(si.quantity) AS quantity_sold, SUM(si.quantity * si.selling_price) AS revenue
                FROM sale_items si
                JOIN sales s ON s.sale_id = si.sale_id
                JOIN products p ON p.product_id = si.product_id
                WHERE s.sale_date >= ? AND s.sale_date < ?
                GROUP BY p.name
                ORDER BY quantity_sold DESC
                LIMIT ?
                """ : """
                SELECT p.name, SUM(si.quantity) AS quantity_sold, SUM(si.quantity * si.selling_price) AS revenue
                FROM sale_items si
                JOIN products p ON p.product_id = si.product_id
                GROUP BY p.name
                ORDER BY quantity_sold DESC
                LIMIT ?
                """;

        List<TopSellingProduct> products = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (filtered) {
                statement.setObject(1, start);
                statement.setObject(2, end.plusDays(1));
                statement.setInt(3, limit);
            } else {
                statement.setInt(1, limit);
            }

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
            throw new RuntimeException("Unable to load top selling products.", e);
        }

        return products;
    }

    private int getInt(String sql) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

        return 0;

    }

    private double getDouble(String sql) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

        return 0;

    }

    private int getInt(String sql, LocalDate start, LocalDate end) {
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, start); statement.setObject(2, end);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private double getDouble(String sql, LocalDate start, LocalDate end) {
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, start); statement.setObject(2, end);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

}
