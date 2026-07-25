@echo off
title Bemo HR Frontend (Angular Dev Server)
echo ========================================================
echo Starting Bemo HR Angular Frontend (Port 4200)...
echo ========================================================
echo.
cd /d "%~dp0fe"
call npm start
pause
