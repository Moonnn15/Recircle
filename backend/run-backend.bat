@echo off
title ReCircle Backend
cd /d "%~dp0"
echo ===================================================
echo Starting ReCircle Spring Boot Backend (Port 8080)
echo ===================================================

if not exist "target\recircle-backend-1.0.0.jar" (
    echo Building backend JAR package...
    call .\mvnw.cmd package -DskipTests
)

echo Launching ReCircle Application...
java -jar target\recircle-backend-1.0.0.jar
pause
