param([switch]$Audit,[string]$Pack)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskJava = Join-Path $env:USERPROFILE '.gradle\jdks\azul_systems__inc_-21-amd64-windows.2'
if (Test-Path -LiteralPath "$taskJava\bin\java.exe") { $env:JAVA_HOME = $taskJava }
$env:GRADLE_USER_HOME = Join-Path (Split-Path $taskRoot -Parent) '.gradle-user'
Push-Location -LiteralPath $taskRoot
try {
    & .\gradlew.bat --console=plain build
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($Audit) {
        if (-not $Pack) { throw 'Use -Audit -Pack <resource-pack.zip>' }
        & .\gradlew.bat --console=plain auditPack "-Ppack=$Pack"
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
} finally { Pop-Location }
