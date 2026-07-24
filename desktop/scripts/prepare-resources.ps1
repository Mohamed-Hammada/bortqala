param(
    [string]$PostgresDistributionDir = $env:BEMO_POSTGRES_DISTRIBUTION_DIR
)

$ErrorActionPreference = 'Stop'
$desktopRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repositoryRoot = (Resolve-Path (Join-Path $desktopRoot '..')).Path
$resources = Join-Path $desktopRoot 'src-tauri\resources'
$backendResources = Join-Path $resources 'backend'
$runtimeResources = Join-Path $resources 'runtime'
$postgresResources = Join-Path $resources 'postgres'

New-Item -ItemType Directory -Force -Path $backendResources | Out-Null

Push-Location (Join-Path $repositoryRoot 'be')
try {
    & '.\gradlew.bat' clean bootJar
    if ($LASTEXITCODE -ne 0) { throw 'Spring Boot packaging failed.' }
} finally { Pop-Location }

$jar = Get-ChildItem (Join-Path $repositoryRoot 'be\build\libs\*.jar') | Where-Object Name -NotMatch 'plain' | Select-Object -First 1
if (-not $jar) { throw 'Spring Boot jar was not produced.' }
Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $backendResources 'hr-platform.jar') -Force

if (Test-Path -LiteralPath $runtimeResources) { Remove-Item -LiteralPath $runtimeResources -Recurse -Force }
$jlink = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
if (-not (Test-Path -LiteralPath $jlink)) { throw 'JAVA_HOME must point to JDK 26 with jlink.' }
& $jlink --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.unsupported --strip-debug --no-header-files --no-man-pages --compress zip-6 --output $runtimeResources
if ($LASTEXITCODE -ne 0) { throw 'Java runtime creation failed.' }

if (-not $PostgresDistributionDir) {
    $installed = Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($installed) { $PostgresDistributionDir = $installed.FullName }
}
if (-not $PostgresDistributionDir -or -not (Test-Path (Join-Path $PostgresDistributionDir 'bin\postgres.exe'))) {
    throw 'Set BEMO_POSTGRES_DISTRIBUTION_DIR to an official PostgreSQL Windows binary distribution before packaging.'
}
if (Test-Path -LiteralPath $postgresResources) { Remove-Item -LiteralPath $postgresResources -Recurse -Force }
Copy-Item -LiteralPath $PostgresDistributionDir -Destination $postgresResources -Recurse

Write-Host 'Desktop resources prepared successfully.'
