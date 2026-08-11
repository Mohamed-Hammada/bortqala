#!/usr/bin/env bash
set -euo pipefail
python3 ./APPLY_PENDING_FIXES.py
printf '\nPatch applied. Run the frontend and backend tests listed in APPLY_NOTES.md.\n'
