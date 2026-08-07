$ErrorActionPreference = "Stop"
python .\APPLY_PENDING_FIXES.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host ""
Write-Host "Patch applied. Run the frontend and backend tests listed in APPLY_NOTES.md."
