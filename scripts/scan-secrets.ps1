$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $root
try {
    $trackedOrPublishable = @(git ls-files --cached --others --exclude-standard)
    $trackedIgnored = @(git ls-files --cached --ignored --exclude-standard)
    $sensitiveTracked = $trackedIgnored | Where-Object {
        $_ -match '(^|/)(database\.properties|\.env(?:\..*)?|.*\.(?:pem|key|p12|pfx|keystore)|.*\.(?:dump|backup|bak|sqlite|db))$'
    }
    if ($sensitiveTracked.Count -gt 0) {
        $sensitiveTracked | ForEach-Object { Write-Output "TRACKED_IGNORED_FILE $_" }
    }

    $patterns = @(
        @{ Name = 'private-key'; Regex = '-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----' },
        @{ Name = 'cloud-access-key'; Regex = '\bAKIA[0-9A-Z]{16}\b' },
        @{ Name = 'github-token'; Regex = '\b(?:gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{30,})\b' },
        @{ Name = 'credential-assignment'; Regex = '(?i)(?:password|passwd|secret|api[_-]?key|client[_-]?secret|access[_-]?token)\s*[:=]\s*["'']?([^\s"''<>]{8,})' }
    )
    $findings = [System.Collections.Generic.List[string]]::new()
    $knownDefaultPassword = [string]::Concat('admin', '123')

    function Scan-Lines([string]$relative, [string[]]$lines) {
        $normalized = $relative -replace '\\', '/'
        if ($normalized -eq 'scripts/scan-secrets.ps1') { return }
        if ($normalized -match '(^|/)(\.git|target|build|dist)/' -or $normalized -match '\.(?:png|jpe?g|gif|ico|pdf|jar|class|zip|7z|exe|dll|so|dylib|woff2?|ttf|otf)$') { return }
        $lineNumber = 0
        foreach ($line in $lines) {
            $lineNumber++
            if ($line.Contains($knownDefaultPassword)) {
                $findings.Add("$relative`:$lineNumber [default application credential]")
            }
            if ($line -match '(?i)pbkdf2\$\d{5,}\$') {
                $findings.Add("$relative`:$lineNumber [password hash]")
            }
            foreach ($pattern in $patterns) {
                $match = [regex]::Match($line, $pattern.Regex)
                if (-not $match.Success) { continue }
                if ($pattern.Name -eq 'credential-assignment') {
                    # In Java, assignments commonly copy password fields or call the
                    # hasher; scan only literal assignments, which are the hard-coded risk.
                    if ($normalized -match '\.java$' -and $line -notmatch '(?i)(?:password|passwd|secret|api[_-]?key|client[_-]?secret|access[_-]?token)\s*[:=]\s*["'']') { continue }
                    $value = $match.Groups[1].Value.TrimEnd(',', ';')
                    if ($value -match '(?i)^(?:replace[-_ ]?with|your[-_ ]|changeme|change[-_ ]?me|placeholder|\$\{|<|\*+|password$|secret$|none$|null$)') { continue }
                    if ($value.Length -lt 10) { continue }
                }
                $findings.Add("$relative`:$lineNumber [$($pattern.Name)]")
            }
        }
    }

    foreach ($relative in $trackedOrPublishable) {
        $path = Join-Path $root $relative
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
        $item = Get-Item -LiteralPath $path
        if ($item.Length -gt 1MB) { continue }
        Scan-Lines $relative ([System.IO.File]::ReadAllLines($path))
    }

    # Check staged snapshots too: a staged file can differ from the working copy.
    $stagedFiles = @(git diff --cached --name-only --diff-filter=ACMRTUXB)
    foreach ($relative in $stagedFiles) {
        $stagedLines = @(git show ":$relative" 2>$null)
        if ($LASTEXITCODE -eq 0) { Scan-Lines $relative $stagedLines }
    }

    $allFindings = @($findings | Sort-Object -Unique)
    if ($sensitiveTracked.Count -gt 0 -or $allFindings.Count -gt 0) {
        $allFindings | ForEach-Object { Write-Output $_ }
        Write-Error 'Potential sensitive data found. Review the listed locations; matched values are intentionally withheld.'
    }
    Write-Output "Secret scan passed ($($trackedOrPublishable.Count) tracked or publishable working-tree files and $($stagedFiles.Count) staged snapshots inspected). Ignored local config is excluded from Git's publishable file list."
} finally {
    Pop-Location
}
