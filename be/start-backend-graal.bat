@echo off
REM WP-18 T-1: GraalVM launcher script for Bemo ERP backend
REM Probes GRAALVM_HOME env variable and common install paths.
REM Sets JAVA_HOME and starts the application.

setlocal enabledelayedexpansion

echo [GraalVM Launcher] Probing GraalVM installation...

REM Check GRAALVM_HOME environment variable
if defined GRAALVM_HOME (
    echo [GraalVM Launcher] Found GRAALVM_HOME=%GRAALVM_HOME%
    set "JAVA_HOME=%GRAALVM_HOME%"
    goto :start
)

REM Check common Windows install paths
set "GRAALVM_PATHS=C:\graalvm C:\tools\graalvm C:\Program Files\GraalVM %USERPROFILE%\.sdkman\candidates\graalvm\current"

for %%P in (%GRAALVM_PATHS%) do (
    if exist "%%P\bin\java.exe" (
        echo [GraalVM Launcher] Found GraalVM at %%P
        set "JAVA_HOME=%%P"
        goto :start
    )
)

REM Check if sdkman is available
where sdk >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('sdk home graalvm 2^>nul') do (
        if exist "%%i\bin\java.exe" (
            echo [GraalVM Launcher] Found GraalVM via sdkman at %%i
            set "JAVA_HOME=%%i"
            goto :start
        )
    )
)

echo.
echo [GraalVM Launcher] ERROR: GraalVM not found.
echo.
echo To use this script, install GraalVM and set GRAALVM_HOME:
echo   set GRAALVM_HOME=C:\path\to\graalvm
echo.
echo Or install via SDKMAN (Linux/Mac/WSL):
echo   sdk install java 21.0.2-graalce
echo.
echo To build a native image (requires GraalVM):
echo   cd be
echo   .\gradlew.bat nativeCompile
echo   .\build\native\images\bemo-erp
echo.
echo Starting with standard JDK instead...
set "JAVA_HOME="
goto :start

:start
echo [GraalVM Launcher] JAVA_HOME=%JAVA_HOME%
echo [GraalVM Launcher] Starting Bemo ERP backend...
cd /d "%~dp0"
.\gradlew.bat bootRun
