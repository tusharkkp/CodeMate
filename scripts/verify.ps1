param([string]$Mode = "fast")
$Pkg = "com.mit.tushar_kaldate"; $fail = 0

# Locate adb
$AdbCmd = "adb"
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    if (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe") {
        $AdbCmd = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    }
}

function Run($name, [scriptblock]$cmd) {
    Write-Host "=== $name"
    & $cmd
    if ($LASTEXITCODE -ne 0) {
        Write-Host "!!! FAILED: $name"
        $script:fail = 1
    }
}

Run "G1 build" { .\gradlew.bat --console=plain assembleDebug }
if ($fail) { exit 1 }

Run "G2 lint"  { .\gradlew.bat --console=plain lintDebug }
Run "G3 unit"  { .\gradlew.bat --console=plain testDebugUnitTest }

if ($Mode -eq "full") {
    $deviceState = (& $AdbCmd get-state 2>$null)
    if ($deviceState -ne "device") {
        Write-Host "!!! no emulator/device (G4-G7 skipped)"
        exit 2
    }
    Run "G4 instrumented" { .\gradlew.bat --console=plain connectedDebugAndroidTest }
    & $AdbCmd logcat -c
    & $AdbCmd install -r app\build\outputs\apk\debug\app-debug.apk | Out-Null
    & $AdbCmd shell monkey -p $Pkg -c android.intent.category.LAUNCHER 1 | Out-Null
    Start-Sleep 4
    New-Item -ItemType Directory -Force docs\screens | Out-Null
    cmd /c "$AdbCmd exec-out screencap -p > docs\screens\smoke-launch.png"
    & $AdbCmd shell monkey -p $Pkg --throttle 150 --pct-syskeys 0 -v 300 | Out-Null
    if ((& $AdbCmd logcat -d) -match "FATAL EXCEPTION|ANR in $Pkg") {
        Write-Host "!!! crash or ANR"
        $fail = 1
    }
}

if ($fail) {
    Write-Host "GATES FAILED ($Mode)"
} else {
    Write-Host "ALL GATES GREEN ($Mode)"
}
exit $fail
