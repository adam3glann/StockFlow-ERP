package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    private static final String INSERT = """
            INSERT INTO activity_logs
            (employee_id,
             employee_name,
             module,
             action,
             description,
             timestamp)
            VALUES (?,?,?,?,?,?)
            """;

    private static final String GET_ALL = """
            SELECT *
            FROM activity_logs
            ORDER BY timestamp DESC
            """;

    private static final String GET_BY_EMPLOYEE = """
            SELECT *
            FROM activity_logs
            WHERE employee_id = ?
            ORDER BY timestamp DESC
            """;

    private static final String SEARCH = """
            SELECT *
            FROM activity_logs
            WHERE
                LOWER(employee_name) LIKE LOWER(?)
                OR LOWER(module) LIKE LOWER(?)
                OR LOWER(action) LIKE LOWER(?)
                OR LOWER(description) LIKE LOWER(?)
            ORDER BY timestamp DESC
            """;

    public void save(ActivityLog log) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT)) {

            statement.setInt(1, log.getEmployeeId());
            statement.setString(2, log.getEmployeeName());
            statement.setString(3, log.getModule());
            statement.setString(4, log.getAction());
            statement.setString(5, log.getDescription());
            statement.setTimestamp(
                    6,
                    Timestamp.valueOf(log.getTimestamp())
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to save activity log.",
                    e
            );

        }

    }

    public List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(GET_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                logs.add(map(rs));

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load activity logs.",
                    e
            );

        }

        return logs;

    }

    public List<ActivityLog> getEmployeeLogs(int employeeId) {

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(GET_BY_EMPLOYEE)) {

            statement.setInt(1, employeeId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    logs.add(map(rs));

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load employee logs.",
                    e
            );

        }

        return logs;

    }

    public List<ActivityLog> searchLogs(String keyword) {

        List<ActivityLog> logs = new ArrayList<>();

        String search = "%" + keyword + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SEARCH)) {

            statement.setString(1, search);
            statement.setString(2, search);
            statement.setString(3, search);
            statement.setString(4, search);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    logs.add(map(rs));

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to search activity logs.",
                    e
            );

        }

        return logs;

    }

    private ActivityLog map(ResultSet rs)
            throws SQLException {

        ActivityLog log = new ActivityLog();

        log.setLogId(
                rs.getInt("log_id"));

        log.setEmployeeId(
                rs.getInt("employee_id"));

        log.setEmployeeName(
                rs.getString("employee_name"));

        log.setModule(
                rs.getString("module"));

        log.setAction(
                rs.getString("action"));

        log.setDescription(
                rs.getString("description"));

        Timestamp timestamp =
                rs.getTimestamp("timestamp");

        if (timestamp != null) {

            log.setTimestamp(
                    timestamp.toLocalDateTime());

        }

        return log;

    }

}