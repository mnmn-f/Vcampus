[CmdletBinding()]
param(
    [string]$JarPath = '',
    [string]$ServerHost = '',
    [string]$ServerPort = '',
    [string]$ConnectTimeoutMillis = '',
    [string]$ReadTimeoutMillis = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Read-Setting {
    param(
        [string]$Explicit,
        [string]$EnvironmentValue,
        [string]$Fallback,
        [string]$Name,
        [int]$Minimum,
        [int]$Maximum
    )
    $text = $Explicit
    if ([string]::IsNullOrWhiteSpace($text)) { $text = $EnvironmentValue }
    if ([string]::IsNullOrWhiteSpace($text)) { $text = $Fallback }
    [int]$value = 0
    if (-not [int]::TryParse($text, [ref]$value) -or $value -lt $Minimum -or $value -gt $Maximum) {
        throw "$Name must be an integer from $Minimum to $Maximum"
    }
    return $value
}

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $PSScriptRoot '..\vcampus-client\target\vCampusClient.jar'
}
if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw "Client jar not found: $JarPath. Run .\mvnw.cmd -Pquality verify first."
}
$jar = (Resolve-Path -LiteralPath $JarPath).Path
$hostValue = $ServerHost
if ([string]::IsNullOrWhiteSpace($hostValue)) { $hostValue = $env:VCAMPUS_SERVER_HOST }
if ([string]::IsNullOrWhiteSpace($hostValue)) { $hostValue = '127.0.0.1' }
$portValue = Read-Setting $ServerPort $env:VCAMPUS_SERVER_PORT '8888' 'ServerPort' 1 65535
$connectValue = Read-Setting $ConnectTimeoutMillis `
    $env:VCAMPUS_CLIENT_CONNECT_TIMEOUT '5000' 'ConnectTimeoutMillis' 1 2147483647
$readValue = Read-Setting $ReadTimeoutMillis $env:VCAMPUS_CLIENT_READ_TIMEOUT `
    '5000' 'ReadTimeoutMillis' 1 2147483647

& java '-Dvcampus.client.mode=network' "-Dvcampus.server.host=$hostValue" `
    "-Dvcampus.server.port=$portValue" `
    "-Dvcampus.server.connect-timeout=$connectValue" `
    "-Dvcampus.server.read-timeout=$readValue" -jar $jar
exit $LASTEXITCODE
