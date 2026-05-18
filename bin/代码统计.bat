@echo off
chcp 65001 >nul
pwsh -NoProfile -File "%~dp0代码统计.ps1"
pause
