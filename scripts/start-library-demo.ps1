[CmdletBinding()]
param([string]$JarPath = '')

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $PSScriptRoot '..\vcampus-client\target\vCampusClient.jar'
}
if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw "Client jar not found: $JarPath. Run .\mvnw.cmd -pl vcampus-client -am -DskipTests package first."
}

$jar = (Resolve-Path -LiteralPath $JarPath).Path
& java '-Dvcampus.client.mode=demo' -jar $jar
exit $LASTEXITCODE
