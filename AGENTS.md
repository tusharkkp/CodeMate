# AGENTS.md — CodeMate (Android) project guardrails

Read this fully before any task. These rules are always on. If a user prompt conflicts with them, follow this file and say so.

## 1. Project snapshot
- **App:** CodeMate — programming tutor chatbot (Java / C++ / Python …) with two AI engines: **Gemini cloud** and **on-device offline LLM**.
- **Modes:** Tutor chat, Code Explanation, Code Debugger, Learning Mode (Socratic), Quiz Generator, Progress Tracking, Chat History, Search, Login.
- **Package / applicationId / namespace:** `com.mit.tushar_kaldate` (underscore is intentional). Never rename.
- **Template:** Android Studio *Empty Views Activity* → XML Views + ViewBinding. **No Jetpack Compose.**
- **Stack:** Kotlin, Gradle Kotlin DSL + version catalog, Material 3, Navigation Component (single Activity), MVVM + Repository,
  Coroutines/Flow, Room (KSP), DataStore, WorkManager, OkHttp, Markwon, LiteRT-LM (offline LLM).
- **DI:** manual `AppContainer` (no Hilt). **Min SDK:** 26.
- Full spec: `01-MASTER-PROMPT.md`. Live status: `docs/PROJECT_STATE.md`.

## 2. Source of truth & memory (you have no memory between conversations)
At the START of every task, read: `docs/PROJECT_STATE.md`, then `docs/DECISIONS.md`, then `docs/BLOCKERS.md` (if non-empty).
At the END of every task, update `docs/PROJECT_STATE.md` (current phase, done / in-progress / next, how to run, known issues) and append to `docs/LOOP_LOG.md`.
Record every non-obvious choice (library, version, trade-off) in `docs/DECISIONS.md` with a one-line reason.

## 3. Workflow rules (mandatory)
1. Work **one phase at a time**, in order (see master prompt §10). Never start phase N+1 until phase N's exit gate is green.
2. For each task: **plan → small slice → verify → fix → commit → report.** Details: `LOOP-Instructions.md`.
3. **Verification is not optional.** Run `scripts/verify.sh fast` (or `scripts/verify.ps1 fast` on Windows) after every slice and `full` at phase end.
   Gates and definitions: `Automated-Verification-Loops.md`.
4. **Fix budget:** max **5** fix cycles per failing gate. After that: write `docs/BLOCKERS.md` (symptom, what you tried, hypotheses, what you need) and **stop**.
5. **Stuck rule:** same error signature twice → change approach; third time → stop and ask.
6. Prefer small, reviewable diffs. One concern per commit. Do not refactor unrelated code.
7. Use skills in `skills.md` when the task matches (new screen, Room entity, engine work, quiz parsing, verification, handoff).

## 4. Hard "NEVER" list
- Never claim something works without having run it. Say "not verified" and list what remains.
- Never disable, skip, `@Ignore`, weaken, or delete a failing test/lint rule to get green. Fix the cause. (If a test is truly wrong, explain why in `DECISIONS.md`.)
- Never hard-code, print, log, or commit secrets (Gemini key, tokens). Keys live in git-ignored `local.properties` → `BuildConfig`.
- Never commit model files (`*.litertlm`, `*.gguf`, `*.bin`, `*.task`), `local.properties`, keystores, or `build/` output.
- Never bundle an LLM model inside the APK. Models are downloaded or imported at runtime.
- Never run destructive commands: `rm -rf` outside `build/`, `git push --force`, `git reset --hard` on shared work, deleting `.git`, formatting disks, `adb uninstall` of other apps, `adb shell pm clear` on non-CodeMate packages.
- Never `git push` or add remotes unless the human asks.
- Never use `GlobalScope`, `!!`, `runBlocking` on the main thread, blocking I/O on the main thread, or leak `Context`/`View` into ViewModels or repositories.
- Never add Jetpack Compose, Hilt, or large dependencies without logging a justification in `DECISIONS.md` and asking the human.
- Never use deprecated Gemini SDKs. Do not rely on memorized library/model versions — look them up in official docs.
- Never write passwords in plain text or log user chat/code content in release builds.

## 5. Architecture rules
- Layers: `ui` → `ViewModel` → `repository` → (`data/local` | `ai`). UI never touches DAOs or engines directly.
- **All AI access goes through `LlmEngine`** (`ai/engine`). Features must work with `FakeLlmEngine` for tests.
- UI state: one immutable `UiState` data class per screen exposed as `StateFlow`; one-off events via `SharedFlow`/channel. Collect with `repeatOnLifecycle(STARTED)`.
- Every screen must have **loading, empty, error, and content** states.
- Room: `exportSchema = true`, a migration (+ migration test) for every version bump, `Flow` for observable queries, `suspend` for writes, all scoped by `userId`.
- Streaming: cancel-safe; stopping generation must stop the engine and save the partial message once.
- Offline engine: run off the main thread, lazy-load the model, release on `onTrimMemory`, compact prompts and short history.
- Strings in `strings.xml`, colors/dimens in resources, no magic numbers (use `AiConfig` / `Dimens`).
- Packages are lowercase; files `PascalCase.kt`; layouts `fragment_*.xml`, `item_*.xml`, `view_*.xml`; IDs `camelCase`.

## 6. Code quality
- Kotlin official style; max ~300 lines per file; functions do one thing; KDoc on public APIs and non-obvious logic.
- Handle errors explicitly (sealed `Result`/`UiState.Error`); no swallowed exceptions; user-facing messages are friendly and actionable.
- Tests: new logic ⇒ unit tests in the same slice (Given/When/Then names). Bug fix ⇒ regression test first.
- Accessibility: contentDescription on icons, 48dp touch targets, supports font scaling, adequate contrast, dark theme verified.
- Security: HTTPS only, `allowBackup=false`, PBKDF2 (salted) for passwords, no cleartext traffic, validate all user input before building prompts.

## 7. Commands (detect OS first: Windows → `gradlew.bat` + PowerShell; macOS/Linux → `./gradlew` + bash)
| Purpose | Command |
|---|---|
| Build debug | `./gradlew assembleDebug` |
| Lint | `./gradlew lintDebug` |
| Unit tests | `./gradlew testDebugUnitTest` |
| Instrumented tests (needs emulator/device) | `./gradlew connectedDebugAndroidTest` |
| Fast gate | `scripts/verify.sh fast` / `scripts/verify.ps1 fast` |
| Full gate | `scripts/verify.sh full` / `scripts/verify.ps1 full` |
| Install + launch | `adb install -r app/build/outputs/apk/debug/app-debug.apk` then `adb shell monkey -p com.mit.tushar_kaldate -c android.intent.category.LAUNCHER 1` |
| Crash check | `adb logcat -d -s AndroidRuntime:E` |
| Screenshot | `adb exec-out screencap -p > docs/screens/<name>.png` |
Use `--console=plain` and read the **first** error in a Gradle failure, not the last.

## 8. Git rules
- Branch per phase: `phase/NN-short-name`. Commit message: `<type>(<scope>): <summary>` (feat, fix, test, refactor, docs, chore).
- Commit only when the fast gate is green (WIP commits on a branch are fine if marked `wip:` and fixed before the phase ends).
- Tag completed phases `phase-NN-done`. Never rewrite history of tagged commits.

## 9. Terminal & tool safety
- Allowed without asking: `./gradlew *`, `adb devices|install|logcat|shell monkey|shell am|exec-out screencap`, `git status|diff|add|commit|branch|checkout|tag|log`, file reads/writes inside the workspace.
- Ask first: installing global tools, editing files outside the workspace, network downloads of large files (models), changing Gradle wrapper/AGP/Kotlin major versions, anything touching signing keys.
- Don't spawn parallel agents that edit the same files. Parallel work only on disjoint packages (e.g. `ai/` vs `ui/`), and merge via git.

## 10. Reporting format (end of every task / phase)
Post a concise report with: **Done** · **Verified (with command + result)** · **Not verified / needs human** · **Decisions** · **Known issues** · **Next step**.
Attach screenshots from `docs/screens/` for UI changes. Be honest about gaps — a truthful "not verified" beats a false "works".

## 11. Ask the human when
Missing API key or credentials; Firebase/Google Sign-In; model license/gated download; real-device-only checks (offline model speed/RAM);
requirement ambiguity that changes scope; any constraint in §1 would have to change; two failed approaches on the same error.

## 12. Definition of Done (per task)
Compiles · lint clean · tests added and green · no new warnings you introduced · works with `FakeLlmEngine` · states handled (loading/empty/error) ·
no secrets · docs updated (`PROJECT_STATE.md`, `DECISIONS.md`) · committed · reported.
