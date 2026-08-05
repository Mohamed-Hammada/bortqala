@echo off
title Bemo ERP Backend (PROD)
echo ========================================================
echo Starting Bemo ERP Spring Boot Backend - PROD (Port 8080)
echo   Database : bemo_erp_prod (localhost:5432)
echo   Profile  : prod
echo   Demo no-login SUPER_ADMIN link: DISABLED
echo ========================================================
echo.

set "JAVA_HOME=C:\Users\wolfn\scoop\apps\openjdk26\current"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ==== Spring profile (prod - no secret fallbacks, fails fast) ====
set "SPRING_PROFILES_ACTIVE=prod"

rem ==== Database (PROD - never share this with the dev script) ====
set "DB_URL=jdbc:postgresql://localhost:5432/bemo_erp_prod"
set "DB_USERNAME=root"
set "DB_PASSWORD=root"

rem Allow both local Angular and the Cloudflare domain
set "HR_CORS_ALLOWED_ORIGINS=http://localhost:4200,http://127.0.0.1:4200,https://app.eysawy.dpdns.org"

rem Respect X-Forwarded-Proto and other Cloudflare proxy headers
set "SERVER_FORWARD_HEADERS_STRATEGY=framework"

rem ==== Bootstrap accounts (change these before any real use) ====
set "HR_BOOTSTRAP_APP_CODE=DEMO"
set "HR_BOOTSTRAP_APP_NAME=Bemo ERP"
set "HR_BOOTSTRAP_ADMIN_USERNAME=admin"
set "HR_BOOTSTRAP_ADMIN_PASSWORD=Admin@12345"
set "HR_BOOTSTRAP_SUPER_ADMIN_USERNAME=superadmin"
set "HR_BOOTSTRAP_SUPER_ADMIN_PASSWORD=SuperAdmin@12345"

rem ==== Secrets - the prod profile has NO fallbacks, so these are required ====
set "HR_JWT_SECRET=local-development-jwt-secret-key-32bytes-minimum"
set "HR_DEVICE_CREDENTIALS_SECRET=ZGV2aWNlLWNyZWRlbnRpYWxzLTMyLWJ5dGVzLWtleSE="
set "HR_JWT_ISSUER=bemo-erp-prod"
set "HR_COMPANY_ZONE=Africa/Cairo"
rem WARNING: override HR_JWT_SECRET and HR_DEVICE_CREDENTIALS_SECRET with unique
rem production values. Keep them out of version control.

rem ==== Demo no-login SUPER_ADMIN link: explicitly DISABLED in prod ====
rem The demo link must never be available on the production database. Even if
rem HR_DEMO_NO_LOGIN_ENABLED were turned on here, the default profiles
rem allow-list (test) would not match the prod profile, so the link stays dead.
set "HR_DEMO_NO_LOGIN_ENABLED=false"

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
