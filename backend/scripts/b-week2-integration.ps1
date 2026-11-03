param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Password = '123456',
    [string]$AccountPhone = '13900000001',
    [string]$AccountPassword = 'Test1234',
    [string]$OtherMemberPhone = '13900000009',
    [string]$OtherMemberPassword = 'Test1234'
)

$ErrorActionPreference = 'Stop'
$script:Root = Split-Path -Parent $PSScriptRoot
$script:Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$script:Ev = Join-Path ([System.IO.Path]::GetTempPath()) ('b-week2-integration-' + $script:Stamp)
New-Item -ItemType Directory -Force -Path $script:Ev | Out-Null
$script:LogFile = Join-Path $script:Ev "integration-$($script:Stamp).log"
$script:Steps = New-Object System.Collections.Generic.List[object]

function Write-Log {
    param([string]$Text)
    Add-Content -LiteralPath $script:LogFile -Value $Text -Encoding UTF8
    Write-Host $Text
}

function Invoke-Api {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Token,
        $Body,
        [string]$IdempotencyKey,
        [switch]$Quiet
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    if ($IdempotencyKey) { $headers['Idempotency-Key'] = $IdempotencyKey }
    $params = @{
        Uri               = "$BaseUrl$Path"
        Method            = $Method
        Headers           = $headers
        TimeoutSec        = 60
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $Body) {
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 10 -Compress }
        $params['Body'] = $json
        $params['ContentType'] = 'application/json; charset=utf-8'
    }
    $status = -1
    $raw = ''
    try {
        $resp = Invoke-WebRequest @params
        $status = [int]$resp.StatusCode
        $raw = $resp.Content
    }
    catch {
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        $raw = $_.ErrorDetails.Message
        if (-not $raw) { $raw = $_.Exception.Message }
    }
    if ($raw -is [byte[]]) { $raw = [System.Text.Encoding]::UTF8.GetString($raw) }
    $parsed = $null
    try { $parsed = $raw | ConvertFrom-Json } catch { }
    $code = if ($null -ne $parsed) { $parsed.code } else { $null }
    $message = if ($null -ne $parsed) { $parsed.message } else { $null }
    $step = [pscustomobject]@{
        name    = $Name
        method  = $Method
        path    = $Path
        http    = $status
        code    = $code
        message = $message
        data    = if ($null -ne $parsed) { $parsed.data } else { $null }
        raw     = $raw
    }
    if (-not $Quiet) {
        $script:Steps.Add($step)
        Write-Log ("[{0}] {1} {2} [{3}] -> HTTP {4} code={5} message={6}" -f (Get-Date -Format 'HH:mm:ss'), $Method, $Path, $Name, $status, $code, $message)
        $detail = Join-Path $script:Ev ("{0}-{1}.json" -f $script:Stamp, ($Name -replace '[^A-Za-z0-9_.-]', '_'))
        Set-Content -LiteralPath $detail -Value $raw -Encoding UTF8
    }
    return $step
}

function Assert-Code {
    param($Step, [int]$Expected, [string]$Note = '')
    if ($Step.code -ne $Expected) {
        Write-Log ("  !! FAIL {0}: expected {1}, got {2} ({3})" -f $Step.name, $Expected, $Step.code, $Step.message)
        throw "assert failed: $($Step.name) expected $Expected got $($Step.code)"
    }
    Write-Log ("  PASS {0}: code {1} {2}" -f $Step.name, $Expected, $Note)
}

function Login {
    param([string]$Phone, [string]$Pwd, [string]$Name)
    $r = Invoke-Api -Name $Name -Method POST -Path '/api/v1/auth/login' -Body @{ phone = $Phone; password = $Pwd }
    Assert-Code -Step $r -Expected 0
    return $r.data.token
}

function Get-OrderStatus {
    param([string]$OrderNo, [string]$Token)
    $r = Invoke-Api -Name 'orderStatus' -Method GET -Path "/api/v1/orders/$OrderNo" -Token $Token -Quiet
    if ($null -eq $r.data) { return $null }
    return $r.data.status
}

function Wait-OrderStatus {
    param([string]$OrderNo, [string]$Token, [string]$Expected, [int]$TimeoutSeconds = 25)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = Get-OrderStatus -OrderNo $OrderNo -Token $Token
        if ($status -eq $Expected) { return $status }
        Start-Sleep -Milliseconds 800
    }
    throw "order $OrderNo did not reach $Expected within $TimeoutSeconds s (last=$status)"
}

function New-CompletedOrder {
    param([string]$MemberToken, [string]$AdminToken, [long]$ProductId, [int]$Quantity, [string]$Label)
    $no = Invoke-Api -Name "createOrder-$Label" -Method POST -Path '/api/v1/orders' -Token $MemberToken `
        -IdempotencyKey ([guid]::NewGuid().ToString()) `
        -Body @{ fulfillType = 2; remark = "B 联调演示-$Label"; items = @(@{ productId = $ProductId; quantity = $Quantity }) }
    Assert-Code -Step $no -Expected 0
    $orderNo = $no.data.orderNo
    $pay = Invoke-Api -Name "payOrder-$Label" -Method POST -Path '/api/v1/payments' -Token $MemberToken `
        -IdempotencyKey ([guid]::NewGuid().ToString()) `
        -Body @{ orderNo = $orderNo; channel = 1 }
    Assert-Code -Step $pay -Expected 0
    Wait-OrderStatus -OrderNo $orderNo -Token $MemberToken -Expected 'PAID' | Out-Null
    Write-Log "  order $orderNo paid (label=$Label)"
    $ship = Invoke-Api -Name "shipOrder-$Label" -Method POST -Path "/api/v1/admin/orders/$orderNo/ship" -Token $AdminToken
    Assert-Code -Step $ship -Expected 0
    $confirm = Invoke-Api -Name "confirmOrder-$Label" -Method POST -Path "/api/v1/orders/$orderNo/confirm" -Token $MemberToken
    Assert-Code -Step $confirm -Expected 0
    $status = Wait-OrderStatus -OrderNo $orderNo -Token $MemberToken -Expected 'COMPLETED'
    Write-Log "  order $orderNo completed (label=$Label)"
    return $orderNo
}

Write-Log "==== B 第二周真实联调开始 $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===="
Write-Log "base=$BaseUrl"

# 0. 健康检查
$health = Invoke-Api -Name 'health' -Method GET -Path '/actuator/health' -Quiet
Write-Log ("  health: " + $health.raw)

# 1. 登录
$memberToken = Login -Phone '13800000003' -Pwd $Password -Name 'login-member'
$adminToken = Login -Phone '13800000001' -Pwd $Password -Name 'login-admin'
$operatorToken = Login -Phone '13800000002' -Pwd $Password -Name 'login-operator'
$adminToken2 = Login -Phone '13800000001' -Pwd $Password -Name 'login-admin-second'

# 2. 造已完成订单：A/B 商品 6，D 商品 2；C 商品 1 用于退款负向
$orderA = New-CompletedOrder -MemberToken $memberToken -AdminToken $adminToken -ProductId 6 -Quantity 1 -Label 'A'
$orderB = New-CompletedOrder -MemberToken $memberToken -AdminToken $adminToken -ProductId 6 -Quantity 1 -Label 'B'
$orderD = New-CompletedOrder -MemberToken $memberToken -AdminToken $adminToken -ProductId 2 -Quantity 1 -Label 'D'
$orderC = New-CompletedOrder -MemberToken $memberToken -AdminToken $adminToken -ProductId 1 -Quantity 1 -Label 'C'

# 3. 评价 A（真实链路）
$reviewA = Invoke-Api -Name 'review-A-create' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderA; productId = 6; rating = 5; content = '抽纸很厚实，包装完整，家用很合适。'; images = @('https://img.example.com/review/6-1.jpg'); anonymous = $false }
Assert-Code -Step $reviewA -Expected 0
$reviewAId = $reviewA.data.id
Write-Log "  review A id=$reviewAId product=$($reviewA.data.productId)"

$reviewB = Invoke-Api -Name 'review-B-create' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderB; productId = 6; score = 4; content = '第二单抽纸，吸水不错，希望多做活动。' }
Assert-Code -Step $reviewB -Expected 0

$reviewD = Invoke-Api -Name 'review-D-create' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderD; productId = 2; rating = 5; content = '香蕉自然成熟，甜度刚好。' }
Assert-Code -Step $reviewD -Expected 0

# 4. 负向：重复评价
$dup = Invoke-Api -Name 'review-duplicate' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderA; productId = 6; rating = 5; content = '重复评价应失败' }
Assert-Code -Step $dup -Expected 60002

# 5. 负向：空内容 / 缺评分 / 图片超长
$empty = Invoke-Api -Name 'review-empty-content' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderD; productId = 2; rating = 5; content = '   ' }
Assert-Code -Step $empty -Expected 10001

$noScore = Invoke-Api -Name 'review-missing-score' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderD; productId = 2; content = '缺评分' }
Assert-Code -Step $noScore -Expected 10001

$longImage = 'https://img.example.com/' + ('a' * 520) + '.jpg'
$img = Invoke-Api -Name 'review-image-too-long' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderD; productId = 2; rating = 5; content = '图片地址超长'; images = @($longImage) }
Assert-Code -Step $img -Expected 10001

# 6. 负向：已退款订单评价（C）
$afterSale = Invoke-Api -Name 'aftersale-apply-C' -Method POST -Path '/api/v1/after-sales' -Token $memberToken `
    -IdempotencyKey ([guid]::NewGuid().ToString()) `
    -Body @{ orderNo = $orderC; type = 'REFUND'; reason = 'B 联调退款负向用例'; description = '演示已退款订单不可评价' }
Assert-Code -Step $afterSale -Expected 0
$saleNo = $afterSale.data.saleNo
Write-Log "  after-sale $saleNo status=$($afterSale.data.status)"
$audit = Invoke-Api -Name 'aftersale-audit-C' -Method POST -Path "/api/v1/admin/after-sales/$saleNo/audit" -Token $adminToken `
    -IdempotencyKey ([guid]::NewGuid().ToString()) `
    -Body @{ approved = $true; remark = '同意退款' }
Assert-Code -Step $audit -Expected 0
$cStatus = Wait-OrderStatus -OrderNo $orderC -Token $memberToken -Expected 'REFUNDED' -TimeoutSeconds 20
Write-Log "  order C status=$cStatus"
$refundedReview = Invoke-Api -Name 'review-refunded-order' -Method POST -Path '/api/v1/reviews' -Token $memberToken `
    -Body @{ orderNo = $orderC; productId = 1; rating = 5; content = '已退款订单评价应失败' }
Assert-Code -Step $refundedReview -Expected 60003

# 7. 负向：评价别人订单
$otherReg = Invoke-Api -Name 'register-other-member' -Method POST -Path '/api/v1/auth/register' `
    -Body @{ phone = $OtherMemberPhone; password = $OtherMemberPassword; nickname = 'B联调路人' }
if ($otherReg.code -ne 0 -and $otherReg.code -ne 10006) { Assert-Code -Step $otherReg -Expected 0 }
if ($otherReg.code -eq 10006) { Write-Log '  other member already exists, reuse it' }
$otherToken = Login -Phone $OtherMemberPhone -Pwd $OtherMemberPassword -Name 'login-other-member'
$otherReview = Invoke-Api -Name 'review-other-order' -Method POST -Path '/api/v1/reviews' -Token $otherToken `
    -Body @{ orderNo = $orderD; productId = 2; rating = 5; content = '评价别人订单应失败' }
Assert-Code -Step $otherReview -Expected 60003

# 8. 后台查询与回复
$adminList = Invoke-Api -Name 'admin-review-list' -Method GET -Path '/api/v1/admin/reviews?pageNum=1&pageSize=10' -Token $adminToken
Assert-Code -Step $adminList -Expected 0
$replyA = Invoke-Api -Name 'admin-reply-A' -Method POST -Path "/api/v1/admin/reviews/$reviewAId/reply" -Token $adminToken `
    -Body @{ reply = '感谢支持，我们会持续改进包装与配送体验。' }
Assert-Code -Step $replyA -Expected 0
$replyB = Invoke-Api -Name 'admin-reply-B' -Method POST -Path ("/api/v1/admin/reviews/" + $reviewB.data.id + '/reply') -Token $adminToken `
    -Body @{ reply = '感谢复购，活动信息请关注首页公告。' }
Assert-Code -Step $replyB -Expected 0
$replyD = Invoke-Api -Name 'admin-reply-D' -Method POST -Path ("/api/v1/admin/reviews/" + $reviewD.data.id + '/reply') -Token $adminToken `
    -Body @{ reply = '感谢认可，我们会保持当日采摘。' }
Assert-Code -Step $replyD -Expected 0

$public6 = Invoke-Api -Name 'public-review-product6' -Method GET -Path '/api/v1/products/6/reviews?pageNum=1&pageSize=20' -Token $null
Assert-Code -Step $public6 -Expected 0
$public2 = Invoke-Api -Name 'public-review-product2' -Method GET -Path '/api/v1/products/2/reviews?pageNum=1&pageSize=20' -Token $null
Assert-Code -Step $public2 -Expected 0
$public6WithReply = @($public6.data.list | Where-Object { $_.reply -and $_.reply.Length -gt 0 }).Count
Write-Log "  public product 6 reviews=$($public6.data.total), withReply=$public6WithReply"
if ($public6WithReply -lt 1) { throw 'public review list missing reply' }

# 9. 后台账号：新建 -> 登录 -> 禁用 -> 旧 Token 失效 -> 启用
$acc = Invoke-Api -Name 'account-create' -Method POST -Path '/api/v1/admin/accounts' -Token $adminToken `
    -Body @{ phone = $AccountPhone; nickname = 'B联调运营'; role = 'operator'; password = $AccountPassword }
if ($acc.code -eq 0) {
    $accountId = $acc.data.id
    Write-Log "  new account id=$accountId"
} elseif ($acc.code -eq 10006) {
    $existing = Invoke-Api -Name 'account-lookup' -Method GET -Path ("/api/v1/admin/accounts?keyword=" + $AccountPhone + '&pageSize=10') -Token $adminToken
    Assert-Code -Step $existing -Expected 0
    $accountId = ($existing.data.list | Where-Object { $_.phone -eq $AccountPhone } | Select-Object -First 1).id
    Write-Log "  account already exists, id=$accountId"
    $reenable = Invoke-Api -Name 'account-force-enable' -Method PUT -Path "/api/v1/admin/accounts/$accountId/status" -Token $adminToken -Body @{ status = 1 }
    Assert-Code -Step $reenable -Expected 0
} else {
    Assert-Code -Step $acc -Expected 0
}
$accLogin = Invoke-Api -Name 'account-login' -Method POST -Path '/api/v1/auth/login' -Body @{ phone = $AccountPhone; password = $AccountPassword }
Assert-Code -Step $accLogin -Expected 0
$accToken = $accLogin.data.token
$accProbe = Invoke-Api -Name 'account-probe-before-disable' -Method GET -Path '/api/v1/admin/reviews?pageSize=1' -Token $accToken
Assert-Code -Step $accProbe -Expected 0
$accDisable = Invoke-Api -Name 'account-disable' -Method PUT -Path "/api/v1/admin/accounts/$accountId/status" -Token $adminToken -Body @{ status = 0 }
Assert-Code -Step $accDisable -Expected 0
$accAfterDisable = Invoke-Api -Name 'account-token-after-disable' -Method GET -Path '/api/v1/admin/reviews?pageSize=1' -Token $accToken
Assert-Code -Step $accAfterDisable -Expected 10002
$accLoginDisabled = Invoke-Api -Name 'account-login-disabled' -Method POST -Path '/api/v1/auth/login' -Body @{ phone = $AccountPhone; password = $AccountPassword }
Assert-Code -Step $accLoginDisabled -Expected 10005
$accEnable = Invoke-Api -Name 'account-enable' -Method PUT -Path "/api/v1/admin/accounts/$accountId/status" -Token $adminToken -Body @{ status = 1 }
Assert-Code -Step $accEnable -Expected 0
$accLogin2 = Invoke-Api -Name 'account-login-after-enable' -Method POST -Path '/api/v1/auth/login' -Body @{ phone = $AccountPhone; password = $AccountPassword }
Assert-Code -Step $accLogin2 -Expected 0

# 10. 角色权限：备份 -> 去掉评价回复 -> 立即生效 -> 恢复
$roles = Invoke-Api -Name 'roles-list' -Method GET -Path '/api/v1/admin/roles' -Token $adminToken
Assert-Code -Step $roles -Expected 0
$operatorRole = $roles.data | Where-Object { $_.code -eq 'OPERATOR' }
$adminRole = $roles.data | Where-Object { $_.code -eq 'ADMIN' }
$operatorBackup = @($operatorRole.permissions)
Write-Log ("  operator role id=" + $operatorRole.id + " permissions=" + $operatorBackup.Count)
$reduced = @($operatorBackup | Where-Object { $_ -ne 'review:goods:reply' })
$roleChange = Invoke-Api -Name 'operator-remove-review-reply' -Method PUT -Path ("/api/v1/admin/roles/" + $operatorRole.id + '/permissions') -Token $adminToken -Body @{ permissions = $reduced }
Assert-Code -Step $roleChange -Expected 0
$operatorDenied = Invoke-Api -Name 'operator-review-after-remove' -Method GET -Path '/api/v1/admin/reviews?pageSize=1' -Token $operatorToken
Assert-Code -Step $operatorDenied -Expected 10003
$roleRestore = Invoke-Api -Name 'operator-restore-permissions' -Method PUT -Path ("/api/v1/admin/roles/" + $operatorRole.id + '/permissions') -Token $adminToken -Body @{ permissions = $operatorBackup }
Assert-Code -Step $roleRestore -Expected 0
$operatorAllowed = Invoke-Api -Name 'operator-review-after-restore' -Method GET -Path '/api/v1/admin/reviews?pageSize=1' -Token $operatorToken
Assert-Code -Step $operatorAllowed -Expected 0

# 11. 管理员不会被降权（传空列表仍保留全部权限）
$adminDemote = Invoke-Api -Name 'admin-demote-attempt' -Method PUT -Path ("/api/v1/admin/roles/" + $adminRole.id + '/permissions') -Token $adminToken -Body @{ permissions = @() }
Assert-Code -Step $adminDemote -Expected 0
Write-Log ("  admin permission count after empty update=" + $adminDemote.data.permissionCount)
if ($adminDemote.data.permissionCount -lt 1) { throw 'admin role was demoted unexpectedly' }

# 12. 会员不能进后台
$memberAdmin = Invoke-Api -Name 'member-access-admin' -Method GET -Path '/api/v1/admin/members?pageSize=1' -Token $memberToken
Assert-Code -Step $memberAdmin -Expected 10003

# 13. 最后管理员保护（HTTP 层验证：不能禁用自己；代码与单测覆盖最后管理员）
$selfDisable = Invoke-Api -Name 'admin-disable-self' -Method PUT -Path '/api/v1/admin/accounts/1/status' -Token $adminToken -Body @{ status = 0 }
Assert-Code -Step $selfDisable -Expected 10001

# 14. 会员禁用 -> Token 失效 -> 恢复
$memberDisable = Invoke-Api -Name 'member-disable' -Method PUT -Path '/api/v1/admin/members/3/status' -Token $adminToken -Body @{ status = 0 }
Assert-Code -Step $memberDisable -Expected 0
$memberAfterDisable = Invoke-Api -Name 'member-token-after-disable' -Method GET -Path '/api/v1/orders?pageSize=1' -Token $memberToken
Assert-Code -Step $memberAfterDisable -Expected 10002
$memberEnable = Invoke-Api -Name 'member-enable' -Method PUT -Path '/api/v1/admin/members/3/status' -Token $adminToken -Body @{ status = 1 }
Assert-Code -Step $memberEnable -Expected 0

# 15. 汇总
$summary = [pscustomobject]@{
    stamp        = $script:Stamp
    baseUrl      = $BaseUrl
    orders       = @{ A = $orderA; B = $orderB; C = $orderC; D = $orderD }
    reviews      = @{ A = $reviewA.data.id; B = $reviewB.data.id; D = $reviewD.data.id }
    accountId    = $accountId
    afterSaleNo  = $saleNo
    public6Total = $public6.data.total
    steps        = $script:Steps
}
$summaryPath = Join-Path $script:Ev "summary-$($script:Stamp).json"
$summary | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $summaryPath -Encoding UTF8
Write-Log "==== 联调结束，evidence=$summaryPath ===="
Write-Host "SUMMARY=$summaryPath"



