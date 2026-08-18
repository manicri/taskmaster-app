@echo off
title Servidor TaskMaster - Sincronizacion Web & Android
cd /d "%~dp0\TodoApp-Server"
if not exist node_modules (
    echo Instalando dependencias de Node.js...
    call npm install
)
echo =======================================================
echo           INICIANDO SERVIDOR TASKMASTER 🚀
echo =======================================================
echo.
node server.js
pause
