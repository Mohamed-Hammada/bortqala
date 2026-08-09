#!/usr/bin/env bash
set -euo pipefail
NAME=${1:-multivendor-biometric-access-hub}
git init
git add .
git commit -m "Initial multi-vendor biometric/access integration hub"
gh repo create "$NAME" --private --source=. --remote=origin --push
