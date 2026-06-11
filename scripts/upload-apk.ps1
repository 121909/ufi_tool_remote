<#
.SYNOPSIS
    Upload app-debug.apk to OpenList via PUT /api/fs/put.
.DESCRIPTION
    Uploads the debug APK to an OpenList / Alist server.
    Uses File-Path header and As-Task: false, which is required by OpenList v4.
.PARAMETER Server
    OpenList server base URL, for example http://192.168.1.100:5244.
.PARAMETER Token
    OpenList API token. If empty, OPENLIST_TOKEN is used. If both are empty,
    the request is sent without Authorization.
.PARAMETER LocalFile
    Path to the local APK. Defaults to app/build/outputs/apk/debug/app-debug.apk.
.PARAMETER RemotePath
    Destination path on the server. Defaults to /codex/app-debug.apk.
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Server,
    [string]$Token = $env:OPENLIST_TOKEN,
    [string]$LocalFile = "",
    [string]$RemotePath = "/codex/app-debug.apk"
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not $LocalFile) {
    $LocalFile = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
}

$localPath = (Resolve-Path $LocalFile -ErrorAction Stop).Path
$localItem = Get-Item $localPath
$serverBase = $Server.TrimEnd("/")
$uploadUrl = "$serverBase/api/fs/put"

$headers = @{
    "File-Path" = $RemotePath
    "As-Task"   = "false"
}
if ($Token) {
    $headers["Authorization"] = $Token
}

Write-Host "Local file: $localPath ($($localItem.Length) bytes)"
Write-Host "Uploading to $RemotePath ..."

try {
    $response = Invoke-WebRequest `
        -Uri $uploadUrl `
        -Method Put `
        -InFile $localPath `
        -Headers $headers `
        -ContentType "application/vnd.android.package-archive" `
        -UseBasicParsing `
        -TimeoutSec 600

    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        Write-Error "Upload failed (HTTP $($response.StatusCode)): $($response.Content)"
        exit 1
    }

    if (-not $response.Content) {
        Write-Host "Upload successful!"
        exit 0
    }

    $body = $response.Content | ConvertFrom-Json
    if ($body.code -ne 200) {
        Write-Error "Upload failed: $($body.message)"
        exit 1
    }

    Write-Host "Upload successful!"
} catch {
    Write-Error "Upload failed: $_"
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "Response body: $body"
    }
    exit 1
}
