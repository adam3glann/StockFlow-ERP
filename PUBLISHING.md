# Build and publish StockFlow for Windows

## Local release build

Use Windows x64, a Java 21 JDK with `jpackage`, Maven 3.9+, and WiX Toolset 3 on `PATH` to create an MSI. From the project root:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\scan-secrets.ps1
.\scripts\build-windows-release.ps1 -InstallerType msi
```

The script runs `mvn clean verify`, stages runtime dependencies, and creates a self-contained app image, a portable ZIP, an MSI, and a deployment-tools ZIP under `target/release`. `-InstallerType exe` builds an EXE instead; `-InstallerType app-image` creates only the ZIP. The package includes its Java runtime and does not require Java on customer workstations. It does require PostgreSQL on the branch server.

Before distributing, inspect `target/release`, install the MSI on a clean Windows test machine, configure it against a disposable PostgreSQL database, exercise first-admin setup and a basic sale and purchase, and verify uninstall/reinstall retains the database. Never include `config/database.properties`, `.env`, passwords, or database backups in release assets. The workflow currently produces unsigned installers, so Windows may show an unknown-publisher warning; a trusted code-signing certificate and protected CI secrets are needed to sign releases under a verified publisher name.

## GitHub release

Push a version tag such as `v1.0.1`. The Windows release workflow builds and tests the tagged source, creates the MSI and portable ZIP, and attaches them with the Windows installation guide and branch connection script to a GitHub Release. Review the generated release and downloaded assets before sharing the release link. GitHub Actions needs repository `contents: write` permission to publish.

## Release limits

- This workflow currently targets Windows x64 only.
- Each branch uses its own PostgreSQL database. There is no headquarters consolidation or synchronization service.
- The app connects directly to PostgreSQL. Secure the branch network and database role; an internet deployment should put an authenticated application/API service between clients and the database.
- Tax defaults to 0% and must be set by the branch administrator according to local accounting guidance.
