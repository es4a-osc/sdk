@ECHO OFF
REM ES4A SDK 项目工具入口。 xhwsd@qq.com 2026-10-28
SETLOCAL
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0project.ps1" %*
EXIT /B %ERRORLEVEL%
