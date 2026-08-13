@echo off
setlocal
call "%~dp0desktop\build-native.bat"
exit /b %ERRORLEVEL%
