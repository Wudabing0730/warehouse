@echo off
:: ============================================================
::  Warehouse Management System - One-Click Launcher
::  Usage: start.bat [stop|status|db]
:: ============================================================
title WMS Launch Panel

:: Fix 1: Use cd /d first, then detect encoding-safe codepage
cd /d "%~dp0" 2>nul
if errorlevel 1 (
    echo Failed to change directory
    pause
    exit /b 1
)

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
echo   ========================================================
echo   ^|    Warehouse Management System (WMS) Launcher        ^|
echo   ^|    cang ku guan li xi tong                          ^|
echo   ========================================================
echo.

:: ===================== Command Dispatch =====================
:: Fix 2: No & inside if blocks, each command on its own line
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
:: Fix 3: Use if errorlevel 1 instead of || for reliability
call :checkPrereqs
if errorlevel 1 goto :end

call :setupDatabase
if errorlevel 1 goto :end

call :setupFrontend
if errorlevel 1 goto :end

call :startBackend
if errorlevel 1 goto :end

call :startFrontend
if errorlevel 1 goto :end

:: ==================== Launch Complete =======================
echo   ========================================================
echo   ^|  [OK] System started!                               ^|
echo   ^|                                                     ^|
echo   ^|  Frontend : http://localhost:%WEB_PORT%             ^|
echo   ^|  API Docs : http://localhost:%SERVER_PORT%/swagger-ui.html
echo   ^|  Login    : admin / admin123                       ^|
echo   ^|                                                     ^|
echo   ^|  stop.bat  - Stop all services                     ^|
echo   ^|  start.bat status - Check status                   ^|
echo   ========================================================
echo.
echo   Press any key to open browser...
pause
start http://localhost:%WEB_PORT%
goto :end


:: ===================== checkPrereqs =========================
:checkPrereqs
echo  [1/5] Checking environment...
echo.

:: Java 17+
where java >nul 2>&1
if errorlevel 1 (
    echo   [FAIL] Java not found. Install JDK 17+
    echo          https://adoptium.net/
    pause
    exit /b 1
)
echo   [OK] Java

:: Maven - prefer system mvn, fall back to project mvnw
set "MVN_CMD="
where mvn >nul 2>&1
if not errorlevel 1 (
    set "MVN_CMD=mvn"
)
:: Fix 4: nested if instead of else if
if "%MVN_CMD%"=="" (
    if exist "warehouse-server\mvnw.cmd" (
        set "MVN_CMD=warehouse-server\mvnw.cmd"
    )
)
if "%MVN_CMD%"=="" (
    echo   [FAIL] Maven not found
    echo          https://maven.apache.org/
    pause
    exit /b 1
)
echo   [OK] Maven

:: Node.js
where node >nul 2>&1
if errorlevel 1 (
    echo   [FAIL] Node.js not found. Install Node.js 18+
    echo          https://nodejs.org/
    pause
    exit /b 1
)
echo   [OK] Node.js

:: MySQL
mysqladmin -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% ping >nul 2>&1
if errorlevel 1 (
    echo   [WARN] MySQL not reachable (%MYSQL_HOST%:%MYSQL_PORT%)
    echo          Check: service running / firewall / credentials
    echo.
) else (
    echo   [OK] MySQL (%MYSQL_HOST%:%MYSQL_PORT%)
)

:: Redis
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% ping >nul 2>&1
if errorlevel 1 (
    echo   [WARN] Redis not running (%REDIS_HOST%:%REDIS_PORT%)
    echo          Start it: redis-server or net start Redis
    echo.
) else (
    echo   [OK] Redis (%REDIS_HOST%:%REDIS_PORT%)
)

echo.
echo   Environment check done.
exit /b 0


:: ===================== setupDatabase ========================
:setupDatabase
echo  [2/5] Initializing database...

:: Create database
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% -e "CREATE DATABASE IF NOT EXISTS %MYSQL_DB% DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if errorlevel 1 (
    echo   [FAIL] Cannot connect to MySQL
    echo          Check credentials at top of this script
    pause
    exit /b 1
)
echo   Database ready

:: Import schema
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "sql\01-schema.sql" 2>nul
if errorlevel 1 (
    echo   [WARN] Schema import - tables may already exist
) else (
    echo   Tables created (19)
)

:: Import seed data
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "sql\02-seed.sql" 2>nul
if errorlevel 1 (
    echo   [WARN] Seed import - may already exist
) else (
    echo   Seed data imported
)

echo   Login: admin / admin123
echo.
echo   Database setup done.
exit /b 0


:: ===================== setupFrontend ========================
:setupFrontend
echo  [3/5] Installing frontend dependencies...
cd /d "%~dp0warehouse-web"
if errorlevel 1 (
    echo   [FAIL] Cannot find warehouse-web directory
    cd /d "%~dp0"
    pause
    exit /b 1
)

if not exist "node_modules\" (
    echo   First run - installing packages (1-2 min)...
    echo.
    call npm install
    if errorlevel 1 (
        echo.
        echo   [FAIL] npm install failed
        echo          Check network connection and Node.js
        cd /d "%~dp0"
        pause
        exit /b 1
    )
    echo   Packages installed.
) else (
    echo   Dependencies already installed.
)

cd /d "%~dp0"
echo.
exit /b 0


:: ===================== startBackend =========================
:startBackend
echo  [4/5] Starting backend (Spring Boot)...

:: Check if already running
powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 (
    echo   Backend already running, skip.
    exit /b 0
)

cd /d "%~dp0warehouse-server"
if errorlevel 1 (
    echo   [FAIL] Cannot find warehouse-server directory
    cd /d "%~dp0"
    pause
    exit /b 1
)

:: Launch backend in a new minimized window
start "WMS-Backend" /min cmd /c "echo Starting Spring Boot... && %MVN_CMD% spring-boot:run && pause"

:: Wait for it to be ready (max 180s)
echo   Waiting for backend (first compile ~1-3 min)...
set COUNT=0
:waitServer
timeout /t 3 /nobreak >nul
set /a COUNT=%COUNT%+1

powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 goto :serverReady

if %COUNT% geq 60 (
    echo   [WARN] Backend startup timeout (3 min)
    echo          Check the WMS-Backend window for errors
    cd /d "%~dp0"
    pause
    exit /b 1
)
echo   Still waiting... (%COUNT%/60)
goto :waitServer

:serverReady
echo   [OK] Backend ready: http://localhost:%SERVER_PORT%
echo   API docs: http://localhost:%SERVER_PORT%/swagger-ui.html
cd /d "%~dp0"
echo.
exit /b 0


:: ===================== startFrontend ========================
:startFrontend
echo  [5/5] Starting frontend (Vite)...

:: Check if already running
powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 (
    echo   Frontend already running, skip.
    exit /b 0
)

cd /d "%~dp0warehouse-web"
if errorlevel 1 (
    echo   [FAIL] Cannot find warehouse-web directory
    cd /d "%~dp0"
    pause
    exit /b 1
)

:: Launch frontend in a new minimized window
start "WMS-Frontend" /min cmd /c "echo WMS Frontend && echo http://localhost:%WEB_PORT% && echo. && npm run dev"

:: Wait for it to be ready
echo   Waiting for frontend...
set COUNT=0
:waitWeb
timeout /t 2 /nobreak >nul
set /a COUNT=%COUNT%+1

powershell -Command "try{$r=Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing;exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 goto :webReady

if %COUNT% geq 25 (
    echo   [WARN] Frontend startup timeout
    echo          Check the WMS-Frontend window for errors
    cd /d "%~dp0"
    pause
    exit /b 1
)
goto :waitWeb

:webReady
echo   [OK] Frontend ready: http://localhost:%WEB_PORT%
cd /d "%~dp0"
echo.
exit /b 0


:: ===================== stopServices =========================
:stopServices
echo   Stopping WMS services...
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
echo   Service Status:
echo   -------------------------------------------------
powershell -Command "$s='stopped';try{$r=Invoke-WebRequest -Uri 'http://localhost:%SERVER_PORT%/v3/api-docs' -TimeoutSec 2 -UseBasicParsing;$s='RUNNING'}catch{};Write-Host ('   Backend  :%SERVER_PORT%   ['+$s+']')"
powershell -Command "$s='stopped';try{$r=Invoke-WebRequest -Uri 'http://localhost:%WEB_PORT%' -TimeoutSec 2 -UseBasicParsing;$s='RUNNING'}catch{};Write-Host ('   Frontend :%WEB_PORT%   ['+$s+']')"
echo   -------------------------------------------------
echo   Login: admin / admin123
exit /b 0


:: ======================== END ===============================
:end
echo.
echo   Press any key to close...
pause
