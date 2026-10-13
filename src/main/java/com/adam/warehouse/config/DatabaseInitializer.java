package com.adam.warehouse.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Applies idempotent schema upgrades and a non-overwriting administrator seed at startup. */
public final class DatabaseInitializer {
    private static final Path SCHEMA_PATH = Path.of("config", "schema.sql");

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException, IOException {
        if (Files.notExists(SCHEMA_PATH)) {
            throw new IOException("Database schema was not found at " + SCHEMA_PATH.toAbsolutePath());
        }

        String schema = Files.readString(SCHEMA_PATH);
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            for (String command : splitStatements(schema)) {
                String sql = command.strip();
                boolean containsSql = sql.lines().anyMatch(line ->
                        !line.isBlank() && !line.stripLeading().startsWith("--"));
                if (containsSql) {
                    statement.execute(sql);
                }
            }
        }
    }

    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;
        String dollarQuote = null;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';

            if (lineComment) {
                current.append(ch);
                if (ch == '\n' || ch == '\r') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                current.append(ch);
                if (ch == '*' && next == '/') {
                    current.append(next);
                    i++;
                    blockComment = false;
                }
                continue;
            }
            if (dollarQuote != null) {
                if (script.startsWith(dollarQuote, i)) {
                    current.append(dollarQuote);
                    i += dollarQuote.length() - 1;
                    dollarQuote = null;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (singleQuoted) {
                current.append(ch);
                if (ch == '\'' && next == '\'') {
                    current.append(next);
                    i++;
                } else if (ch == '\'') {
                    singleQuoted = false;
                }
                continue;
            }
            if (doubleQuoted) {
                current.append(ch);
                if (ch == '"' && next == '"') {
                    current.append(next);
                    i++;
                } else if (ch == '"') {
                    doubleQuoted = false;
                }
                continue;
            }

            if (ch == '-' && next == '-') {
                current.append(ch).append(next);
                i++;
                lineComment = true;
            } else if (ch == '/' && next == '*') {
                current.append(ch).append(next);
                i++;
                blockComment = true;
            } else if (ch == '\'') {
                current.append(ch);
                singleQuoted = true;
            } else if (ch == '"') {
                current.append(ch);
                doubleQuoted = true;
            } else if (ch == '$') {
                int tagEnd = script.indexOf('$', i + 1);
                if (tagEnd >= 0 && script.substring(i + 1, tagEnd).matches("[A-Za-z_][A-Za-z_0-9]*|")) {
                    dollarQuote = script.substring(i, tagEnd + 1);
                    current.append(dollarQuote);
                    i = tagEnd;
                } else {
                    current.append(ch);
                }
            } else if (ch == ';') {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        if (!current.isEmpty()) {
            statements.add(current.toString());
        }
        return statements;
    }
}
