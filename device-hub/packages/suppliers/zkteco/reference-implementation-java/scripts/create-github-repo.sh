#!/usr/bin/env bash
set -euo pipefail

repo="${1:-Mohamed-Hammada/zkteco-universal-gateway}"
visibility="${2:-private}"

command -v gh >/dev/null 2>&1 || {
  echo "GitHub CLI (gh) is required. Install it and run: gh auth login" >&2
  exit 1
}

gh repo create "$repo" "--$visibility" --source . --remote origin --push
