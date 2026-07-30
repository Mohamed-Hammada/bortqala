@echo off
title Bemo ERP Platform - Docker Deployment
echo ========================================================
echo       BEMO ERP PLATFORM - DOCKER ONE-CLICK DEPLOY
echo ========================================================
echo.

if not exist ".env" (
    echo [.env] file not found. Copying .env.example to .env ...
    copy .env.example .env
)

echo Building and starting Docker containers (PostgreSQL, Backend, Frontend)...
docker compose up --build -d

echo.
echo ========================================================
echo  Deployment complete!
echo  Application Web UI:  http://localhost
echo  Spring Boot API:     http://localhost:8080
echo ========================================================
echo.
pause
