@echo off
title Subir TaskMaster a GitHub
cd /d "%~dp0"
echo =======================================================
echo          SUBIENDO PROYECTO A GITHUB (manicri) 🚀
echo =======================================================
echo.
git branch -M main
git add .
git commit -m "Update TaskMaster full project"
git push -u origin main
echo.
if %ERRORLEVEL% EQU 0 (
    echo [EXITO] Proyecto subido correctamente a GitHub!
) else (
    echo [AVISO] Si el repositorio no existe, crealo primero en:
    echo https://github.com/new con el nombre 'taskmaster-app'
)
echo.
pause
