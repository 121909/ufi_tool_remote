param(
    [string]$ToolsRoot = "D:\ufi-remote-build-tools\android-build-tools",
    [string]$Task = "test"
)

$ErrorActionPreference = "Stop"

$jdkDir = Join-Path $ToolsRoot "jdk"
$gradleDir = Join-Path $ToolsRoot "gradle"
$androidSdk = Join-Path $ToolsRoot "android-sdk"
$gradleHome = Join-Path $ToolsRoot "gradle-home"
$androidHome = Join-Path $ToolsRoot "android-home"
$cmdlineLatest = Join-Path $androidSdk "cmdline-tools\latest"
$gradleBat = Join-Path $gradleDir "bin\gradle.bat"

if (!(Test-Path (Join-Path $jdkDir "bin\java.exe"))) { throw "Missing JDK. Run scripts\install-android-build-tools.ps1 first." }
if (!(Test-Path $gradleBat)) { throw "Missing Gradle. Run scripts\install-android-build-tools.ps1 first." }
if (!(Test-Path (Join-Path $cmdlineLatest "bin\sdkmanager.bat"))) { throw "Missing Android SDK command line tools. Run scripts\install-android-build-tools.ps1 first." }

$env:JAVA_HOME = $jdkDir
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
$env:GRADLE_USER_HOME = $gradleHome
$env:ANDROID_USER_HOME = $androidHome
$env:ANDROID_AVD_HOME = Join-Path $androidHome "avd"
$env:Path = "$jdkDir\bin;$gradleDir\bin;$cmdlineLatest\bin;$androidSdk\platform-tools;$env:Path"

& $gradleBat $Task --no-daemon --stacktrace
