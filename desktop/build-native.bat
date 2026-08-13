@echo off
setlocal
title Bemo ERP Native Desktop Build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build-native.ps1"
exit /b %ERRORLEVEL%
