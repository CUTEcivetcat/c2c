@echo off
setlocal EnableExtensions
title C2C Frontend Launcher

rem ============================================================
rem  C2C frontend one-click launcher (single entry)
rem
rem  Starts:
rem   1. frontend       http://localhost:5173  (opens browser)
rem   2. admin-frontend http://localhost:5174  (silent, no browser)
rem   3. blog           http://localhost:5175  (skipped if missing)
rem
rem  Logs: logs/frontend.log, logs/admin-frontend.log, logs/blog.log
rem  Auto npm install on first run.
rem ============================================================

set "ROOT=%~dp0.."
set "LOG=%ROOT%\logs"
if not exist "%LOG%" mkdir "%LOG%"

echo.
echo  ============================================
echo    C2C Frontend Launcher
echo  ============================================
echo.

rem ---------- 1. frontend ----------
if not exist "%ROOT%\frontend\package.json" goto :no_frontend
echo  [1/3] Starting frontend  (http://localhost:5173, opens browser) ...
if not exist "%ROOT%\frontend\node_modules" (
  echo        installing frontend dependencies ...
  pushd "%ROOT%\frontend"
  call npm install >nul 2>&1
  popd
)
start "c2c-frontend" /min cmd /c "cd /d %ROOT%\frontend && npm run dev > %LOG%\frontend.log 2>&1"
goto :after_frontend
:no_frontend
echo  [1/3] frontend dir missing, skipped.
:after_frontend

rem ---------- 2. admin-frontend (silent) ----------
if not exist "%ROOT%\admin-frontend\package.json" goto :no_admin
echo  [2/3] Starting admin-frontend  (http://localhost:5174, silent) ...
if not exist "%ROOT%\admin-frontend\node_modules" (
  echo        installing admin-frontend dependencies ...
  pushd "%ROOT%\admin-frontend"
  call npm install >nul 2>&1
  popd
)
start "c2c-admin" /min cmd /c "cd /d %ROOT%\admin-frontend && npm run dev > %LOG%\admin-frontend.log 2>&1"
goto :after_admin
:no_admin
echo  [2/3] admin-frontend dir missing, skipped.
:after_admin

rem ---------- 3. blog ----------
if not exist "%ROOT%\blog\package.json" goto :no_blog
echo  [3/3] Starting blog  (http://localhost:5175) ...
if not exist "%ROOT%\blog\node_modules" (
  echo        installing blog dependencies ...
  pushd "%ROOT%\blog"
  call npm install >nul 2>&1
  popd
)
start "c2c-blog" /min cmd /c "cd /d %ROOT%\blog && npm run dev > %LOG%\blog.log 2>&1"
goto :after_blog
:no_blog
echo  [3/3] blog dir missing, skipped.
:after_blog

echo.
echo  Waiting for services (about 10s) ...
ping -n 11 127.0.0.1 >nul

echo.
echo  ============================================
echo    Startup Status
echo  ============================================
call :check_port 5173 "frontend"
call :check_port 5174 "admin-frontend"
call :check_port 5175 "blog"
echo.
echo  Logs: %LOG%
echo  ============================================
pause
exit /b 0

:check_port
set "PORT=%~1"
set "NAME=%~2"
netstat -ano | findstr ":%PORT% " | findstr /i "LISTENING" >nul 2>&1
if %errorlevel%==0 goto :port_ok
echo   [....] %NAME%  -  http://localhost:%PORT%  not ready, check logs
exit /b 0
:port_ok
echo   [OK]   %NAME%  -  http://localhost:%PORT%  running
exit /b 0