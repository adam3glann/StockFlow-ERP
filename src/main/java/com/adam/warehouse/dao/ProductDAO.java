package com.adam.warehouse.dao;

import com.adam.warehouse.config.DatabaseConnection;
import com.adam.warehouse.model.InventoryMovement;
import com.adam.warehouse.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String INSERT = """
            INSERT INTO products
            (barcode,name,description,category_id,
             purchase_price,selling_price,
             quantity,minimum_stock,active)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;

    private static final String UPDATE = """
            UPDATE products
            SET barcode=?,
                name=?,
                description=?,
                category_id=?,
                purchase_price=?,
                selling_price=?,
                minimum_stock=?,
                active=?
            WHERE product_id=?
            """;

    private static final String ADJUST_STOCK = """
            UPDATE products
            SET quantity = quantity + ?
            WHERE product_id = ? AND active = TRUE AND quantity + ? >= 0
            RETURNING name, quantity
            """;

    private final InventoryMovementDAO movementDAO = new InventoryMovementDAO();

    private static final String DEACTIVATE = """
            UPDATE products
            SET active=false
            WHERE product_id=?
            """;

    private static final String GET_ALL = """
            SELECT
                p.*,
                c.name category_name
            FROM products p
            LEFT JOIN categories c
                ON p.category_id=c.category_id
            WHERE p.active=true
            ORDER BY p.name
            """;

    private static final String GET_BY_ID = """
            SELECT
                p.*,
                c.name category_name
            FROM products p
            LEFT JOIN categories c
                ON p.category_id=c.category_id
            WHERE p.product_id=?
            """;

    private static final String GET_BY_BARCODE = """
            SELECT
                p.*,
                c.name category_name
            FROM products p
            LEFT JOIN categories c
                ON p.category_id=c.category_id
            WHERE barcode=?
            AND p.active=true
            """;

    private static final String SEARCH = """
            SELECT
                p.*,
                c.name category_name
            FROM products p
            LEFT JOIN categories c
                ON p.category_id=c.category_id
            WHERE p.active=true
            AND
            (
                LOWER(p.name) LIKE LOWER(?)
                OR LOWER(p.barcode) LIKE LOWER(?)
            )
            ORDER BY p.name
            """;

    public void addProduct(Product product, int employeeId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    fillInsertStatement(statement, product);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Unable to add product.");
                        product.setProductId(keys.getInt(1));
                    }
                }
                InventoryMovement opening = new InventoryMovement(
                        product.getProductId(), product.getName(), "ADJUSTMENT", product.getQuantity(),
                        0, product.getQuantity(), employeeId, null, "Opening stock");
                opening.setNotes("Opening stock recorded when the product was created.");
                movementDAO.record(connection, opening);
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to add product.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to add product.", exception);
        }
    }

    public void updateProduct(Product product){

        try(Connection connection=
                    DatabaseConnection.getConnection();

            PreparedStatement statement=
                    connection.prepareStatement(UPDATE)){

            fillUpdateStatement(statement,product);

            statement.setInt(
                    9,
                    product.getProductId()
            );

            statement.executeUpdate();

        }catch(SQLException e){

            throw new RuntimeException(
                    "Unable to update product.",e);

        }

    }

    public void adjustStock(int productId, int quantityChange, int employeeId, String reason) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String productName;
                int quantityAfter;
                try (PreparedStatement statement = connection.prepareStatement(ADJUST_STOCK)) {
                    statement.setInt(1, quantityChange);
                    statement.setInt(2, productId);
                    statement.setInt(3, quantityChange);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Product is unavailable or the adjustment would make stock negative.");
                        }
                        productName = rs.getString("name");
                        quantityAfter = rs.getInt("quantity");
                    }
                }
                InventoryMovement movement = new InventoryMovement(productId, productName, "ADJUSTMENT",
                        quantityChange, quantityAfter - quantityChange, quantityAfter,
                        employeeId, null, "Manual stock adjustment");
                movement.setNotes(reason);
                movementDAO.record(connection, movement);
                connection.commit();
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Unable to adjust stock.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to adjust stock.", exception);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    public void deactivateProduct(int productId){

        try(Connection connection=
                    DatabaseConnection.getConnection();

            PreparedStatement statement=
                    connection.prepareStatement(DEACTIVATE)){

            statement.setInt(1,productId);

            statement.executeUpdate();

        }catch(SQLException e){

            throw new RuntimeException(
                    "Unable to deactivate product.",e);

        }

    }

    public List<Product> getAllProducts(){

        List<Product> products=
                new ArrayList<>();

        try(Connection connection=
                    DatabaseConnection.getConnection();

            PreparedStatement statement=
                    connection.prepareStatement(GET_ALL);

            ResultSet rs=
                    statement.executeQuery()){

            while(rs.next()){

                products.add(map(rs));

            }

        }catch(SQLException e){

            throw new RuntimeException(e);

        }

        return products;

    }

    public Product getProductById(int productId){

        try(Connection connection=
                    DatabaseConnection.getConnection();

            PreparedStatement statement=
                    connection.prepareStatement(GET_BY_ID)){

            statement.setInt(1,productId);

            try(ResultSet rs=
                        statement.executeQuery()){

                if(rs.next()){

                    return map(rs);

                }

            }

        }catch(SQLException e){

            throw new RuntimeException(e);

        }

        return null;

    }

    public Product getProductByBarcode(String barcode){

        try(Connection connection=
                    DatabaseConnection.getConnection();

            PreparedStatement statement=
                    connection.prepareStatement(GET_BY_BARCODE)){

            statement.setString(1,barcode);

            try(ResultSet rs=
                        statement.executeQuery()){

                if(rs.next()){

                    return map(rs);

                }

            }

        }catch(SQLException e){

            throw new RuntimeException(e);

        }

        return null;

    }
    public List<Product> searchProducts(String keyword) {

        List<Product> products = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(SEARCH)) {

            String search = "%" + keyword + "%";

            statement.setString(1, search);
            statement.setString(2, search);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    products.add(map(rs));

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to search products.", e);

        }

        return products;

    }

    public boolean barcodeExists(String barcode) {

        String sql = """
                SELECT COUNT(*)
                FROM products
                WHERE barcode = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, barcode);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1) > 0;

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

        return false;

    }

    public boolean barcodeExists(String barcode,
                                 int productId) {

        String sql = """
                SELECT COUNT(*)
                FROM products
                WHERE barcode = ?
                AND product_id <> ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, barcode);
            statement.setInt(2, productId);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1) > 0;

                }

            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

        return false;

    }

    private void fillInsertStatement(
            PreparedStatement statement,
            Product product) throws SQLException {

        statement.setString(1,
                product.getBarcode());

        statement.setString(2,
                product.getName());

        statement.setString(3,
                product.getDescription());

        statement.setInt(4,
                product.getCategoryId());

        statement.setDouble(5,
                product.getPurchasePrice());

        statement.setDouble(6,
                product.getSellingPrice());

        statement.setInt(7,
                product.getQuantity());

        statement.setInt(8,
                product.getMinimumStock());

        statement.setBoolean(9,
                product.isActive());

    }

    private void fillUpdateStatement(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getBarcode());
        statement.setString(2, product.getName());
        statement.setString(3, product.getDescription());
        statement.setInt(4, product.getCategoryId());
        statement.setDouble(5, product.getPurchasePrice());
        statement.setDouble(6, product.getSellingPrice());
        statement.setInt(7, product.getMinimumStock());
        statement.setBoolean(8, product.isActive());
    }

    private Product map(ResultSet rs)
            throws SQLException {

        Product product = new Product();

        product.setProductId(
                rs.getInt("product_id"));

        product.setBarcode(
                rs.getString("barcode"));

        product.setName(
                rs.getString("name"));

        product.setDescription(
                rs.getString("description"));

        product.setCategoryId(
                rs.getInt("category_id"));

        product.setCategoryName(
                rs.getString("category_name"));

        product.setPurchasePrice(
                rs.getDouble("purchase_price"));

        product.setSellingPrice(
                rs.getDouble("selling_price"));

        product.setQuantity(
                rs.getInt("quantity"));

        product.setMinimumStock(
                rs.getInt("minimum_stock"));

        product.setActive(
                rs.getBoolean("active"));

        return product;

    }

}
