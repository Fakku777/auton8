@echo off
echo Stopping Auton8 stack...
cd /d %~dp0
docker compose down
pause
