@echo off
title Bemo HR Backend (Spring Boot)
echo ========================================================
echo Starting Bemo HR Spring Boot Backend (Port 8080)...
echo ========================================================
echo.

set "JAVA_HOME=C:\Users\wolfn\scoop\apps\openjdk26\current"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0be"

call gradlew.bat bootJar
if errorlevel 1 (
 echo.
 echo [ERROR] Backend build failed. The previous JAR will NOT be started.
 echo Fix the build errors above, then run this file again.
 pause
 exit /b 1
)

if not exist "build\libs\hr-platform-0.0.1-SNAPSHOT.jar" (
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
 -jar build\libs\hr-platform-0.0.1-SNAPSHOT.jar ^
 --spring.profiles.active=dev

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
