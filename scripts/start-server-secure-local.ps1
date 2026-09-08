[CmdletBinding()]
param(
    [string]$DatabaseUrl = 'jdbc:mysql://127.0.0.1:3306/vcampus?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true',
    [string]$DatabaseUser = 'root',
    [int]$Port = 8888,
    [int]$ClientReadTimeoutMillis = 1800000,
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
    $existingListener = Get-NetTCPConnection -LocalPort $Port -State Listen `
        -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $existingListener) {
        throw "Port $Port is already listening (PID $($existingListener.OwningProcess)). " +
            'Stop that process before starting a new VCampus Server.'
    }

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
        "-Dvcampus.server.client-read-timeout=$ClientReadTimeoutMillis"
    )

    # 宿舍的月度出账任务要在账单上记「是谁出的账」，拿不到这个用户号它就整月跳过不执行。
    # 填宿管员账号的用户号——登录客户端后右上角「用户编号」那一栏就是，演示库里是 7。
    # 例：$env:VCAMPUS_DORM_SCHEDULER_OPERATOR = '7'
    if (-not [string]::IsNullOrWhiteSpace($env:VCAMPUS_DORM_SCHEDULER_OPERATOR)) {
        $arguments += "-Dvcampus.dorm.scheduler.operator=$($env:VCAMPUS_DORM_SCHEDULER_OPERATOR)"
    }

    $arguments += @('-jar', $jar)

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
