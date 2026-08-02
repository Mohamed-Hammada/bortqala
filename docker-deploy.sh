#!/usr/bin/env bash
set -e

echo "========================================================"
echo "      BEMO HR PLATFORM - DOCKER ONE-CLICK DEPLOY"
echo "========================================================"
echo ""

if [ ! -f ".env" ]; then
    echo "[.env] file not found. Copying .env.development.example to .env ..."
    cp .env.development.example .env
fi

echo "Building and starting Docker containers (PostgreSQL, Backend, Frontend)..."
docker compose up --build -d

echo ""
echo "========================================================"
echo "  Deployment complete!"
echo "  Application Web UI:  http://localhost"
echo "  Spring Boot API:     http://localhost:8080"
echo "========================================================"
