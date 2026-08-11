<#
Reset-Eysawy-DNS-To-DHCP.ps1

Optional rollback companion.
Restores DNS on active physical adapters to the addresses supplied by DHCP.
#>

$ErrorActionPreference = "Stop"

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)

if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process powershell.exe -Verb RunAs -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", "`"$PSCommandPath`""
    )
    exit
}

$adapters = @(
    Get-NetAdapter |
        Where-Object {
            $_.Status -eq "Up" -and
            $_.HardwareInterface -eq $true
        }
)

if ($adapters.Count -eq 0) {
    Write-Host "No active physical adapter found." -ForegroundColor Red
    Read-Host "Press Enter to close"
    exit 1
}

foreach ($adapter in $adapters) {
    Write-Host "Resetting DNS on $($adapter.Name)..." -ForegroundColor Cyan
    Set-DnsClientServerAddress -InterfaceIndex $adapter.ifIndex -ResetServerAddresses
    Restart-NetAdapter -Name $adapter.Name -Confirm:$false -ErrorAction SilentlyContinue
}

Clear-DnsClientCache -ErrorAction SilentlyContinue
ipconfig /flushdns | Out-Host

Write-Host ""
Write-Host "DNS was reset to DHCP/default settings." -ForegroundColor Green
Read-Host "Press Enter to close"
