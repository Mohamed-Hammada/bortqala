@echo off
title Bemo ERP Frontend (Angular Dev Server)
echo ========================================================
echo Starting Bemo ERP Angular Frontend (Port 4200)...
echo ========================================================
echo.
cd /d "%~dp0fe"
call npm start
pause
