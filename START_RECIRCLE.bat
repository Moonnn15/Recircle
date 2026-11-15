@echo off
title Start ReCircle Full-Stack Application
echo ===================================================
echo Launching ReCircle Application
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo ===================================================

start "ReCircle Backend" cmd /k "cd /d %~dp0backend && run-backend.bat"
start "ReCircle Frontend" cmd /k "cd /d %~dp0frontend && run-frontend.bat"

echo Both services have been launched in separate windows!
echo Once they start, open http://localhost:5173 in your browser.
pause
