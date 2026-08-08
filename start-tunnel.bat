@echo off
title Bemo Cloudflare Tunnel Launcher
echo ========================================================
echo Starting Cloudflare Tunnel for Bemo Angular Frontend...
echo Target: http://localhost:8080 (Host Header: localhost:8080)
echo ========================================================
echo.
"%~dp0cloudflared-windows-amd64.exe" tunnel --url http://localhost:8080 --http-host-header localhost:8080
pause
