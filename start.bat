@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
::  仓库管理系统 (WMS) — 一键启动脚本 (Windows)
::  用法: 双击运行 | start.bat stop | start.bat status
:: ============================================================

title 仓库管理系统 - 启动面板
cd /d "%~dp0"

:: --------------------- 配置 (按需修改) ---------------------
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=3306"
set "MYSQL_USER=root"
set "MYSQL_PASS=root"
set "MYSQL_DB=warehouse"
set "REDIS_HOST=localhost"
set "REDIS_PORT=6379"
set "SERVER_PORT=8080"
set "WEB_PORT=5173"

:: --------------------- Banner ---------------------
echo.
echo   ╔══════════════════════════════════════════════════╗
echo   ║           仓库管理系统 (WMS) 启动面板              ║
echo   ║           Warehouse Management System             ║
echo   ╚══════════════════════════════════════════════════╝
echo.

:: ===================== 命令分发 =====================

if /i "%~1"=="stop" (
    call :stopServices
    goto :end
)

if /i "%~1"=="status" (
    call :checkStatus
    goto :end
)

:: ===================== 完整启动流程 =====================

call :checkPrereqs   || goto :end
call :setupDatabase  || goto :end
call :setupFrontend  || goto :end
call :startBackend   || goto :end
call :startFrontend  || goto :end

:: --------------------- 启动完成 ---------------------
echo   ╔══════════════════════════════════════════════════╗
echo   ║  [+] 系统启动完成！                              ║
echo   ║                                                  ║
echo   ║  前端页面: http://localhost:%WEB_PORT%            ║
echo   ║  API 文档: http://localhost:%SERVER_PORT%/swagger-ui.html
echo   ║  登录账号: admin / admin123                      ║
echo   ║                                                  ║
echo   ║  停止服务: start.bat stop                        ║
echo   ║  查看状态: start.bat status                      ║
echo   ╚══════════════════════════════════════════════════╝
echo.
echo   按任意键打开前端页面...

pause >nul
start http://localhost:%WEB_PORT%
goto :end


:: ===================== checkPrereqs =====================
:checkPrereqs
echo  [1/5] 检查运行环境...
echo.

:: Java 17+
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo   [X] 未找到 Java，请安装 JDK 17+
    echo       下载: https://adoptium.net/
    pause
    exit /b 1
)
echo   [√] Java: 已安装

:: Maven (优先用系统mvn，其次用项目自带mvnw)
set "MVN_CMD="
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    set "MVN_CMD=mvn"
) else if exist "warehouse-server\mvnw.cmd" (
    set "MVN_CMD=warehouse-server\mvnw.cmd"
)
if "%MVN_CMD%"=="" (
    echo   [X] 未找到 Maven，请安装 Maven 或放入 mvnw
    echo       下载: https://maven.apache.org/
    pause
    exit /b 1
)
echo   [√] Maven: 已就绪

:: Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo   [X] 未找到 Node.js，请安装 Node.js 18+
    echo       下载: https://nodejs.org/
    pause
    exit /b 1
)
echo   [√] Node.js: 已安装

:: MySQL
mysqladmin -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% ping >nul 2>&1
if %errorlevel% neq 0 (
    echo   [!] MySQL 连接失败 (%MYSQL_HOST%:%MYSQL_PORT%)
    echo       请确认:
    echo       1. MySQL 服务已启动
    echo       2. 用户名密码正确 (本脚本头部可修改)
    echo       3. 端口未被防火墙阻止
    echo.
    echo       如不修正，后续数据库步骤将失败。
    echo.
) else (
    echo   [√] MySQL: 已连接
)

:: Redis
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% ping >nul 2>&1
if %errorlevel% neq 0 (
    echo   [!] Redis 未运行 (%REDIS_HOST%:%REDIS_PORT%)
    echo       启动 Redis: redis-server 或 net start Redis
    echo       后端启动可能报错但不影响核心功能
    echo.
) else (
    echo   [√] Redis: 已连接
)

echo.
echo   环境检查完毕。
exit /b 0


:: ===================== setupDatabase =====================
:setupDatabase
echo  [2/5] 初始化数据库...

:: 创建数据库
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% -e "CREATE DATABASE IF NOT EXISTS %MYSQL_DB% DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if %errorlevel% neq 0 (
    echo   [X] 无法连接 MySQL / 创建数据库失败
    pause
    exit /b 1
)
echo   数据库 %MYSQL_DB% 已就绪

:: 建表
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "sql\01-schema.sql" 2>nul
if %errorlevel% neq 0 (
    echo   [!] 建表可能有问题 (可能表已存在，继续...)
) else (
    echo   表结构已创建 (19张表)
)

:: 种子数据
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "sql\02-seed.sql" 2>nul
if %errorlevel% neq 0 (
    echo   [!] 种子数据可能已导入过，跳过
) else (
    echo   种子数据已导入
)
echo   登录账号: admin / admin123

echo.
echo   数据库初始化完成。
exit /b 0


:: ===================== setupFrontend =====================
:setupFrontend
echo  [3/5] 安装前端依赖...
cd /d "%~dp0warehouse-web"

if not exist "node_modules\" (
    echo   首次运行，正在 npm install (约1-2分钟)...
    call npm install
    if %errorlevel% neq 0 (
        echo   [X] npm install 失败，请检查网络或 Node.js 安装
        cd /d "%~dp0"
        pause
        exit /b 1
    )
    echo   依赖安装完成。
) else (
    echo   前端依赖已就绪。
)

cd /d "%~dp0"
echo.
exit /b 0


:: ===================== startBackend =====================
:startBackend
echo  [4/5] 启动后端服务 (Spring Boot :%SERVER_PORT%)...

:: 先检查是否已有后端在运行
curl -s http://localhost:%SERVER_PORT%/v3/api-docs >nul 2>&1
if %errorlevel% equ 0 (
    echo   后端已在运行，跳过启动。
    exit /b 0
)

cd /d "%~dp0warehouse-server"

:: 在新窗口启动后端
start "WMS-Backend" /min cmd /c "echo 正在编译启动 Spring Boot... && %MVN_CMD% spring-boot:run && pause"

:: 等待就绪 (最多等待 180 秒)
echo   等待后端启动 (首次编译约1-3分钟)...
set /a COUNT=0
:waitServer
timeout /t 3 /nobreak >nul
set /a COUNT+=1

:: 用 PowerShell 检测端口 (Windows 自带)
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% equ 0 goto :serverReady

if %COUNT% geq 60 (
    echo   [!] 后端启动超时 (3分钟)
    echo       请检查 WMS-Backend 窗口中的错误信息
    cd /d "%~dp0"
    pause
    exit /b 1
)

echo   等待中... (!COUNT!/60)
goto :waitServer

:serverReady
echo   [√] 后端服务已就绪 (http://localhost:%SERVER_PORT%)
echo   API 文档: http://localhost:%SERVER_PORT%/swagger-ui.html
cd /d "%~dp0"
echo.
exit /b 0


:: ===================== startFrontend =====================
:startFrontend
echo  [5/5] 启动前端开发服务器 (Vite :%WEB_PORT%)...

:: 检查是否已在运行
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% equ 0 (
    echo   前端已在运行，跳过启动。
    exit /b 0
)

cd /d "%~dp0warehouse-web"

:: 在新窗口启动前端
start "WMS-Frontend" /min cmd /c "echo WMS 前端开发服务器 && echo 地址: http://localhost:%WEB_PORT% && echo. && npm run dev"

:: 等待就绪
echo   等待前端启动...
set /a COUNT=0
:waitWeb
timeout /t 2 /nobreak >nul
set /a COUNT+=1

powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% equ 0 goto :webReady

if %COUNT% geq 25 (
    echo   [!] 前端启动超时，请检查 WMS-Frontend 窗口
    cd /d "%~dp0"
    pause
    exit /b 1
)
goto :waitWeb

:webReady
echo   [√] 前端服务已就绪 (http://localhost:%WEB_PORT%)
cd /d "%~dp0"
echo.
exit /b 0


:: ===================== stopServices =====================
:stopServices
echo   正在停止 WMS 服务...
taskkill /fi "WINDOWTITLE eq WMS-Backend*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq WMS-Frontend*" /f >nul 2>&1

:: 释放端口
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f >nul 2>&1
)
echo   [√] 所有服务已停止。
exit /b 0


:: ===================== checkStatus =====================
:checkStatus
echo   服务状态:
echo   ─────────────────────────────────────

powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing; Write-Host '  后端 :%SERVER_PORT%  [运行中]' } catch { Write-Host '  后端 :%SERVER_PORT%  [未运行]' }"
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing; Write-Host '  前端 :%WEB_PORT%  [运行中]' } catch { Write-Host '  前端 :%WEB_PORT%  [未运行]' }"

echo   ─────────────────────────────────────
exit /b 0


:: ===================== 结束 =====================
:end
echo.
echo   按任意键关闭此窗口...
pause >nul
endlocal
