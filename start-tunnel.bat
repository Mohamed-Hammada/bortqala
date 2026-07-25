@echo off
title Bemo Cloudflare Tunnel Launcher
echo ========================================================
echo Starting Cloudflare Tunnel for Bemo Angular Frontend...
echo Target: http://localhost:4200 (Host Header: localhost:4200)
echo ========================================================
echo.
"%~dp0cloudflared-windows-amd64.exe" tunnel --url http://localhost:4200 --http-host-header localhost:4200
pause
