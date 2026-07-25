@echo off
title Bemo HR Backend (Spring Boot)
echo ========================================================
echo Starting Bemo HR Spring Boot Backend (Port 8080)...
echo ========================================================
echo.
cd /d "%~dp0be"
call .\gradlew.bat bootRun --args="--spring.profiles.active=dev"
pause
