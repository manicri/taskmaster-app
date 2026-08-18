@echo off
title Servidor TaskMaster - Sincronizacion Web & Android
cd /d "%~dp0"
echo =======================================================
echo           INICIANDO SERVIDOR TASKMASTER 🚀
echo =======================================================
echo.
if not exist node_modules (
    echo Instalando dependencias de Node.js...
    call npm install
)
echo.
echo Iniciando servicio en el puerto 3000...
node server.js
pause
