@echo off
setlocal

:: Force UTF-8 codepage
chcp 65001 >nul 2>&1

title WMS - Stop Services
echo.
echo  +========================================================+
echo  ^|          Warehouse Management System - STOP            ^|
echo  +========================================================+
echo.

echo    Stopping WMS services...

taskkill /fi "WINDOWTITLE eq WMS-Backend*" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq WMS-Frontend*" /f >nul 2>&1

echo    Releasing ports 8080 / 5173...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING" 2^>nul') do (
    taskkill /pid %%a /f >nul 2>&1
)

echo.
echo   [OK] All WMS services stopped.
echo.
echo   Press any key to close...
pause >nul
endlocal
