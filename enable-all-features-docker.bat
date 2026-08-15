@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Bemo ERP - Enable All Features

if not defined POSTGRES_USER set "POSTGRES_USER=root"
if not defined POSTGRES_DB set "POSTGRES_DB=bemo_erp"
if not defined BEMO_FEATURE_BOOTSTRAP_WAIT_ATTEMPTS set "BEMO_FEATURE_BOOTSTRAP_WAIT_ATTEMPTS=60"

echo [INFO] Waiting for Liquibase to create tenant feature tables...
set /a ATTEMPT=1

:wait_for_tables
set "READY="
for /f "usebackq delims=" %%R in (`docker compose exec -T db psql -U "%POSTGRES_USER%" -d "%POSTGRES_DB%" -tAc "SELECT (to_regclass('public.tenant_features') IS NOT NULL AND to_regclass('public.system_settings') IS NOT NULL);" 2^>nul`) do (
    set "READY=%%R"
)
set "READY=!READY: =!"
if /I "!READY!"=="t" goto apply_features

if !ATTEMPT! GEQ %BEMO_FEATURE_BOOTSTRAP_WAIT_ATTEMPTS% (
    echo [ERROR] tenant_features/system_settings were not ready in time.
    exit /b 1
)

set /a ATTEMPT+=1
timeout /t 2 /nobreak >nul
goto wait_for_tables

:apply_features
echo [INFO] Applying one-time enable-all entitlement bootstrap...
docker compose exec -T db psql -v ON_ERROR_STOP=1 -U "%POSTGRES_USER%" -d "%POSTGRES_DB%" < "%~dp0scripts\enable-all-features.sql"
if errorlevel 1 (
    echo [ERROR] Enable-all entitlement bootstrap failed.
    exit /b 1
)

echo [OK] Entitlement bootstrap completed ^(or was already applied^).
exit /b 0
