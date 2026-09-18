# BugTrace Automatic Android Deployment Script
# Usage: .\deploy.ps1

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$AndroidDir = Join-Path $ProjectRoot "android"
$Gradlew = Join-Path $AndroidDir "gradlew.bat"
$ApkPath = Join-Path $AndroidDir "app\build\outputs\apk\debug\app-debug.apk"
$AppId = "com.bugtrace.app"
$MainActivity = "com.bugtrace.app/.MainActivity"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "       BugTrace Android Auto-Deploy Tool" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

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
    Write-Host "[!] ADB executable not found in PATH or standard Android SDK locations." -ForegroundColor Red
    Write-Host "    Please ensure Android SDK / ADB is installed." -ForegroundColor Yellow
    exit 1
}

Write-Host "[+] ADB Path: $AdbPath" -ForegroundColor Green

# 2. Build Debug APK
Write-Host "[+] Building latest Debug APK with Gradle..." -ForegroundColor Yellow
Push-Location $AndroidDir
try {
    & $Gradlew assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[!] Gradle build failed with exit code $LASTEXITCODE" -ForegroundColor Red
        Pop-Location
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}

if (-not (Test-Path $ApkPath)) {
    Write-Host "[!] APK not found at expected path: $ApkPath" -ForegroundColor Red
    exit 1
}

Write-Host "[+] Debug APK built successfully at:" -ForegroundColor Green
Write-Host "    $ApkPath" -ForegroundColor Gray

# 3. Detect Connected ADB Devices
Write-Host "[+] Checking for connected Android devices..." -ForegroundColor Yellow
$DeviceOutput = & $AdbPath devices -l
$Devices = @()

foreach ($line in $DeviceOutput) {
    $line = $line.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("List of devices attached") -or $line.StartsWith("* daemon")) {
        continue
    }
    $parts = -split $line
    if ($parts.Count -ge 2) {
        $serial = $parts[0]
        $state = $parts[1]
        if ($state -eq "device") {
            $model = "Unknown"
            foreach ($p in $parts) {
                if ($p.StartsWith("model:")) {
                    $model = $p.Substring(6)
                }
            }
            $Devices += [PSCustomObject]@{
                Serial = $serial
                Model  = $model
                Line   = $line
            }
        } elseif ($state -eq "unauthorized") {
            Write-Host "[!] Device detected ($serial) but UNAUTHORIZED. Please allow USB debugging prompt on phone." -ForegroundColor Red
        } elseif ($state -eq "offline") {
            Write-Host "[!] Device detected ($serial) but OFFLINE. Try reconnecting USB cable." -ForegroundColor Red
        }
    }
}

if ($Devices.Count -eq 0) {
    Write-Host ""
    Write-Host "--------------------------------------------------" -ForegroundColor Yellow
    Write-Host "[!] NO CONNECTED ANDROID DEVICE FOUND VIA ADB" -ForegroundColor Yellow
    Write-Host "--------------------------------------------------" -ForegroundColor Yellow
    Write-Host "  The APK was built successfully, but no active phone was detected." -ForegroundColor White
    Write-Host "  To install automatically on your iQOO device:" -ForegroundColor White
    Write-Host "   1. Connect phone via USB cable" -ForegroundColor White
    Write-Host "   2. Enable USB Debugging in Developer Options" -ForegroundColor White
    Write-Host "   3. Accept the 'Allow USB debugging' prompt on the phone screen" -ForegroundColor White
    Write-Host "   4. Re-run .\deploy.ps1" -ForegroundColor White
    Write-Host "--------------------------------------------------" -ForegroundColor Yellow
    exit 0
}

# 4. Install & Launch on Connected Device(s)
foreach ($dev in $Devices) {
    Write-Host "[+] Target Device: $($dev.Model) (Serial: $($dev.Serial))" -ForegroundColor Cyan
    Write-Host "[+] Installing APK over existing installation (preserving app data)..." -ForegroundColor Yellow
    
    $InstallResult = & $AdbPath -s $($dev.Serial) install -r $ApkPath 2>&1
    Write-Host $InstallResult
    
    if ($InstallResult -match "Success") {
        Write-Host "[+] App installed successfully!" -ForegroundColor Green
        Write-Host "[+] Launching BugTrace ($MainActivity)..." -ForegroundColor Yellow
        $LaunchResult = & $AdbPath -s $($dev.Serial) shell am start -n $MainActivity 2>&1
        Write-Host $LaunchResult
        Write-Host "[+] BugTrace is now running on $($dev.Model)!" -ForegroundColor Green
    } else {
        Write-Host "[!] Installation failed on device $($dev.Serial)." -ForegroundColor Red
    }
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "          Deployment Process Complete" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
