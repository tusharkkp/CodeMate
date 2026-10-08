#!/usr/bin/env bash
# usage: scripts/verify.sh [fast|full]
set -u
PKG=com.mit.tushar_kaldate
MODE=${1:-fast}
fail=0

run() { local name="$1"; shift; echo "=== $name"; "$@" || { echo "!!! FAILED: $name"; fail=1; return 1; }; }

run "G1 build"  ./gradlew --console=plain assembleDebug     || exit 1
run "G2 lint"   ./gradlew --console=plain lintDebug
run "G3 unit"   ./gradlew --console=plain testDebugUnitTest

if [ "$MODE" = "full" ]; then
  adb get-state >/dev/null 2>&1 || { echo "!!! no emulator/device connected (G4-G7 skipped)"; exit 2; }
  run "G4 instrumented" ./gradlew --console=plain connectedDebugAndroidTest
  adb logcat -c
  adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
  adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 4
  mkdir -p docs/screens && adb exec-out screencap -p > docs/screens/smoke-launch.png
  adb shell monkey -p "$PKG" --throttle 150 --pct-syskeys 0 -v 300 > build/monkey.log 2>&1
  if adb logcat -d | grep -E "FATAL EXCEPTION|ANR in $PKG"; then echo "!!! G5/G6 crash or ANR"; fail=1; fi
fi

[ $fail -eq 0 ] && echo "ALL GATES GREEN ($MODE)" || echo "GATES FAILED ($MODE)"
exit $fail
