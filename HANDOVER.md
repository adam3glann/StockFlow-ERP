# StockFlow project handover

## Current project state

StockFlow is a Java 21, JavaFX, Maven, JDBC, and PostgreSQL desktop inventory application. Features include role-based access, products and barcode entry, purchasing and sales, customer/supplier returns, stock movement history, physical stock counts, reporting, activity history, CSV/PDF exports, Arabic localization, and light/dark themes.

## Security changes

- Removed the shared administrator account and password hash from the public schema. A new database prompts for one-time administrator setup. The first administrator password is chosen locally and stored using PBKDF2. Existing employee accounts are not overwritten.
- Database credentials are supplied via ignored `config/database.properties` or environment variables. Copy `config/database.properties.example` for local setup; it contains placeholders only. Use a dedicated PostgreSQL role, not a superuser.
- Startup errors do not display JDBC exception text. This avoids disclosing connection details in dialogs.
- `.gitignore` excludes local credentials, IDE workspace files, logs, exports, backups, and generated builds. A local PowerShell scan is available at `scripts/scan-secrets.ps1`.
- A prior handover contained an actual local database password. That value has been removed from the current checkout. Treat it as exposed and rotate it if it is still valid.

## Build and verification

Run `mvn clean package` to compile and execute tests. Run `scripts/scan-secrets.ps1` before publishing. Schema migrations and transaction workflows should also be verified against a disposable PostgreSQL database. Do not point tests or migration verification at a production database.

## Git publication status

At the security-audit start, the repository had no commits and no configured remotes. Consequently no prior local commit history was available to inspect. Confirm this again before the first push. If any other clone or remote ever contained credentials, remove them from that history through coordinated history rewriting and rotate those credentials.
