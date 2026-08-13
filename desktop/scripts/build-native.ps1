param(
    [string]$GraalVmHome = $env:GRAALVM_HOME
)

$ErrorActionPreference = 'Stop'
$desktopRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$downloadArchive = Join-Path $env:USERPROFILE 'Downloads\graalvm-jdk-25i2-25.0.4_windows-x64_bin.zip'
$downloadExtraction = Join-Path $env:USERPROFILE 'Downloads\graalvm-jdk-25.0.4-extracted'

if (-not $GraalVmHome) {
    $knownHome = Join-Path $downloadExtraction 'graalvm-25.2.4+7.1'
    if (Test-Path -LiteralPath (Join-Path $knownHome 'bin\native-image.cmd')) {
        $GraalVmHome = $knownHome
    } elseif (Test-Path -LiteralPath $downloadArchive) {
        New-Item -ItemType Directory -Force -Path $downloadExtraction | Out-Null
        Expand-Archive -LiteralPath $downloadArchive -DestinationPath $downloadExtraction -Force
        $GraalVmHome = Get-ChildItem -LiteralPath $downloadExtraction -Directory |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\native-image.cmd') } |
            Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $GraalVmHome -or -not (Test-Path -LiteralPath (Join-Path $GraalVmHome 'bin\native-image.cmd'))) {
    throw 'GraalVM Native Image was not found. Set GRAALVM_HOME or place the GraalVM ZIP in Downloads.'
}

$vswhere = 'C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe'
if (-not (Test-Path -LiteralPath $vswhere)) {
    throw 'Visual Studio Build Tools with Desktop development with C++ is required by native-image.'
}
$visualStudio = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $visualStudio) {
    throw 'Install Visual Studio Build Tools and the Desktop development with C++ workload.'
}
$developerShell = Join-Path $visualStudio 'Common7\Tools\Launch-VsDevShell.ps1'
if (-not (Test-Path -LiteralPath $developerShell)) { throw 'Visual Studio developer shell was not found.' }
& $developerShell -Arch amd64 -HostArch amd64 -SkipAutomaticLocation

$env:GRAALVM_HOME = $GraalVmHome
$env:JAVA_HOME = $GraalVmHome
$env:PATH = "$(Join-Path $GraalVmHome 'bin');$env:PATH"
$nativeTemp = Join-Path $desktopRoot 'src-tauri\target\native-image-temp'
New-Item -ItemType Directory -Force -Path $nativeTemp | Out-Null
$env:TEMP = $nativeTemp
$env:TMP = $nativeTemp

Push-Location $desktopRoot
try {
    & npm.cmd run build:native
    if ($LASTEXITCODE -ne 0) { throw 'Native desktop build failed.' }
} finally {
    Pop-Location
}
