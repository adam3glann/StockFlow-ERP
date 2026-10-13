package com.adam.warehouse.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {

    private static final Path LOCAL_CONFIG_PATH = Path.of("config", "database.properties");
    private static final Properties LOCAL_CONFIG = loadLocalConfiguration();

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("stockflow.db.url", "url", "STOCKFLOW_DB_URL");
        if (url == null) {
            String host = setting("stockflow.db.host", "host", "DB_HOST");
            String port = setting("stockflow.db.port", "port", "DB_PORT");
            String database = setting("stockflow.db.name", "name", "DB_NAME");
            if (host != null && !host.matches(".*[/@?#\\s].*")
                    && port != null && port.matches("\\d{1,5}")
                    && Integer.parseInt(port) > 0 && Integer.parseInt(port) <= 65535
                    && database != null && database.matches("[A-Za-z0-9_.-]+")) {
                url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            }
        }
        if (url == null || !url.startsWith("jdbc:postgresql://")
                || url.matches("(?is).*([?&](?:password|user)=|jdbc:postgresql://[^/]*@).*")) {
            throw new SQLException("Missing or invalid database URL. Set STOCKFLOW_DB_URL or DB_HOST, DB_PORT, and DB_NAME.");
        }
        return DriverManager.getConnection(
                url,
                requiredSetting("stockflow.db.username", "username", "STOCKFLOW_DB_USERNAME", "DB_USER"),
                requiredSetting("stockflow.db.password", "password", "STOCKFLOW_DB_PASSWORD", "DB_PASSWORD")
        );
    }

    private static Properties loadLocalConfiguration() {
        Properties properties = new Properties();

        if (Files.notExists(LOCAL_CONFIG_PATH)) {
            return properties;
        }

        try (InputStream inputStream = Files.newInputStream(LOCAL_CONFIG_PATH)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read the local database configuration file.", exception
            );
        }
    }

    private static String requiredSetting(String systemProperty, String propertyName,
                                          String... environmentVariables) throws SQLException {
        String value = setting(systemProperty, propertyName, environmentVariables);
        if (value == null) {
            throw new SQLException("Missing database setting '" + propertyName
                    + "'. Set the documented environment variable or create config/database.properties.");
        }
        return value;
    }

    private static String setting(String systemProperty, String propertyName,
                                  String... environmentVariables) {
        String value = System.getProperty(systemProperty);
        if (value != null && !value.isBlank()) return value.trim();
        for (String environmentVariable : environmentVariables) {
            value = System.getenv(environmentVariable);
            if (value != null && !value.isBlank()) return value.trim();
        }
        if (value == null || value.isBlank()) {
            value = LOCAL_CONFIG.getProperty(propertyName);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }
}
