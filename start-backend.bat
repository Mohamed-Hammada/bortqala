@echo off
title Bemo ERP Backend (Spring Boot)
echo ========================================================
echo Starting Bemo ERP Spring Boot Backend (Port 8080)...
echo ========================================================
echo.

set "JAVA_HOME=C:\Users\wolfn\scoop\apps\openjdk26\current"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ==== Dev environment (see be/src/main/resources/application-dev.properties) ====
set "SPRING_PROFILES_ACTIVE=dev"
set "HR_BOOTSTRAP_APP_CODE=DEMO"
set "HR_BOOTSTRAP_APP_NAME=Bemo ERP"
set "HR_BOOTSTRAP_ADMIN_USERNAME=admin"
set "HR_BOOTSTRAP_ADMIN_PASSWORD=Admin@12345"
set "HR_BOOTSTRAP_SUPER_ADMIN_USERNAME=superadmin"
set "HR_BOOTSTRAP_SUPER_ADMIN_PASSWORD=SuperAdmin@12345"
set "HR_JWT_SECRET=local-development-jwt-secret-key-32bytes-minimum"
set "HR_DEVICE_CREDENTIALS_SECRET=ZGV2aWNlLWNyZWRlbnRpYWxzLTMyLWJ5dGVzLWtleSE="
rem HR_JWT_SECRET / HR_DEVICE_CREDENTIALS_SECRET are also fine to leave unset here;
rem the dev profile has fallbacks. Override these values for local testing.

cd /d "%~dp0be"

call gradlew.bat bootJar
if errorlevel 1 (
 echo.
 echo [ERROR] Backend build failed. The previous JAR will NOT be started.
 echo Fix the build errors above, then run this file again.
 pause
 exit /b 1
)

if not exist "build\libs\bemo-erp-0.0.1-SNAPSHOT.jar" (
 echo.
 echo [ERROR] Backend JAR was not created.
 pause
 exit /b 1
)

echo.
echo [OK] Backend build completed successfully.
echo [INFO] Starting the newly built JAR. Runtime errors will appear below.
echo.

java ^
 -Xms512m ^
 -Xmx2g ^
 -jar build\libs\bemo-erp-0.0.1-SNAPSHOT.jar

set "BACKEND_EXIT_CODE=%ERRORLEVEL%"
if not "%BACKEND_EXIT_CODE%"=="0" (
 echo.
 echo [ERROR] Backend stopped with exit code %BACKEND_EXIT_CODE%.
) else (
 echo.
 echo [INFO] Backend stopped normally.
)

pause
exit /b %BACKEND_EXIT_CODE%
