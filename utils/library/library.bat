@ECHO OFF
REM ES4A SDK 扩展库开发入口。 xhwsd@qq.com 2026-10-25
SETLOCAL
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0library.ps1" %*
EXIT /B %ERRORLEVEL%
