<#
.SYNOPSIS
    邻里鲜生 · 数据库环境切换（本机 MySQL80 <-> Docker 容器 MySQL）

.DESCRIPTION
    本机 3306 端口只能被一方占用，本脚本负责两边来回切换，并显示当前实际在用哪个库。

.EXAMPLE
    .\deploy\switch-db.ps1
    .\deploy\switch-db.ps1 project
    .\deploy\switch-db.ps1 local
#>
[CmdletBinding()]
param(
    [ValidateSet('status', 'project', 'local')]
    [string]$Mode = 'status',

    # 内部使用：切换完成后保留窗口，方便查看结果
    [switch]$KeepOpen
)

$ErrorActionPreference = 'Stop'

$RepoRoot    = Split-Path -Parent $PSScriptRoot
$ComposeFile = 'deploy/docker-compose.dev.yml'
$ServiceName = 'MySQL80'

Set-Location $RepoRoot

function Test-IsAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($id)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-DockerExe {
    $cmd = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $cmd) {
        throw '未找到 docker 命令。请确认 Docker Desktop 已启动，或本机 Docker 包装脚本已加入 PATH。'
    }
    return $cmd.Source
}

function Get-ContainerHealth {
    param([string]$Name)
    $ErrorActionPreference = 'SilentlyContinue'
    try {
        $state = & docker inspect --format '{{.State.Status}}' $Name 2>$null
        if (-not $state) { return '不存在' }
        if ($state -eq 'running') {
            $health = & docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}-{{end}}' $Name 2>$null
            if ($health -and $health -ne '-') { return "$state/$health" }
        }
        return $state
    }
    catch {
        return '未知'
    }
}

function Wait-MySqlHealthy {
    param([int]$TimeoutSeconds = 120)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $health = Get-ContainerHealth -Name 'linli-mysql'
        if ($health -like '*healthy*') { return $true }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    return $false
}

function Get-PortOwner {
    $conn = Get-NetTCPConnection -State Listen -LocalPort 3306 -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $conn) { return $null }
    $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
    return [pscustomobject]@{
        Pid  = $conn.OwningProcess
        Name = if ($proc) { $proc.ProcessName } else { '未知' }
    }
}

function Show-Status {
    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    $localStatus = if ($svc) { $svc.Status } else { '未安装' }

    $dockerOk = $true
    try { Get-DockerExe | Out-Null } catch { $dockerOk = $false }

    $mysqlState = if ($dockerOk) { Get-ContainerHealth -Name 'linli-mysql' } else { '不可用' }
    $redisState = if ($dockerOk) { Get-ContainerHealth -Name 'linli-redis' } else { '不可用' }

    $owner = Get-PortOwner
    $ownerText = if ($owner) { "$($owner.Name) (PID $($owner.Pid))" } else { '无进程监听' }

    $inUse = '未知'
    if ($localStatus -eq 'Running' -and $mysqlState -like '*running*') {
        $inUse = '冲突：两套都在运行，请执行 project 或 local 收敛'
    }
    elseif ($localStatus -eq 'Running') {
        $inUse = '本机 MySQL80'
    }
    elseif ($mysqlState -like '*running*') {
        $inUse = 'Docker 容器（linli-mysql）'
    }
    elseif ($owner) {
        $inUse = "未知进程：$ownerText"
    }
    else {
        $inUse = '未启动'
    }

    Write-Host ''
    Write-Host '================ 当前数据库状态 ================' -ForegroundColor Cyan
    Write-Host ("  本机服务 MySQL80 : {0}" -f $localStatus)
    Write-Host ("  容器 linli-mysql : {0}" -f $mysqlState)
    Write-Host ("  容器 linli-redis : {0}" -f $redisState)
    Write-Host ("  3306 端口占用    : {0}" -f $ownerText)
    Write-Host ''
    Write-Host ("  >>> 当前使用：{0}" -f $inUse) -ForegroundColor Yellow
    Write-Host ''
    Write-Host '  切到项目环境（Docker）：.\deploy\switch-db.ps1 project'
    Write-Host '  切回本机数据库        ：.\deploy\switch-db.ps1 local'
    Write-Host '================================================' -ForegroundColor Cyan
    Write-Host ''
}

function Use-ProjectDb {
    Get-DockerExe | Out-Null

    Write-Host '[1/4] 停止本机 MySQL80 ...' -ForegroundColor Green
    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -ne 'Stopped') {
        Stop-Service -Name $ServiceName -Force
        (Get-Service -Name $ServiceName).WaitForStatus('Stopped', '00:01:00')
    }

    Write-Host '[2/4] 启动项目容器 ...' -ForegroundColor Green
    & docker compose -f $ComposeFile up -d | Out-Host

    Write-Host '[3/4] 等待 MySQL 就绪 ...' -ForegroundColor Green
    if (-not (Wait-MySqlHealthy)) {
        throw 'MySQL 容器未在 120 秒内变为 healthy，请执行：docker compose -f deploy/docker-compose.dev.yml logs mysql'
    }

    Write-Host '[4/4] 校验连接 ...' -ForegroundColor Green
    & docker exec linli-mysql mysqladmin ping -uroot -proot123456 | Out-Host

    Write-Host '已切换到项目数据库：127.0.0.1:3306 -> linli-mysql 容器' -ForegroundColor Cyan
    Write-Host '测试账号：13800000001 / 13800000002 / 13800000003，口令 123456'
}

function Use-LocalDb {
    Get-DockerExe | Out-Null

    Write-Host '[1/3] 停止项目容器 ...' -ForegroundColor Green
    & docker compose -f $ComposeFile down | Out-Host

    Write-Host '[2/3] 启动本机 MySQL80 ...' -ForegroundColor Green
    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if (-not $svc) { throw "未找到服务 $ServiceName" }
    if ($svc.Status -ne 'Running') {
        Start-Service -Name $ServiceName
        (Get-Service -Name $ServiceName).WaitForStatus('Running', '00:01:00')
    }

    Write-Host '[3/3] 等待 3306 监听 ...' -ForegroundColor Green
    for ($i = 0; $i -lt 30; $i++) {
        if (Get-PortOwner) { break }
        Start-Sleep -Seconds 1
    }

    Write-Host '已切回本机数据库：MySQL80 正在监听 3306' -ForegroundColor Cyan
}

# ---------- 入口 ----------
if ($Mode -ne 'status' -and -not (Test-IsAdmin)) {
    Write-Host '切换数据库需要管理员权限，正在请求提权 ...' -ForegroundColor Yellow
    $argList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $PSCommandPath, $Mode, '-KeepOpen')
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $argList | Out-Null
    return
}

Write-Host ''
Write-Host '邻里鲜生 · 数据库环境切换' -ForegroundColor Cyan
Write-Host ''

switch ($Mode) {
    'project' { Use-ProjectDb }
    'local'   { Use-LocalDb }
    default   { }
}

Show-Status

if ($KeepOpen) {
    Read-Host '按回车关闭窗口' | Out-Null
}
