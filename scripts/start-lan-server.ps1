[CmdletBinding()]
param(
    [string]$DatabaseUrl = 'jdbc:mysql://127.0.0.1:3306/vcampus?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true',
    [string]$DatabaseUser = 'root',
    [int]$Port = 8888
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$addresses = @(Get-NetIPAddress -AddressFamily IPv4 -AddressState Preferred `
    -ErrorAction SilentlyContinue | Where-Object {
        $_.IPAddress -ne '127.0.0.1' -and -not $_.IPAddress.StartsWith('169.254.')
    } | Sort-Object InterfaceMetric | Select-Object -ExpandProperty IPAddress -Unique)

& (Join-Path $PSScriptRoot 'start-server-secure-local.ps1') `
    -DatabaseUrl $DatabaseUrl -DatabaseUser $DatabaseUser -Port $Port

Write-Host ''
Write-Host 'VCampus shared server is running.' -ForegroundColor Green
if ($addresses.Count -eq 0) {
    Write-Warning 'No LAN IPv4 address was detected. Run ipconfig and use the active adapter IPv4.'
} else {
    foreach ($address in $addresses) {
        Write-Host "Teammate command: .\scripts\start-lan-client.ps1 -ServerHost $address -ServerPort $Port"
    }
}
Write-Host 'Teammates connect only to this TCP port; MySQL remains on the server computer.'
Write-Host "If remote Test-NetConnection fails, allow inbound TCP $Port for the Private firewall profile."
