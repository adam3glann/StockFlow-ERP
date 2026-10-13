param(
    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')]
    [string]$DailyAt = '02:00',
    [string]$TaskName = 'StockFlow Daily PostgreSQL Backup'
)

$backupScript = Join-Path $PSScriptRoot 'backup-database.ps1'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$powerShell = (Get-Command 'powershell.exe' -ErrorAction SilentlyContinue)
if (!$powerShell) { throw 'Windows PowerShell was not found.' }

$taskAction = New-ScheduledTaskAction -Execute $powerShell.Source `
    -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$backupScript`"" `
    -WorkingDirectory $projectRoot
$time = [DateTime]::Today.Add([TimeSpan]::Parse($DailyAt))
$taskTrigger = New-ScheduledTaskTrigger -Daily -At $time
$taskPrincipal = New-ScheduledTaskPrincipal -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType S4U -RunLevel Limited
Register-ScheduledTask -TaskName $TaskName -Action $taskAction -Trigger $taskTrigger `
    -Principal $taskPrincipal -Description 'Creates a StockFlow PostgreSQL custom-format backup each day.' -Force | Out-Null
Write-Output "Scheduled daily StockFlow backup at $DailyAt under the current Windows account."
