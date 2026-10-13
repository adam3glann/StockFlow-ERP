package com.adam.warehouse.util;

import com.adam.warehouse.config.DatabaseInitializer;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectSafetyTest {

    @Test
    void englishAndArabicBundlesHaveMatchingKeysAndCountWorkflowTranslations() throws Exception {
        Properties english = load(Path.of("src/main/resources/languages/messages_en.properties"));
        Properties arabic = load(Path.of("src/main/resources/languages/messages_ar.properties"));
        assertEquals(english.stringPropertyNames(), arabic.stringPropertyNames());
        for (String key : List.of(
                "inventory.audit.title", "inventory.count.button", "inventory.count.resolve",
                "inventory.count.status.resolved", "permission.action.resolve.a.stock.variance",
                "report.metric.gross.profit.after.refunds", "error.message.sale.total.is.outside.the.supported.amount.range",
                "admin.setup.title", "admin.setup.password.hint", "admin.setup.failed")) {
            assertFalse(english.getProperty(key).startsWith("!"), key);
            assertFalse(arabic.getProperty(key).startsWith("!"), key);
        }
    }

    @Test
    void schemaDoesNotShipAnAdministratorAccountOrPasswordHash() throws Exception {
        String schema = Files.readString(Path.of("config/schema.sql"));
        assertFalse(schema.contains("INSERT INTO employees"));
        assertFalse(schema.contains("pbkdf2$"));
        String app = Files.readString(Path.of("src/main/java/com/adam/warehouse/app/WarehouseApplication.java"));
        assertTrue(app.contains("InitialAdminSetupView"));
    }

    @Test
    void localCredentialsAndIdeWorkspaceAreExcludedFromGit() throws Exception {
        String ignore = Files.readString(Path.of(".gitignore"));
        assertTrue(ignore.contains("/config/database.properties"));
        assertTrue(ignore.contains("/.idea/"));
        assertTrue(ignore.contains(".env.*"));
        assertFalse(Files.readString(Path.of("config/database.properties.example"))
                .matches("(?s).*(?i:password)\\s*=\\s*(?!replace-with)[^\\r\\n]{10,}.*"));
    }

    @Test
    void databaseDriverDetailsAreReplacedWithASafeUserMessage() {
        String message = LanguageManager.message("jdbc:postgresql://db.example:5432/stockflow: password authentication failed");
        assertTrue(message.equals(LanguageManager.get("database.error.generic")));
        assertFalse(message.contains("jdbc:"));
        assertFalse(message.contains("db.example"));
    }

    @Test
    void schemaParserKeepsDollarQuotedMigrationBlocksIntact() throws Exception {
        Method splitStatements = DatabaseInitializer.class.getDeclaredMethod("splitStatements", String.class);
        splitStatements.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> statements = (List<String>) splitStatements.invoke(null,
                "DO $$ BEGIN PERFORM 1; PERFORM 2; END $$; SELECT 3;");
        assertEquals(2, statements.size());
        assertTrue(statements.getFirst().contains("PERFORM 1; PERFORM 2;"));
    }

    private Properties load(Path path) throws Exception {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        return properties;
    }
}
