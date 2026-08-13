param([string]$Name = "multivendor-biometric-access-hub")
$ErrorActionPreference="Stop"
git init
git add .
git commit -m "Initial multi-vendor biometric/access integration hub"
gh repo create $Name --private --source=. --remote=origin --push
