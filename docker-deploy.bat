@echo off
setlocal EnableExtensions
title Bemo ERP Platform - Docker Deployment
echo ========================================================
echo       BEMO ERP PLATFORM - DOCKER ONE-CLICK DEPLOY
echo ========================================================
echo.

if not exist ".env" (
    echo [.env] file not found. Copying .env.development.example to .env ...
    copy .env.development.example .env
)

rem Load only the deploy-helper switch/database names from .env when they are
rem not already supplied by the caller. Docker Compose still reads .env itself.
if not defined BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY (
    for /f "usebackq tokens=1,* delims==" %%A in (`findstr /B /C:"BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY=" ".env"`) do set "BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY=%%B"
)
if not defined POSTGRES_USER (
    for /f "usebackq tokens=1,* delims==" %%A in (`findstr /B /C:"POSTGRES_USER=" ".env"`) do set "POSTGRES_USER=%%B"
)
if not defined POSTGRES_DB (
    for /f "usebackq tokens=1,* delims==" %%A in (`findstr /B /C:"POSTGRES_DB=" ".env"`) do set "POSTGRES_DB=%%B"
)
if not defined BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY set "BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY=true"

echo Building and starting Docker containers (PostgreSQL, Backend, Frontend)...
docker compose up --build -d
if errorlevel 1 (
    echo [ERROR] Docker deployment failed.
    exit /b 1
)

if /I "%BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY%"=="true" (
    echo.
    echo Applying one-time all-feature entitlement bootstrap...
    call "%~dp0enable-all-features-docker.bat"
    if errorlevel 1 exit /b 1
)

echo.
echo ========================================================
echo  Deployment complete!
echo  Application Web UI:  http://localhost
echo  Spring Boot API:     http://localhost:8080
echo ========================================================
echo.
pause
