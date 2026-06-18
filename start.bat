@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
::  仓库管理系统 (WMS) — 一键启动脚本 (Windows)
::  用法: 双击运行 或 start.bat [setup|start|stop|status]
:: ============================================================

title 仓库管理系统 - 启动面板
cd /d "%~dp0"

:: --------------------- 配置 ---------------------
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=3306"
set "MYSQL_USER=root"
set "MYSQL_PASS=root"
set "MYSQL_DB=warehouse"
set "REDIS_HOST=localhost"
set "REDIS_PORT=6379"
set "SERVER_PORT=8080"
set "WEB_PORT=5173"

:: --------------------- 函数: 显示Banner ---------------------
:showBanner
echo.
echo   ╔══════════════════════════════════════════════════╗
echo   ║           仓库管理系统 (WMS) 启动面板              ║
echo   ║           Warehouse Management System             ║
echo   ╚══════════════════════════════════════════════════╝
echo.
goto :eof

:: --------------------- 函数: 检查依赖 ---------------------
:checkPrereqs
echo  [1/5] 检查运行环境...
echo.

:: Java 17+
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo   [错误] 未找到 Java，请安装 JDK 17+
    pause
    exit /b 1
)
for /f "tokens=1" %%i in ('java -version 2^>^&1 ^| find "version"') do set JAVA_VER=%%i
echo    Java: 已安装

:: Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    if not exist "warehouse-server\mvnw.cmd" (
        echo   [错误] 未找到 Maven，请安装或放入 mvnw
        pause
        exit /b 1
    )
    set "MVN_CMD=warehouse-server\mvnw.cmd"
) else (
    set "MVN_CMD=mvn"
)
echo    Maven: 已就绪

:: Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo   [错误] 未找到 Node.js，请安装 Node.js 18+
    pause
    exit /b 1
)
echo    Node.js: 已安装

:: MySQL
mysqladmin -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% ping >nul 2>&1
if %errorlevel% neq 0 (
    echo   [警告] MySQL 连接失败 (%MYSQL_HOST%:%MYSQL_PORT%)
    echo          请确认 MySQL 已启动且用户名密码正确
    echo          继续前请修改本脚本开头的 MYSQL_* 配置
) else (
    echo    MySQL: 已连接
)

:: Redis
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% ping >nul 2>&1
if %errorlevel% neq 0 (
    echo   [警告] Redis 未运行 (%REDIS_HOST%:%REDIS_PORT%)
    echo          请启动 Redis 服务
    echo          或将本脚本开头的 REDIS_HOST/REDIS_PORT 改为已运行的实例
) else (
    echo    Redis: 已连接
)

echo.
echo   环境检查完毕。
echo.
goto :eof

:: --------------------- 函数: 初始化数据库 ---------------------
:setupDatabase
echo  [2/5] 初始化数据库...

:: 创建数据库
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% -e "CREATE DATABASE IF NOT EXISTS %MYSQL_DB% DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if %errorlevel% neq 0 (
    echo   [错误] 无法创建数据库，请检查 MySQL 连接
    pause
    exit /b 1
)

:: 执行建表脚本
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < sql\01-schema.sql 2>nul
if %errorlevel% neq 0 (
    echo   [警告] 建表脚本执行有问题，可能表已存在
) else (
    echo   表结构已创建 (19张表)
)

:: 执行种子数据
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < sql\02-seed.sql 2>nul
if %errorlevel% neq 0 (
    echo   [警告] 种子数据导入有问题，可能已导入过
) else (
    echo   种子数据已导入 (管理员: admin / admin123)
)

echo.
echo   数据库初始化完成。
echo.
goto :eof

:: --------------------- 函数: 安装前端依赖 ---------------------
:setupFrontend
echo  [3/5] 安装前端依赖...
cd /d "%~dp0warehouse-web"

if not exist "node_modules\" (
    echo   正在执行 npm install (首次运行需要几分钟)...
    call npm install --silent
    if %errorlevel% neq 0 (
        echo   [错误] npm install 失败
        cd /d "%~dp0"
        pause
        exit /b 1
    )
    echo   前端依赖安装完成。
) else (
    echo   前端依赖已就绪。
)

cd /d "%~dp0"
echo.
goto :eof

:: --------------------- 函数: 启动后端 ---------------------
:startBackend
echo  [4/5] 启动后端服务 (Spring Boot :%SERVER_PORT%)...
cd /d "%~dp0warehouse-server"

:: 后台编译并启动
start "WMS-Backend" cmd /c "title WMS后端服务 ^&^& echo 正在编译启动后端... ^&^& %MVN_CMD% spring-boot:run -q 2>&1"

:: 等待后端就绪
echo   等待后端启动中...
set /a COUNT=0
:waitServer
timeout /t 2 /nobreak >nul
set /a COUNT+=1
curl -s http://localhost:%SERVER_PORT%/v3/api-docs >nul 2>&1
if %errorlevel% equ 0 goto serverReady
if %COUNT% geq 60 (
    echo   [警告] 后端启动超时 (120秒)，请检查 warehouse-server 窗口
    goto :eof
)
goto waitServer

:serverReady
echo   后端服务已就绪 (http://localhost:%SERVER_PORT%)
echo   API 文档: http://localhost:%SERVER_PORT%/swagger-ui.html
cd /d "%~dp0"
echo.
goto :eof

:: --------------------- 函数: 启动前端 ---------------------
:startFrontend
echo  [5/5] 启动前端开发服务器 (Vite :%WEB_PORT%)...
cd /d "%~dp0warehouse-web"

start "WMS-Frontend" cmd /c "title WMS前端服务 ^&^& echo 正在启动前端... ^&^& npm run dev"

:: 等待前端就绪
echo   等待前端启动中...
set /a COUNT=0
:waitWeb
timeout /t 2 /nobreak >nul
set /a COUNT+=1
curl -s http://localhost:%WEB_PORT% >nul 2>&1
if %errorlevel% equ 0 goto webReady
if %COUNT% geq 30 (
    echo   [警告] 前端启动超时，请检查 WMS-Frontend 窗口
    goto :eof
)
goto waitWeb

:webReady
echo   前端服务已就绪 (http://localhost:%WEB_PORT%)
cd /d "%~dp0"
echo.
goto :eof

:: --------------------- 函数: 停止服务 ---------------------
:stopServices
echo  停止所有 WMS 服务...
taskkill /fi "WINDOWTITLE eq WMS-Backend*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq WMS-Frontend*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq WMS后端*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq WMS前端*" /f >nul 2>&1
echo  已停止。
goto :eof

:: --------------------- 函数: 状态检查 ---------------------
:checkStatus
echo  服务状态:
curl -s http://localhost:%SERVER_PORT%/v3/api-docs >nul 2>&1
if %errorlevel% equ 0 (
    echo   后端 :%SERVER_PORT%  [运行中]
) else (
    echo   后端 :%SERVER_PORT%  [未运行]
)
curl -s http://localhost:%WEB_PORT% >nul 2>&1
if %errorlevel% equ 0 (
    echo   前端 :%WEB_PORT%  [运行中]
) else (
    echo   前端 :%WEB_PORT%  [未运行]
)
goto :eof

:: ===================== 主流程 =====================

call :showBanner

:: 解析命令参数
if /i "%~1"=="stop"  call :stopServices & goto :end
if /i "%~1"=="status" call :checkStatus & goto :end

:: setup / start 都走完整流程
call :checkPrereqs
if %errorlevel% neq 0 goto :end

call :setupDatabase
if %errorlevel% neq 0 goto :end

call :setupFrontend
if %errorlevel% neq 0 goto :end

:: 启动后端
call :startBackend

:: 启动前端
call :startFrontend

:: 完成
echo   ╔══════════════════════════════════════════════════╗
echo   ║  🎉 系统启动完成！                               ║
echo   ║                                                  ║
echo   ║  前端页面: http://localhost:%WEB_PORT%             ║
echo   ║  API 文档: http://localhost:%SERVER_PORT%/swagger-ui.html
echo   ║  登录账号: admin / admin123                      ║
echo   ║                                                  ║
echo   ║  管理窗口:                                       ║
echo   ║   - WMS后端服务 (Spring Boot)                    ║
echo   ║   - WMS前端服务 (Vite)                           ║
echo   ║                                                  ║
echo   ║  停止服务: start.bat stop                        ║
echo   ║  查看状态: start.bat status                      ║
echo   ╚══════════════════════════════════════════════════╝
echo.
echo  按任意键打开前端页面...
pause >nul
start http://localhost:%WEB_PORT%

:end
echo.
endlocal
