param(
    [ValidateSet('app-image', 'msi', 'exe')]
    [string]$InstallerType = 'msi',
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$pom = Join-Path $projectRoot 'pom.xml'
$artifactId = 'WarehouseManagementSystem'
$version = ([xml](Get-Content -LiteralPath $pom -Raw)).project.version
$jarName = "$artifactId-$version.jar"
$target = Join-Path $projectRoot 'target'
$inputDirectory = Join-Path $target 'package-input'
$releaseDirectory = Join-Path $target 'release'
$maven = Get-Command 'mvn.cmd' -ErrorAction SilentlyContinue
if (!$maven) { $maven = Get-Command 'mvn' -ErrorAction SilentlyContinue }
if (!$maven) { throw 'Maven 3.9+ was not found. Install Maven and use a Java 21 JDK.' }
$javaHome = $env:JAVA_HOME
if (!$javaHome) { throw 'JAVA_HOME must point to a Java 21 JDK that includes jpackage.' }
$jpackage = Join-Path $javaHome 'bin/jpackage.exe'
if (!(Test-Path -LiteralPath $jpackage)) { throw "jpackage was not found at '$jpackage'. Use a full Java 21 JDK, not a JRE." }

Push-Location $projectRoot
try {
    $verifyArgs = @('clean', 'verify')
    if ($SkipTests) { $verifyArgs += '-DskipTests' }
    & $maven.Source @verifyArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven verification failed with exit code $LASTEXITCODE." }

    New-Item -ItemType Directory -Path (Join-Path $inputDirectory 'lib') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $target $jarName) -Destination $inputDirectory -Force
    & $maven.Source 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies' `
        "-DoutputDirectory=$(Join-Path $inputDirectory 'lib')" -DincludeScope=runtime
    if ($LASTEXITCODE -ne 0) { throw "Dependency packaging failed with exit code $LASTEXITCODE." }

    New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
    $common = @(
        '--name', 'StockFlow', '--app-version', $version,
        '--description', 'StockFlow branch inventory and sales management',
        '--vendor', 'StockFlow', '--input', $inputDirectory,
        '--main-jar', $jarName,
        '--main-class', 'com.adam.warehouse.app.WarehouseApplication',
        '--java-options', '-Dfile.encoding=UTF-8'
    )
    $imageDirectory = Join-Path $releaseDirectory 'app-image'
    New-Item -ItemType Directory -Path $imageDirectory -Force | Out-Null
    & $jpackage '--type' 'app-image' @common '--dest' $imageDirectory
    if ($LASTEXITCODE -ne 0) { throw "jpackage app-image failed with exit code $LASTEXITCODE." }
    Compress-Archive -Path (Join-Path $imageDirectory 'StockFlow') `
        -DestinationPath (Join-Path $releaseDirectory "StockFlow-$version-windows-x64.zip") -Force
    if ($InstallerType -ne 'app-image') {
        $installerArgs = @('--type', $InstallerType) + $common + @('--win-menu', '--win-shortcut', '--win-per-user-install')
        $installerArgs += @('--dest', $releaseDirectory)
        & $jpackage @installerArgs
        if ($LASTEXITCODE -ne 0) {
            throw "jpackage $InstallerType failed. MSI/EXE builds need WiX Toolset 3 on PATH; see the publishing guide. Exit code $LASTEXITCODE."
        }
        Write-Output "Installer created in '$releaseDirectory'."
    }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'configure-branch-client.ps1') -Destination $releaseDirectory -Force
    Copy-Item -LiteralPath (Join-Path $projectRoot 'INSTALL-WINDOWS.md') -Destination $releaseDirectory -Force
    $deploymentScripts = Join-Path $releaseDirectory 'deployment-tools'
    New-Item -ItemType Directory -Path (Join-Path $deploymentScripts 'scripts') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'INSTALL-WINDOWS.md') -Destination $deploymentScripts -Force
    Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1' | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $deploymentScripts 'scripts') -Force
    }
    Compress-Archive -Path $deploymentScripts -DestinationPath `
        (Join-Path $releaseDirectory "StockFlow-$version-deployment-tools.zip") -Force
    Write-Output "Portable package: $(Join-Path $releaseDirectory "StockFlow-$version-windows-x64.zip")"
} finally {
    Pop-Location
}
