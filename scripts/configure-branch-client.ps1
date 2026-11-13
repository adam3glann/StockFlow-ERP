param(
    [string]$HostName,
    [string]$DatabaseName = 'stockflow_erp',
    [ValidateRange(1, 65535)]
    [int]$Port = 5432,
    [string]$DatabaseUser
)

$ErrorActionPreference = 'Stop'
$appDirectory = if ($env:APPDATA) { Join-Path $env:APPDATA 'StockFlow' } else { Join-Path $env:USERPROFILE '.stockflow' }
$configFile = Join-Path $appDirectory 'database.properties'
$passwordPointer = [IntPtr]::Zero

try {
    if (!$HostName) { $HostName = Read-Host 'Branch PostgreSQL server name or private network address' }
    if (!$DatabaseUser) { $DatabaseUser = Read-Host 'StockFlow database username' }
    if ($HostName -notmatch '^[A-Za-z0-9.-]+$' -and $HostName -notmatch '^\[[0-9A-Fa-f:]+\]$') {
        throw 'The server name or IPv4/IPv6 address is invalid.'
    }
    if ($DatabaseName -notmatch '^[A-Za-z0-9_.-]+$') { throw 'The database name is invalid.' }
    if (!$DatabaseUser -or $DatabaseUser.Contains("`n") -or $DatabaseUser.Contains("`r")) { throw 'The database username is invalid.' }

    $secureInput = Read-Host 'StockFlow database password' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureInput)
    $secretText = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    if ([string]::IsNullOrEmpty($secretText)) { throw 'The database password cannot be empty.' }

    $escapeValue = { param([string]$Value) $Value.Replace('\', '\\').Replace(' ', '\ ').Replace('=', '\=').Replace(':', '\:').Replace('#', '\#').Replace('!', '\!').Replace("`t", '\t').Replace("`r", '\r').Replace("`n", '\n').Replace("`f", '\f') }
    $jdbcHost = if ($HostName.StartsWith('[')) { $HostName } else { $HostName }
    $lines = @(
        'url=jdbc:postgresql://{0}:{1}/{2}' -f $jdbcHost, $Port, $DatabaseName,
        'username={0}' -f (& $escapeValue $DatabaseUser),
        'password={0}' -f (& $escapeValue $secretText)
    )
    New-Item -ItemType Directory -Path $appDirectory -Force | Out-Null
    $temporaryFile = $configFile + '.tmp'
    [IO.File]::WriteAllText($temporaryFile, ($lines -join "`n") + "`n", [Text.UTF8Encoding]::new($false))

    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $acl = [Security.AccessControl.FileSecurity]::new()
    $acl.SetOwner($identity.User)
    $acl.SetAccessRuleProtection($true, $false)
    $rule = [Security.AccessControl.FileSystemAccessRule]::new(
        $identity.User, [Security.AccessControl.FileSystemRights]::FullControl,
        [Security.AccessControl.AccessControlType]::Allow)
    [void]$acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $temporaryFile -AclObject $acl
    Move-Item -LiteralPath $temporaryFile -Destination $configFile -Force
    Write-Output "StockFlow connection saved for database '$DatabaseName' at '$HostName'."
    Write-Output 'Restart StockFlow to connect. The password is stored in this Windows account profile.'
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    $secretText = $null
    $secureInput = $null
    if (Test-Path -LiteralPath ($configFile + '.tmp')) {
        Remove-Item -LiteralPath ($configFile + '.tmp') -Force -ErrorAction SilentlyContinue
    }
}
