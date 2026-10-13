Set-StrictMode -Version Latest

function Get-StockFlowDatabaseSettings {
    param([string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path)

    $fileSettings = @{}
    $filePath = Join-Path $ProjectRoot 'config/database.properties'
    if (Test-Path -LiteralPath $filePath) {
        foreach ($line in Get-Content -LiteralPath $filePath) {
            if ($line -match '^\s*([A-Za-z0-9_.-]+)\s*=\s*(.*)$' -and $Matches[1] -notmatch '^(#|!)') {
                $fileSettings[$Matches[1]] = $Matches[2].Trim()
            }
        }
    }

    $dbUrl = if ($env:STOCKFLOW_DB_URL) { $env:STOCKFLOW_DB_URL } else { $fileSettings['url'] }
    $dbHostName = if ($env:DB_HOST) { $env:DB_HOST } else { $null }
    $dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { '5432' }
    $dbName = if ($env:DB_NAME) { $env:DB_NAME } else { $null }

    if ($dbUrl) {
        if ($dbUrl -notmatch '^jdbc:postgresql://(?<authority>[^/]+)/(?<database>[^?]+)') {
            throw 'The configured PostgreSQL URL is not supported by the backup scripts.'
        }
        $authority = $Matches['authority']
        $dbName = $Matches['database']
        if ($authority -match '^(?<host>\[[^]]+\]|[^:]+)(:(?<port>\d+))?$') {
            $dbHostName = $Matches['host'].Trim('[', ']')
            if ($Matches['port']) { $dbPort = $Matches['port'] }
        } else {
            throw 'The configured PostgreSQL host or port could not be parsed.'
        }
    }

    $dbUser = if ($env:STOCKFLOW_DB_USERNAME) { $env:STOCKFLOW_DB_USERNAME }
        elseif ($env:DB_USER) { $env:DB_USER } else { $fileSettings['username'] }
    $dbPassword = if ($env:STOCKFLOW_DB_PASSWORD) { $env:STOCKFLOW_DB_PASSWORD }
        elseif ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { $fileSettings['password'] }

    if (!$dbHostName -or !$dbName -or !$dbUser -or !$dbPassword) {
        throw 'Set the documented database environment variables or config/database.properties before running this script.'
    }
    if ($dbName -notmatch '^[A-Za-z0-9_.-]+$' -or $dbPort -notmatch '^\d{1,5}$') {
        throw 'The configured database name or port is invalid.'
    }

    return [pscustomobject]@{
        Host = $dbHostName
        Port = $dbPort
        Name = $dbName
        User = $dbUser
        Password = $dbPassword
        ProjectRoot = $ProjectRoot
    }
}

function New-StockFlowPasswordFile {
    param([Parameter(Mandatory)]$DatabaseSettings)

    $path = Join-Path ([IO.Path]::GetTempPath()) ("stockflow-pgpass-" + [guid]::NewGuid().ToString('N'))
    $escapeField = { param([string]$Value) $Value.Replace('\', '\\').Replace(':', '\:') }
    $line = "{0}:{1}:{2}:{3}:{4}" -f (& $escapeField $DatabaseSettings.Host),
        (& $escapeField $DatabaseSettings.Port), (& $escapeField $DatabaseSettings.Name),
        (& $escapeField $DatabaseSettings.User), (& $escapeField $DatabaseSettings.Password)
    [IO.File]::WriteAllText($path, $line, [Text.UTF8Encoding]::new($false))
    try {
        $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
        $acl = [Security.AccessControl.FileSecurity]::new()
        $acl.SetOwner($identity.User)
        $acl.SetAccessRuleProtection($true, $false)
        $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new(
            $identity.User, [Security.AccessControl.FileSystemRights]::FullControl,
            [Security.AccessControl.AccessControlType]::Allow))
        Set-Acl -LiteralPath $path -AclObject $acl
    } catch {
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        throw 'Could not restrict access to the temporary PostgreSQL password file.'
    }
    return $path
}
