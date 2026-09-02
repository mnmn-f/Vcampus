[CmdletBinding()]
param(
    [string]$JarPath = '',
    [string]$Port = '',
    [string]$MaxConnections = '',
    [string]$ClientReadTimeoutMillis = ''
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
    $JarPath = Join-Path $PSScriptRoot '..\vcampus-server\target\vCampusServer.jar'
}
if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw "Server jar not found: $JarPath. Run .\mvnw.cmd -Pquality verify first."
}
$jar = (Resolve-Path -LiteralPath $JarPath).Path
$portValue = Read-Setting $Port $env:VCAMPUS_SERVER_PORT '8888' 'Port' 1 65535
$maxValue = Read-Setting $MaxConnections $env:VCAMPUS_SERVER_MAX_CONNECTIONS '32' `
    'MaxConnections' 1 2147483647
$timeoutValue = Read-Setting $ClientReadTimeoutMillis `
    $env:VCAMPUS_SERVER_CLIENT_READ_TIMEOUT '30000' 'ClientReadTimeoutMillis' 1 2147483647

$aiModel = $env:VCAMPUS_AI_MODEL
if ([string]::IsNullOrWhiteSpace($aiModel)) { $aiModel = 'gpt-4.1-mini' }
$aiEndpoint = $env:VCAMPUS_AI_ENDPOINT
if ([string]::IsNullOrWhiteSpace($aiEndpoint)) {
    $aiEndpoint = 'https://api.openai.com/v1/responses'
}
if ([string]::IsNullOrWhiteSpace($env:VCAMPUS_AI_API_KEY)) {
    Write-Warning 'VCAMPUS_AI_API_KEY is not set. VCampus will use knowledge-base fallback.'
} else {
    Write-Host "AI Responses API configured: $aiModel at $aiEndpoint" -ForegroundColor Green
}

& java "-Dvcampus.server.port=$portValue" `
    "-Dvcampus.server.max-connections=$maxValue" `
    "-Dvcampus.server.client-read-timeout=$timeoutValue" -jar $jar
exit $LASTEXITCODE
