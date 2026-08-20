@echo off
rem ============================================================
rem C2C 前端启动脚本（Windows 双击运行）
rem
rem 双击本文件：启动用户端 frontend
rem 命令行带参数：start-frontend.bat admin  启动管理端
rem               start-frontend.bat blog   启动博客
rem               start-frontend.bat all    依次启动全部
rem
rem 首次运行会自动执行 npm install，之后直接 npm run dev。
rem 启动成功会自动打开浏览器；启动失败窗口会暂停显示错误。
rem ============================================================
setlocal
set "ROOT=%~dp0.."
set "MODE=%~1"
if "%MODE%"=="" set "MODE=frontend"

if "%MODE%"=="all" (
  echo 依次启动全部前端，分别按 Ctrl+C 切换……
  call :run_one "frontend"        "用户端"
  call :run_one "admin-frontend"  "管理端"
  call :run_one "blog"            "博客"
  exit /b 0
)

if "%MODE%"=="frontend" (
  call :run_one "frontend" "用户端"
  exit /b 0
)
if "%MODE%"=="admin" (
  call :run_one "admin-frontend" "管理端"
  exit /b 0
)
if "%MODE%"=="blog" (
  call :run_one "blog" "博客"
  exit /b 0
)

echo 用法: start-frontend.bat [frontend^|admin^|blog^|all] （缺省 frontend）
pause
exit /b 1

:run_one
  set "DIR=%~1"
  set "NAME=%~2"
  if not exist "%ROOT%\%DIR%\package.json" (
    echo [错误] 目录不存在: %ROOT%\%DIR%
    echo 请确认参数是 frontend / admin / blog 之一。
    pause
    exit /b 1
  )
  echo.
  echo ==============================================
  echo  启动 %NAME%  (%DIR%\)
  echo ==============================================
  cd /d "%ROOT%\%DIR%"
  if not exist "node_modules" (
    echo 首次运行，正在安装依赖（npm install）...
    call npm install
    if errorlevel 1 (
      echo [错误] npm install 失败，请检查网络后重试。
      pause
      exit /b 1
    )
  )
  echo 启动完成后会自动打开浏览器（首次启动稍慢）...
  call npm run dev
  if errorlevel 1 (
    echo.
    echo [错误] 前端启动失败，请查看上方报错信息。
    pause
  )
  exit /b 0