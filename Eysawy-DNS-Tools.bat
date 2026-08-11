@echo off
setlocal
title Eysawy DNS Tools

set "SCRIPT_DIR=%~dp0"
set "FIX_SCRIPT=%SCRIPT_DIR%Fix-Eysawy-DNS.ps1"
set "RESET_SCRIPT=%SCRIPT_DIR%Reset-Eysawy-DNS-To-DHCP.ps1"

:MENU
cls
echo ============================================================
echo                  Eysawy DNS Tools
echo ============================================================
echo.
echo   [1] Fix Eysawy / Cloudflare DNS
echo       - Set Cloudflare IPv4 + IPv6 DNS
echo       - Flush DNS cache
echo       - Restart network adapter
echo       - Test app.eysawy.dpdns.org
echo.
echo   [2] Reset DNS to DHCP / Automatic
echo       - Restore default DNS supplied by router/network
echo.
echo   [3] Exit
echo.
echo ============================================================
choice /C 123 /N /M "Select an option [1-3]: "

if errorlevel 3 goto :EOF
if errorlevel 2 goto RESET
if errorlevel 1 goto FIX

:FIX
cls
echo Running DNS repair...
echo.

if not exist "%FIX_SCRIPT%" (
    echo ERROR: File not found:
    echo "%FIX_SCRIPT%"
    echo.
    echo Keep this BAT file in the same folder as:
    echo Fix-Eysawy-DNS.ps1
    echo.
    pause
    goto MENU
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%FIX_SCRIPT%"
goto AFTER

:RESET
cls
echo Resetting DNS to DHCP / automatic settings...
echo.

if not exist "%RESET_SCRIPT%" (
    echo ERROR: File not found:
    echo "%RESET_SCRIPT%"
    echo.
    echo Keep this BAT file in the same folder as:
    echo Reset-Eysawy-DNS-To-DHCP.ps1
    echo.
    pause
    goto MENU
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%RESET_SCRIPT%"
goto AFTER

:AFTER
echo.
echo ============================================================
echo Finished.
echo ============================================================
echo.
choice /C YN /N /M "Return to menu? [Y/N]: "
if errorlevel 2 goto :EOF
if errorlevel 1 goto MENU
