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

# Read only the small set of values needed by the deploy helper.
# Docker Compose continues to parse the full .env file itself.
read_env_value() {
    key="$1"
    value="$(grep -E "^${key}=" .env | tail -n 1 | cut -d= -f2- || true)"
    printf '%s' "$value"
}
if [ -z "${BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY:-}" ]; then
    BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY="$(read_env_value BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY)"
fi
if [ -z "${POSTGRES_USER:-}" ]; then
    POSTGRES_USER="$(read_env_value POSTGRES_USER)"
fi
if [ -z "${POSTGRES_DB:-}" ]; then
    POSTGRES_DB="$(read_env_value POSTGRES_DB)"
fi
export BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY POSTGRES_USER POSTGRES_DB

echo "Building and starting Docker containers (PostgreSQL, Backend, Frontend)..."
docker compose up --build -d

if [ "${BEMO_ENABLE_ALL_FEATURES_ON_DEPLOY:-true}" = "true" ]; then
    echo ""
    echo "Applying one-time all-feature entitlement bootstrap..."
    ./enable-all-features-docker.sh
fi

echo ""
echo "========================================================"
echo "  Deployment complete!"
echo "  Application Web UI:  http://localhost"
echo "  Spring Boot API:     http://localhost:8080"
echo "========================================================"
