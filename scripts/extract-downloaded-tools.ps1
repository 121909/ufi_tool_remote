param(
    [string]$Root = "D:\ufi-remote-build-tools\android-build-tools"
)

$ErrorActionPreference = "Stop"

$jdkDir = Join-Path $Root "jdk"
$gradleDir = Join-Path $Root "gradle"
$downloads = Join-Path $Root "downloads"

New-Item -ItemType Directory -Force -Path $jdkDir, $gradleDir | Out-Null

if (!(Test-Path (Join-Path $jdkDir "bin\java.exe"))) {
    Write-Host "Extracting JDK..."
    Expand-Archive -Path (Join-Path $downloads "microsoft-jdk-17-windows-x64.zip") -DestinationPath $jdkDir -Force
    $jdkNested = Get-ChildItem -Path $jdkDir -Directory | Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") } | Select-Object -First 1
    if ($jdkNested -and $jdkNested.FullName -ne $jdkDir) {
        Get-ChildItem -Path $jdkNested.FullName -Force | Move-Item -Destination $jdkDir -Force
        Remove-Item -LiteralPath $jdkNested.FullName -Recurse -Force
    }
}

if (!(Test-Path (Join-Path $gradleDir "bin\gradle.bat"))) {
    Write-Host "Extracting Gradle..."
    Expand-Archive -Path (Join-Path $downloads "gradle-8.9-bin.zip") -DestinationPath $gradleDir -Force
    $gradleNested = Get-ChildItem -Path $gradleDir -Directory | Where-Object { Test-Path (Join-Path $_.FullName "bin\gradle.bat") } | Select-Object -First 1
    if ($gradleNested -and $gradleNested.FullName -ne $gradleDir) {
        Get-ChildItem -Path $gradleNested.FullName -Force | Move-Item -Destination $gradleDir -Force
        Remove-Item -LiteralPath $gradleNested.FullName -Recurse -Force
    }
}

& (Join-Path $jdkDir "bin\java.exe") -version
& (Join-Path $gradleDir "bin\gradle.bat") -v
