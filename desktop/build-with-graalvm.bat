@echo off
setlocal
title Bemo ERP Desktop Build (GraalVM)

if not defined GRAALVM_HOME (
  echo [ERROR] Set GRAALVM_HOME to a GraalVM JDK 21 or newer with jlink.
  exit /b 1
)
if not exist "%GRAALVM_HOME%\bin\jlink.exe" (
  echo [ERROR] "%GRAALVM_HOME%\bin\jlink.exe" was not found.
  exit /b 1
)

set "JAVA_HOME=%GRAALVM_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
pushd "%~dp0"
call npm run build
set "BEMO_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %BEMO_EXIT_CODE%
