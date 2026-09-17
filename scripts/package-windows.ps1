param([string]$OutputDirectory = '', [switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (-not $SkipBuild) {
    Push-Location $root
    try {
        & .\mvnw.cmd -pl vcampus-client,vcampus-server -am package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'Maven package failed.' }
    } finally { Pop-Location }
}
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $root ('target\windows-release-' + (Get-Date -Format yyyyMMdd-HHmmss))
}
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Output directory already exists. Choose a new directory.' }
$null = New-Item -ItemType Directory -Path $OutputDirectory
$templates = Join-Path $PSScriptRoot 'distribution'
foreach ($kind in @('Client', 'Server')) {
    $directory = Join-Path $OutputDirectory "VCampus-$kind"
    $null = New-Item -ItemType Directory -Path $directory
    $module = $kind.ToLowerInvariant()
    Copy-Item -LiteralPath (Join-Path $root "vcampus-$module\target\vCampus$kind.jar") -Destination $directory
    Copy-Item -LiteralPath (Join-Path $templates 'launch.ps1') -Destination $directory
    Get-ChildItem -LiteralPath $templates -Filter '*.txt' | Copy-Item -Destination $directory
    Copy-Item -LiteralPath (Join-Path $templates "start-$module.cmd") -Destination $directory
    if ($kind -eq 'Client') {
        Copy-Item -LiteralPath (Join-Path $templates 'start-local-client.cmd') -Destination $directory
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $directory 'launch.ps1') -Mode $module -CheckOnly
    if ($LASTEXITCODE -ne 0) { throw "$kind launcher validation failed." }
    Compress-Archive -LiteralPath $directory -DestinationPath (Join-Path $OutputDirectory "VCampus-$kind.zip")
}
Write-Host "Packages ready: $OutputDirectory"
