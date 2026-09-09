[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ServerHost,
    [int]$ServerPort = 8888
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$probe = Test-NetConnection $ServerHost -Port $ServerPort -WarningAction SilentlyContinue
if (-not $probe.TcpTestSucceeded) {
    throw "Cannot reach VCampus Server at ${ServerHost}:$ServerPort. Check server, IP and firewall."
}
& (Join-Path $PSScriptRoot 'start-client.ps1') `
    -ServerHost $ServerHost -ServerPort $ServerPort
exit $LASTEXITCODE
