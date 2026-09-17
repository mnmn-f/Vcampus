param(
    [ValidateSet('server', 'client', 'local')]
    [string]$Mode = 'client',
    [string]$ServerHost = '',
    [ValidateRange(1, 65535)]
    [int]$Port = 8888,
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
try {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCommand) { throw 'Java not found. Install JDK 17+ and reopen this launcher.' }
    $java = $javaCommand.Source
    # Windows PowerShell treats native stderr as errors with Stop enabled.
    $ErrorActionPreference = 'Continue'
    $versionText = (& $java -version 2>&1 | Out-String)
    $versionExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($versionExit -ne 0 -or $versionText -notmatch 'version "(?<major>\d+)') {
        throw 'Cannot detect Java version. Install JDK 17 or newer.'
    }
    if ([int]$Matches.major -lt 17) { throw 'Java 17 or newer is required.' }
    $jarName = if ($Mode -eq 'server') { 'vCampusServer.jar' } else { 'vCampusClient.jar' }
    $jar = Join-Path $PSScriptRoot $jarName
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing $jarName. Extract the entire ZIP first." }
    if ($CheckOnly) {
        Write-Host "OK: Java, launcher and $jarName are ready."
        exit 0
    }

    if ($Mode -ne 'server') {
        if ($Mode -eq 'local') { $ServerHost = '127.0.0.1' }
        if ([string]::IsNullOrWhiteSpace($ServerHost)) {
            $ServerHost = Read-Host 'Server IP or hostname (Enter for localhost: 127.0.0.1)'
            if ([string]::IsNullOrWhiteSpace($ServerHost)) { $ServerHost = '127.0.0.1' }
        }
        $ServerHost = $ServerHost.Trim()
        $probe = New-Object System.Net.Sockets.TcpClient
        try {
            $pending = $probe.ConnectAsync($ServerHost, $Port)
            if (-not $pending.Wait(5000)) { throw 'Connection timed out.' }
            $pending.GetAwaiter().GetResult()
        } catch {
            throw "Cannot connect to ${ServerHost}:$Port. Check server, hotspot and firewall."
        } finally { $probe.Dispose() }
        Write-Host "Starting client -> ${ServerHost}:$Port"
        & $java '-Dvcampus.client.mode=network' "-Dvcampus.server.host=$ServerHost" "-Dvcampus.server.port=$Port" -jar $jar
        if ($LASTEXITCODE -ne 0) { throw "Client exited with code $LASTEXITCODE." }
    } else {
        $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($listener) {
            throw "Port $Port is already in use. If VCampus is running, just start the client."
        }
        $dbUser = Read-Host 'MySQL username (Enter for root)'
        if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = 'root' }
        $password = Read-Host 'MySQL password (hidden input)' -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
        $previousUser = $env:VCAMPUS_DB_USER
        $previousPassword = $env:VCAMPUS_DB_PASSWORD
        try {
            $env:VCAMPUS_DB_USER = $dbUser
            $env:VCAMPUS_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
            Write-Host "Starting server on TCP $Port. Keep this window open. Ctrl+C stops it."
            Write-Host 'MySQL must be running with the existing vcampus database.'
            Write-Host 'LAN IP addresses (share the active Wi-Fi IP with classmates):'
            Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway } |
                ForEach-Object { Write-Host "$($_.InterfaceAlias): $($_.IPv4Address.IPAddress)" }
            & $java "-Dvcampus.server.port=$Port" -jar $jar
            if ($LASTEXITCODE -ne 0) { throw "Server exited with code $LASTEXITCODE." }
        } finally {
            $env:VCAMPUS_DB_USER = $previousUser
            $env:VCAMPUS_DB_PASSWORD = $previousPassword
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
            $password.Dispose()
        }
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
