# StockFlow ERP

StockFlow is a JavaFX and PostgreSQL warehouse-management ERP with role-aware operations for products, categories, suppliers, customers, employees, purchases, sales, inventory, dashboard metrics, activity history, and reports.

## First run

1. Create a PostgreSQL database for StockFlow.
2. Create a dedicated PostgreSQL role with only the permissions StockFlow needs to create/update its schema and data. Do not use the PostgreSQL superuser.
3. Copy `config/database.properties.example` to `config/database.properties` and enter your local connection settings. The local file is excluded by Git. For deployment, use `STOCKFLOW_DB_URL`, `STOCKFLOW_DB_USERNAME`, and `STOCKFLOW_DB_PASSWORD`, or `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`.
4. Start the application with `mvn javafx:run`. The schema is initialized on startup. If no active administrator exists, StockFlow prompts you to create one with a password you choose.

## Security and build

Application passwords use PBKDF2-HMAC-SHA256. No shared administrator account or password is seeded. Schema startup preserves existing employee credentials and business rows. The application uses parameterized SQL, session-bound transactions, database-backed session validity checks, and service-layer permission checks.

The project targets Java 21. Build it using a Java 21+ JDK and Maven 3.9+:

```text
mvn clean package
mvn test
```

The JUnit tests cover transaction models, return and profit calculations, permissions, and project safety. Live PostgreSQL transaction and rollback tests require a dedicated disposable test database; the test suite never connects to the configured business database.

## Before publishing

Run `scripts/scan-secrets.ps1` from PowerShell before committing or pushing. It checks tracked and untracked repository files for common secret patterns and sensitive local files without sending code anywhere. Review Git history separately if a credential was committed in any earlier clone or remote; deleting a value from the current files does not remove it from prior commits. Rotate any real credential that was ever committed.

## Language and appearance

Open **Settings** to select English or Arabic and toggle dark mode. Arabic uses right-to-left screen layout; restart StockFlow after changing language so every open screen is rebuilt in the selected language. Dark mode is applied to the application windows and dialogs. Invoice and report CSV/PDF exports use the selected language; CSV files include a UTF-8 signature for spreadsheet compatibility.

## Included workflows

StockFlow includes barcode scanning and product labels, low-stock/reorder reporting, opening stock and audited stock adjustments, movement history, software stock audits, physical count sessions with variance investigation, sales and purchase entry, customer and supplier returns, role-based access, activity history, dashboard metrics, and CSV/PDF invoice and report exports. See [FEATURE_GUIDE.md](FEATURE_GUIDE.md) for operator steps and known audit limitations.
