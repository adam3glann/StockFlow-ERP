package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private static final String INSERT = """
            INSERT INTO customers
            (full_name, phone, email, address, active)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE customers
            SET full_name = ?,
                phone = ?,
                email = ?,
                address = ?,
                active = ?
            WHERE customer_id = ?
            """;

    private static final String DEACTIVATE = """
            UPDATE customers
            SET active = FALSE
            WHERE customer_id = ?
            """;

    private static final String GET_ALL = """
            SELECT *
            FROM customers
            WHERE active = TRUE
            ORDER BY full_name
            """;

    private static final String GET_BY_ID = """
            SELECT *
            FROM customers
            WHERE customer_id = ?
            """;

    private static final String SEARCH = """
            SELECT *
            FROM customers
            WHERE active = TRUE
            AND (
                    LOWER(full_name) LIKE LOWER(?)
                    OR phone LIKE ?
                    OR LOWER(email) LIKE LOWER(?)
            )
            ORDER BY full_name
            """;

    public void addCustomer(Customer customer) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {

            fillStatement(statement, customer);

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException("Failed to add customer.", e);

        }

    }

    public void updateCustomer(Customer customer) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            fillStatement(statement, customer);

            statement.setInt(6, customer.getCustomerId());

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException("Failed to update customer.", e);

        }

    }

    public void deactivateCustomer(int customerId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DEACTIVATE)) {

            statement.setInt(1, customerId);

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException("Failed to deactivate customer.", e);

        }

    }

    public Customer getCustomerById(int customerId) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_BY_ID)) {

            statement.setInt(1, customerId);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return map(rs);
                }

            }

        } catch (SQLException e) {

            throw new RuntimeException("Failed to load customer.", e);

        }

        return null;

    }

    public List<Customer> getAllCustomers() {

        List<Customer> customers = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                customers.add(map(rs));

            }

        } catch (SQLException e) {

            throw new RuntimeException("Failed to load customers.", e);

        }

        return customers;

    }

    public List<Customer> searchCustomers(String keyword) {

        List<Customer> customers = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH)) {

            String search = "%" + keyword + "%";

            statement.setString(1, search);
            statement.setString(2, search);
            statement.setString(3, search);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    customers.add(map(rs));

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException("Failed to search customers.", e);

        }

        return customers;

    }

    private void fillStatement(PreparedStatement statement,
                               Customer customer) throws SQLException {

        statement.setString(1, customer.getFullName());
        statement.setString(2, customer.getPhone());
        statement.setString(3, customer.getEmail());
        statement.setString(4, customer.getAddress());
        statement.setBoolean(5, customer.isActive());

    }

    private Customer map(ResultSet rs) throws SQLException {

        Customer customer = new Customer();

        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setFullName(rs.getString("full_name"));
        customer.setPhone(rs.getString("phone"));
        customer.setEmail(rs.getString("email"));
        customer.setAddress(rs.getString("address"));
        customer.setActive(rs.getBoolean("active"));

        return customer;

    }

}