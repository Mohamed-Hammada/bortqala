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
if not defined HR_WEB_PUSH_ENABLED set "HR_WEB_PUSH_ENABLED=true"
if not defined HR_WEB_PUSH_PUBLIC_KEY set "HR_WEB_PUSH_PUBLIC_KEY=BGMHGzdrTGM-gl0Mz9N3Vxa4ikgTuVpBkuRtliKK135FS-TYaCGmUzMSSpfgt4womxH-In5uo9dhg8NowHPBp9c"
if not defined HR_WEB_PUSH_PRIVATE_KEY set "HR_WEB_PUSH_PRIVATE_KEY=jdl2o2sjy6-fXiu4RqynsTTdnfjwbFYbQibGokehV10"
if not defined HR_WEB_PUSH_SUBJECT set "HR_WEB_PUSH_SUBJECT=mailto:admin@bemo-erp.local"
if not defined HR_WEB_PUSH_TTL_SECONDS set "HR_WEB_PUSH_TTL_SECONDS=86400"
echo [INFO] Runtime:
echo [INFO] Entitlement defaults: ALL IMPLEMENTED FEATURES ENABLED
"%JAVA_HOME%\bin\java.exe" -version
pushd "%~dp0be"
call gradlew.bat "-Dorg.gradle.java.home=%JAVA_HOME%" bootJar
if errorlevel 1 (
  popd
  exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -Xms512m -Xmx2g -jar build\libs\bemo-erp-0.0.1-SNAPSHOT.jar %*
set "BEMO_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %BEMO_EXIT_CODE%
