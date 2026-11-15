@echo off
title ReCircle Frontend
cd /d "%~dp0"
echo ===================================================
echo Starting ReCircle Frontend (Port 5173)
echo ===================================================
if not exist "node_modules\" (
    echo Installing npm dependencies...
    call npm install
)
call npm run dev
pause
