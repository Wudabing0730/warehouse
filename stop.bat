@echo off
chcp 65001 >nul
title 仓库管理系统 - 停止服务

echo.
echo   ╔══════════════════════════════════════════════════╗
echo   ║          仓库管理系统 - 停止所有服务               ║
echo   ╚══════════════════════════════════════════════════╝
echo.

echo   正在停止后端服务 (Spring Boot)...
taskkill /fi "WINDOWTITLE eq WMS-Backend*" /f 2>nul
taskkill /fi "WINDOWTITLE eq WMS后端*" /f 2>nul

:: 也杀掉占用的端口进程
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    echo   释放端口 8080 (PID: %%a)
    taskkill /pid %%a /f 2>nul
)

echo   正在停止前端服务 (Vite)...
taskkill /fi "WINDOWTITLE eq WMS-Frontend*" /f 2>nul
taskkill /fi "WINDOWTITLE eq WMS前端*" /f 2>nul

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING" 2^>nul') do (
    echo   释放端口 5173 (PID: %%a)
    taskkill /pid %%a /f 2>nul
)

echo.
echo   所有服务已停止。
echo.
pause
