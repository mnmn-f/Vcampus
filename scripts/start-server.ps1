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
    $env:VCAMPUS_SERVER_CLIENT_READ_TIMEOUT '1800000' 'ClientReadTimeoutMillis' 1 2147483647

$aiModel = $env:VCAMPUS_AI_MODEL
if ([string]::IsNullOrWhiteSpace($aiModel)) { $aiModel = 'deepseek-v4-flash' }
$aiEndpoint = $env:VCAMPUS_AI_ENDPOINT
if ([string]::IsNullOrWhiteSpace($aiEndpoint)) {
    $aiEndpoint = 'https://api.deepseek.com/responses'
}
$aiKey = $env:VCAMPUS_AI_API_KEY
if ([string]::IsNullOrWhiteSpace($aiKey)) { $aiKey = $env:DEEPSEEK_API_KEY }
if ([string]::IsNullOrWhiteSpace($aiKey)) {
    Write-Warning 'VCAMPUS_AI_API_KEY/DEEPSEEK_API_KEY is not set. VCampus will use knowledge-base fallback.'
} else {
    Write-Host "AI Responses API configured: $aiModel at $aiEndpoint" -ForegroundColor Green
}

$arguments = @(
    "-Dvcampus.server.port=$portValue",
    "-Dvcampus.server.max-connections=$maxValue",
    "-Dvcampus.server.client-read-timeout=$timeoutValue"
)

# 宿舍的月度出账任务要在账单上记「是谁出的账」，拿不到这个用户号它就整月跳过不执行。
# 填宿管员账号的用户号——登录客户端后右上角「用户编号」那一栏就是，演示库里是 7。
# 例：$env:VCAMPUS_DORM_SCHEDULER_OPERATOR = '7'
if (-not [string]::IsNullOrWhiteSpace($env:VCAMPUS_DORM_SCHEDULER_OPERATOR)) {
    $arguments += "-Dvcampus.dorm.scheduler.operator=$($env:VCAMPUS_DORM_SCHEDULER_OPERATOR)"
}

$arguments += @('-jar', $jar)
& java @arguments
exit $LASTEXITCODE
