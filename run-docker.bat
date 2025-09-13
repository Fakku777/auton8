@echo off
echo Starting Auton8 stack...
cd /d %~dp0

:: Bring up containers in detached mode
docker compose up -d

echo.
echo Auton8 services are now running.
echo - Mosquitto on 127.0.0.1:1883
echo - n8n editor on http://localhost:5678
pause
