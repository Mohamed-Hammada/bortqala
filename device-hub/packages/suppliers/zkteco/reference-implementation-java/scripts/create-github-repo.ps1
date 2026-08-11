param(
    [string]$Repository = "Mohamed-Hammada/zkteco-universal-gateway",
    [ValidateSet("public", "private", "internal")]
    [string]$Visibility = "private"
)

$ErrorActionPreference = "Stop"
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) is required. Install it and run: gh auth login"
}

gh repo create $Repository --$Visibility --source . --remote origin --push
