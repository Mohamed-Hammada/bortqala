@echo off
setlocal
title Bemo ERP Backend (GraalVM JVM)

if not defined GRAALVM_HOME (
  echo [ERROR] Set GRAALVM_HOME to a GraalVM JDK 21 or newer.
  exit /b 1
)
if not exist "%GRAALVM_HOME%\bin\java.exe" (
  echo [ERROR] "%GRAALVM_HOME%\bin\java.exe" was not found.
  exit /b 1
)

set "JAVA_HOME=%GRAALVM_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
if not defined SPRING_PROFILES_ACTIVE set "SPRING_PROFILES_ACTIVE=dev"

echo [INFO] Runtime: 
"%JAVA_HOME%\bin\java.exe" -version
pushd "%~dp0be"
call gradlew.bat bootJar
if errorlevel 1 (
  popd
  exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -Xms512m -Xmx2g -jar build\libs\bemo-erp-0.0.1-SNAPSHOT.jar %*
set "BEMO_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %BEMO_EXIT_CODE%
