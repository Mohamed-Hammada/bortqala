<#
Fix-Eysawy-DNS.ps1

Repairs the Windows DNS condition previously seen with:
  app.eysawy.dpdns.org

What it does:
  1. Elevates to Administrator automatically.
  2. Finds active physical network adapters (Wi-Fi/Ethernet).
  3. Sets Cloudflare DNS for BOTH IPv4 and IPv6.
  4. Flushes the Windows DNS cache.
  5. Restarts the affected adapter(s).
  6. Compares normal Windows DNS with Cloudflare 1.1.1.1.
  7. Tests HTTPS access to the site.

Cloudflare DNS used:
  IPv4: 1.1.1.1, 1.0.0.1
  IPv6: 2606:4700:4700::1111, 2606:4700:4700::1001
#>

param(
    [string]$HostName = "app.eysawy.dpdns.org",
    [switch]$NoAdapterRestart
)

$ErrorActionPreference = "Stop"

function Write-Step($Text) {
    Write-Host ""
    Write-Host "==> $Text" -ForegroundColor Cyan
}

function Write-OK($Text) {
    Write-Host "[OK] $Text" -ForegroundColor Green
}

function Write-WarnMsg($Text) {
    Write-Host "[WARN] $Text" -ForegroundColor Yellow
}

function Write-Fail($Text) {
    Write-Host "[FAIL] $Text" -ForegroundColor Red
}

# --- Elevate automatically ---
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
$isAdmin = $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "Administrator rights are required. Opening an elevated PowerShell window..."

    $argsList = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", "`"$PSCommandPath`"",
        "-HostName", "`"$HostName`""
    )

    if ($NoAdapterRestart) {
        $argsList += "-NoAdapterRestart"
    }

    Start-Process -FilePath "powershell.exe" -Verb RunAs -ArgumentList $argsList
    exit
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkCyan
Write-Host " Eysawy / Cloudflare DNS Repair" -ForegroundColor Cyan
Write-Host " Target: $HostName" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkCyan

$ipv4Dns = @("1.1.1.1", "1.0.0.1")
$ipv6Dns = @("2606:4700:4700::1111", "2606:4700:4700::1001")

# --- Check public Cloudflare DNS first ---
Write-Step "Checking the domain directly against Cloudflare DNS (1.1.1.1)"

$publicDnsOK = $false
try {
    $public = Resolve-DnsName -Name $HostName -Server "1.1.1.1" -Type A -DnsOnly -ErrorAction Stop
    $publicIPs = @($public | Where-Object { $_.IPAddress } | Select-Object -ExpandProperty IPAddress -Unique)

    if ($publicIPs.Count -gt 0) {
        Write-OK "Cloudflare public DNS resolves $HostName -> $($publicIPs -join ', ')"
        $publicDnsOK = $true
    } else {
        Write-WarnMsg "Cloudflare DNS responded, but no IPv4 address was returned."
    }
}
catch {
    Write-Fail "Cloudflare public DNS could not resolve $HostName."
    Write-Host "This is probably NOT a Windows DNS-cache problem."
    Write-Host "Check the Cloudflare DNS record / tunnel before changing the PC."
    Write-Host ""
    Read-Host "Press Enter to close"
    exit 2
}

# --- Find active physical adapters ---
Write-Step "Finding active physical network adapters"

$adapters = @(
    Get-NetAdapter -ErrorAction Stop |
        Where-Object {
            $_.Status -eq "Up" -and
            $_.HardwareInterface -eq $true
        }
)

if ($adapters.Count -eq 0) {
    Write-Fail "No active physical Wi-Fi/Ethernet adapter was found."
    Write-Host ""
    Get-NetAdapter | Format-Table -AutoSize Name, InterfaceDescription, Status, ifIndex
    Write-Host ""
    Read-Host "Press Enter to close"
    exit 3
}

foreach ($adapter in $adapters) {
    Write-Host ("  - {0} (ifIndex {1})" -f $adapter.Name, $adapter.ifIndex)
}

# --- Show previous DNS ---
Write-Step "Current DNS configuration"

foreach ($adapter in $adapters) {
    Write-Host ""
    Write-Host "[$($adapter.Name)]" -ForegroundColor White

    Get-DnsClientServerAddress -InterfaceIndex $adapter.ifIndex |
        Where-Object { $_.ServerAddresses.Count -gt 0 } |
        Format-Table -AutoSize AddressFamily, ServerAddresses
}

# --- Apply Cloudflare IPv4 + IPv6 DNS separately ---
Write-Step "Setting Cloudflare DNS for IPv4 and IPv6"

foreach ($adapter in $adapters) {
    try {
        $v4Obj = Get-DnsClientServerAddress `
            -InterfaceIndex $adapter.ifIndex `
            -AddressFamily IPv4 `
            -ErrorAction Stop

        $v4Obj | Set-DnsClientServerAddress `
            -ServerAddresses $ipv4Dns `
            -Validate `
            -ErrorAction Stop

        Write-OK "$($adapter.Name): IPv4 DNS = $($ipv4Dns -join ', ')"

        $v6Obj = Get-DnsClientServerAddress `
            -InterfaceIndex $adapter.ifIndex `
            -AddressFamily IPv6 `
            -ErrorAction Stop

        $v6Obj | Set-DnsClientServerAddress `
            -ServerAddresses $ipv6Dns `
            -Validate `
            -ErrorAction Stop

        Write-OK "$($adapter.Name): IPv6 DNS = $($ipv6Dns -join ', ')"
    }
    catch {
        Write-Fail "$($adapter.Name): could not update DNS."
        Write-Host $_.Exception.Message
    }
}

# --- Flush DNS ---
Write-Step "Flushing Windows DNS caches"

try {
    Clear-DnsClientCache -ErrorAction Stop
    Write-OK "Clear-DnsClientCache completed."
}
catch {
    Write-WarnMsg "Clear-DnsClientCache failed: $($_.Exception.Message)"
}

try {
    ipconfig /flushdns | Out-Host
}
catch {
    Write-WarnMsg "ipconfig /flushdns failed: $($_.Exception.Message)"
}

# --- Restart adapters, as was required in the successful repair ---
if (-not $NoAdapterRestart) {
    Write-Step "Restarting active physical network adapter(s)"
    Write-WarnMsg "Internet may disconnect briefly."

    foreach ($adapter in $adapters) {
        try {
            Restart-NetAdapter -Name $adapter.Name -Confirm:$false -ErrorAction Stop
            Write-OK "Restarted $($adapter.Name)"
        }
        catch {
            Write-WarnMsg "Could not restart $($adapter.Name): $($_.Exception.Message)"
        }
    }

    Start-Sleep -Seconds 4
} else {
    Write-WarnMsg "Adapter restart skipped because -NoAdapterRestart was used."
}

# Flush once more after adapter restart.
try {
    Clear-DnsClientCache -ErrorAction SilentlyContinue
    ipconfig /flushdns | Out-Null
}
catch {}

# --- Verify configured DNS ---
Write-Step "DNS configuration after repair"

foreach ($adapter in $adapters) {
    Write-Host ""
    Write-Host "[$($adapter.Name)]" -ForegroundColor White

    Get-DnsClientServerAddress -InterfaceIndex $adapter.ifIndex |
        Where-Object { $_.ServerAddresses.Count -gt 0 } |
        Format-Table -AutoSize AddressFamily, ServerAddresses
}

# --- Test normal Windows DNS ---
Write-Step "Testing Windows DNS resolution"

$windowsDnsOK = $false
try {
    $normal = Resolve-DnsName -Name $HostName -Type A -DnsOnly -ErrorAction Stop
    $normalIPs = @($normal | Where-Object { $_.IPAddress } | Select-Object -ExpandProperty IPAddress -Unique)

    if ($normalIPs.Count -gt 0) {
        Write-OK "Windows DNS resolves $HostName -> $($normalIPs -join ', ')"
        $windowsDnsOK = $true
    } else {
        Write-Fail "Windows DNS returned no IPv4 address."
    }
}
catch {
    Write-Fail "Windows still cannot resolve $HostName."
    Write-Host $_.Exception.Message
}

# --- Test HTTPS ---
Write-Step "Testing HTTPS"

$httpsOK = $false
try {
    $curlOutput = & curl.exe `
        --silent `
        --show-error `
        --location `
        --head `
        --max-time 15 `
        "https://$HostName/" 2>&1

    $curlOutput | Select-Object -First 15 | ForEach-Object { Write-Host $_ }

    if ($LASTEXITCODE -eq 0) {
        Write-OK "HTTPS request completed."
        $httpsOK = $true
    } else {
        Write-Fail "curl exited with code $LASTEXITCODE."
    }
}
catch {
    Write-Fail "HTTPS test failed: $($_.Exception.Message)"
}

# --- Result ---
Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkCyan

if ($publicDnsOK -and $windowsDnsOK -and $httpsOK) {
    Write-Host " REPAIR SUCCESSFUL" -ForegroundColor Green
    Write-Host " $HostName should now load normally." -ForegroundColor Green
}
elseif ($publicDnsOK -and -not $windowsDnsOK) {
    Write-Host " LOCAL DNS STILL HAS A PROBLEM" -ForegroundColor Red
    Write-Host " Cloudflare resolves correctly, but Windows does not." -ForegroundColor Yellow
    Write-Host " Reboot Windows or run this script once more after disconnecting/reconnecting Wi-Fi." -ForegroundColor Yellow
}
elseif ($publicDnsOK -and $windowsDnsOK -and -not $httpsOK) {
    Write-Host " DNS IS WORKING, BUT HTTPS/APP IS NOT" -ForegroundColor Yellow
    Write-Host " The next place to check is the Cloudflare Tunnel / origin application." -ForegroundColor Yellow
}
else {
    Write-Host " REPAIR INCOMPLETE" -ForegroundColor Red
    Write-Host " Review the messages above." -ForegroundColor Yellow
}

Write-Host "============================================================" -ForegroundColor DarkCyan
Write-Host ""
Read-Host "Press Enter to close"
