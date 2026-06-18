@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ============================================================
echo   仓库管理系统 - 一键启动脚本
echo ============================================================
echo.

set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"
set "SERVER_DIR=%PROJECT_DIR%\warehouse-server"
set "WEB_DIR=%PROJECT_DIR%\warehouse-web"
set "SQL_DIR=%PROJECT_DIR%\sql"
set "JAR_PATH="
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"
set "MYSQL_SERVICE=MySQL80"
set "REDIS_SERVICE=Redis"

:: ============================================================
:: 1. 检查必要工具
:: ============================================================
echo [1/7] 检查运行环境...

where java >nul 2>&1
if !errorlevel! neq 0 (
    echo [错误] 未找到 Java，请安装 JDK 17+ 并配置 PATH
    pause
    exit /b 1
)
echo       Java     - OK

where mvn >nul 2>&1
if !errorlevel! neq 0 (
    echo [错误] 未找到 Maven，请安装并配置 PATH
    pause
    exit /b 1
)
echo       Maven    - OK

where node >nul 2>&1
if !errorlevel! neq 0 (
    echo [错误] 未找到 Node.js，请安装 Node.js 16+
    pause
    exit /b 1
)
echo       Node.js  - OK

where npm >nul 2>&1
if !errorlevel! neq 0 (
    echo [错误] 未找到 npm
    pause
    exit /b 1
)
echo       npm      - OK

where mysql >nul 2>&1
if !errorlevel! neq 0 (
    echo       MySQL客户端 - 未找到(可忽略，将通过服务管理)
) else (
    echo       MySQL客户端 - OK
)

echo.

:: ============================================================
:: 2. 启动 MySQL
:: ============================================================
echo [2/7] 检查 MySQL 服务...

sc query %MYSQL_SERVICE% >nul 2>&1
if !errorlevel! neq 0 (
    echo [警告] 未找到 MySQL 服务 "%MYSQL_SERVICE%", 请确认服务名称
    echo         如果 MySQL 服务名称不同，请修改脚本中的 MYSQL_SERVICE 变量
    echo         继续前请确保 MySQL 已手动启动...
    pause
) else (
    sc query %MYSQL_SERVICE% | findstr /C:"RUNNING" >nul 2>&1
    if !errorlevel! neq 0 (
        echo       MySQL 服务未运行，正在启动...
        net start %MYSQL_SERVICE%
        if !errorlevel! neq 0 (
            echo [错误] MySQL 服务启动失败，请手动启动后重试
            pause
            exit /b 1
        )
        echo       MySQL 服务已启动
        timeout /t 3 /nobreak >nul
    ) else (
        echo       MySQL 服务已运行
    )
)

echo.

:: ============================================================
:: 3. 启动 Redis
:: ============================================================
echo [3/7] 检查 Redis 服务...

sc query %REDIS_SERVICE% >nul 2>&1
if !errorlevel! neq 0 (
    echo [警告] 未找到 Redis 服务 "%REDIS_SERVICE%"
    echo         请确保 Redis 已手动启动在 localhost:6379
) else (
    sc query %REDIS_SERVICE% | findstr /C:"RUNNING" >nul 2>&1
    if !errorlevel! neq 0 (
        echo       Redis 服务未运行，正在启动...
        net start %REDIS_SERVICE%
        if !errorlevel! neq 0 (
            echo [错误] Redis 服务启动失败，请手动启动后重试
            pause
            exit /b 1
        )
        echo       Redis 服务已启动
    ) else (
        echo       Redis 服务已运行
    )
)

echo.

:: ============================================================
:: 4. 初始化数据库（仅在数据库不存在时执行）
:: ============================================================
echo [4/7] 检查数据库初始化...

set "DB_INITIALIZED=0"
where mysql >nul 2>&1
if !errorlevel! equ 0 (
    mysql -u root -p123456 -e "USE warehouse;" >nul 2>&1
    if !errorlevel! neq 0 (
        echo       数据库未初始化，正在执行 SQL 脚本...
        for %%f in ("%SQL_DIR%\01-schema.sql" "%SQL_DIR%\02-seed.sql" "%SQL_DIR%\03-demo-data.sql") do (
            echo       执行: %%~nxf ...
            mysql -u root -p123456 < "%%f" 2>nul
            if !errorlevel! neq 0 (
                echo [警告] %%~nxf 执行失败，可能需要手动导入
            ) else (
                echo       %%~nxf 执行完成
            )
        )
        set "DB_INITIALIZED=1"
    ) else (
        echo       数据库已存在，跳过初始化
    )
) else (
    echo       未找到 mysql 客户端，跳过自动初始化
    echo       请确保 warehouse 数据库已手动创建并导入 SQL 脚本
)

echo.

:: ============================================================
:: 5. 构建后端
:: ============================================================
echo [5/7] 构建后端服务...

set "JAR_FOUND=0"
for %%f in ("%SERVER_DIR%\target\warehouse-server-*.jar") do (
    set "JAR_PATH=%%f"
    set "JAR_FOUND=1"
)

if "!JAR_FOUND!"=="1" (
    echo       检测到已有构建产物，跳过构建
    echo       如需重新构建，请先删除 target 目录或运行: mvn clean package
) else (
    echo       未检测到构建产物，正在执行 Maven 构建...
    echo       首次构建可能需要下载依赖，请耐心等待...
    pushd "%SERVER_DIR%"
    call mvn clean package -DskipTests -q
    if !errorlevel! neq 0 (
        echo [错误] Maven 构建失败，请检查错误信息
        popd
        pause
        exit /b 1
    )
    popd

    for %%f in ("%SERVER_DIR%\target\warehouse-server-*.jar") do (
        set "JAR_PATH=%%f"
    )

    if not defined JAR_PATH (
        echo [错误] 构建完成但未找到 JAR 文件
        pause
        exit /b 1
    )
    echo       构建完成
)

echo.

:: ============================================================
:: 6. 启动后端服务
:: ============================================================
echo [6/7] 启动后端服务（端口 %BACKEND_PORT%）...

netstat -ano | findstr ":%BACKEND_PORT% " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo [警告] 端口 %BACKEND_PORT% 已被占用，后端服务可能已在运行
    echo         如需重启，请先运行 stop.bat
    echo.
) else (
    start "Warehouse-Backend" cmd /k "cd /d !SERVER_DIR! && java -jar "!JAR_PATH!" --spring.profiles.active=dev"
    echo       后端服务正在启动，等待...
    timeout /t 8 /nobreak >nul
)

echo.

:: ============================================================
:: 7. 启动前端服务
:: ============================================================
echo [7/7] 启动前端服务（端口 %FRONTEND_PORT%）...

if not exist "%WEB_DIR%\node_modules" (
    echo       首次运行，正在安装前端依赖...
    pushd "%WEB_DIR%"
    call npm install
    if !errorlevel! neq 0 (
        echo [错误] 前端依赖安装失败
        popd
        pause
        exit /b 1
    )
    popd
    echo       依赖安装完成
) else (
    echo       前端依赖已就绪
)

netstat -ano | findstr ":%FRONTEND_PORT% " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo [警告] 端口 %FRONTEND_PORT% 已被占用，前端服务可能已在运行
    echo         如需重启，请先运行 stop.bat
) else (
    start "Warehouse-Frontend" cmd /k "cd /d !WEB_DIR! && npm run dev"
    echo       前端服务正在启动...
)

echo.
echo ============================================================
echo   所有服务启动完成！
echo ============================================================
echo.
echo   后端地址:  http://localhost:%BACKEND_PORT%
echo   API文档:   http://localhost:%BACKEND_PORT%/swagger-ui.html
echo   前端地址:  http://localhost:%FRONTEND_PORT%
echo.
echo   要停止所有服务，请运行: stop.bat
echo   或关闭对应窗口即可
echo ============================================================
pause