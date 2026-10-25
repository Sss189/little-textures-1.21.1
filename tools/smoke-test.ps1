$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskJava = Join-Path $env:USERPROFILE '.gradle\jdks\azul_systems__inc_-21-amd64-windows.2'
if (Test-Path -LiteralPath "$taskJava\bin\java.exe") { $env:JAVA_HOME = $taskJava }
$env:GRADLE_USER_HOME = Join-Path (Split-Path $taskRoot -Parent) '.gradle-user'
Push-Location -LiteralPath $taskRoot
try {
    & .\gradlew.bat --console=plain runClient -Psmoke
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $taskLog = Get-Content -LiteralPath 'run\logs\latest.log' -Raw
    if ($taskLog -notmatch 'LTTEXTURES_SMOKE_PASS' -or $taskLog -match 'LTTEXTURES_SMOKE_FAIL') {
        throw 'Client smoke assertions failed. Inspect run/logs/latest.log.'
    }
    Write-Output 'Client smoke assertions passed.'
} finally { Pop-Location }
