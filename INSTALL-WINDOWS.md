# Install StockFlow on Windows

StockFlow is a desktop client. Each branch needs a PostgreSQL server and database reachable over the branch's private LAN or VPN. The installer includes a Java runtime; it does not include PostgreSQL or customer data.

## Prepare the branch server

1. Install PostgreSQL on a branch server. Keep it behind the branch firewall or VPN; do not expose its port to the public internet.
2. Create one database for this branch and a dedicated StockFlow database role. Use a strong unique password. The role needs schema creation/update rights because StockFlow applies its schema migrations at startup. Do not use the PostgreSQL `postgres` superuser.
3. Allow port 5432 only from approved branch workstations, and configure PostgreSQL authentication for the branch network.
4. Note the server's private DNS name or address, database name, role name, and password. Each isolated branch uses its own database; there is no cross-branch synchronization.

## Install and connect a workstation

1. Download and run `StockFlow-<version>.msi`, or extract the Windows ZIP and run `StockFlow/StockFlow.exe`.
2. Open PowerShell in the folder containing `configure-branch-client.ps1` and run:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\configure-branch-client.ps1 -HostName 192.168.1.20 -DatabaseName stockflow_erp -DatabaseUser stockflow_app
   ```

   Use the branch server's private address and the values created above. The script prompts for the password without echoing it and saves connection settings in the current Windows user's `%APPDATA%\StockFlow\database.properties` with a user-only access rule. The password is stored as plain text in that protected file so JDBC can read it; use a dedicated least-privilege database role and do not share the Windows account. Credentials do not go in the install directory or release files.

3. Start StockFlow. The first startup initializes the schema. If the database has no active administrator, create the first administrator when prompted. Set the branch code, name, location, and tax rate in **Settings → Branch Information**. Tax starts at 0%.
4. Repeat the connection setup on every authorized workstation. Clients using the same database share branch records and identity.

## Backups and upgrades

Install PostgreSQL client tools (`pg_dump` and `pg_restore`) on the branch server or backup workstation. The GitHub release includes `StockFlow-<version>-deployment-tools.zip`, with backup, restore, and scheduled-backup scripts. Configure `STOCKFLOW_DB_URL`, `STOCKFLOW_DB_USERNAME`, and `STOCKFLOW_DB_PASSWORD` for that Windows account, or create `%APPDATA%\StockFlow\database.properties` with the connection setup script. Extract the deployment tools and run `scripts/backup-database.ps1`. Store backups outside the branch server and test restores to a disposable database.

Install a new StockFlow version over the previous one. Business records are stored in PostgreSQL, and user settings remain in `%APPDATA%\StockFlow`. Back up the database before upgrading. Startup applies versioned migrations. Keep the prior installer until the new version has connected successfully.

## System requirements

- Windows 10 or 11, 64-bit.
- Network access to the branch PostgreSQL server.
- Display capable of at least 1280 × 720 for comfortable use.

The Windows release is x64. macOS and Linux installers are not produced by this workflow.
