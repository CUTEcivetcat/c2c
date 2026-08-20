@echo off
rem ============================================================
rem C2C 前端打包脚本（Windows 双击运行）
rem
rem 双击本文件：打包用户端 frontend
rem 命令行带参数：build-frontend.bat admin  打包管理端
rem               build-frontend.bat blog   打包博客
rem               build-frontend.bat all    打包全部
rem
rem 产物输出到各项目 dist/ 目录，可配合 deploy/upload-frontend.sh 上传。
rem ============================================================
setlocal
set "ROOT=%~dp0.."
set "MODE=%~1"
if "%MODE%"=="" set "MODE=frontend"

if "%MODE%"=="all" (
  echo 依次打包全部前端……
  call :build_one "frontend"       "用户端"
  call :build_one "admin-frontend" "管理端"
  call :build_one "blog"           "博客"
  echo.
  echo 全部打包完成。
  pause
  exit /b 0
)

if "%MODE%"=="frontend" (
  call :build_one "frontend" "用户端"
  exit /b 0
)
if "%MODE%"=="admin" (
  call :build_one "admin-frontend" "管理端"
  exit /b 0
)
if "%MODE%"=="blog" (
  call :build_one "blog" "博客"
  exit /b 0
)

echo 用法: build-frontend.bat [frontend^|admin^|blog^|all] （缺省 frontend）
pause
exit /b 1

:build_one
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
  echo  打包 %NAME%  (%DIR%\)
  echo ==============================================
  cd /d "%ROOT%\%DIR%"
  if not exist "node_modules" (
    echo 首次打包，正在安装依赖（npm install）...
    call npm install
    if errorlevel 1 (
      echo [错误] npm install 失败，请检查网络后重试。
      pause
      exit /b 1
    )
  )
  call npm run build
  if errorlevel 1 (
    echo.
    echo [错误] %NAME% 打包失败，请查看上方报错信息。
    pause
    exit /b 1
  )
  echo.
  echo ? %NAME% 打包成功：%ROOT%\%DIR%\dist\
  exit /b 0