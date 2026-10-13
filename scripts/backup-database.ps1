param(
    [string]$OutputFile
)

. (Join-Path $PSScriptRoot 'PostgresConfig.ps1')
$ErrorActionPreference = 'Stop'
$passwordFile = $null
$outputMayBePartial = $false

try {
    $database = Get-StockFlowDatabaseSettings
    $pgDump = Get-Command 'pg_dump.exe' -ErrorAction SilentlyContinue
    if (!$pgDump) { $pgDump = Get-Command 'pg_dump' -ErrorAction SilentlyContinue }
    if (!$pgDump) { throw 'pg_dump was not found. Install the PostgreSQL client tools on the branch server.' }

    if (!$OutputFile) {
        $backupFolder = Join-Path $database.ProjectRoot 'backups'
        New-Item -ItemType Directory -Path $backupFolder -Force | Out-Null
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $OutputFile = Join-Path $backupFolder ("stockflow-{0}-{1}.dump" -f $database.Name, $stamp)
    } else {
        $OutputFile = [IO.Path]::GetFullPath($OutputFile)
        New-Item -ItemType Directory -Path (Split-Path -Parent $OutputFile) -Force | Out-Null
    }
    if (Test-Path -LiteralPath $OutputFile) { throw 'The backup destination already exists. Choose a new file name.' }

    $passwordFile = New-StockFlowPasswordFile $database
    $env:PGPASSFILE = $passwordFile
    $arguments = @(
        '--no-password', '--format=custom', '--no-owner', '--no-privileges',
        '--host', $database.Host, '--port', $database.Port,
        '--username', $database.User, '--dbname', $database.Name,
        '--file', $OutputFile
    )
    $outputMayBePartial = $true
    & $pgDump.Source @arguments
    if ($LASTEXITCODE -ne 0) { throw "pg_dump exited with code $LASTEXITCODE." }
    $outputMayBePartial = $false
    Write-Output "Backup completed: $OutputFile"
} catch {
    if ($outputMayBePartial -and $OutputFile -and (Test-Path -LiteralPath $OutputFile)) {
        Remove-Item -LiteralPath $OutputFile -Force -ErrorAction SilentlyContinue
    }
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    Remove-Item Env:PGPASSFILE -ErrorAction SilentlyContinue
    if ($passwordFile) { Remove-Item -LiteralPath $passwordFile -Force -ErrorAction SilentlyContinue }
}
