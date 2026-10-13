param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$BackupFile
)

. (Join-Path $PSScriptRoot 'PostgresConfig.ps1')
$ErrorActionPreference = 'Stop'
$passwordFile = $null

try {
    $BackupFile = (Resolve-Path -LiteralPath $BackupFile).Path
    if ([IO.Path]::GetExtension($BackupFile) -ne '.dump') {
        throw 'Choose a StockFlow custom-format .dump backup created with backup-database.ps1.'
    }
    $database = Get-StockFlowDatabaseSettings
    $pgRestore = Get-Command 'pg_restore.exe' -ErrorAction SilentlyContinue
    if (!$pgRestore) { $pgRestore = Get-Command 'pg_restore' -ErrorAction SilentlyContinue }
    if (!$pgRestore) { throw 'pg_restore was not found. Install the PostgreSQL client tools on the branch server.' }

    Write-Warning "This will replace database objects in '$($database.Name)'. Stop StockFlow clients and make a fresh backup first."
    $confirmation = Read-Host "Type the exact database name '$($database.Name)' to continue"
    if ($confirmation -cne $database.Name) { throw 'Confirmation did not match. No database changes were made.' }

    $passwordFile = New-StockFlowPasswordFile $database
    $env:PGPASSFILE = $passwordFile
    $arguments = @(
        '--no-password', '--exit-on-error', '--clean', '--if-exists',
        '--no-owner', '--no-privileges',
        '--host', $database.Host, '--port', $database.Port,
        '--username', $database.User, '--dbname', $database.Name,
        $BackupFile
    )
    & $pgRestore.Source @arguments
    if ($LASTEXITCODE -ne 0) { throw "pg_restore exited with code $LASTEXITCODE. Review PostgreSQL output; restore may be incomplete." }
    Write-Output "Restore completed into database '$($database.Name)'."
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    Remove-Item Env:PGPASSFILE -ErrorAction SilentlyContinue
    if ($passwordFile) { Remove-Item -LiteralPath $passwordFile -Force -ErrorAction SilentlyContinue }
}
