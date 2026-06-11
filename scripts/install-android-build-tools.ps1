param(
    [string]$Root = "D:\ufi-remote-build-tools\android-build-tools"
)

$ErrorActionPreference = "Stop"

$downloads = Join-Path $Root "downloads"
$jdkDir = Join-Path $Root "jdk"
$gradleDir = Join-Path $Root "gradle"
$androidSdk = Join-Path $Root "android-sdk"
$gradleHome = Join-Path $Root "gradle-home"
$androidHome = Join-Path $Root "android-home"

New-Item -ItemType Directory -Force -Path $downloads, $jdkDir, $gradleDir, $androidSdk, $gradleHome, $androidHome | Out-Null

$jdkZip = Join-Path $downloads "microsoft-jdk-17-windows-x64.zip"
$gradleZip = Join-Path $downloads "gradle-8.9-bin.zip"
$cmdlineZip = Join-Path $downloads "commandlinetools-win-14742923_latest.zip"

$items = @(
    @{
        Url = "https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip"
        Out = $jdkZip
        Name = "Microsoft OpenJDK 17"
    },
    @{
        Url = "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
        Out = $gradleZip
        Name = "Gradle 8.9"
    },
    @{
        Url = "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"
        Out = $cmdlineZip
        Name = "Android command line tools"
    }
)

foreach ($item in $items) {
    if (!(Test-Path $item.Out)) {
        Write-Host "Downloading $($item.Name)..."
        Invoke-WebRequest -Uri $item.Url -OutFile $item.Out
    } else {
        Write-Host "Already downloaded $($item.Name)."
    }
}

if (!(Test-Path (Join-Path $jdkDir "bin\java.exe"))) {
    Write-Host "Extracting JDK..."
    Expand-Archive -Path $jdkZip -DestinationPath $jdkDir -Force
    $jdkNested = Get-ChildItem -Path $jdkDir -Directory | Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") } | Select-Object -First 1
    if ($jdkNested -and $jdkNested.FullName -ne $jdkDir) {
        Get-ChildItem -Path $jdkNested.FullName -Force | Move-Item -Destination $jdkDir -Force
        Remove-Item -LiteralPath $jdkNested.FullName -Recurse -Force
    }
}

if (!(Test-Path (Join-Path $gradleDir "bin\gradle.bat"))) {
    Write-Host "Extracting Gradle..."
    Expand-Archive -Path $gradleZip -DestinationPath $gradleDir -Force
    $gradleNested = Get-ChildItem -Path $gradleDir -Directory | Where-Object { Test-Path (Join-Path $_.FullName "bin\gradle.bat") } | Select-Object -First 1
    if ($gradleNested -and $gradleNested.FullName -ne $gradleDir) {
        Get-ChildItem -Path $gradleNested.FullName -Force | Move-Item -Destination $gradleDir -Force
        Remove-Item -LiteralPath $gradleNested.FullName -Recurse -Force
    }
}

$cmdlineLatest = Join-Path $androidSdk "cmdline-tools\latest"
if (!(Test-Path (Join-Path $cmdlineLatest "bin\sdkmanager.bat"))) {
    Write-Host "Extracting Android command line tools..."
    $tmpCmdline = Join-Path $Root "tmp-cmdline-tools"
    if (Test-Path $tmpCmdline) {
        Remove-Item -LiteralPath $tmpCmdline -Recurse -Force
    }
    Expand-Archive -Path $cmdlineZip -DestinationPath $tmpCmdline -Force
    New-Item -ItemType Directory -Force -Path $cmdlineLatest | Out-Null
    $source = Join-Path $tmpCmdline "cmdline-tools"
    Get-ChildItem -Path $source -Force | Move-Item -Destination $cmdlineLatest -Force
    Remove-Item -LiteralPath $tmpCmdline -Recurse -Force
}

$env:JAVA_HOME = $jdkDir
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
$env:GRADLE_USER_HOME = $gradleHome
$env:ANDROID_USER_HOME = $androidHome
$env:ANDROID_AVD_HOME = Join-Path $androidHome "avd"
$env:Path = "$jdkDir\bin;$gradleDir\bin;$cmdlineLatest\bin;$androidSdk\platform-tools;$env:Path"

Write-Host "Installing Android SDK packages into $androidSdk..."
$sdkmanager = Join-Path $cmdlineLatest "bin\sdkmanager.bat"
& $sdkmanager --sdk_root=$androidSdk "platform-tools" "platforms;android-35" "build-tools;35.0.0"

Write-Host "Accepting Android SDK licenses..."
cmd /c "for /L %i in (1,1,200) do @echo y" | & $sdkmanager --sdk_root=$androidSdk --licenses

Write-Host ""
Write-Host "Done."
Write-Host "JAVA_HOME=$jdkDir"
Write-Host "ANDROID_HOME=$androidSdk"
Write-Host "GRADLE_USER_HOME=$gradleHome"
Write-Host "Gradle=$gradleDir\bin\gradle.bat"
