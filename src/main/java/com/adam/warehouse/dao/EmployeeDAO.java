package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.enums.UserRole;
import com.adam.warehouse.model.Employee;
import com.adam.warehouse.util.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    /** Returns whether the configured database already has an active administrator. */
    public boolean hasActiveAdministrator() {
        String sql = "SELECT EXISTS (SELECT 1 FROM employees WHERE role = 'ADMIN' AND active = TRUE)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() && result.getBoolean(1);
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to check administrator setup.", exception);
        }
    }

    /** Creates the first administrator only, serialized against concurrent first-run setup. */
    public boolean createInitialAdministrator(Employee employee) {
        String lockSql = "SELECT pg_advisory_xact_lock(4815162343)";
        String countSql = "SELECT EXISTS (SELECT 1 FROM employees WHERE role = 'ADMIN' AND active = TRUE)";
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement(lockSql)) {
                    lock.execute();
                }
                try (PreparedStatement check = connection.prepareStatement(countSql);
                     ResultSet result = check.executeQuery()) {
                    if (result.next() && result.getBoolean(1)) {
                        connection.rollback();
                        return false;
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
                    insert.setString(1, employee.getFirstName());
                    insert.setString(2, employee.getLastName());
                    insert.setString(3, employee.getUsername());
                    insert.setString(4, PasswordHasher.hash(employee.getPassword()));
                    insert.setString(5, UserRole.ADMIN.name());
                    insert.setString(6, "");
                    insert.setString(7, null);
                    insert.setString(8, null);
                    insert.setBoolean(9, true);
                    insert.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to create the initial administrator.", exception);
        }
    }

    private static final String INSERT = """
            INSERT INTO employees
            (first_name,last_name,username,password,role,phone,email,notes,active)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;


    private static final String UPDATE = """
            UPDATE employees
            SET first_name=?,
                last_name=?,
                username=?,
                password=COALESCE(?,password),
                role=?,
                phone=?,
                email=?,
                notes=?,
                active=?
            WHERE employee_id=?
            """;


    private static final String GET_ALL = """
            SELECT *
            FROM employees
            ORDER BY employee_id
            """;


    private static final String LOGIN = """
            SELECT *
            FROM employees
            WHERE username=?
            AND active=true
            """;


    private static final String SEARCH = """
            SELECT *
            FROM employees
            WHERE (
                    LOWER(first_name) LIKE LOWER(?)
                    OR LOWER(last_name) LIKE LOWER(?)
                    OR LOWER(username) LIKE LOWER(?)
            )
            ORDER BY first_name,last_name
            """;

    private static final String UPDATE_PASSWORD = """
            UPDATE employees SET password = ? WHERE employee_id = ?
            """;

    private static final String FIND_BY_ID = """
            SELECT * FROM employees WHERE employee_id = ?
            """;

    private static final String UPDATE_PROFILE = """
            UPDATE employees
            SET first_name = ?, last_name = ?, phone = ?, email = ?,
                password = COALESCE(?, password)
            WHERE employee_id = ? AND active = true
            """;


    public void addEmployee(Employee employee) {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT)) {


            statement.setString(1,
                    employee.getFirstName());

            statement.setString(2,
                    employee.getLastName());

            statement.setString(3,
                    employee.getUsername());

            statement.setString(4,
                    PasswordHasher.hash(employee.getPassword()));

            statement.setString(5,
                    employee.getRole().name());

            statement.setString(6,
                    employee.getPhone());

            statement.setString(7,
                    employee.getEmail());

            statement.setString(8,
                    employee.getNotes());

            statement.setBoolean(9,
                    employee.isActive());


            statement.executeUpdate();


        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

    }


    public void updateEmployee(Employee employee) {


        String password = employee.getPassword();


        if (password != null && !password.isBlank()) {

            password = PasswordHasher.hash(password);

        } else {

            password = null;

        }


        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockAdministratorChanges(connection);
                ensureAdministratorRemains(connection, employee.getEmployeeId(), employee.getRole(), employee.isActive());
                try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
                    statement.setString(1, employee.getFirstName());
                    statement.setString(2, employee.getLastName());
                    statement.setString(3, employee.getUsername());
                    statement.setString(4, password);
                    statement.setString(5, employee.getRole().name());
                    statement.setString(6, employee.getPhone());
                    statement.setString(7, employee.getEmail());
                    statement.setString(8, employee.getNotes());
                    statement.setBoolean(9, employee.isActive());
                    statement.setInt(10, employee.getEmployeeId());
                    if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Employee not found.");
                }
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to update employee.", exception);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update employee.", e);
        }

    }


    public void deactivateEmployee(int employeeId) {


        String sql = """
                UPDATE employees
                SET active=false
                WHERE employee_id=?
                """;


        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockAdministratorChanges(connection);
                ensureAdministratorRemains(connection, employeeId, null, false);
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, employeeId);
                    if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Employee not found.");
                }
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to deactivate employee.", exception);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to deactivate employee.", e);
        }

    }

    private void lockAdministratorChanges(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(74014761)")) {
            try (ResultSet ignored = statement.executeQuery()) {
                // The transaction-scoped lock serializes changes that could remove the last admin.
            }
        }
    }

    private void ensureAdministratorRemains(Connection connection, int employeeId,
                                             UserRole nextRole, boolean nextActive) throws SQLException {
        String sql = """
                SELECT role, active FROM employees WHERE employee_id = ? FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            boolean currentlyActiveAdmin;
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return;
                currentlyActiveAdmin = "ADMIN".equals(rs.getString("role")) && rs.getBoolean("active");
            }
            boolean willRemainActiveAdmin = nextRole != null && nextRole == UserRole.ADMIN && nextActive;
            if (currentlyActiveAdmin && !willRemainActiveAdmin
                    && countOtherActiveAdministrators(connection, employeeId) == 0) {
                throw new IllegalArgumentException("At least one active administrator account must remain.");
            }
        }
    }

    private int countOtherActiveAdministrators(Connection connection, int employeeId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM employees WHERE role = 'ADMIN' AND active = TRUE AND employee_id <> ?")) {
            statement.setInt(1, employeeId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    public Employee getEmployeeById(int employeeId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setInt(1, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public List<Employee> getAllEmployees() {


        List<Employee> employees = new ArrayList<>();


        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(GET_ALL);
             ResultSet rs = statement.executeQuery()) {


            while (rs.next()) {

                employees.add(map(rs));

            }


        } catch (SQLException e) {

            throw new RuntimeException(e);

        }


        return employees;

    }


    public Employee login(String username,
                          String password) {


        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(LOGIN)) {


            statement.setString(1, username);


            try (ResultSet rs = statement.executeQuery()) {


                if (!rs.next()) {

                    return null;

                }


                String stored =
                        rs.getString("password");


                if (!PasswordHasher.matches(password, stored)) {

                    return null;

                }

                if (!PasswordHasher.isHashed(stored)) {
                    upgradeLegacyPassword(connection, rs.getInt("employee_id"), password);
                }


                return map(rs);

            }


        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

    }

    private void upgradeLegacyPassword(Connection connection, int employeeId, String password)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD)) {
            statement.setString(1, PasswordHasher.hash(password));
            statement.setInt(2, employeeId);
            statement.executeUpdate();
        }
    }

    /** Updates only the fields an employee may change for their own account. */
    public void updateOwnProfile(Employee employee) {
        String password = employee.getPassword();
        if (password != null && !password.isBlank()) {
            password = PasswordHasher.hash(password);
        } else {
            password = null;
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PROFILE)) {
            statement.setString(1, employee.getFirstName());
            statement.setString(2, employee.getLastName());
            statement.setString(3, employee.getPhone());
            statement.setString(4, employee.getEmail());
            statement.setString(5, password);
            statement.setInt(6, employee.getEmployeeId());
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Your account is no longer active.");
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }


    public List<Employee> searchEmployees(String keyword) {


        List<Employee> employees = new ArrayList<>();


        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SEARCH)) {


            String search = "%" + keyword + "%";


            statement.setString(1, search);
            statement.setString(2, search);
            statement.setString(3, search);


            try (ResultSet rs = statement.executeQuery()) {


                while (rs.next()) {

                    employees.add(map(rs));

                }

            }


        } catch (SQLException e) {

            throw new RuntimeException(e);

        }


        return employees;

    }


    private Employee map(ResultSet rs)
            throws SQLException {


        Employee employee = new Employee();


        employee.setEmployeeId(
                rs.getInt("employee_id")
        );


        employee.setFirstName(
                rs.getString("first_name")
        );


        employee.setLastName(
                rs.getString("last_name")
        );


        employee.setUsername(
                rs.getString("username")
        );


        employee.setRole(
                UserRole.valueOf(
                        rs.getString("role")
                                .toUpperCase()
                )
        );


        employee.setPhone(
                rs.getString("phone")
        );


        employee.setEmail(
                rs.getString("email")
        );


        employee.setNotes(
                rs.getString("notes")
        );


        employee.setActive(
                rs.getBoolean("active")
        );


        Timestamp created =
                rs.getTimestamp("created_at");


        if (created != null) {

            employee.setCreatedAt(
                    created.toLocalDateTime()
            );

        }


        return employee;

    }

}
