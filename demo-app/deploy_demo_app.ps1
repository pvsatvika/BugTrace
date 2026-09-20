param (
    [string]$Version = "v1"
)

# 1. Locate ADB executable
$AdbPath = $null
if (Get-Command adb -ErrorAction SilentlyContinue) {
    $AdbPath = (Get-Command adb).Path
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe") {
    $AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
} elseif ($env:ANDROID_HOME -and (Test-Path "$env:ANDROID_HOME\platform-tools\adb.exe")) {
    $AdbPath = "$env:ANDROID_HOME\platform-tools\adb.exe"
} elseif ($env:ANDROID_SDK_ROOT -and (Test-Path "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe")) {
    $AdbPath = "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
} elseif (Test-Path "C:\Android\sdk\platform-tools\adb.exe") {
    $AdbPath = "C:\Android\sdk\platform-tools\adb.exe"
}

if (-not $AdbPath) {
    Write-Host "Error: ADB executable not found in PATH or standard Android SDK locations." -ForegroundColor Red
    Write-Host "Please ensure Android SDK / ADB is installed." -ForegroundColor Yellow
    exit 1
}

$deviceOutput = & $AdbPath devices
$device = ($deviceOutput | Select-String -Pattern "\tdevice$")

if (-not $device) {
    Write-Host "Error: No connected Android device found via ADB." -ForegroundColor Red
    Write-Host "Please connect your phone via USB and enable USB debugging." -ForegroundColor Yellow
    exit 1
}

$deviceSerial = ($device -split '\s+')[0]
Write-Host "Connected device detected: $deviceSerial" -ForegroundColor Green

if ($Version -eq "v1") {
    $apkPath = "$PSScriptRoot\app\build\outputs\apk\v1buggy\debug\app-v1buggy-debug.apk"
    $packageName = "com.example.shopdemo.v1"
    $activityName = "com.example.shopdemo.MainActivity"
    $label = "ShopDemo V1 (Buggy)"
} elseif ($Version -eq "v2") {
    $apkPath = "$PSScriptRoot\app\build\outputs\apk\v2fixed\debug\app-v2fixed-debug.apk"
    $packageName = "com.example.shopdemo.v2"
    $activityName = "com.example.shopdemo.MainActivity"
    $label = "ShopDemo V2 (Fixed)"
} else {
    Write-Host "Error: Unknown version '$Version'. Use 'v1' or 'v2'." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $apkPath)) {
    Write-Host "Error: APK not found at $apkPath" -ForegroundColor Red
    Write-Host "Run '.\gradlew.bat assembleV1buggyDebug assembleV2fixedDebug' first." -ForegroundColor Yellow
    exit 1
}

Write-Host "Installing $label..." -ForegroundColor Cyan
& $AdbPath -s $deviceSerial install -r $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host "Successfully installed $label." -ForegroundColor Green
    Write-Host "Launching $packageName..." -ForegroundColor Cyan
    & $AdbPath -s $deviceSerial shell am start -n "$packageName/$activityName"
} else {
    Write-Host "Failed to install $label." -ForegroundColor Red
}
