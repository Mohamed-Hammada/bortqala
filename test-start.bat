@echo off
set "JAVA_HOME=C:\Users\wolfn\scoop\apps\openjdk26\current"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0be"
call gradlew.bat --version
