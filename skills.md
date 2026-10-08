# skills.md — Reusable skills for CodeMate

How to use: `AGENTS.md` tells the agent to consult this file. Each `## SKILL:` section is self-contained and follows the
Agent-Skill shape (name + description + steps). Optional: split each section into `.agents/skills/<name>/SKILL.md`
(keep the `---` frontmatter at the top of each) so Antigravity can load them on demand.

---

## SKILL: new-view-screen
```
---
name: new-view-screen
description: Use when adding any new screen (Fragment) to CodeMate — XML + ViewBinding + ViewModel + nav destination.
---
```
1. Create `ui/<feature>/<Feature>Fragment.kt`, `<Feature>ViewModel.kt`, `<Feature>UiState.kt`, `res/layout/fragment_<feature>.xml`.
2. `UiState` = immutable data class with `isLoading`, `error: UiText?`, content fields. ViewModel exposes `StateFlow<UiState>` + one-shot `SharedFlow<Event>`.
3. Fragment: ViewBinding (null in `onDestroyView`), collect with `viewLifecycleOwner.repeatOnLifecycle(STARTED)`. No logic in the Fragment beyond rendering/events.
4. Add destination + actions to `nav_graph.xml`; add strings to `strings.xml`.
5. Render **loading / empty / error / content** using shared `common/` views.
6. Tests: ViewModel unit test (Turbine) + one Espresso test using `FakeLlmEngine`/fake repository.
7. Screenshot light + dark → `docs/screens/<feature>-{light,dark}.png`.

## SKILL: room-entity-dao
```
---
name: room-entity-dao
description: Use when adding or changing Room entities, DAOs, FTS, or database version.
---
```
1. Entity in `data/local/entity/`, DAO in `data/local/dao/` (`Flow` for reads, `suspend` for writes), add to `CodeMateDatabase`.
2. Always include `userId` where data is per-user; index foreign keys; `onDelete = CASCADE` for child rows.
3. Version bump ⇒ write `Migration(n, n+1)` + `MigrationTestHelper` test; keep exported schema JSON in git.
4. FTS: use `@Fts4(contentEntity = …)`; add triggers via Room's content-entity sync; test `MATCH` queries with prefix (`term*`).
5. Repository wraps the DAO; map entities → domain models; never expose entities to the UI.
6. Tests with in-memory DB (`Room.inMemoryDatabaseBuilder`, `allowMainThreadQueries` only in tests).

## SKILL: llm-engine-integration
```
---
name: llm-engine-integration
description: Use when touching anything under ai/engine, ai/cloud, ai/local, or when a feature needs the model.
---
```
1. Features depend ONLY on `LlmEngine`/`EngineRouter`. Never import Gemini or LiteRT classes outside `ai/cloud` / `ai/local`.
2. `generate(request): Flow<LlmChunk>` — emit `Delta(text)`, then `Done(usage?)` or `Failure(error)`; honour coroutine cancellation (close connection / stop native inference).
3. Map every failure to a sealed `LlmError` (`NoNetwork, BadKey, RateLimited, Server, Timeout, ModelMissing, OutOfMemory, SafetyBlocked, Unknown`) with a user-friendly message resource.
4. Always add/extend `FakeLlmEngine` scenarios when you add a behaviour (Instant, Streaming, Error, Malformed).
5. History/context trimming lives in `PromptBuilder`, not in engines. Budgets come from `AiConfig`.
6. Never log prompts or responses in release; in debug log only lengths/timing.

## SKILL: gemini-cloud-client
```
---
name: gemini-cloud-client
description: Use when implementing or debugging the Gemini cloud engine (REST + SSE streaming via OkHttp).
---
```
1. Look up the **current** Gemini API REST reference (generateContent / streamGenerateContent, request shape, SSE format) and the current recommended Flash-class model id BEFORE coding. Put the id in `AiConfig`.
2. Build request: system instruction + role-tagged `contents` from history; set `temperature`, `maxOutputTokens` from `AiConfig`.
3. Stream with OkHttp (`alt=sse`): parse `data:` lines → text parts → `Delta`. Handle partial lines, empty chunks, `finishReason`, `promptFeedback` safety blocks.
4. Key from `BuildConfig.GEMINI_API_KEY`; send via header/query as the docs specify; never log the URL with the key.
5. Timeouts: connect 15 s, read 60 s. 429 ⇒ exponential backoff (max 3). Offline ⇒ `NoNetwork` quickly (ConnectivityManager check + exception mapping).
6. Test with `MockWebServer`: happy stream, split chunk across reads, 401, 429, 500, mid-stream disconnect, cancellation.

## SKILL: litert-offline-model
```
---
name: litert-offline-model
description: Use when implementing or debugging the on-device engine, model download/import, or memory handling.
---
```
1. Look up the **current** LiteRT-LM Android (Kotlin) docs: Maven artifact, engine/session API, model file format (`.litertlm`), supported models. Record versions in `DECISIONS.md`.
2. `LiteRtLocalEngine`: lazy `ensureLoaded()` on `Dispatchers.Default/IO`; mutex so only one load/inference at a time; expose `Loading → Ready` state to UI; `close()` on `onTrimMemory(TRIM_MEMORY_RUNNING_LOW+)` and when the model is deleted.
3. Prompt formatting must follow the model's chat template (Gemma vs Qwen differ) — keep templates in `ModelCatalog` entries, covered by unit tests.
4. `ModelManager`: free-space + RAM check before download; WorkManager worker with foreground notification; HTTP Range resume; verify size/SHA-256 when known; atomic rename `.part → .litertlm`.
5. Import path: file picker (SAF) → copy into `getExternalFilesDir("models")` with progress; also auto-detect side-loaded files there.
6. Offline defaults: shorter history, lower `maxTokens`, COMPACT prompts, "Stop" button always visible. Show a one-time "First answer may take longer while the model loads".
7. Verification needs a **real phone**; if unavailable, mark NOT VERIFIED and list the exact manual steps (model path, device RAM, expected result).

## SKILL: prompt-templates
```
---
name: prompt-templates
description: Use when creating or editing system prompts for Chat, Explain, Debug, Learn, or Quiz modes.
---
```
1. Keep all prompt text in `ai/prompt/PromptTemplates.kt`; two variants per mode: FULL (cloud) and COMPACT (offline, ≤ ~250 tokens system prompt).
2. Every prompt states: role, language, level, output template with exact section headings, constraints (programming-only, correct > clever, fenced code with language tag).
3. For structured outputs, tell the model the exact headings; renderer should still work if a heading is missing (graceful fallback to plain markdown).
4. Unit tests assert: required headings present, placeholders fully replaced, COMPACT is under budget, no leftover `{}`.
5. Learning Mode: ≤ 120 words/turn, exactly one question per turn, hint ladder before reveal.

## SKILL: quiz-json-pipeline
```
---
name: quiz-json-pipeline
description: Use for quiz generation, parsing/repair of model JSON, and quiz persistence.
---
```
1. Generation: cloud ⇒ one request for N questions; offline ⇒ N sequential single-question requests (show "Question 3/10…").
2. `QuizJsonParser.parse(raw)`: strip markdown fences/prose → find first balanced `{…}` or `[…]` → lenient decode (kotlinx-serialization `isLenient`, `ignoreUnknownKeys`) → validate (exactly 4 non-blank options, `correctIndex` 0..3, non-blank question/explanation) → dedupe questions.
3. On failure: retry ≤ 2 with "Return ONLY valid JSON matching the schema" + the previous error; then show a friendly "Couldn't generate — try again / switch engine".
4. Every new real-world failure sample ⇒ add to `test/resources/quiz_malformed/` corpus + test. Corpus ≥ 15 cases.
5. Persist attempt + answers in a single transaction; update `TopicProgress` via `ProgressCalculator` (`0.7*old + 0.3*score`).

## SKILL: verify-and-fix
```
---
name: verify-and-fix
description: Use after every code slice and whenever a build/test/lint/app run fails.
---
```
1. Run `scripts/verify fast` (or `full`). Read the **first** error only.
2. Follow `LOOP-Instructions.md` §C (reproduce → hypothesis → minimal fix → regression test → re-verify). Max 5 cycles.
3. Consult the triage table in `Automated-Verification-Loops.md` §4.
4. Report with VERIFIED / NOT VERIFIED / ASSUMED tags and the exact command + result line.

## SKILL: android-ui-verification
```
---
name: android-ui-verification
description: Use to visually and functionally verify a screen on the emulator/device.
---
```
1. `adb devices` must list a device; otherwise report NOT VERIFIED and ask the human to start the AVD.
2. Install + launch: `adb install -r app/build/outputs/apk/debug/app-debug.apk`; `adb shell monkey -p com.mit.tushar_kaldate -c android.intent.category.LAUNCHER 1`.
3. Navigate with `adb shell input tap/swipe/text` or deep links; dump hierarchy with `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml` to find tap coordinates/ids.
4. Screenshot: `adb exec-out screencap -p > docs/screens/<name>.png` — then **open and look at the image**: check clipped text, overlaps, empty states, contrast, dark mode (`adb shell cmd uimode night yes|no`).
5. Check crashes: `adb logcat -d -s AndroidRuntime:E` and `adb logcat -d | grep -i "ANR in"`.
6. Large font test: `adb shell settings put system font_scale 1.5` (reset to `1.0` after).

## SKILL: phase-handoff
```
---
name: phase-handoff
description: Use at the end of every phase or when the conversation gets long.
---
```
1. Update `docs/PROJECT_STATE.md` using the template in `LOOP-Instructions.md` §G.
2. Append decisions to `docs/DECISIONS.md`; resolve or carry over `docs/BLOCKERS.md`.
3. `git add -A && git commit -m "docs: handoff phase N"`; confirm `git status` is clean and no secrets/models are staged.
4. Print: current phase status, what's verified/not verified, and the exact "Start Phase N+1" message for the human.

## SKILL: demo-polish
```
---
name: demo-polish
description: Use in Phases 10–11 to make the app demo-ready.
---
```
1. Seed a **demo account + sample data** (debug-only "Load demo data" in Settings): 3 conversations, 2 quiz attempts, progress on 3 topics.
2. Polish: app icon + splash, smooth streaming (no flicker), copy-code toast, empty-state illustrations, consistent spacing.
3. Prepare `docs/DEMO_SCRIPT.md` (2-min + 5-min flows) and make sure every step has a fallback (cloud down → offline; offline slow → pre-recorded video).
4. Build a release/debug APK and put it in `docs/releases/` (git-ignored) for backup phones.
