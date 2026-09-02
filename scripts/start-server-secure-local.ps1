[CmdletBinding()]
param(
    [string]$DatabaseUrl = 'jdbc:mysql://127.0.0.1:3306/vcampus?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false',
    [string]$DatabaseUser = 'root',
    [int]$Port = 8888,
    [int]$StartupTimeoutSeconds = 15
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$originalUrl = $env:VCAMPUS_DB_URL
$originalUser = $env:VCAMPUS_DB_USER
$originalPassword = $env:VCAMPUS_DB_PASSWORD
$passwordPointer = [IntPtr]::Zero
$environmentRestored = $false

try {
    $securePassword = Read-Host 'MySQL password (input is hidden)' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    Write-Host 'Password received'

    $env:VCAMPUS_DB_URL = $DatabaseUrl
    $env:VCAMPUS_DB_USER = $DatabaseUser
    $env:VCAMPUS_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
        $passwordPointer)

    $jar = (Resolve-Path (Join-Path $PSScriptRoot `
        '..\vcampus-server\target\vCampusServer.jar')).Path
    $java = (Get-Command java -ErrorAction Stop).Source
    $arguments = @(
        "-Dvcampus.server.port=$Port",
        '-Dvcampus.server.max-connections=32',
        '-Dvcampus.server.client-read-timeout=30000',
        '-jar',
        $jar
    )

    Write-Host 'Starting VCampus Server'
    Write-Host "Java command: $java -Dvcampus.server.port=$Port -jar $jar"
    $serverProcess = Start-Process -FilePath $java -ArgumentList $arguments `
        -WorkingDirectory (Split-Path $PSScriptRoot -Parent) -NoNewWindow -PassThru
    Write-Host "Server process started (PID $($serverProcess.Id))"

    $env:VCAMPUS_DB_URL = $originalUrl
    $env:VCAMPUS_DB_USER = $originalUser
    $env:VCAMPUS_DB_PASSWORD = $originalPassword
    $environmentRestored = $true
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    $passwordPointer = [IntPtr]::Zero
    $securePassword = $null

    Write-Host "Waiting for port $Port"
    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    $listening = $false
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($serverProcess.HasExited) {
            throw "VCampus Server exited with code $($serverProcess.ExitCode). " +
                'See the Java error output above.'
        }
        $probe = New-Object System.Net.Sockets.TcpClient
        try {
            $probe.Connect('127.0.0.1', $Port)
            $listening = $true
            break
        } catch [System.Net.Sockets.SocketException] {
            Start-Sleep -Milliseconds 250
        } finally {
            $probe.Dispose()
        }
    }
    if (-not $listening) {
        throw "VCampus Server did not listen on port $Port within " +
            "$StartupTimeoutSeconds seconds. See the Java error output above."
    }
    Write-Host "Port $Port is listening"
} finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    if (-not $environmentRestored) {
        $env:VCAMPUS_DB_URL = $originalUrl
        $env:VCAMPUS_DB_USER = $originalUser
        $env:VCAMPUS_DB_PASSWORD = $originalPassword
    }
}
