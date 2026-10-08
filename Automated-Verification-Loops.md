# Automated Verification Loops — CodeMate

Purpose: make the agent **prove** every change works, automatically, before moving on.
Rule of thumb: *no green gate = not done.*

## 1. The gates

| Gate | Name | Command | Pass criteria | When |
|---|---|---|---|---|
| G1 | Build | `./gradlew --console=plain assembleDebug` | exit 0, no new warnings introduced | every slice |
| G2 | Lint | `./gradlew --console=plain lintDebug` | 0 errors; no new warnings in files you touched | every slice |
| G3 | Unit tests | `./gradlew --console=plain testDebugUnitTest` | all pass; new logic has new tests | every slice |
| G4 | Instrumented tests | `./gradlew --console=plain connectedDebugAndroidTest` | all pass on emulator (API 34+) | end of phase / UI changes |
| G5 | Emulator smoke | install → launch → navigate → check logcat | no `FATAL EXCEPTION`, no ANR, app reaches Home | end of phase / UI changes |
| G6 | Monkey stress | `adb shell monkey -p com.mit.tushar_kaldate --throttle 150 -v 300` | no crash/ANR in logcat | phase end, Phase 11 (500 events) |
| G7 | Visual check | `adb exec-out screencap -p > docs/screens/<name>.png`, then view the image | screen matches spec: no clipped text, correct states, dark + light | every UI change |
| G8 | Release smoke | `./gradlew assembleRelease` + install + launch | R8 doesn't break Room/serialization/LiteRT | Phase 11 |

`fast` = G1–G3. `full` = G1–G7 (+G8 in Phase 11).

## 2. Scripts (create in Phase 0; the agent must keep them working)

### `scripts/verify.sh` (macOS/Linux/Git-Bash)
```bash
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
```

### `scripts/verify.ps1` (Windows PowerShell)
```powershell
param([string]$Mode = "fast")
$Pkg = "com.mit.tushar_kaldate"; $fail = 0
function Run($name, [scriptblock]$cmd) {
  Write-Host "=== $name"; & $cmd
  if ($LASTEXITCODE -ne 0) { Write-Host "!!! FAILED: $name"; $script:fail = 1 }
}
Run "G1 build" { .\gradlew.bat --console=plain assembleDebug }
if ($fail) { exit 1 }
Run "G2 lint"  { .\gradlew.bat --console=plain lintDebug }
Run "G3 unit"  { .\gradlew.bat --console=plain testDebugUnitTest }
if ($Mode -eq "full") {
  if ((adb get-state 2>$null) -ne "device") { Write-Host "!!! no emulator/device (G4-G7 skipped)"; exit 2 }
  Run "G4 instrumented" { .\gradlew.bat --console=plain connectedDebugAndroidTest }
  adb logcat -c
  adb install -r app\build\outputs\apk\debug\app-debug.apk | Out-Null
  adb shell monkey -p $Pkg -c android.intent.category.LAUNCHER 1 | Out-Null
  Start-Sleep 4
  New-Item -ItemType Directory -Force docs\screens | Out-Null
  cmd /c "adb exec-out screencap -p > docs\screens\smoke-launch.png"
  adb shell monkey -p $Pkg --throttle 150 --pct-syskeys 0 -v 300 | Out-Null
  if ((adb logcat -d) -match "FATAL EXCEPTION|ANR in $Pkg") { Write-Host "!!! crash or ANR"; $fail = 1 }
}
if ($fail) { Write-Host "GATES FAILED ($Mode)" } else { Write-Host "ALL GATES GREEN ($Mode)" }
exit $fail
```

Gradle config to add in Phase 0: `lint { abortOnError = true; checkDependencies = true }`, `testOptions { unitTests.isReturnDefaultValues = true }`, Room schema export dir, `buildConfig = true`.

## 3. The verification loop (what the agent does after every slice)

```
┌─► 1. RUN   scripts/verify (fast, or full if UI/phase end)
│   2. READ  the FIRST failure only (not the whole log)
│   3. CLASSIFY the failure (table §4)
│   4. DIAGNOSE root cause — form one hypothesis, find evidence in code/log
│   5. FIX   smallest change that addresses the root cause (+ regression test if a bug)
│   6. RE-RUN the SAME gate, then the whole set
└── repeat until green  — max 5 cycles per gate → then write docs/BLOCKERS.md and STOP
```
After green: append one line to `docs/LOOP_LOG.md`:
`YYYY-MM-DD HH:MM | phase N | slice: <what> | gates: G1✔ G2✔ G3✔ | cycles: 2 | note: <1 line>`

## 4. Failure triage table

| Symptom | Likely cause | First action |
|---|---|---|
| `Unresolved reference` / KSP error | missing import, Room annotation, ViewBinding not enabled | check `buildFeatures { viewBinding = true }`, rebuild after KSP change |
| Room: "Schema export directory not set" / migration error | missing `room.schemaLocation` / no migration | add arg; write migration + `MigrationTestHelper` test |
| Lint: `MissingPermission`, `HardcodedText`, `ContentDescription` | spec violation | fix the code, don't suppress |
| Unit test flaky (coroutines) | real dispatchers in tests | inject dispatchers; use `runTest` + `StandardTestDispatcher` |
| `NetworkOnMainThreadException` | engine call on Main | move to `Dispatchers.IO` in the engine/repository |
| Streaming UI jank / ANR | per-token full-list rerender | append to the last message item only, batch updates (e.g. 50 ms) |
| LiteRT load OOM / native crash | model too big for device | pick lower tier, release model on trim, log RAM; do NOT catch-and-hide |
| Gemini 401/403 | key missing/invalid | verify `BuildConfig` key present (never print it); ask human |
| Gemini 429 | rate limit | exponential backoff, friendly message |
| Quiz JSON parse error | model emitted prose/fences/truncation | add the sample to parser test corpus, then fix the parser |
| Espresso `NoMatchingViewException` | wrong ID / idling | use Idling for coroutines or fake engine with instant mode |
| `adb: no devices` | emulator off | ask human to start AVD (API 34+, x86_64) |

## 5. Anti-cheat rules (the agent must obey)
1. Never edit/skip/delete a test or lint rule to make a gate pass. If a test is wrong, fix it **and** justify in `DECISIONS.md`.
2. Never mark a gate "passed" without pasting the command and its final result line in the report.
3. If a gate cannot run (no emulator, no key, no device), report it as **NOT VERIFIED**, not "skipped successfully".
4. Don't widen `try/catch` to hide errors; handle specific exceptions and surface them as UI errors.
5. Don't reduce coverage/lint strictness; `abortOnError` stays `true`.

## 6. Required automated tests (minimum set — build up phase by phase)

| Area | Tests |
|---|---|
| `PromptBuilder` | each mode contains required sections; COMPACT variant is under token budget; level/language interpolated |
| `QuizJsonParser` | ≥ 15 malformed samples → either valid result or typed error, never crash |
| `EngineRouter` | CLOUD / LOCAL / AUTO decision matrix (online/offline × key/no key × model/no model) |
| `GeminiCloudEngine` | `MockWebServer`: SSE chunk parsing, 401/429/500, timeout, cancellation |
| `ModelManager` | tier choice by RAM/storage, resume download with Range, checksum fail, import file |
| Room | DAO CRUD, cascade deletes, FTS search, userId scoping, migrations |
| `AuthRepository` | hash/verify, duplicate email, wrong password, guest flow |
| Progress | mastery formula, streak across midnight / missed day / timezone change |
| Learning state machine | hint ladder: no reveal before 2 hints unless asked |
| UI (Espresso, `FakeLlmEngine`) | login→home; chat send/stream/stop; explain; debug; learn; quiz full flow; history resume; search |

`FakeLlmEngine` modes: `Instant`, `Streaming(delay)`, `Error(type)`, `Malformed(json)` — enables deterministic UI/tests without network or model.

## 7. Phase-specific extra checks

| Phase | Extra verification |
|---|---|
| 1 | screenshots of every placeholder screen, light + dark |
| 2 | exported Room schema JSON committed; perf: insert 5k messages < 2 s |
| 3 | restart-persistence test; wrong-password error visible |
| 4 | manual real-key chat check by human; cancel mid-stream leaves 1 partial message |
| 5 | **human on real phone:** airplane mode → offline answer; record tokens/sec + RAM in `DECISIONS.md` |
| 6 | screenshot of each mode's structured output; check section headers present |
| 7 | feed 20 generated quizzes (fake + real) through parser; none crash |
| 8 | seed data → charts render; streak correct |
| 9 | search latency < 300 ms @ 5k messages |
| 11 | clean clone build, release build smoke, monkey 500 events, demo rehearsal |

## 8. Phase exit checklist (agent pastes this in the phase report, all boxes must be ticked or marked NOT VERIFIED with a reason)
- [ ] G1 build ✔  - [ ] G2 lint ✔  - [ ] G3 unit ✔
- [ ] G4 instrumented ✔  - [ ] G5 smoke ✔  - [ ] G6 monkey ✔  - [ ] G7 screenshots attached
- [ ] New logic has tests  - [ ] No secrets/models in git (`git status` clean of them)
- [ ] `PROJECT_STATE.md`, `DECISIONS.md`, `LOOP_LOG.md` updated  - [ ] Committed + tagged
