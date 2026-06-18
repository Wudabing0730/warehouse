@echo off
setlocal enabledelayedexpansion

:: ============================================================
::  Warehouse Management System - One-Click Launcher
::  Ensure UTF-8 encoding for Chinese characters
:: ============================================================

:: Force UTF-8 codepage (must be before any output)
chcp 65001 >nul 2>&1

:: Set console to UTF-8 capable mode
reg query "HKCU\Console" /v CodePage 2>nul | find "65001" >nul
if %errorlevel% neq 0 (
    reg add "HKCU\Console" /v CodePage /t REG_DWORD /d 65001 /f >nul 2>&1
)

title WMS Launch Panel
cd /d "%~dp0"

:: ======================= Configuration =======================
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=3306"
set "MYSQL_USER=root"
set "MYSQL_PASS=123456"
set "MYSQL_DB=warehouse"
set "REDIS_HOST=localhost"
set "REDIS_PORT=6379"
set "SERVER_PORT=8080"
set "WEB_PORT=5173"

:: ========================== Banner ===========================
echo.
echo  +========================================================+
echo  ^|         Warehouse Management System (WMS)              ^|
echo  ^|         cang ku guan li xi tong                         ^|
echo  +========================================================+
echo.

:: ===================== Command Dispatch =====================
if /i "%~1"=="stop" (
    call :stopServices
    goto :end
)
if /i "%~1"=="status" (
    call :checkStatus
    goto :end
)
if /i "%~1"=="db" (
    call :setupDatabase
    goto :end
)

:: ==================== Full Launch Flow ======================
call :checkPrereqs   || goto :end
call :setupDatabase  || goto :end
call :setupFrontend  || goto :end
call :startBackend   || goto :end
call :startFrontend  || goto :end

:: ==================== Launch Complete =======================
echo  +========================================================+
echo  ^|  [OK] System started!                                  ^|
echo  ^|                                                        ^|
echo  ^|  Frontend : http://localhost:%WEB_PORT%                 ^|
echo  ^|  API Docs : http://localhost:%SERVER_PORT%/swagger-ui.html
echo  ^|  Login    : admin / admin123                          ^|
echo  ^|                                                        ^|
echo  ^|  stop.bat  - Stop all services                        ^|
echo  ^|  start.bat status - Check status                      ^|
echo  +========================================================+
echo.
echo    Press any key to open browser...

pause
start http://localhost:%WEB_PORT%
goto :end


:: ===================== checkPrereqs =========================
:checkPrereqs
echo  [1/5] Checking environment...
echo.

:: Java 17+
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo   [FAIL] Java not found. Install JDK 17+
    echo          Download: https://adoptium.net/
    pause
    exit /b 1
)
echo   [OK] Java

:: Maven
set "MVN_CMD="
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    set "MVN_CMD=mvn"
) else if exist "warehouse-server\mvnw.cmd" (
    set "MVN_CMD=warehouse-server\mvnw.cmd"
)
if "%MVN_CMD%"=="" (
    echo   [FAIL] Maven not found.
    echo          Download: https://maven.apache.org/
    pause
    exit /b 1
)
echo   [OK] Maven (%MVN_CMD%)

:: Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo   [FAIL] Node.js not found. Install Node.js 18+
    echo          Download: https://nodejs.org/
    pause
    exit /b 1
)
echo   [OK] Node.js

:: MySQL
mysqladmin -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% ping >nul 2>&1
if %errorlevel% neq 0 (
    echo   [WARN] MySQL connection failed (%MYSQL_HOST%:%MYSQL_PORT%)
    echo          - Check MySQL service is running
    echo          - Check username/password at top of this script
    echo.
) else (
    echo   [OK] MySQL (%MYSQL_HOST%:%MYSQL_PORT%)
)

:: Redis
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% ping >nul 2>&1
if %errorlevel% neq 0 (
    echo   [WARN] Redis not running (%REDIS_HOST%:%REDIS_PORT%)
    echo          Run: redis-server (or) net start Redis
    echo.
) else (
    echo   [OK] Redis (%REDIS_HOST%:%REDIS_PORT%)
)

echo.
echo    Environment check done.
exit /b 0


:: ===================== setupDatabase ========================
:setupDatabase
echo  [2/5] Initializing database...

mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% -e "CREATE DATABASE IF NOT EXISTS %MYSQL_DB% DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if %errorlevel% neq 0 (
    echo   [FAIL] Cannot connect to MySQL or create database
    echo          Check your MySQL credentials at the top of this script
    pause
    exit /b 1
)
echo    Database '%MYSQL_DB%' ready

:: Schema
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "sql\01-schema.sql" 2>nul
if %errorlevel% neq 0 (
    echo   [WARN] Schema import had issues (tables may already exist)
) else (
    echo    Tables created (19 tables)
)

:: Seed data
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "sql\02-seed.sql" 2>nul
if %errorlevel% neq 0 (
    echo   [WARN] Seed data may already exist, skipping
) else (
    echo    Seed data imported
)
echo    Default login: admin / admin123

echo.
echo    Database setup done.
exit /b 0


:: ===================== setupFrontend ========================
:setupFrontend
echo  [3/5] Installing frontend dependencies...
cd /d "%~dp0warehouse-web"

if not exist "node_modules\" (
    echo    First run: npm install (~1-2 minutes)...
    call npm install
    if %errorlevel% neq 0 (
        echo   [FAIL] npm install failed. Check network / Node.js
        cd /d "%~dp0"
        pause
        exit /b 1
    )
    echo    Dependencies installed.
) else (
    echo    Dependencies already installed.
)

cd /d "%~dp0"
echo.
exit /b 0


:: ===================== startBackend =========================
:startBackend
echo  [4/5] Starting backend (Spring Boot :%SERVER_PORT%)...

:: Check if already running
powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if %errorlevel% equ 0 (
    echo    Backend already running, skip.
    exit /b 0
)

cd /d "%~dp0warehouse-server"

:: Launch in new window
start "WMS-Backend" /min cmd /c "chcp 65001>nul 2>&1 && title WMS Backend && echo Compiling Spring Boot... && %MVN_CMD% spring-boot:run && pause"

:: Wait for ready (max 180s)
echo    Waiting for backend startup (first compile may take 1-3 min)...
set /a COUNT=0
:waitServer
timeout /t 3 /nobreak >nul
set /a COUNT+=1

powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if %errorlevel% equ 0 goto :serverReady

if %COUNT% geq 60 (
    echo   [WARN] Backend startup timeout (3 min)
    echo          Check the WMS-Backend window for errors
    cd /d "%~dp0"
    pause
    exit /b 1
)

echo    Still waiting... (!COUNT!/60)
goto :waitServer

:serverReady
echo   [OK] Backend ready (http://localhost:%SERVER_PORT%)
echo   API docs: http://localhost:%SERVER_PORT%/swagger-ui.html
cd /d "%~dp0"
echo.
exit /b 0


:: ===================== startFrontend ========================
:startFrontend
echo  [5/5] Starting frontend (Vite :%WEB_PORT%)...

:: Check if already running
powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if %errorlevel% equ 0 (
    echo    Frontend already running, skip.
    exit /b 0
)

cd /d "%~dp0warehouse-web"

:: Launch in new window
start "WMS-Frontend" /min cmd /c "chcp 65001>nul 2>&1 && title WMS Frontend && echo WMS Frontend && echo http://localhost:%WEB_PORT% && echo. && npm run dev"

:: Wait for ready
echo    Waiting for frontend startup...
set /a COUNT=0
:waitWeb
timeout /t 2 /nobreak >nul
set /a COUNT+=1

powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if %errorlevel% equ 0 goto :webReady

if %COUNT% geq 25 (
    echo   [WARN] Frontend startup timeout
    echo          Check the WMS-Frontend window for errors
    cd /d "%~dp0"
    pause
    exit /b 1
)
goto :waitWeb

:webReady
echo   [OK] Frontend ready (http://localhost:%WEB_PORT%)
cd /d "%~dp0"
echo.
exit /b 0


:: ===================== stopServices =========================
:stopServices
echo    Stopping WMS services...
taskkill /fi "WINDOWTITLE eq WMS-Backend*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq WMS-Frontend*" /f >nul 2>&1

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f >nul 2>&1
)
echo   [OK] All services stopped.
exit /b 0


:: ===================== checkStatus ==========================
:checkStatus
echo    Service Status:
echo    -------------------------------------------------
powershell -Command "$s='stopped';try{$r=Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing;$s='RUNNING'}catch{};Write-Host ('   Backend  :%SERVER_PORT%   ['+$s+']')"
powershell -Command "$s='stopped';try{$r=Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing;$s='RUNNING'}catch{};Write-Host ('   Frontend :%WEB_PORT%   ['+$s+']')"
echo    -------------------------------------------------
echo    Login: admin / admin123
exit /b 0


:: ======================== END ===============================
:end
echo.
echo    Press any key to close...
pause
endlocal
