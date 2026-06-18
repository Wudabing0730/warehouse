@echo off
chcp 65001 >nul
title 仓库管理系统 - 停止服务

echo.
echo   ╔══════════════════════════════════════════════════╗
echo   ║          仓库管理系统 - 停止所有服务               ║
echo   ╚══════════════════════════════════════════════════╝
echo.

echo   正在停止 WMS 后端服务...
taskkill /fi "WINDOWTITLE eq WMS-Backend*" /f 2>nul

echo   正在停止 WMS 前端服务...
taskkill /fi "WINDOWTITLE eq WMS-Frontend*" /f 2>nul

echo   释放端口 8080 / 5173...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f 2>nul
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f 2>nul
)

echo.
echo   [√] 所有 WMS 服务已停止。
echo.
echo   按任意键关闭...
pause >nul
