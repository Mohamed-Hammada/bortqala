@echo off
title Bemo ERP Backend (DEV)
echo ========================================================
echo Starting Bemo ERP Spring Boot Backend - DEV (Port 8080)
echo   Database : bemo_erp_dev (localhost:5432)
echo   Profile  : dev
echo   Demo no-login SUPER_ADMIN link: ENABLED
echo ========================================================
echo.

set "JAVA_HOME=C:\Users\wolfn\scoop\apps\openjdk26\current"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ==== Spring profile (dev) ====
set "SPRING_PROFILES_ACTIVE=dev"

rem ==== Database (DEV - never share this with the prod script) ====
set "DB_URL=jdbc:postgresql://localhost:5432/bemo_erp_dev"
set "DB_USERNAME=root"
set "DB_PASSWORD=root"

rem Allow both local Angular and the Cloudflare domain
set "HR_CORS_ALLOWED_ORIGINS=http://localhost:4200,http://127.0.0.1:4200,https://app.eysawy.dpdns.org"

rem Respect X-Forwarded-Proto and other Cloudflare proxy headers
set "SERVER_FORWARD_HEADERS_STRATEGY=framework"

set "HR_BOOTSTRAP_APP_CODE=DEMO"
set "HR_BOOTSTRAP_APP_NAME=Bemo ERP"
set "HR_BOOTSTRAP_ADMIN_USERNAME=admin"
set "HR_BOOTSTRAP_ADMIN_PASSWORD=Admin@12345"
set "HR_BOOTSTRAP_SUPER_ADMIN_USERNAME=superadmin"
set "HR_BOOTSTRAP_SUPER_ADMIN_PASSWORD=SuperSameh@12345"
set "HR_JWT_SECRET=local-development-jwt-secret-key-32bytes-minimum"
set "HR_DEVICE_CREDENTIALS_SECRET=ZGV2aWNlLWNyZWRlbnRpYWxzLTMyLWJ5dGVzLWtleSE="
rem HR_JWT_SECRET / HR_DEVICE_CREDENTIALS_SECRET are also fine to leave unset here;
rem the dev profile has fallbacks. Override these values for local testing.

rem ==== Demo no-login SUPER_ADMIN link (DEV only) ====
rem Adds a password-less SUPER_ADMIN dashboard link for the DEMO app.
rem The link is only active when one of the running profiles matches
rem HR_DEMO_NO_LOGIN_PROFILES (a comma-separated allow-list). The secret is
rem printed in the backend console at startup. Leave HR_DEMO_SECRET unset to get
rem a fresh random secret on every run.
set "HR_DEMO_NO_LOGIN_ENABLED=true"
set "HR_DEMO_NO_LOGIN_APP_CODE=DEMO"
set "HR_DEMO_NO_LOGIN_APP_NAME=Bemo ERP"
set "HR_DEMO_NO_LOGIN_PROFILES=dev"
rem set "HR_DEMO_SECRET=optional-fixed-secret"

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
