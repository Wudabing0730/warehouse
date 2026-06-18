@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ============================================================
echo   仓库管理系统 - 一键停止脚本
echo ============================================================
echo.

set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"
set "MYSQL_SERVICE=MySQL80"
set "REDIS_SERVICE=Redis"

:: ============================================================
:: 1. 停止后端服务（Java进程 on port 8080）
:: ============================================================
echo [1/3] 停止后端服务（端口 %BACKEND_PORT%）...

set "BACKEND_KILLED=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%BACKEND_PORT% " ^| findstr "LISTENING"') do (
    set "PID=%%a"
    echo       找到进程 PID: !PID!，正在终止...
    taskkill /F /PID !PID! >nul 2>&1
    if !errorlevel! equ 0 (
        echo       后端服务已停止
        set "BACKEND_KILLED=1"
    ) else (
        echo       进程终止失败，可能需要管理员权限
    )
)

if "!BACKEND_KILLED!"=="0" (
    echo       后端服务未在运行
)

echo.

:: ============================================================
:: 2. 停止前端服务（Node进程 on port 5173）
:: ============================================================
echo [2/3] 停止前端服务（端口 %FRONTEND_PORT%）...

set "FRONTEND_KILLED=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%FRONTEND_PORT% " ^| findstr "LISTENING"') do (
    set "PID=%%a"
    echo       找到进程 PID: !PID!，正在终止...
    taskkill /F /PID !PID! >nul 2>&1
    if !errorlevel! equ 0 (
        echo       前端服务已停止
        set "FRONTEND_KILLED=1"
    )
)

if "!FRONTEND_KILLED!"=="0" (
    echo       前端服务未在运行
)

:: Kill cmd windows spawned by start.bat
echo       清理残留的启动窗口...
for /f "tokens=2" %%a in ('tasklist /fi "windowtitle eq Warehouse-*" /fo list 2^>nul ^| findstr /C:"PID"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo.

:: ============================================================
:: 3. 停止 MySQL 和 Redis 服务（可选）
:: ============================================================
echo [3/3] 检查中间件服务...

set /p "STOP_DB=      是否停止 MySQL 服务？(y/N): "
if /i "!STOP_DB!"=="y" (
    net stop %MYSQL_SERVICE% >nul 2>&1
    if !errorlevel! equ 0 (
        echo       MySQL 服务已停止
    ) else (
        echo       MySQL 服务停止失败或未运行
    )
) else (
    echo       保留 MySQL 服务运行
)

set /p "STOP_REDIS=      是否停止 Redis 服务？(y/N): "
if /i "!STOP_REDIS!"=="y" (
    net stop %REDIS_SERVICE% >nul 2>&1
    if !errorlevel! equ 0 (
        echo       Redis 服务已停止
    ) else (
        echo       Redis 服务停止失败或未运行
    )
) else (
    echo       保留 Redis 服务运行
)

echo.
echo ============================================================
echo   所有服务已停止！
echo ============================================================
pause