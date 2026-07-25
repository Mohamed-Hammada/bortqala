@echo off
title Bemo HR Platform Launcher
echo ========================================================
echo           BEMO HR & OPERATIONS PLATFORM
echo ========================================================
echo Launching Backend, Frontend, and Cloudflare Tunnel...
echo ========================================================
echo.

:: 1. Launch Backend in a new window
echo [1/3] Starting Backend (Spring Boot)...
start "Bemo HR Backend" cmd /k "%~dp0start-backend.bat"

:: 2. Launch Frontend in a new window
echo [2/3] Starting Frontend (Angular)...
start "Bemo HR Frontend" cmd /k "%~dp0start-frontend.bat"

:: 3. Launch Cloudflare Tunnel in a new window
if exist "%~dp0cloudflared-windows-amd64.exe" (
    echo [3/3] Starting Cloudflare Tunnel...
    start "Bemo Cloudflare Tunnel" cmd /k "%~dp0start-tunnel.bat"
)

echo.
echo All services launched!
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:4200
echo.
echo Opening browser...
timeout /t 5 >nul
start http://localhost:4200

echo.
echo Press any key to exit launcher window...
pause >nul
