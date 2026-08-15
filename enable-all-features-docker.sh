#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POSTGRES_USER_VALUE="${POSTGRES_USER:-root}"
POSTGRES_DB_VALUE="${POSTGRES_DB:-bemo_erp}"
MAX_ATTEMPTS="${BEMO_FEATURE_BOOTSTRAP_WAIT_ATTEMPTS:-60}"

echo "[INFO] Waiting for Liquibase to create tenant feature tables..."
attempt=1
while [ "$attempt" -le "$MAX_ATTEMPTS" ]; do
  ready="$(docker compose exec -T db psql -U "$POSTGRES_USER_VALUE" -d "$POSTGRES_DB_VALUE" -tAc "SELECT (to_regclass('public.tenant_features') IS NOT NULL AND to_regclass('public.system_settings') IS NOT NULL);" 2>/dev/null | tr -d '[:space:]' || true)"
  if [ "$ready" = "t" ]; then
    break
  fi
  sleep 2
  attempt=$((attempt + 1))
done

if [ "$attempt" -gt "$MAX_ATTEMPTS" ]; then
  echo "[ERROR] tenant_features/system_settings were not ready in time." >&2
  exit 1
fi

echo "[INFO] Applying one-time enable-all entitlement bootstrap..."
docker compose exec -T db   psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER_VALUE" -d "$POSTGRES_DB_VALUE"   < "$SCRIPT_DIR/scripts/enable-all-features.sql"

echo "[OK] Entitlement bootstrap completed (or was already applied)."
