@echo off
title Bemo HR Backend (Spring Boot)
echo ========================================================
echo Starting Bemo HR Spring Boot Backend (Port 8080)...
echo ========================================================
echo.

set "JAVA_HOME=C:\Users\wolfn\scoop\apps\openjdk26\current"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0be"

call gradlew bootJar

java ^
 -Xms512m ^
 -Xmx2g ^
 -jar build\libs\hr-platform-0.0.1-SNAPSHOT.jar ^
 --spring.profiles.active=dev



pause
